### 最佳实践
我们要解决的问题是：
1. 从项目中去除nexus私服的地址、账号和密码配置？
2. 多模块项目的配置？
3. 如何统一管理第三方依赖（类似maven的dependencyManager功能）？
4. 如何发布构件到nexus私服？
5. 如何发布构件时，把源码打包一起发布？
6. 如何按照版本号（release或snapshot）发布到不同的nexus私服仓库中？
7. 如何让springboot项目打包为一个可运行的jar包？

解决问题：
#### 1. 从项目中去除nexus私服的地址、账号和密码配置？
可以在$GRADLE_USER_HOME(一般为~/.gradle/)目录下创建一个gradle.properties文件保存nexus的相关配置（如仓库地址、用户、密码等）
```java
HhhtMavenCentralRepository=
HhhtReleases=
HhhtSnapShots=
HhhtPublic=
HhhtNexusUserName=
HhhtNexusPassword=
```
通过在build.gradle脚本中引用变量来使用仓库地址、用户、密码
```groovy
repositories {
    mavenLocal()
    maven {
        url HhhtPublic
        credentials {
            username HhhtNexusUserName
            password HhhtNexusPassword
        }
    }
}
```
#### 2. 多模块项目的配置？
修改setting.gradle文件，配置rootProject以及其下包含的子项目
```java
rootProject.name = 'demo'
include 'common'
include 'repository'
include 'web'
```
修改rootProject的build.gradle脚本，通过allprojects定义所有模块（包含rootProject）的公共配置
```java
allprojects {
    apply plugin: 'java'
    sourceCompatibility = 1.8
    targetCompatibility = 1.8
    // 统一构件名称和版本号
    group = 'com.luhc.gradle'
    version = '1.0'
}
```
修改rootProject的build.gradle脚本，通过subprojects定义子模块，公共的配置
```groovy
subprojects {
    repositories {
        mavenLocal()
        maven {
            url HhhtPublic
            credentials {
                username HhhtNexusUserName
                password HhhtNexusPassword
            }
        }
    }
}
```
修改子项目的build.gradle文件，可以使用以下方式引用其上层依赖
```groovy
dependencies {
    compile project(':common')
}
```
#### 如何统一管理第三方依赖（类似maven的dependencyManager功能）？
项目根目录下创建一个library.gradle文件，定义第三方依赖
```groovy
ext {
    junitVersion = "4.12"
    apolloVersion = "0.11.0-SNAPSHOT"
    springBootVersion = '2.0.6.RELEASE'
    
    commonDependencies = [
            springBootGradlePlugin      : "org.springframework.boot:spring-boot-gradle-plugin:${springBootVersion}",
            apollo                      : "com.ctrip.framework.apollo:apollo-client:$apolloVersion",
            junit                       : "junit:junit:$junitVersion"
    ]
}
```
在rootProject的build.gralde文件中，以插件的方式，引用定义第三方依赖的文件。可以在buildscript{}代码块中引用
```groovy
// 读取统一依赖变量
apply from: './library.gradle'
```
在子项目的build.gradle文件中，引用第三方依赖的定义
```groovy
def globalConf = rootProject.ext

dependencies {
    // 统一依赖定义
    Map<String, String> commonDependencies = globalConf.commonDependencies
    
    implementation(commonDependencies.apollo)
}
```

#### 4. 如何发布构件到nexus私服？
#### 5. 如何发布构件时，把源码打包一起发布？
#### 6. 如何按照版本号（release或snapshot）发布到不同的nexus私服仓库中？
我们可以在rootProject的build.gradle文件中的allproject定义相关配置
```groovy
// 发布构建到maven私服的配置
apply plugin: 'maven-publish'
// 生成源码包
task sourcesJar(type: Jar) {
    from sourceSets.main.allJava
    classifier = 'sources'
}
// 生成javadoc
task javadocJar(type: Jar) {
    from javadoc
    classifier = 'javadoc'
}
publishing {
    // 定义要publish的构件,此处为hhhtPublish。构件为jar包形式
    publications {
        hhhtPublish(MavenPublication) {
            from components.java // jar包形式
            // from components.web  // war包形式，使用这个配置需要增加war插件
            artifact sourcesJar // 发布源码包
            artifact javadocJar // 发布javadoc
        }
    }
    repositories {
        maven {
            name = "hhhtNexus"
            // 如果项目版本以SNAPSHOT结尾，那么发布到nexu私服的snashot仓库去，其他情况发布到release仓库去
            url = project.version.endsWith('-SNAPSHOT') ? HhhtSnapShots : HhhtReleases
            credentials {
                username HhhtNexusUserName
                password HhhtNexusPassword
            }
        }
    }
}
```
#### 7. 如何让springboot项目打包为一个可运行的jar包？
在rootProject中引用springboot官方提供的插件（spring-boot-gradle-plugin）用于统一管理spring相关的依赖和为我们打包一个可运行的jar包
```groovy
buildscript {
    // 读取统一依赖变量
    apply from: './library.gradle'
    // 定义仓库
    repositories {
        mavenLocal()
        maven {
            url HhhtPublic
            credentials {
                username HhhtNexusUserName
                password HhhtNexusPassword
            }
        }
    }
    // 项目公共变量
    def globalConf = rootProject.ext
    
    // 为构建脚本添加外部依赖
    dependencies {
        Map<String, String> commonDependencies = globalConf.commonDependencies
        classpath(commonDependencies.springBootGradlePlugin)
    }
}

allprojects {
    // 省略其他配置内容...
    
    // spring统一依赖管理
    apply plugin: 'io.spring.dependency-management'
}

subprojects {
    // 省略其他配置内容...

    // 应用springboot插件, 创建一个可执行jar包
    apply plugin: 'org.springframework.boot'
}
```
由于使用了springboot插件，该插件将为我们自动生成一个bootJar的打包任务（打包一个可运行的jar包）。如果我们是多模块的项目，个别模块只需要打包为一个普通的jar包，并不需要是一个可运行的jar包。我们进行如下配置，启用jar任务，停用bootJar任务。否则在构建工程时会抛出找不到Main Class的异常
```groovy
// 只构建为一个普通的jar包
jar {
    enabled = true
}
bootJar {
    enabled = false
}
```