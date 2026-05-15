package com.king.learn;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ValueController {
    @Value("${com.king.name}")
    private String name;

    @Value("${com.king.age}")
    private Integer age;

    @RequestMapping("/me")
    public String getInfo(){
        return "name: " + name +
                "age: " + age;
    }
}
