package com.dk.learn.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文件信息实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileInfo {
    
    /**
     * 文件ID
     */
    private Long id;
    
    /**
     * 原始文件名
     */
    private String originalName;
    
    /**
     * 存储文件名（UUID）
     */
    private String storedName;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * 文件类型（MIME类型）
     */
    private String contentType;
    
    /**
     * 文件扩展名
     */
    private String extension;
    
    /**
     * 文件存储路径
     */
    private String filePath;
    
    /**
     * 文件访问URL
     */
    private String fileUrl;
    
    /**
     * 上传者ID（可选）
     */
    private Long uploadedBy;
    
    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;
    
    /**
     * 文件描述（可选）
     */
    private String description;
    
    /**
     * 删除标记：0-未删除，1-已删除
     */
    private Integer deleted = 0;
}
