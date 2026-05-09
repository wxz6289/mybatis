package com.dk.learn.entity;

import lombok.*;

import java.time.LocalDateTime;

//@Getter
//@Setter
//@ToString
//@EqualsAndHashCode
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String name;
    private int age;
    private int deptId;
    /** JSON 日期时间由全局 Jackson 配置格式化为 yyyy-MM-dd HH:mm:ss */
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public User(String name, int age, int deptId) {
        this.name = name;
        this.age = age;
        this.deptId = deptId;
    }

    public User(UserVO ther) {
        this.id = ther.getId();
        this.name = ther.getName();
        this.age = ther.getAge();
        this.deptId = ther.getDeptId();
        this.createdTime = ther.getCreatedTime();
        this.updatedTime = ther.getUpdatedTime();
    }
//    public User() {
//    }
//
//    public User(String name, int age) {
//        this.name = name;
//        this.age = age;
//    }
//
//    public String getName() {
//        return name;
//    }
//
//    public void setName(String name) {
//        this.name = name;
//    }
//
//    public int getAge() {
//        return age;
//    }
//
//    public void setAge(int age) {
//        this.age = age;
//    }
//
//    @Override
//    public String toString() {
//        return "User{" +
//                "name='" + name + '\'' +
//                ", age=" + age +
//                '}';
//    }
}
