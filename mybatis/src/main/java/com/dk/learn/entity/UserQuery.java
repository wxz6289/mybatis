package com.dk.learn.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户查询条件封装类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserQuery {
    private String name;      // 姓名（模糊查询）
    private Integer startAge; // 起始年龄
    private Integer endAge;   // 结束年龄
    private Integer offset;   // 分页偏移量
    private Integer size;     // 每页大小

    public UserQuery(String name) {
        this.name = name;
    }
}
