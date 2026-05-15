package com.dk.learn.service;

import com.dk.learn.common.page.PageQuery;
import com.dk.learn.common.page.PageResult;
import com.dk.learn.common.result.FileUploadResult;
import com.dk.learn.config.FileUploadProperties;
import com.dk.learn.entity.FileInfo;
import com.dk.learn.mapper.FileInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.dk.learn.common.util.FileUtils;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 文件管理服务（包含上传和查询）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {
    
    private final FileUploadProperties uploadProperties;
    private final FileInfoMapper fileInfoMapper;
    private final OssService ossService;
    
    /**
     * 上传文件并保存记录
     * @param file 上传的文件
     * @return 文件上传结果
     */
    @Transactional
    public FileUploadResult uploadFile(MultipartFile file) {
        // 验证文件
        validateFile(file);
        
        FileUploadResult result;
        String storageType = uploadProperties.getStorageType();
        
        if ("oss".equalsIgnoreCase(storageType)) {
            // 使用阿里云OSS存储
            result = ossService.uploadToOss(file);
        } else {
            // 使用本地存储
            result = uploadToLocal(file);
        }
        
        // 保存文件信息到数据库
        saveFileInfoToDatabase(file, result);
        
        log.info("文件上传成功: {}, 存储类型: {}", file.getOriginalFilename(), storageType);
        return result;
    }
    
    /**
     * 上传文件到本地存储
     * @param file 上传的文件
     * @return 文件上传结果
     */
    private FileUploadResult uploadToLocal(MultipartFile file) {
        try {
            // 生成存储路径
            String storedName = generateStoredName(file.getOriginalFilename());
            String extension = FileUtils.getFileExtension(file.getOriginalFilename());
            
            // 创建目录（按日期分类）
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            
            // 获取上传根目录的绝对路径
            Path basePath = Paths.get(uploadProperties.getPath()).toAbsolutePath().normalize();
            Path uploadDir = basePath.resolve(datePath).normalize();
            
            // 安全检查：防止路径遍历攻击
            if (!uploadDir.startsWith(basePath)) {
                throw new SecurityException("非法的文件路径: " + datePath);
            }
            
            // 确保目录存在
            Files.createDirectories(uploadDir);
            
            // 保存文件
            Path filePath = uploadDir.resolve(storedName);
            
            // 再次安全检查
            if (!filePath.startsWith(basePath)) {
                throw new SecurityException("非法的文件路径");
            }
            
            file.transferTo(filePath.toFile());
            
            // 构建文件URL（使用 /api/file/view/ 路径）
            String fileUrl = "/api/file/view/" + storedName;
            
            // 构建返回结果
            FileUploadResult result = new FileUploadResult();
            result.setOriginalName(file.getOriginalFilename());
            result.setStoredName(storedName);
            result.setSize(file.getSize());
            result.setContentType(file.getContentType());
            result.setExtension(extension);
            result.setUrl(fileUrl);
            
            return result;
            
        } catch (IOException e) {
            log.error("文件上传失败: {}", file.getOriginalFilename(), e);
            throw new FileOperationException("文件上传失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 保存文件信息到数据库
     * @param file 上传的文件
     * @param result 文件上传结果
     */
    private void saveFileInfoToDatabase(MultipartFile file, FileUploadResult result) {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setOriginalName(file.getOriginalFilename());
        fileInfo.setStoredName(result.getStoredName());
        fileInfo.setFileSize(file.getSize());
        fileInfo.setContentType(file.getContentType());
        fileInfo.setExtension(result.getExtension());
        
        // 根据存储类型设置文件路径
        String storageType = uploadProperties.getStorageType();
        if ("oss".equalsIgnoreCase(storageType)) {
            fileInfo.setFilePath(result.getStoredName()); // OSS中存储的是object key
        } else {
            // 本地存储时，构建完整路径
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            Path basePath = Paths.get(uploadProperties.getPath()).toAbsolutePath().normalize();
            Path filePath = basePath.resolve(datePath).resolve(result.getStoredName());
            fileInfo.setFilePath(filePath.toString());
        }
        
        fileInfo.setFileUrl(result.getUrl());
        fileInfo.setUploadTime(LocalDateTime.now());
        
        fileInfoMapper.save(fileInfo);
    }
    
    /**
     * 批量上传文件
     * @param files 上传的文件数组
     * @return 文件上传结果列表
     */
    public FileUploadResult[] uploadFiles(MultipartFile[] files) {
        return Arrays.stream(files)
                .map(this::uploadFile)
                .toArray(FileUploadResult[]::new);
    }
    
    /**
     * 根据ID获取文件信息
     * @param id 文件ID
     * @return 文件信息
     */
    public FileInfo getFileInfo(Long id) {
        return fileInfoMapper.findById(id);
    }
    
    /**
     * 分页查询文件列表
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    public PageResult<FileInfo> listFiles(PageQuery pageQuery) {
        List<FileInfo> files = fileInfoMapper.list(pageQuery.offset(), pageQuery.size());
        long total = fileInfoMapper.count();
        return PageResult.of(files, total, pageQuery);
    }
    
    /**
     * 根据条件查询文件
     * @param originalName 原始文件名
     * @param contentType 文件类型
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    public PageResult<FileInfo> searchFiles(String originalName, String contentType, PageQuery pageQuery) {
        List<FileInfo> files = fileInfoMapper.listByCondition(originalName, contentType,
                pageQuery.offset(), pageQuery.size());
        long total = fileInfoMapper.countByCondition(originalName, contentType);
        return PageResult.of(files, total, pageQuery);
    }
    
    /**
     * 加载文件资源（用于下载/查看）
     * @param storedName 存储文件名
     * @return 文件资源
     */
    public Resource loadFileAsResource(String storedName) {
        FileInfo fileInfo = fileInfoMapper.findByStoredName(storedName);
        if (fileInfo == null) {
            throw new FileOperationException("文件不存在: " + storedName);
        }

        try {
            Path filePath = Paths.get(fileInfo.getFilePath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return resource;
            } else {
                throw new FileOperationException("文件不存在: " + storedName);
            }
        } catch (MalformedURLException e) {
            throw new FileOperationException("文件路径错误: " + storedName, e);
        }
    }
    
    /**
     * 删除文件（逻辑删除）
     * @param id 文件ID
     */
    @Transactional
    public void deleteFile(Long id) {
        FileInfo fileInfo = fileInfoMapper.findById(id);
        if (fileInfo == null) {
            throw new FileOperationException("文件不存在");
        }
        
        // 如果是OSS存储，删除OSS上的文件
        String storageType = uploadProperties.getStorageType();
        if ("oss".equalsIgnoreCase(storageType)) {
            try {
                ossService.deleteFromOss(fileInfo.getStoredName());
            } catch (Exception e) {
                log.error("删除OSS文件失败: {}", fileInfo.getStoredName(), e);
                // 即使OSS删除失败，也继续删除数据库记录
            }
        }
        
        // 逻辑删除
        fileInfoMapper.deleteById(id);
        log.info("文件已删除: ID={}, 存储类型={}", id, storageType);
    }
    
    /**
     * 批量删除文件
     * @param ids 文件ID列表
     */
    @Transactional
    public void batchDeleteFiles(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            fileInfoMapper.batchDelete(ids);
            log.info("批量删除文件: IDs={}", ids);
        }
    }
    
    /**
     * 验证文件
     * @param file 待验证的文件
     */
    private void validateFile(MultipartFile file) {
        // 检查文件是否为空
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        
        // 检查文件大小
        if (file.getSize() > uploadProperties.getMaxSize()) {
            throw new IllegalArgumentException(
                String.format("文件大小超过限制，最大允许 %d MB", 
                    uploadProperties.getMaxSize() / 1024 / 1024)
            );
        }
        
        // 检查文件类型
        String contentType = file.getContentType();
        if (contentType != null && !isAllowedType(contentType)) {
            throw new IllegalArgumentException("不支持的文件类型: " + contentType);
        }
        
        // 检查文件扩展名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && !isAllowedExtension(originalFilename)) {
            throw new IllegalArgumentException("不支持的文件扩展名");
        }
    }
    
    /**
     * 检查文件类型是否允许
     */
    private boolean isAllowedType(String contentType) {
        return Arrays.stream(uploadProperties.getAllowedTypes())
                .anyMatch(type -> type.equalsIgnoreCase(contentType));
    }
    
    /**
     * 检查文件扩展名是否允许
     */
    private boolean isAllowedExtension(String filename) {
        String extension = FileUtils.getFileExtension(filename).toLowerCase();
        return Arrays.stream(uploadProperties.getAllowedExtensions())
                .anyMatch(ext -> ext.equalsIgnoreCase(extension));
    }

    private String generateStoredName(String originalFilename) {
        String extension = FileUtils.getFileExtension(originalFilename);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return extension.isEmpty() ? uuid : uuid + "." + extension;
    }
}

