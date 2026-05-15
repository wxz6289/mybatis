package com.learn.aop;

public class PrimaryOptionImpl implements PrimaryOption {
    private  int a;
    private  int b;

    public void setA(int a) {
        this.a = a;
    }
    public void setB(int b) {
        this.b = b;
    }

    public int getA() {
        return a;
    }
    public int getB() {
        return b;
    }

    @Override
    public void plus() {
        System.out.println("执行加法:" + (a + b));
    }

    @Override
    public void minus() {
        System.out.println("执行减法:" + (a - b));
    }

    @Override
    public void multiply() {
        System.out.println("执行乘法:" + (a * b));
    }

    @Override
    public void divide() {
        System.out.println("执行除法:" + (a / b));
    }
}
