package com.king.learn;

public class Light {
    public String brand;
    public String type;

    public void setBrand(String brand) {
        this.brand = brand;
    }
    public void setType(String type) {
        this.type = type;
    }
    public String getBrand() {
      System.out.println("Brand: " + brand);
        return brand;
    }
    public String getType() {
        System.out.println("Type: " + type);
        return type;
    }
    public void lighten() {
        System.out.println("The light is on.");
    }
    public void turnOff() {
        System.out.println("The light is off.");
    }
}