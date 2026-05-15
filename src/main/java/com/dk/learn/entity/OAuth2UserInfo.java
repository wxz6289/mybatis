package com.dk.learn.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 第三方OAuth2用户信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2UserInfo {
    private String openId;          // 第三方平台用户唯一标识
    private String unionId;         // 联合ID（某些平台支持）
    private String nickname;        // 昵称
    private String avatar;          // 头像URL
    private String email;           // 邮箱
    private String gender;          // 性别
    private String location;        // 位置
    private String accessToken;     // 访问令牌
    private String refreshToken;    // 刷新令牌
    private Long expiresIn;         // 过期时间（秒）
    private OAuth2Platform platform; // 平台类型
}
