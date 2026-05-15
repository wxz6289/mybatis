package com.dk.learn.controller;

import com.dk.learn.common.result.Result;
import com.dk.learn.common.util.JwtUtil;
import com.dk.learn.common.util.OAuth2Client;
import com.dk.learn.entity.JwtResponse;
import com.dk.learn.entity.OAuth2Platform;
import com.dk.learn.entity.UserThirdParty;
import com.dk.learn.service.OAuth2LoginService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 第三方登录控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/oauth2")
@RequiredArgsConstructor
public class OAuth2Controller {
    
    private final OAuth2Client oauth2Client;
    private final OAuth2LoginService oauth2LoginService;
    private final JwtUtil jwtUtil;
    
    /**
     * 获取第三方授权URL
     * @param platform 平台名称（github, wechat, qq, google, weibo）
     */
    @GetMapping("/authorize/{platform}")
    public Result<String> getAuthorizationUrl(@PathVariable String platform) {
        try {
            OAuth2Platform oauth2Platform = OAuth2Platform.fromCode(platform);
            // 生成state用于防止CSRF攻击
            String state = UUID.randomUUID().toString();
            String authorizationUrl = oauth2Client.getAuthorizationUrl(oauth2Platform, state);
            
            log.info("生成{}授权URL: {}", platform, authorizationUrl);
            return Result.ok(authorizationUrl);
        } catch (Exception e) {
            log.error("获取授权URL失败", e);
            return Result.error("不支持的平台: " + platform);
        }
    }
    
    /**
     * 第三方登录回调
     * @param platform 平台名称
     * @param code 授权码
     * @param state 状态参数
     */
    @GetMapping("/callback/{platform}")
    public Result<JwtResponse> callback(
            @PathVariable String platform,
            @RequestParam String code,
            @RequestParam(required = false) String state) {
        try {
            OAuth2Platform oauth2Platform = OAuth2Platform.fromCode(platform);
            
            // TODO: 验证state参数（生产环境必须）
            
            JwtResponse response = oauth2LoginService.handleOAuth2Callback(oauth2Platform, code);
            return Result.ok(response);
        } catch (Exception e) {
            log.error("OAuth2回调处理失败", e);
            return Result.error("登录失败: " + e.getMessage());
        }
    }
    
    /**
     * 绑定第三方账号
     * @param platform 平台名称
     * @param code 授权码
     */
    @PostMapping("/bind/{platform}")
    public Result<Void> bindAccount(
            @PathVariable String platform,
            @RequestParam String code,
            @RequestHeader("Authorization") String authorization) {
        try {
            // 从Token中获取用户ID（需要先解析Token）
            Long userId = extractUserIdFromToken(authorization);
            if (userId == null) {
                return Result.error("未授权访问");
            }
            
            OAuth2Platform oauth2Platform = OAuth2Platform.fromCode(platform);
            oauth2LoginService.bindThirdPartyAccount(userId, oauth2Platform, code);
            
            return Result.ok(null);
        } catch (Exception e) {
            log.error("绑定账号失败", e);
            return Result.error("绑定失败: " + e.getMessage());
        }
    }
    
    /**
     * 解绑第三方账号
     * @param platform 平台名称
     */
    @DeleteMapping("/unbind/{platform}")
    public Result<Void> unbindAccount(
            @PathVariable String platform,
            @RequestHeader("Authorization") String authorization) {
        try {
            Long userId = extractUserIdFromToken(authorization);
            if (userId == null) {
                return Result.error("未授权访问");
            }
            
            OAuth2Platform oauth2Platform = OAuth2Platform.fromCode(platform);
            oauth2LoginService.unbindThirdPartyAccount(userId, oauth2Platform);
            
            return Result.ok(null);
        } catch (Exception e) {
            log.error("解绑账号失败", e);
            return Result.error("解绑失败: " + e.getMessage());
        }
    }
    
    /**
     * 查询用户的第三方绑定列表
     */
    @GetMapping("/bindings")
    public Result<List<UserThirdParty>> getBindings(
            @RequestHeader("Authorization") String authorization) {
        try {
            Long userId = extractUserIdFromToken(authorization);
            if (userId == null) {
                return Result.error("未授权访问");
            }
            
            List<UserThirdParty> bindings = oauth2LoginService.getUserBindings(userId);
            return Result.ok(bindings);
        } catch (Exception e) {
            log.error("查询绑定列表失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }
    
    /**
     * 从 Token 中提取用户ID
     */
    private Long extractUserIdFromToken(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            if (jwtUtil.validateToken(token)) {
                return jwtUtil.getUserIdFromToken(token);
            }
        }
        return null;
    }
}
