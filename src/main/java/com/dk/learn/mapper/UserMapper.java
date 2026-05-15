package com.dk.learn.mapper;

import com.dk.learn.common.page.PageQuery;
import com.dk.learn.entity.User;
import com.dk.learn.entity.UserQuery;
import com.dk.learn.entity.UserVO;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {

	@Results(id = "userMap", value = {
			@Result(column = "createdAt", property = "createdTime"),
			@Result(column = "updatedAt", property = "updatedTime")
	})
	@Select("SELECT *, COUNT(*) OVER() AS total  FROM user ORDER BY createdAt DESC LIMIT #{offset}, #{size}")
	List<UserVO> listWithPagination(@Param("offset") int offset, @Param("size") int size);

	@Select("SELECT COUNT(*) FROM user")
	long count();

    @Delete("DELETE FROM user WHERE id = #{id}")
    void removeUser(@Param("id") long id);

//    @Select("INSERT INTO user (name, age, deptId, createdAt, updatedAt) VALUES (#{name}, #{age}, #{deptId}, #{createdTime}, #{updatedTime})")
    @Options(useGeneratedKeys = true, keyColumn = "id", keyProperty = "id")
    @Insert("INSERT INTO user (name, age, deptId) VALUES (#{name}, #{age}, #{deptId})")
    void addUser(User user);

    /**
     * 更新用户信息（支持部分字段更新）
     * 只有非 null 的字段才会被更新
     */
    void updateUserInfo(User user);

    @Select("SELECT * FROM user WHERE id = #{id}")
    @ResultMap("userMap")
    User getUser(@Param("id") long id);

    @ResultMap("userMap")
    @Select("SELECT * FROM user WHERE name LIKE CONCAT('%', #{name}, '%') AND age BETWEEN #{start} AND #{end}")
    List<User> getUsers(@Param("name")  String name, @Param("start") int start, @Param("end") int end);

    /**
     * 方案1：使用对象封装参数 + XML动态SQL（推荐）
     * 所有参数都是可选的，会根据传入的值动态构建SQL
     */
    List<User> list(UserQuery query);

    void batchRemoveUsers(@Param("ids") List<Long> ids);

    /**
     * 方案2：使用@Param + 注解方式（简单场景）
     * 注意：这种方式不能真正实现可选，需要配合XML或Provider
     */
    @ResultMap("userMap")
    @Select("<script>" +
            "SELECT * FROM user " +
            "<where>" +
            "  <if test='name != null and name != \"\"'>" +
            "    AND name LIKE CONCAT('%', #{name}, '%')" +
            "  </if>" +
            "  <if test='startAge != null'>" +
            "    AND age &gt;= #{startAge}" +
            "  </if>" +
            "  <if test='endAge != null'>" +
            "    AND age &lt;= #{endAge}" +
            "  </if>" +
            "</where>" +
            "ORDER BY createdAt DESC" +
            "</script>")
    List<User> listWithAnnotation(@Param("name") String name,
                                   @Param("startAge") Integer startAge,
                                   @Param("endAge") Integer endAge);

}
