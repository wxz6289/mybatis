package com.dk.learn.controller;

import com.dk.learn.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试控制器 - 用于验证JWT认证
 */
@Slf4j
@RestController
@RequestMapping("/api/test")
public class TestController {
    
    /**
     * 需要认证的测试接口
     */
    @GetMapping("/protected")
    public Result<String> protectedEndpoint() {
        return Result.ok("这是一个受保护的接口，您已成功认证！");
    }
    
    /**
     * 公开访问的测试接口
     */
    @GetMapping("/public")
    public Result<String> publicEndpoint() {
        return Result.ok("这是一个公开的接口，任何人都可以访问！");
    }
}