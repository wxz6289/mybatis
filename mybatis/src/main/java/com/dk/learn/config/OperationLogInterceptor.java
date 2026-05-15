package com.dk.learn.config;

import com.dk.learn.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * 操作日志拦截器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OperationLogInterceptor implements HandlerInterceptor {
    
    private final OperationLogService operationLogService;
    
    private static final ThreadLocal<Long> START_TIME = new ThreadLocal<>();
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 记录请求开始时间
        START_TIME.set(System.currentTimeMillis());
        return true;
    }
    
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        // 在请求处理后记录日志
        logOperation(request, response, true, null);
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 如果发生异常，记录失败日志
        if (ex != null) {
            logOperation(request, response, false, ex.getMessage());
        }
        // 清理ThreadLocal
        START_TIME.remove();
    }
    
    /**
     * 记录操作日志
     */
    private void logOperation(HttpServletRequest request, HttpServletResponse response, boolean success, String errorMsg) {
        try {
            // 获取用户信息（从JWT拦截器中设置）
            Long userId = (Long) request.getAttribute("userId");
            String username = (String) request.getAttribute("username");
            
            // 如果用户信息为空，可能是未认证的请求
            if (userId == null) {
                userId = 0L;
                username = "anonymous";
            }
            
            // 构建操作描述
            String operation = buildOperationDescription(request);
            
            // 获取请求参数
            String params = getRequestParamSummary(request);
            
            // 获取IP地址
            String ip = getClientIp(request);
            
            // 获取User-Agent
            String userAgent = request.getHeader("User-Agent");
            
            // 记录日志
            if (success) {
                operationLogService.logSuccess(userId, username, operation, 
                    request.getMethod(), params, ip, userAgent);
            } else {
                operationLogService.logFailure(userId, username, operation, 
                    request.getMethod(), params, ip, userAgent, errorMsg);
            }
        } catch (Exception e) {
            log.error("记录操作日志失败", e);
        }
    }
    
    /**
     * 构建操作描述
     */
    private String buildOperationDescription(HttpServletRequest request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        return method + " " + uri;
    }
    
    /**
     * 获取请求参数摘要
     */
    private String getRequestParamSummary(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();
        request.getParameterMap().forEach((key, values) -> {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(key).append("=");
            if (values != null && values.length > 0) {
                sb.append(values[0]);
            }
        });
        return sb.toString();
    }
    
    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 对于通过多个代理的情况，第一个IP为客户端真实IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}