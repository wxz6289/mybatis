package com.dk.learn.interceptor;

import com.dk.learn.common.result.Result;
import com.dk.learn.common.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@Component
public class LoginCheckInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        System.out.println("LoginCheckInterceptor.preHandle");
        String url = request.getRequestURI();
        log.info("url: {}", url);
        if (url.contains("/api/auth/login") || url.contains("/api/auth/register")) {
            log.info("放行登录和注册接口");
            return true;
        }
        String jwt = request.getHeader("Authorization");
        if (jwt == null || jwt.isEmpty()) {
            log.info("未授权访问");
            Result<Object> noLogin = Result.error("未授权访问");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(noLogin.toString());
            return false;
        }
        String token = jwt.startsWith("Bearer ") ? jwt.substring(7) : jwt;
        if (!jwtUtil.validateToken(token)) {
            log.info("无效的token");
            Result<Object> error = Result.error("无效的token");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(error.toString());
            return false;
        }
        return HandlerInterceptor.super.preHandle(request, response, handler);
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        System.out.println("LoginCheckInterceptor.postHandle");
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }

    @Override // 最后运行
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        System.out.println("LoginCheckInterceptor.afterCompletion");
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
