package com.dk.learn.mapper;

import com.dk.learn.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper {

	@Results(id = "userMap", value = {
			@Result(column = "createdAt", property = "createdTime"),
			@Result(column = "updatedAt", property = "updatedTime")
	})
	@Select("SELECT * FROM user ORDER BY createdAt DESC LIMIT #{offset}, #{size}")
	List<User> listPage(@Param("offset") int offset, @Param("size") int size);

	@Select("SELECT COUNT(*) FROM user")
	long count();
}
