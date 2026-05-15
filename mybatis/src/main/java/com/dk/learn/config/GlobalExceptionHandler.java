package com.dk.learn.config;

import com.dk.learn.common.result.Result;
import com.dk.learn.service.FileOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(FileOperationException.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public Result<Void> handleFileOperation(FileOperationException e) {
		log.error("文件操作失败", e);
		return Result.fail(500, e.getMessage());
	}

	@ExceptionHandler(IllegalArgumentException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public Result<Void> badRequest(IllegalArgumentException e) {
		return Result.fail(400, e.getMessage());
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
		String parameterName = e.getName();
		String requiredType = e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "Unknown";
		Object value = e.getValue();
		
		String message = String.format("参数 '%s' 类型错误，期望类型: %s，实际值: %s", 
				parameterName, requiredType, value);
		
		log.warn("参数类型转换失败: {}", message);
		return Result.fail(400, message);
	}

	@ExceptionHandler(Exception.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public Result<Void> serverError(Exception e) {
		log.error("Unhandled error", e);
		return Result.fail(500, "服务器内部错误");
	}
}
