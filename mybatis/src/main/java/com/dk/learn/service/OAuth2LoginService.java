package com.dk.learn.service;

import com.dk.learn.common.util.JwtUtil;
import com.dk.learn.common.util.OAuth2Client;
import com.dk.learn.entity.*;
import com.dk.learn.mapper.UserAuthMapper;
import com.dk.learn.mapper.UserMapper;
import com.dk.learn.mapper.UserThirdPartyMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 第三方登录服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2LoginService {
    
    private final OAuth2Client oauth2Client;
    private final UserThirdPartyMapper thirdPartyMapper;
    private final UserAuthMapper userAuthMapper;
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 处理第三方登录回调
     */
    @Transactional
    public JwtResponse handleOAuth2Callback(OAuth2Platform platform, String code) {
        log.info("处理{}平台OAuth2回调，code: {}", platform.getName(), code);
        
        // 1. 获取AccessToken
        Map<String, Object> tokenResponse = oauth2Client.getAccessToken(platform, code);
        String accessToken = (String) tokenResponse.get("access_token");
        String refreshToken = (String) tokenResponse.get("refresh_token");
        Long expiresIn = tokenResponse.containsKey("expires_in") ? 
            ((Number) tokenResponse.get("expires_in")).longValue() : 7200L;
        
        // 2. 获取用户信息
        OAuth2UserInfo userInfo = oauth2Client.getUserInfo(platform, accessToken);
        userInfo.setAccessToken(accessToken);
        userInfo.setRefreshToken(refreshToken);
        userInfo.setExpiresIn(expiresIn);
        
        // 3. 查找是否已绑定
        UserThirdParty existingBinding = thirdPartyMapper.findByPlatformAndOpenId(
            platform.getCode(), userInfo.getOpenId());
        
        if (existingBinding != null) {
            // 已绑定，直接登录
            log.info("用户已绑定，userId: {}", existingBinding.getUserId());
            
            // 更新token
            updateToken(existingBinding, accessToken, refreshToken, expiresIn);
            
            // 生成JWT
            UserAuth userAuth = userAuthMapper.findById(existingBinding.getUserId());
            return new JwtResponse(
                jwtUtil.generateToken(userAuth.getId(), userAuth.getUsername()),
                userAuth.getId(),
                userAuth.getUsername()
            );
        } else {
            // 未绑定，创建新用户
            log.info("用户未绑定，创建新账号");
            return createNewUserAndBind(platform, userInfo, accessToken, refreshToken, expiresIn);
        }
    }
    
    /**
     * 绑定第三方账号到当前用户
     */
    @Transactional
    public void bindThirdPartyAccount(Long userId, OAuth2Platform platform, String code) {
        log.info("用户{}绑定{}账号", userId, platform.getName());
        
        // 检查是否已经绑定
        UserThirdParty existing = thirdPartyMapper.findByUserIdAndPlatform(userId, platform.getCode());
        if (existing != null) {
            throw new RuntimeException("已经绑定了该平台的账号");
        }
        
        // 获取AccessToken和用户信息
        Map<String, Object> tokenResponse = oauth2Client.getAccessToken(platform, code);
        String accessToken = (String) tokenResponse.get("access_token");
        String refreshToken = (String) tokenResponse.get("refresh_token");
        Long expiresIn = tokenResponse.containsKey("expires_in") ? 
            ((Number) tokenResponse.get("expires_in")).longValue() : 7200L;
        
        OAuth2UserInfo userInfo = oauth2Client.getUserInfo(platform, accessToken);
        
        // 检查该第三方账号是否已被其他用户绑定
        UserThirdParty otherBinding = thirdPartyMapper.findByPlatformAndOpenId(
            platform.getCode(), userInfo.getOpenId());
        if (otherBinding != null && !otherBinding.getUserId().equals(userId)) {
            throw new RuntimeException("该第三方账号已被其他用户绑定");
        }
        
        // 创建绑定
        UserThirdParty binding = new UserThirdParty();
        binding.setUserId(userId);
        binding.setPlatform(platform.getCode());
        binding.setOpenId(userInfo.getOpenId());
        binding.setUnionId(userInfo.getUnionId());
        binding.setAccessToken(accessToken);
        binding.setRefreshToken(refreshToken);
        binding.setExpireTime(LocalDateTime.now().plusSeconds(expiresIn));
        binding.setCreatedAt(LocalDateTime.now());
        binding.setUpdatedAt(LocalDateTime.now());
        
        try {
            binding.setExtraInfo(objectMapper.writeValueAsString(userInfo));
        } catch (Exception e) {
            log.error("序列化用户信息失败", e);
        }
        
        thirdPartyMapper.insert(binding);
        log.info("绑定成功");
    }
    
    /**
     * 解绑第三方账号
     */
    @Transactional
    public void unbindThirdPartyAccount(Long userId, OAuth2Platform platform) {
        log.info("用户{}解绑{}账号", userId, platform.getName());
        
        UserThirdParty binding = thirdPartyMapper.findByUserIdAndPlatform(userId, platform.getCode());
        if (binding == null) {
            throw new RuntimeException("未绑定该平台的账号");
        }
        
        thirdPartyMapper.deleteById(binding.getId());
        log.info("解绑成功");
    }
    
    /**
     * 查询用户的第三方绑定列表
     */
    public java.util.List<UserThirdParty> getUserBindings(Long userId) {
        return thirdPartyMapper.findByUserId(userId);
    }
    
    /**
     * 更新Token
     */
    private void updateToken(UserThirdParty binding, String accessToken, String refreshToken, Long expiresIn) {
        binding.setAccessToken(accessToken);
        binding.setRefreshToken(refreshToken);
        binding.setExpireTime(LocalDateTime.now().plusSeconds(expiresIn));
        binding.setUpdatedAt(LocalDateTime.now());
        thirdPartyMapper.update(binding);
    }
    
    /**
     * 创建新用户并绑定第三方账号
     */
    private JwtResponse createNewUserAndBind(OAuth2Platform platform, OAuth2UserInfo userInfo, 
                                             String accessToken, String refreshToken, Long expiresIn) {
        // 创建用户认证信息
        UserAuth userAuth = new UserAuth();
        String username = generateUsername(platform, userInfo);
        userAuth.setUsername(username);
        userAuth.setPassword(""); // 第三方登录无需密码
        userAuth.setEmail(userInfo.getEmail());
        userAuth.setStatus(1);
        userAuth.setCreatedTime(LocalDateTime.now());
        userAuth.setUpdatedTime(LocalDateTime.now());
        
        userAuthMapper.insert(userAuth);
        
        // 创建普通用户信息
        User user = new User();
        user.setName(userInfo.getNickname() != null ? userInfo.getNickname() : username);
        userMapper.addUser(user);
        
        // 创建绑定关系
        UserThirdParty binding = new UserThirdParty();
        binding.setUserId(userAuth.getId());
        binding.setPlatform(platform.getCode());
        binding.setOpenId(userInfo.getOpenId());
        binding.setUnionId(userInfo.getUnionId());
        binding.setAccessToken(accessToken);
        binding.setRefreshToken(refreshToken);
        binding.setExpireTime(LocalDateTime.now().plusSeconds(expiresIn));
        binding.setCreatedAt(LocalDateTime.now());
        binding.setUpdatedAt(LocalDateTime.now());
        
        try {
            binding.setExtraInfo(objectMapper.writeValueAsString(userInfo));
        } catch (Exception e) {
            log.error("序列化用户信息失败", e);
        }
        
        thirdPartyMapper.insert(binding);
        
        // 生成JWT
        String token = jwtUtil.generateToken(userAuth.getId(), userAuth.getUsername());
        return new JwtResponse(token, userAuth.getId(), userAuth.getUsername());
    }
    
    /**
     * 生成用户名
     */
    private String generateUsername(OAuth2Platform platform, OAuth2UserInfo userInfo) {
        String baseName = userInfo.getNickname() != null ? userInfo.getNickname() : platform.getCode();
        // 确保用户名唯一
        String username = baseName + "_" + platform.getCode();
        
        // 检查是否已存在
        UserAuth existing = userAuthMapper.findByUsername(username);
        if (existing != null) {
            username = username + "_" + UUID.randomUUID().toString().substring(0, 8);
        }
        
        return username;
    }
}
