package com.dk.learn.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件上传配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "file.upload")
public class FileUploadProperties {
    
    /**
     * 上传路径
     */
    private String path = "./uploads";
    
    /**
     * 最大文件大小（默认10MB）
     */
    private long maxSize = 10 * 1024 * 1024;
    
    /**
     * 允许的文件类型
     */
    private String[] allowedTypes = {
        "image/jpeg", "image/png", "image/gif", "image/webp",
        "application/pdf",
        "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "text/plain"
    };
    
    /**
     * 允许的文件扩展名
     */
    private String[] allowedExtensions = {
        "jpg", "jpeg", "png", "gif", "webp",
        "pdf",
        "doc", "docx",
        "xls", "xlsx",
        "txt"
    };
    
    /**
     * 存储类型：local-本地存储，oss-阿里云OSS
     */
    private String storageType = "local";
}
