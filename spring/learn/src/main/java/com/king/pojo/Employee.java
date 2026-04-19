package com.king.pojo;

public class Employee {
    private int id;
    private String name;
    private int gender;
    private int job;
    private int age;

    public Employee() {}

    public Employee(int id, String name, int gender, int job, int age) {
        this.id = id;
        this.name = name;
        this.gender = gender;
        this.job = job;
        this.age = age;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getGender() {
        return gender;
    }

    public void setGender(int gender) {
        this.gender = gender;
    }

    public int getJob() {
        return job;
    }

    public void setJob(int job) {
        this.job = job;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    @Override
    public String toString() {
        return "Employee{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", gender=" + gender +
                ", job=" + job +
                ", age=" + age +
                '}';
    }
}