package com.dk.learn.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Slf4j
@Component
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssConfig {
    // @Value("${aliyun.oss.endpoint}")
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;
    private String domain;

    @PostConstruct
    public void validate() {
        if (accessKeyId == null || accessKeyId.startsWith("${")) {
            log.error("OSS_ACCESS_KEY_ID 环境变量未设置，当前值: {}", accessKeyId);
        }
        if (accessKeySecret == null || accessKeySecret.startsWith("${")) {
            log.error("OSS_ACCESS_KEY_SECRET 环境变量未设置，当前值: {}", accessKeySecret);
        }
    }
}
