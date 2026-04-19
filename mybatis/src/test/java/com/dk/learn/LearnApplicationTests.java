package com.dk.learn;

import com.dk.learn.mapper.UserMapper;
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

    @Test
    public void testUserList() {
        List<User> userList = userMapper.listPage(0, 10);
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
            String name = resultSet.getNString("name");
            int age = resultSet.getInt("age");
            int deptId = resultSet.getInt("deptId");
            LocalDateTime createdTime = resultSet.getTimestamp("createdAt").toLocalDateTime();
            LocalDateTime updatedTime = resultSet.getTimestamp("updatedAt").toLocalDateTime();
            User user = new User(name, age, deptId, createdTime, updatedTime);
            userList.add(user);
        }
        statement.close();
        connection.close();

        userList.stream().forEach(user -> {
            System.out.println(user);
        });
    }
}
