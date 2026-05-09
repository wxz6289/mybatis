package com.dk.learn.controller;

import com.dk.learn.common.page.PageQuery;
import com.dk.learn.common.page.PageResult;
import com.dk.learn.common.result.Result;
import com.dk.learn.entity.FileInfo;
import com.dk.learn.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileManageController {
    
    private final FileService fileService;
    
    /**
     * 上传单个文件
     */
    @PostMapping("/upload")
    public Result<?> uploadFile(@RequestParam("file") MultipartFile file) {
        log.info("接收到文件上传请求: {}", file.getOriginalFilename());
        return Result.ok(fileService.uploadFile(file));
    }
    
    /**
     * 批量上传文件
     */
    @PostMapping("/upload/batch")
    public Result<?> uploadFiles(@RequestParam("files") MultipartFile[] files) {
        log.info("接收到批量文件上传请求，文件数量: {}", files.length);
        return Result.ok(fileService.uploadFiles(files));
    }
    
    /**
     * 分页查询文件列表
     */
    @GetMapping("/list")
    public Result<PageResult<FileInfo>> listFiles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResult<FileInfo> result = fileService.listFiles(PageQuery.of(page, size));
        return Result.ok(result);
    }
    
    /**
     * 获取文件信息
     * @param id 文件ID
     */
    @GetMapping("/info/{id}")
    public Result<FileInfo> getFileInfo(@PathVariable Long id) {
        FileInfo fileInfo = fileService.getFileInfo(id);
        if (fileInfo == null) {
            return Result.error("文件不存在");
        }
        return Result.ok(fileInfo);
    }
    
    /**
     * 搜索文件
     */
    @GetMapping("/search")
    public Result<PageResult<FileInfo>> searchFiles(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String contentType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResult<FileInfo> result = fileService.searchFiles(name, contentType, PageQuery.of(page, size));
        return Result.ok(result);
    }
    
    /**
     * 删除文件
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteFile(@PathVariable Long id) {
        fileService.deleteFile(id);
        return Result.ok(null);
    }
    
    /**
     * 批量删除文件
     */
    @DeleteMapping("/batch")
    public Result<Void> batchDeleteFiles(@RequestParam List<Long> ids) {
        fileService.batchDeleteFiles(ids);
        return Result.ok(null);
    }
}
