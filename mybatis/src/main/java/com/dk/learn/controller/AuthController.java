package com.dk.learn.controller;

import com.dk.learn.common.result.Result;
import com.dk.learn.entity.JwtResponse;
import com.dk.learn.entity.LoginRequest;
import com.dk.learn.entity.RegisterRequest;
import com.dk.learn.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 用户认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    
    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<JwtResponse> register(@Valid @RequestBody RegisterRequest request) {
        try {
            JwtResponse response = authService.register(request);
            return Result.ok(response);
        } catch (Exception e) {
            log.error("注册失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<JwtResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            JwtResponse response = authService.login(request);
            return Result.ok(response);
        } catch (Exception e) {
            log.error("登录失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public Result<?> getCurrentUser(@RequestHeader("Authorization") String authorization) {
        try {
            String token = extractToken(authorization);
            if (token == null) {
                return Result.error("未提供认证令牌");
            }
            
            var user = authService.getUserByToken(token);
            // 不返回密码等敏感信息
            user.setPassword(null);
            return Result.ok(user);
        } catch (Exception e) {
            log.error("获取用户信息失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authorization) {
        try {
            String token = extractToken(authorization);
            if (token != null) {
                var user = authService.getUserByToken(token);
                authService.logout(user.getId());
            }
            return Result.ok(null);
        } catch (Exception e) {
            log.error("退出登录失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 从Authorization头中提取Token
     */
    private String extractToken(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }
}