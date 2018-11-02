package com.luhc.controller;

import com.luhc.common.Cat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * created by luhuancheng on 2018/11/2
 */
@RestController
public class HelloController {

    @GetMapping("/hello")
    public Cat hello() {
        return new Cat("hello kity");
    }

}
