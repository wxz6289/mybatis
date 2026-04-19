package com.king.learn;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class XmlBeanDemo {
    public static void main(String[] args) {
        try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("beans.xml")) {
            TrafficLight trafficLight = context.getBean("trafficLight", TrafficLight.class);
            Light light = trafficLight.getLight();
            light.getBrand();
            light.getType();
            trafficLight.setColor("green");
            trafficLight.lightenNow();
        }
    }
}