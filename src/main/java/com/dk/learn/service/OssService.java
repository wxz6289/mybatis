package com.dk.learn.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.dk.learn.common.result.FileUploadResult;
import com.dk.learn.common.util.FileUtils;
import com.dk.learn.config.OssConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 阿里云OSS文件服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OssService {
    
    private final OssConfig ossConfig;
    
    /**
     * 上传文件到OSS
     * @param file 上传的文件
     * @return 文件上传结果
     */
    public FileUploadResult uploadToOss(MultipartFile file) {
        OSS ossClient = null;
        try {
            // 创建OSS客户端
            ossClient = new OSSClientBuilder().build(
                ossConfig.getEndpoint(), 
                ossConfig.getAccessKeyId(), 
                ossConfig.getAccessKeySecret()
            );
            
            // 生成对象键（文件路径）
            String objectKey = generateObjectKey(file.getOriginalFilename());
            
            // 设置元数据
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());
            metadata.setHeader("Content-Disposition", "inline; filename=\"" + file.getOriginalFilename() + "\"");
            
            // 上传文件
            ossClient.putObject(ossConfig.getBucketName(), objectKey, file.getInputStream(), metadata);
            
            // 构建文件URL
            String fileUrl = buildFileUrl(objectKey);
            
            // 构建返回结果
            FileUploadResult result = new FileUploadResult();
            result.setOriginalName(file.getOriginalFilename());
            result.setStoredName(objectKey);
            result.setSize(file.getSize());
            result.setContentType(file.getContentType());
            result.setExtension(FileUtils.getFileExtension(file.getOriginalFilename()));
            result.setUrl(fileUrl);
            
            log.info("文件上传到OSS成功: {}, URL: {}", file.getOriginalFilename(), fileUrl);
            return result;
            
        } catch (IOException e) {
            log.error("文件上传到OSS失败: {}", file.getOriginalFilename(), e);
            throw new FileOperationException("文件上传到OSS失败: " + e.getMessage(), e);
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
    
    /**
     * 从OSS删除文件
     * @param objectKey 对象键
     */
    public void deleteFromOss(String objectKey) {
        OSS ossClient = null;
        try {
            ossClient = new OSSClientBuilder().build(
                ossConfig.getEndpoint(), 
                ossConfig.getAccessKeyId(), 
                ossConfig.getAccessKeySecret()
            );
            
            ossClient.deleteObject(ossConfig.getBucketName(), objectKey);
            log.info("文件从OSS删除成功: {}", objectKey);
            
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
    
    private String generateObjectKey(String originalFilename) {
        String extension = FileUtils.getFileExtension(originalFilename);
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uuid = UUID.randomUUID().toString().replace("-", "");
        
        return extension.isEmpty() ? 
            datePath + "/" + uuid : 
            datePath + "/" + uuid + "." + extension;
    }
    
    /**
     * 构建文件访问URL
     * @param objectKey 对象键
     * @return 文件URL
     */
    private String buildFileUrl(String objectKey) {
        if (ossConfig.getDomain() != null && !ossConfig.getDomain().isEmpty()) {
            return ossConfig.getDomain() + "/" + objectKey;
        } else {
            return "https://" + ossConfig.getBucketName() + "." + 
                   ossConfig.getEndpoint() + "/" + objectKey;
        }
    }
    
}
