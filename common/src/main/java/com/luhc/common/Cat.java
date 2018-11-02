package com.luhc.common;

import lombok.Data;

/**
 * created by luhuancheng on 2018/11/2
 */
@Data
public class Cat {

    private String name;

    public Cat(String name) {
        this.name = name;
    }
}
