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
    
    /**
     * 上传文件并保存记录
     * @param file 上传的文件
     * @return 文件上传结果
     */
    @Transactional
    public FileUploadResult uploadFile(MultipartFile file) {
        // 验证文件
        validateFile(file);
        
        try {
            // 生成存储路径
            String storedName = generateStoredName(file.getOriginalFilename());
            String extension = getFileExtension(file.getOriginalFilename());
            
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
            
            // 保存文件信息到数据库
            FileInfo fileInfo = new FileInfo();
            fileInfo.setOriginalName(file.getOriginalFilename());
            fileInfo.setStoredName(storedName);
            fileInfo.setFileSize(file.getSize());
            fileInfo.setContentType(file.getContentType());
            fileInfo.setExtension(extension);
            fileInfo.setFilePath(filePath.toString());
            fileInfo.setFileUrl(fileUrl);
            fileInfo.setUploadTime(LocalDateTime.now());
            
            fileInfoMapper.save(fileInfo);
            
            // 构建返回结果
            FileUploadResult result = new FileUploadResult();
            result.setOriginalName(file.getOriginalFilename());
            result.setStoredName(storedName);
            result.setSize(file.getSize());
            result.setContentType(file.getContentType());
            result.setExtension(extension);
            result.setUrl(fileUrl);
            
            log.info("文件上传成功: {}, ID: {}", file.getOriginalFilename(), fileInfo.getId());
            return result;
            
        } catch (IOException e) {
            log.error("文件上传失败: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
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
        long total = fileInfoMapper.count();
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
            throw new RuntimeException("文件不存在: " + storedName);
        }
        
        try {
            Path filePath = Paths.get(fileInfo.getFilePath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("文件不存在: " + storedName);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("文件路径错误: " + storedName, e);
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
            throw new RuntimeException("文件不存在");
        }
        
        // 逻辑删除
        fileInfoMapper.deleteById(id);
        log.info("文件已删除: ID={}", id);
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
        String extension = getFileExtension(filename).toLowerCase();
        return Arrays.stream(uploadProperties.getAllowedExtensions())
                .anyMatch(ext -> ext.equalsIgnoreCase(extension));
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
    
    /**
     * 生成存储文件名
     */
    private String generateStoredName(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return extension.isEmpty() ? uuid : uuid + "." + extension;
    }
}

