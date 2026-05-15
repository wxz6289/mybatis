package com.dk.learn.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 第三方账号绑定实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserThirdParty {
    private Long id;
    private Long userId;                    // 本地用户ID
    private String platform;                // 第三方平台（github, wechat, qq等）
    private String openId;                  // 第三方平台用户ID
    private String unionId;                 // 联合ID
    private String accessToken;             // 访问令牌
    private String refreshToken;            // 刷新令牌
    private LocalDateTime expireTime;       // 令牌过期时间
    private String extraInfo;               // 额外信息（JSON格式）
    private LocalDateTime createdAt;        // 创建时间
    private LocalDateTime updatedAt;        // 更新时间
}
