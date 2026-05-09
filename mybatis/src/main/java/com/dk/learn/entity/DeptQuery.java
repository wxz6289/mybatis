package com.dk.learn.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 部门查询参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeptQuery {
    
    /**
     * 部门名称（支持模糊查询）
     */
    private String name;
    
    /**
     * 创建时间开始
     */
    private LocalDateTime createdTimeStart;
    
    /**
     * 创建时间结束
     */
    private LocalDateTime createdTimeEnd;
    
    /**
     * 当前页码
     */
    private Integer pageNum = 1;
    
    /**
     * 每页大小
     */
    private Integer pageSize = 10;
}
