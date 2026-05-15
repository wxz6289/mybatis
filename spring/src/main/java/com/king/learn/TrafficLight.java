package com.king.learn;

public class TrafficLight {
    private Light light;
    private String color;

    public TrafficLight() {
    }

    public TrafficLight(Light light, String color) {
        this.light = light;
        this.color = color;
    }

    public Light getLight() {
        return light;
    }

    public String getColor() {
        return color;
    }

    public void setLight(Light light) {
        this.light = light;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public void lightenNow() {
        light.lighten();
        System.out.println("The traffic light is " + color + ".");
    }
}