package com.dk.learn.mapper;

import com.dk.learn.entity.UserAuth;
import com.dk.learn.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户认证Mapper接口
 */
@Mapper
public interface UserAuthMapper {
    
    /**
     * 根据用户名查询用户认证信息
     */
    UserAuth findByUsername(@Param("username") String username);
    
    /**
     * 根据ID查询用户认证信息
     */
    UserAuth findById(@Param("id") Long id);
    
    /**
     * 插入用户认证信息
     */
    int insert(UserAuth userAuth);
    
    /**
     * 更新用户认证信息
     */
    int update(UserAuth userAuth);
    
    /**
     * 更新最后登录时间
     */
    int updateLastLoginTime(@Param("id") Long id, @Param("lastLoginTime") java.time.LocalDateTime lastLoginTime);
    
    /**
     * 删除用户认证信息
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 插入操作日志
     */
    int insertOperationLog(OperationLog log);
    
    /**
     * 查询操作日志列表
     */
    List<OperationLog> findOperationLogs(@Param("offset") int offset, @Param("size") int size);
    
    /**
     * 统计操作日志总数
     */
    long countOperationLogs();
}