package com.dk.learn.controller;

import com.dk.learn.entity.User;
import com.dk.learn.entity.UserQuery;
import com.dk.learn.common.page.PageQuery;
import com.dk.learn.common.page.PageResult;
import com.dk.learn.common.result.Result;
import com.dk.learn.entity.UserVO;
import com.dk.learn.service.UserService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
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
		return userService.listUsersWithPagination(PageQuery.of(page, size));
	}

    @GetMapping("/{id}")
    public Result<User> getUser(@PathVariable("id") Long id) {
        if (id == null) {
            return Result.error("用户ID不能为空");
        }
        User user = userService.getUser(id);
		return Result.ok(user);
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
	
	/**
	 * 添加用户
	 * @param user 用户信息
	 * @return 添加结果
	 */
	@PostMapping
	public Result<Void> addUser(@RequestBody User user) {
		userService.addUser(user);
		return Result.ok(null);
	}
	
	/**
	 * 删除用户（支持单个和批量删除）
	 * - 单个删除（路径参数）：DELETE /api/users/1
	 * - 批量删除（路径参数）：DELETE /api/users/1,2,3
	 * - 查询参数方式：DELETE /api/users?ids=1,2,3
	 * @param pathIds 路径参数中的ID列表（逗号分隔）
	 * @param ids 查询参数中的ID列表
	 * @return 删除结果
	 */
	@DeleteMapping(value = {"/{pathIds}", ""})
	public Result<Void> deleteUsers(@PathVariable(required = false) String pathIds,
	                                 @RequestParam(required = false) List<Long> ids) {
		List<Long> idList;
		
		// 优先使用路径参数
		if (pathIds != null && !pathIds.isEmpty()) {
			// 将路径参数 "1,2,3" 转换为 List<Long>
			idList = Arrays.stream(pathIds.split(","))
					.map(String::trim)
					.filter(s -> !s.isEmpty())
					.map(Long::parseLong)
					.collect(java.util.stream.Collectors.toList());
		} else if (ids != null && !ids.isEmpty()) {
			// 使用查询参数
			idList = ids;
		} else {
			throw new IllegalArgumentException("请提供要删除的用户ID");
		}
		
		userService.batchRemoveUsers(idList);
		return Result.ok(null);
	}
}
