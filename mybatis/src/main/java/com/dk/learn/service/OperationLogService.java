package com.dk.learn.service;

import com.dk.learn.entity.OperationLog;
import com.dk.learn.mapper.UserAuthMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 操作日志服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogService {
    
    private final UserAuthMapper userAuthMapper;
    
    /**
     * 记录操作日志
     */
    public void logOperation(OperationLog log) {
        log.setOperationTime(LocalDateTime.now());
        userAuthMapper.insertOperationLog(log);
    }
    
    /**
     * 记录成功操作日志
     */
    public void logSuccess(Long userId, String username, String operation, String method, 
                          String params, String ip, String userAgent) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setOperation(operation);
        log.setMethod(method);
        log.setParams(params);
        log.setIp(ip);
        log.setUserAgent(userAgent);
        log.setStatus(1); // 成功
        logOperation(log);
    }
    
    /**
     * 记录失败操作日志
     */
    public void logFailure(Long userId, String username, String operation, String method, 
                          String params, String ip, String userAgent, String errorMsg) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setOperation(operation);
        log.setMethod(method);
        log.setParams(params);
        log.setIp(ip);
        log.setUserAgent(userAgent);
        log.setStatus(0); // 失败
        log.setErrorMsg(errorMsg);
        logOperation(log);
    }
}