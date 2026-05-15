package com.dk.learn;

import com.dk.learn.common.page.PageQuery;
import com.dk.learn.entity.Dept;
import com.dk.learn.entity.User;
import com.dk.learn.entity.UserQuery;
import com.dk.learn.entity.UserVO;
import com.dk.learn.mapper.UserMapper;
import com.dk.learn.mapper.DeptMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
class LearnApplicationTests {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DeptMapper deptMapper;

    @Test
    public void testUserList() {
        List<UserVO> userList = userMapper.listWithPagination(0, 10);
        userList.stream().forEach(user -> {
            System.out.println("user = " + user);
        });
    }

	@Test
	void contextLoads() {
	}

    @Test
    public void testJDBC() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        String url = "jdbc:mysql://localhost:3306/mybatis";
        String usernmae = "king";
        String pwd = "king123";
        Connection connection = DriverManager.getConnection(url, usernmae, pwd);
        String sql = "select * from user";
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(sql);

        List<User> userList = new ArrayList<>();
        while (resultSet.next()) {
            long id = resultSet.getLong("id");
            String name = resultSet.getNString("name");
            int age = resultSet.getInt("age");
            int deptId = resultSet.getInt("deptId");
            LocalDateTime createdTime = resultSet.getTimestamp("createdAt").toLocalDateTime();
            LocalDateTime updatedTime = resultSet.getTimestamp("updatedAt").toLocalDateTime();
            User user = new User(id, name, age, deptId, createdTime, updatedTime);
            userList.add(user);
        }
        statement.close();
        connection.close();

        userList.stream().forEach(user -> {
            System.out.println(user);
        });
    }

    @Test
    public void testRemoveUser() {
        userMapper.removeUser(2);
    }

    @Test
    public void testAddUser() {
        User user = new User("king", 18, 1);
        userMapper.addUser(user);
        System.out.println("user.id = " + user.getId());
    }

    @Test
    public void testUpdateUser() {
        // 测试部分字段更新 - 只更新姓名
        User user1 = new User();
        user1.setId(14L);
        user1.setName("Dreamer");
        userMapper.updateUserInfo(user1);
        System.out.println("只更新姓名完成");
        
        // 测试部分字段更新 - 只更新年龄和部门
        User user2 = new User();
        user2.setId(14L);
        user2.setAge(26);
        user2.setDeptId(2);
        userMapper.updateUserInfo(user2);
        System.out.println("只更新年龄和部门完成");
        
        // 测试全部字段更新
        User user3 = new User();
        user3.setId(14L);
        user3.setName("King");
        user3.setAge(18);
        user3.setDeptId(1);
        userMapper.updateUserInfo(user3);
        System.out.println("全部字段更新完成");
    }

    @Test
    public void testGetUser() {
        User user = userMapper.getUser(14);
        System.out.println("user = " + user);
    }

    @Test
    public void testGetUsers() {
        List<User> users = userMapper.getUsers("k", 18, 20);

        for (User user : users) {
            System.out.println("user = " + user);
        }
    }

    @Test
    public void testListUsers() {
        List<User> users = userMapper.list(new UserQuery("k"));

        for (User user : users) {
            System.out.println("user = " + user);
        }
    }

    @Test
    public void testGetDeptById() {
        Dept dept = deptMapper.getDeptById(1);
        System.out.println("dept = " + dept);
    }

    @Test
    public void testUpdateUserInfo() {
        User user = new User();
        user.setId(14l);
        user.setName("Dreamer");
        user.setAge(26);
        user.setDeptId(2);
        userMapper.updateUserInfo(user);
    }

    @Test
    public void testBatchRemoveUsers() {
        List<Long> ids = new ArrayList<>();
        ids.add(14L);
//        ids.add(18L);
        userMapper.batchRemoveUsers(ids);
    }
}
