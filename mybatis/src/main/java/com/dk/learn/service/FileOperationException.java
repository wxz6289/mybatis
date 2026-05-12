package com.dk.learn.service;

/**
 * 文件操作业务异常，GlobalExceptionHandler 会提取其 message 返回给客户端。
 */
public class FileOperationException extends RuntimeException {

    public FileOperationException(String message) {
        super(message);
    }

    public FileOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}