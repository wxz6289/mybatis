package com.dk.learn.controller;

import com.dk.learn.entity.User;
import com.dk.learn.entity.UserQuery;
import com.dk.learn.common.page.PageQuery;
import com.dk.learn.common.page.PageResult;
import com.dk.learn.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	/** 分页用户列表：page 从 1 开始，size 默认 10、最大 100；JSON 外层统一为 Result。 */
	@GetMapping
	public PageResult<User> listUsers(
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "10") int size) {
		return userService.listUsersPage(PageQuery.of(page, size));
	}

	/**
	 * 动态条件查询用户（可选参数）
	 * 所有参数都是可选的，可以任意组合
	 * 
	 * 示例：
	 * - /api/users/search?name=张三
	 * - /api/users/search?startAge=20&endAge=30
	 * - /api/users/search?name=李&startAge=25&endAge=35
	 * - /api/users/search (无条件，查询全部)
	 */
	@GetMapping("/search")
	public List<User> searchUsers(UserQuery query) {
		return userService.listUsers(query);
	}

	/**
	 * 使用注解方式的动态查询
	 */
	@GetMapping("/search2")
	public List<User> searchUsersWithAnnotation(
			@RequestParam(required = false) String name,
			@RequestParam(required = false) Integer startAge,
			@RequestParam(required = false) Integer endAge) {
		return userService.listUsersWithAnnotation(name, startAge, endAge);
	}
}
