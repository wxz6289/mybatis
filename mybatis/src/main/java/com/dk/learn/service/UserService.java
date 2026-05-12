package com.dk.learn.service;

import com.dk.learn.entity.User;
import com.dk.learn.entity.UserQuery;
import com.dk.learn.common.page.PageQuery;
import com.dk.learn.common.page.PageResult;
import com.dk.learn.entity.UserVO;
import com.dk.learn.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserMapper userMapper;
	/**
	 * 动态条件查询用户（可选参数）
	 * @param query 查询条件，所有字段都是可选的
	 * @return 用户列表
	 */
	public List<User> listUsers(UserQuery query) {
		return userMapper.list(query);
	}

	/**
	 * 使用注解方式的动态查询
	 */
	public List<User> listUsersWithAnnotation(String name, Integer startAge, Integer endAge) {
		return userMapper.listWithAnnotation(name, startAge, endAge);
	}

    public PageResult<User> listUsersWithPagination(PageQuery pq) {
        List<UserVO> records = userMapper.listWithPagination(pq.offset(), pq.size());
        long total = records.isEmpty()? 0 : records.getFirst().getTotal();
        List<User> users = records.stream().map(UserVO::getUser).toList();
        return PageResult.of(users, total, pq);
    }
    
    /**
     * 批量删除用户（支持单个和批量）
     * @param ids 用户ID列表
     */
    public void batchRemoveUsers(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            userMapper.batchRemoveUsers(ids);
        }
    }
    
    /**
     * 添加用户
     * @param user 用户信息
     */
    public void addUser(User user) {
        userMapper.addUser(user);
    }

    public User getUser(Long id) {
        return userMapper.getUser(id);
    }
}
