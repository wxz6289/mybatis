package com.dk.learn.mapper;

import com.dk.learn.entity.UserThirdParty;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 第三方账号绑定Mapper
 */
@Mapper
public interface UserThirdPartyMapper {
    
    /**
     * 根据平台和openId查询绑定信息
     */
    UserThirdParty findByPlatformAndOpenId(@Param("platform") String platform, @Param("openId") String openId);
    
    /**
     * 根据用户ID查询所有绑定
     */
    List<UserThirdParty> findByUserId(@Param("userId") Long userId);
    
    /**
     * 插入绑定信息
     */
    int insert(UserThirdParty userThirdParty);
    
    /**
     * 更新绑定信息
     */
    int update(UserThirdParty userThirdParty);
    
    /**
     * 删除绑定
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 根据用户ID和平台查询
     */
    UserThirdParty findByUserIdAndPlatform(@Param("userId") Long userId, @Param("platform") String platform);
}
