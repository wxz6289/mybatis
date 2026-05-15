package com.dk.learn.service;

/**
 * OSS文件访问异常
 * 用于指示文件存储在OSS上，应该直接通过URL访问
 */
public class OssFileAccessException extends RuntimeException {
    
    private final String ossUrl;
    
    public OssFileAccessException(String message, String ossUrl) {
        super(message);
        this.ossUrl = ossUrl;
    }
    
    public String getOssUrl() {
        return ossUrl;
    }
}
