package com.dk.learn.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * HTTPS配置类
 * 通过application.yaml配置HTTPS，无需额外的Java配置
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "server.ssl.enabled", havingValue = "true")
public class HttpsConfig {
    
    @Value("${server.port:8443}")
    private int httpsPort;
    
    @Value("${server.http-port:8080}")
    private int httpPort;
    
    @PostConstruct
    public void init() {
        log.info("========================================");
        log.info("HTTPS配置已启用");
        log.info("========================================");
        log.info("HTTPS端口: {}", httpsPort);
        log.info("HTTP端口: {} (需手动配置重定向)", httpPort);
        log.info("访问地址: https://localhost:{}", httpsPort);
        log.info("========================================");
        log.info("");
        log.info("注意: Spring Boot 4.x 需要通过以下方式配置HTTP重定向:");
        log.info("1. 在 application.yaml 中配置 server.ssl.enabled=true");
        log.info("2. 使用反向代理（如Nginx）实现HTTP到HTTPS的重定向");
        log.info("3. 或者使用Servlet容器级别的配置");
        log.info("");
    }
}
