package com.dk.learn.service;

import com.dk.learn.common.util.JwtUtil;
import com.dk.learn.common.util.PasswordUtil;
import com.dk.learn.entity.*;
import com.dk.learn.mapper.UserAuthMapper;
import com.dk.learn.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 用户认证服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserAuthMapper userAuthMapper;
    private final UserMapper userMapper;
    private final PasswordUtil passwordUtil;
    private final JwtUtil jwtUtil;
    
    /**
     * 用户注册
     */
    @Transactional
    public JwtResponse register(RegisterRequest request) {
        // 检查用户名是否已存在
        UserAuth existingUser = userAuthMapper.findByUsername(request.getUsername());
        if (existingUser != null) {
            throw new RuntimeException("用户名已存在");
        }
        
        // 创建用户认证信息
        UserAuth userAuth = new UserAuth();
        userAuth.setUsername(request.getUsername());
        userAuth.setPassword(passwordUtil.encode(request.getPassword()));
        userAuth.setEmail(request.getEmail());
        userAuth.setPhone(request.getPhone());
        userAuth.setStatus(1); // 正常状态
        userAuth.setCreatedTime(LocalDateTime.now());
        userAuth.setUpdatedTime(LocalDateTime.now());
        
        // 插入用户认证信息
        userAuthMapper.insert(userAuth);
        
        // 创建普通用户信息
        User user = new User();
        user.setName(request.getName() != null ? request.getName() : request.getUsername());
        user.setAge(request.getAge());
        user.setDeptId(request.getDeptId());
        userMapper.addUser(user);
        
        // 生成JWT Token
        String token = jwtUtil.generateToken(userAuth.getId(), userAuth.getUsername());
        
        log.info("用户注册成功: {}", request.getUsername());
        return new JwtResponse(token, userAuth.getId(), userAuth.getUsername());
    }
    
    /**
     * 用户登录
     */
    public JwtResponse login(LoginRequest request) {
        // 查询用户认证信息
        UserAuth userAuth = userAuthMapper.findByUsername(request.getUsername());
        if (userAuth == null) {
            throw new RuntimeException("用户名或密码错误");
        }
        
        // 验证密码
        if (!passwordUtil.matches(request.getPassword(), userAuth.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }
        
        // 检查用户状态
        if (userAuth.getStatus() == 0) {
            throw new RuntimeException("用户已被禁用");
        }
        if (userAuth.getStatus() == 2) {
            throw new RuntimeException("用户已被锁定");
        }
        
        // 更新最后登录时间
        userAuth.setLastLoginTime(LocalDateTime.now());
        userAuth.setUpdatedTime(LocalDateTime.now());
        userAuthMapper.update(userAuth);
        
        // 生成JWT Token
        String token = jwtUtil.generateToken(userAuth.getId(), userAuth.getUsername());
        
        log.info("用户登录成功: {}", request.getUsername());
        return new JwtResponse(token, userAuth.getId(), userAuth.getUsername());
    }
    
    /**
     * 根据Token获取用户信息
     */
    public UserAuth getUserByToken(String token) {
        if (!jwtUtil.validateToken(token)) {
            throw new RuntimeException("无效的Token");
        }
        
        Long userId = jwtUtil.getUserIdFromToken(token);
        UserAuth userAuth = userAuthMapper.findById(userId);
        if (userAuth == null) {
            throw new RuntimeException("用户不存在");
        }
        
        return userAuth;
    }
    
    /**
     * 退出登录（客户端删除Token即可，服务端可以记录日志）
     */
    public void logout(Long userId) {
        log.info("用户退出登录: userId={}", userId);
    }
}