package com.easycodingnow.dynamic.code;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
public class IndexController {


    @Resource
    private DynamicEngine dynamicEngine;

    private BaseSay baseSay;

    @PostMapping("/loadClass")
    public String loadClass(String className,String code) throws IllegalAccessException, InstantiationException {
        Class<BaseSay> baseSayClass = (Class<BaseSay>) dynamicEngine.compile(className, code);
        baseSay = baseSayClass.newInstance();
        return "ok";
    }

    @GetMapping("/say")
    public String say() {
        if (baseSay == null) {
            return "baseSay was null!";
        }

        return baseSay.say();
    }

}
