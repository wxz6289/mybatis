package com.learn.aop;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class TestAop {

    public static void main(String[] args) {
        try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("aop.xml")) {
            PrimaryOption primaryOption = context.getBean("primaryOption", PrimaryOption.class);
            primaryOption.plus();
            primaryOption.minus();
            primaryOption.multiply();
            primaryOption.divide();
        }
    }
}
