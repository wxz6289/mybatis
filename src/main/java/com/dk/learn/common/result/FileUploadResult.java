package com.dk.learn.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件上传结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResult {
    
    /**
     * 文件原始名称
     */
    private String originalName;
    
    /**
     * 文件存储名称
     */
    private String storedName;
    
    /**
     * 文件大小
     */
    private long size;
    
    /**
     * 文件类型
     */
    private String contentType;
    
    /**
     * 文件访问URL
     */
    private String url;
    
    /**
     * 文件扩展名
     */
    private String extension;
}
