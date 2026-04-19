package com.dk.learn.config;

import com.dk.learn.common.result.Result;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 将 Controller 返回的非 {@link Result} 体统一包装为 {@link Result#ok(Object)}。
 * 已返回 {@link Result} 的接口（如全局异常）不再二次包装。
 */
@RestControllerAdvice(basePackages = "com.dk.learn.controller")
public class ApiResponseBodyAdvice implements ResponseBodyAdvice<Object> {

	@Override
	public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
		Class<?> type = returnType.getParameterType();
		return !Result.class.isAssignableFrom(type);
	}

	@Override
	public Object beforeBodyWrite(
			Object body,
			MethodParameter returnType,
			MediaType selectedContentType,
			Class<? extends HttpMessageConverter<?>> selectedConverterType,
			ServerHttpRequest request,
			ServerHttpResponse response) {
		if (body instanceof Result) {
			return body;
		}
		return Result.ok(body);
	}
}
