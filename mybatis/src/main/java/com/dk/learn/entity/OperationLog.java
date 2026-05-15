package com.dk.learn.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 操作日志实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperationLog {
    private Long id;
    private Long userId;              // 操作用户ID
    private String username;          // 操作用户名
    private String operation;         // 操作描述
    private String method;            // 请求方法
    private String params;            // 请求参数
    private String ip;                // IP地址
    private String userAgent;         // 用户代理
    private Integer status;           // 操作状态：0-失败，1-成功
    private String errorMsg;          // 错误信息
    private LocalDateTime operationTime;  // 操作时间
}