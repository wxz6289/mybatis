-- 第三方账号绑定表
CREATE TABLE IF NOT EXISTS `user_third_party` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '绑定ID',
    `user_id` BIGINT NOT NULL COMMENT '本地用户ID',
    `platform` VARCHAR(20) NOT NULL COMMENT '第三方平台（github, wechat, qq, google, weibo）',
    `open_id` VARCHAR(100) NOT NULL COMMENT '第三方平台用户ID',
    `union_id` VARCHAR(100) DEFAULT NULL COMMENT '联合ID（某些平台支持）',
    `access_token` TEXT COMMENT '访问令牌',
    `refresh_token` TEXT COMMENT '刷新令牌',
    `expire_time` DATETIME DEFAULT NULL COMMENT '令牌过期时间',
    `extra_info` TEXT COMMENT '额外信息（JSON格式）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_platform_openid` (`platform`, `open_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_platform` (`platform`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='第三方账号绑定表';
