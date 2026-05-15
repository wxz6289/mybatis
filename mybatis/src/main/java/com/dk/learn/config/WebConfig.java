package com.dk.learn.config;

import com.dk.learn.interceptor.LoginCheckInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

/**
 * Web MVC 配置
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    
    private final JwtAuthenticationInterceptor jwtAuthenticationInterceptor;
    private final OperationLogInterceptor operationLogInterceptor;
    private final LoginCheckInterceptor loginCheckInterceptor;
    
    /**
     * 配置静态资源处理
     * 添加favicon.ico的支持，避免404错误
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 处理favicon.ico请求
        registry.addResourceHandler("/favicon.ico")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)));
        
        // 处理uploads目录的文件访问
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:./uploads/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(30)));
    }
    
    /**
     * 注册拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册JWT认证拦截器，排除不需要认证的接口
        registry.addInterceptor(jwtAuthenticationInterceptor)
                .addPathPatterns("/api/**")  // 拦截所有/api开头的请求
                .excludePathPatterns(
                        "/api/auth/login",    // 排除登录接口
                        "/api/auth/register", // 排除注册接口
                        "/api/oauth2/**",     // 排除OAuth2第三方登录接口
                        "/api/test/public",   // 排除公开测试接口
                        "/api/users/**",      // 排除用户管理接口（可选）
                        "/api/depts/**",      // 排除部门管理接口（可选）
                        "/api/files/**"       // 排除文件管理接口（可选）
                );

        registry.addInterceptor(loginCheckInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/register");
        // 注册操作日志拦截器
        registry.addInterceptor(operationLogInterceptor)
                .addPathPatterns("/api/**");  // 记录所有API请求的日志
    }
}
