package com.dk.learn.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OAuth2平台配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "oauth2")
public class OAuth2Config {
    
    private PlatformConfig github = new PlatformConfig();
    private PlatformConfig wechat = new PlatformConfig();
    private PlatformConfig qq = new PlatformConfig();
    private PlatformConfig google = new PlatformConfig();
    private PlatformConfig weibo = new PlatformConfig();
    
    @Data
    public static class PlatformConfig {
        private String clientId;
        private String clientSecret;
        private String redirectUri;
        private String authorizationUrl;
        private String tokenUrl;
        private String userInfoUrl;
        private String scope;
    }
}
