package com.dk.learn.filter;

import com.dk.learn.common.result.Result;
import com.dk.learn.common.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
//@WebFilter("/*")
public class TestFilter implements Filter {
    
    @Autowired
    private JwtUtil jwtUtil;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        System.out.println("TestFilter doFilter");
        // 放行
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String url = request.getRequestURI();
        
        // 放行登录和注册接口
        if (url.contains("/api/auth/login") || url.contains("/api/auth/register")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // 检查Authorization header
        String authorization = request.getHeader("Authorization");
        if (authorization == null || authorization.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            Result error = Result.error("未授权访问");
            String json = objectMapper.writeValueAsString(error);
            response.getWriter().write(json);
            return;
        }
        
        // 提取token（去掉Bearer前缀）
        String token = authorization.startsWith("Bearer ") ? authorization.substring(7) : authorization;
        
        try {
            // 验证token
            boolean isValid = jwtUtil.validateToken(token);
            if (!isValid) {
                throw new IllegalArgumentException("无效的token");
            }
            // token有效，放行
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            Result error = Result.error("无效的认证令牌");
            String json = objectMapper.writeValueAsString(error);
            response.getWriter().write(json);
        }

        System.out.println("TestFilter doFilter end");
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
