package com.dk.learn.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 第三方登录平台枚举
 */
@Getter
@AllArgsConstructor
public enum OAuth2Platform {
    GITHUB("github", "GitHub"),
    WECHAT("wechat", "微信"),
    QQ("qq", "QQ"),
    GOOGLE("google", "Google"),
    WEIBO("weibo", "微博");
    
    private final String code;
    private final String name;
    
    public static OAuth2Platform fromCode(String code) {
        for (OAuth2Platform platform : values()) {
            if (platform.getCode().equals(code)) {
                return platform;
            }
        }
        throw new IllegalArgumentException("Unsupported platform: " + code);
    }
}
