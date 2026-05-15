package com.dk.learn.entity;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String name;
    private Integer age;
    private Integer deptId;
    /** JSON 日期时间由全局 Jackson 配置格式化为 yyyy-MM-dd HH:mm:ss */
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public User(String name, int age, int deptId) {
        this.name = name;
        this.age = age;
        this.deptId = deptId;
    }

}
