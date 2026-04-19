package com.dk.learn.service;

import com.dk.learn.User;
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
}
