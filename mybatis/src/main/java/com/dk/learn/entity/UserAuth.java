package com.dk.learn.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户认证实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAuth {
    private Long id;
    private String username;      // 用户名（登录用）
    private String password;      // 密码（加密存储）
    private String email;         // 邮箱
    private String phone;         // 手机号
    private Integer status;       // 状态：0-禁用，1-正常，2-锁定
    private LocalDateTime lastLoginTime;  // 最后登录时间
    private LocalDateTime createdTime;    // 创建时间
    private LocalDateTime updatedTime;    // 更新时间
    
    // 关联的普通用户信息
    private User userInfo;
}