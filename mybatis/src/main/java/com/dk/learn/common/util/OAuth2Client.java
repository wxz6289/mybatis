package com.dk.learn.common.util;

import com.dk.learn.config.OAuth2Config;
import com.dk.learn.entity.OAuth2Platform;
import com.dk.learn.entity.OAuth2UserInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * OAuth2客户端工具类
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2Client {
    
    private final OAuth2Config oauth2Config;
    private final WebClient webClient = WebClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 获取授权URL
     */
    public String getAuthorizationUrl(OAuth2Platform platform, String state) {
        OAuth2Config.PlatformConfig config = getPlatformConfig(platform);
        
        return UriComponentsBuilder.fromUriString(config.getAuthorizationUrl())
                .queryParam("client_id", config.getClientId())
                .queryParam("redirect_uri", config.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", config.getScope())
                .queryParam("state", state)
                .build()
                .toUriString();
    }
    
    /**
     * 通过授权码获取访问令牌
     */
    public Map<String, Object> getAccessToken(OAuth2Platform platform, String code) {
        OAuth2Config.PlatformConfig config = getPlatformConfig(platform);
        
        // GitHub使用form-urlencoded格式，其他平台使用JSON
        String response;
        if (platform == OAuth2Platform.GITHUB) {
            // GitHub需要特殊的Accept header和form-urlencoded格式
            org.springframework.util.LinkedMultiValueMap<String, String> map = 
                new org.springframework.util.LinkedMultiValueMap<>();
            map.add("client_id", config.getClientId());
            map.add("client_secret", config.getClientSecret());
            map.add("code", code);
            map.add("redirect_uri", config.getRedirectUri());
            map.add("grant_type", "authorization_code");
            
            response = webClient.post()
                    .uri(config.getTokenUrl())
                    .header("Accept", "application/json")  // GitHub支持JSON响应
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue(map)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } else {
            // 其他平台使用JSON格式
            Map<String, String> body = new HashMap<>();
            body.put("client_id", config.getClientId());
            body.put("client_secret", config.getClientSecret());
            body.put("code", code);
            body.put("redirect_uri", config.getRedirectUri());
            body.put("grant_type", "authorization_code");
            
            response = webClient.post()
                    .uri(config.getTokenUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        }
        
        log.info("获取{}平台AccessToken响应: {}", platform.getName(), response);
        
        try {
            // 尝试解析为JSON
            return objectMapper.readValue(response, Map.class);
        } catch (Exception e) {
            // 如果JSON解析失败，尝试解析form-urlencoded格式
            log.warn("JSON解析失败，尝试解析form-urlencoded格式");
            return parseFormUrlEncoded(response);
        }
    }
    
    /**
     * 解析form-urlencoded格式的响应
     */
    private Map<String, Object> parseFormUrlEncoded(String response) {
        Map<String, Object> result = new HashMap<>();
        String[] pairs = response.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                String key = java.net.URLDecoder.decode(keyValue[0], java.nio.charset.StandardCharsets.UTF_8);
                String value = java.net.URLDecoder.decode(keyValue[1], java.nio.charset.StandardCharsets.UTF_8);
                result.put(key, value);
            }
        }
        log.info("解析form-urlencoded响应: {}", result);
        return result;
    }
    
    /**
     * 获取用户信息
     */
    public OAuth2UserInfo getUserInfo(OAuth2Platform platform, String accessToken) {
        OAuth2Config.PlatformConfig config = getPlatformConfig(platform);
        
        String response;
        
        // GitHub需要使用Authorization header，其他平台使用query参数
        if (platform == OAuth2Platform.GITHUB) {
            response = webClient.get()
                    .uri(config.getUserInfoUrl())
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/vnd.github.v3+json")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } else {
            // 其他平台使用query参数
            response = webClient.get()
                    .uri(config.getUserInfoUrl(), uriBuilder -> 
                        uriBuilder.queryParam("access_token", accessToken).build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        }
        
        log.info("获取{}平台用户信息响应: {}", platform.getName(), response);
        
        return parseUserInfo(platform, response);
    }
    
    /**
     * 解析用户信息（不同平台格式不同）
     */
    private OAuth2UserInfo parseUserInfo(OAuth2Platform platform, String response) {
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            log.info("解析{}平台用户信息: {}", platform.getName(), jsonNode.toString());
            
            OAuth2UserInfo userInfo = new OAuth2UserInfo();
            userInfo.setPlatform(platform);
            
            switch (platform) {
                case GITHUB:
                    userInfo.setOpenId(jsonNode.has("id") ? jsonNode.get("id").asText() : null);
                    userInfo.setNickname(jsonNode.has("login") ? jsonNode.get("login").asText() : "GitHub User");
                    userInfo.setAvatar(jsonNode.has("avatar_url") ? jsonNode.get("avatar_url").asText() : null);
                    userInfo.setEmail(jsonNode.has("email") && !jsonNode.get("email").isNull() 
                        ? jsonNode.get("email").asText() : null);
                    userInfo.setLocation(jsonNode.has("location") && !jsonNode.get("location").isNull()
                        ? jsonNode.get("location").asText() : null);
                    break;
                    
                case WECHAT:
                    userInfo.setOpenId(jsonNode.has("openid") ? jsonNode.get("openid").asText() : null);
                    userInfo.setUnionId(jsonNode.has("unionid") ? jsonNode.get("unionid").asText() : null);
                    userInfo.setNickname(jsonNode.has("nickname") ? jsonNode.get("nickname").asText() : "微信用户");
                    userInfo.setAvatar(jsonNode.has("headimgurl") ? jsonNode.get("headimgurl").asText() : null);
                    userInfo.setGender(jsonNode.has("sex") ? (jsonNode.get("sex").asInt() == 1 ? "male" : "female") : null);
                    break;
                    
                case QQ:
                    userInfo.setOpenId(jsonNode.has("openid") ? jsonNode.get("openid").asText() : null);
                    userInfo.setNickname(jsonNode.has("nickname") ? jsonNode.get("nickname").asText() : "QQ用户");
                    userInfo.setAvatar(jsonNode.has("figureurl_qq_2") ? jsonNode.get("figureurl_qq_2").asText() : null);
                    userInfo.setGender(jsonNode.has("gender") ? jsonNode.get("gender").asText() : null);
                    break;
                    
                case GOOGLE:
                    // Google用户信息解析
                    userInfo.setOpenId(jsonNode.has("sub") ? jsonNode.get("sub").asText() : null);
                    userInfo.setEmail(jsonNode.has("email") && !jsonNode.get("email").isNull() 
                        ? jsonNode.get("email").asText() : null);
                    userInfo.setNickname(jsonNode.has("name") && !jsonNode.get("name").isNull() 
                        ? jsonNode.get("name").asText() : "Google User");
                    userInfo.setAvatar(jsonNode.has("picture") && !jsonNode.get("picture").isNull() 
                        ? jsonNode.get("picture").asText() : null);
                    break;
                    
                case WEIBO:
                    userInfo.setOpenId(jsonNode.has("id") ? jsonNode.get("id").asText() : null);
                    userInfo.setNickname(jsonNode.has("screen_name") ? jsonNode.get("screen_name").asText() : "微博用户");
                    userInfo.setAvatar(jsonNode.has("avatar_large") ? jsonNode.get("avatar_large").asText() : null);
                    userInfo.setLocation(jsonNode.has("location") ? jsonNode.get("location").asText() : null);
                    break;
                    
                default:
                    throw new IllegalArgumentException("Unsupported platform: " + platform);
            }
            
            log.info("解析成功 - OpenID: {}, Nickname: {}", userInfo.getOpenId(), userInfo.getNickname());
            return userInfo;
        } catch (Exception e) {
            log.error("解析{}平台用户信息失败: {}", platform.getName(), e.getMessage(), e);
            throw new RuntimeException("解析用户信息失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 刷新访问令牌
     */
    public String refreshAccessToken(OAuth2Platform platform, String refreshToken) {
        OAuth2Config.PlatformConfig config = getPlatformConfig(platform);
        
        Map<String, String> body = new HashMap<>();
        body.put("client_id", config.getClientId());
        body.put("client_secret", config.getClientSecret());
        body.put("refresh_token", refreshToken);
        body.put("grant_type", "refresh_token");
        
        String response = webClient.post()
                .uri(config.getTokenUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            return jsonNode.get("access_token").asText();
        } catch (Exception e) {
            throw new RuntimeException("刷新AccessToken失败", e);
        }
    }
    
    /**
     * 获取平台配置
     */
    private OAuth2Config.PlatformConfig getPlatformConfig(OAuth2Platform platform) {
        return switch (platform) {
            case GITHUB -> oauth2Config.getGithub();
            case WECHAT -> oauth2Config.getWechat();
            case QQ -> oauth2Config.getQq();
            case GOOGLE -> oauth2Config.getGoogle();
            case WEIBO -> oauth2Config.getWeibo();
        };
    }
}
