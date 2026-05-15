package com.dk.learn.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 静态资源控制器
 * 处理favicon等静态资源请求，避免404错误
 */
@RestController
public class StaticResourceController {
    
    /**
     * 处理favicon.ico请求
     * 返回204 No Content，避免404错误日志
     */
    @GetMapping("/favicon.ico")
    public ResponseEntity<Void> favicon() {
        return ResponseEntity.noContent().build();
    }
}
