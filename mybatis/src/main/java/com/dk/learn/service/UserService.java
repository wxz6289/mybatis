package com.dk.learn.service;

import com.dk.learn.entity.User;
import com.dk.learn.entity.UserQuery;
import com.dk.learn.common.page.PageQuery;
import com.dk.learn.common.page.PageResult;
import com.dk.learn.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserMapper userMapper;

	public PageResult<User> listUsersPage(PageQuery query) {
		long total = userMapper.count();
		List<User> records = userMapper.listPage(query.offset(), query.size());
		return PageResult.of(records, total, query);
	}

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
}
