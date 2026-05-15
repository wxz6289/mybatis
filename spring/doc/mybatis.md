# MyBatis 全面总结

## 一、MyBatis 概述

### 1.1 什么是 MyBatis
MyBatis 是一款优秀的持久层框架（ORM），用于简化数据库操作。它支持自定义 SQL、存储过程以及高级映射。MyBatis 去除了几乎所有的 JDBC 代码和设置参数以及获取结果集的工作。

### 1.2 核心特点
- **半自动 ORM**：与 Hibernate 全自动 ORM 不同，MyBatis 需要手动编写 SQL，但提供了更大的灵活性
- **SQL 与代码分离**：SQL 语句写在 XML 文件或注解中，便于维护和优化
- **灵活的映射**：支持自动映射和高级结果映射
- **动态 SQL**：提供强大的动态 SQL 功能，可根据条件生成不同的 SQL 语句
- **易于学习**：上手简单，学习成本低

### 1.3 核心组件
- **SqlSessionFactory**：创建 SqlSession 的工厂
- **SqlSession**：用于执行 SQL 命令的主要接口
- **Mapper**：映射器接口，定义数据库操作方法
- **Configuration**：MyBatis 的全局配置对象

---

## 二、环境搭建与配置

### 2.1 依赖引入

**Maven 依赖：**
```xml
<dependency>
    <groupId>org.mybatis</groupId>
    <artifactId>mybatis</artifactId>
    <version>3.5.13</version>
</dependency>

<!-- 数据库驱动 -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.33</version>
</dependency>
```

**Gradle 依赖：**
```gradle
implementation 'org.mybatis:mybatis:3.5.13'
implementation 'mysql:mysql-connector-java:8.0.33'
```

### 2.2 核心配置文件 (mybatis-config.xml)

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE configuration
  PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
  "http://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
    <!-- 环境配置 -->
    <environments default="development">
        <environment id="development">
            <transactionManager type="JDBC"/>
            <dataSource type="POOLED">
                <property name="driver" value="com.mysql.cj.jdbc.Driver"/>
                <property name="url" value="jdbc:mysql://localhost:3306/test"/>
                <property name="username" value="root"/>
                <property name="password" value="root"/>
            </dataSource>
        </environment>
    </environments>

    <!-- 映射器配置 -->
    <mappers>
        <mapper resource="com/example/mapper/UserMapper.xml"/>
    </mappers>
</configuration>
```

### 2.3 构建 SqlSessionFactory

```java
String resource = "mybatis-config.xml";
InputStream inputStream = Resources.getResourceAsStream(resource);
SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
```

---

## 三、核心 API 使用

### 3.1 SqlSession

```java
try (SqlSession session = sqlSessionFactory.openSession()) {
    // 方式1：直接执行 SQL（不推荐）
    User user = session.selectOne("com.example.mapper.UserMapper.selectById", 1);

    // 方式2：使用 Mapper 接口（推荐）
    UserMapper mapper = session.getMapper(UserMapper.class);
    User user = mapper.selectById(1);

    session.commit();
} catch (Exception e) {
    session.rollback();
}
```

### 3.2 Mapper 接口

```java
public interface UserMapper {
    User selectById(Integer id);
    List<User> selectAll();
    int insert(User user);
    int update(User user);
    int deleteById(Integer id);
}
```

---

## 四、Mapper XML 映射文件

### 4.1 基本 CRUD 操作

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
  PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
  "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.mapper.UserMapper">

    <!-- 查询 -->
    <select id="selectById" resultType="com.example.model.User">
        SELECT * FROM users WHERE id = #{id}
    </select>

    <select id="selectAll" resultType="com.example.model.User">
        SELECT * FROM users
    </select>

    <!-- 插入 -->
    <insert id="insert" parameterType="com.example.model.User" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO users (name, email, age)
        VALUES (#{name}, #{email}, #{age})
    </insert>

    <!-- 更新 -->
    <update id="update" parameterType="com.example.model.User">
        UPDATE users
        SET name = #{name}, email = #{email}, age = #{age}
        WHERE id = #{id}
    </update>

    <!-- 删除 -->
    <delete id="deleteById">
        DELETE FROM users WHERE id = #{id}
    </delete>

</mapper>
```

### 4.2 参数传递

#### 单个参数
```xml
<select id="selectById" resultType="User">
    SELECT * FROM users WHERE id = #{id}
</select>
```

#### 多个参数（使用 @Param）
```java
User selectByUsernameAndPassword(@Param("username") String username,
                                  @Param("password") String password);
```

```xml
<select id="selectByUsernameAndPassword" resultType="User">
    SELECT * FROM users
    WHERE username = #{username} AND password = #{password}
</select>
```

#### Map 参数
```xml
<select id="selectByMap" resultType="User">
    SELECT * FROM users
    WHERE name = #{name} AND age = #{age}
</select>
```

### 4.3 #{} 与 ${} 的区别

| 特性     | `#{}`                         | `${}`                    |
|----------|-------------------------------|--------------------------|
| 预处理   | 预编译处理（PreparedStatement） | 字符串直接替换           |
| 安全性   | 防止 SQL 注入                 | 存在 SQL 注入风险        |
| 使用场景 | 大多数情况                    | 动态表名、列名等          |
| 示例     | `WHERE id = #{id}`            | `ORDER BY ${columnName}` |

**推荐使用 `#{}`**，只在必要时使用 `${}`。

---

## 五、结果映射

### 5.1 自动映射

当列名与属性名一致时，MyBatis 会自动映射：

```xml
<select id="selectAll" resultType="com.example.model.User">
    SELECT * FROM users
</select>
```

### 5.2 resultMap 高级映射

当列名与属性名不一致或有关联关系时：

```xml
<resultMap id="userResultMap" type="com.example.model.User">
    <id property="userId" column="user_id"/>
    <result property="userName" column="user_name"/>
    <result property="userEmail" column="email"/>
    <result property="userAge" column="age"/>
</resultMap>

<select id="selectById" resultMap="userResultMap">
    SELECT user_id, user_name, email, age
    FROM users WHERE user_id = #{id}
</select>
```

### 5.3 关联映射（一对一）

```xml
<resultMap id="userWithProfileResultMap" type="User">
    <id property="id" column="id"/>
    <result property="name" column="name"/>
    <association property="profile" javaType="Profile">
        <id property="profileId" column="profile_id"/>
        <result property="bio" column="bio"/>
    </association>
</resultMap>
```

### 5.4 集合映射（一对多）

```xml
<resultMap id="userWithOrdersResultMap" type="User">
    <id property="id" column="user_id"/>
    <result property="name" column="user_name"/>
    <collection property="orders" ofType="Order">
        <id property="orderId" column="order_id"/>
        <result property="orderNo" column="order_no"/>
        <result property="amount" column="amount"/>
    </collection>
</resultMap>
```

### 5.4.1 分步查询（延迟加载）

```xml
<resultMap id="userWithOrdersLazyResultMap" type="User">
    <id property="id" column="id"/>
    <result property="name" column="name"/>
    <collection property="orders"
                column="id"
                select="com.example.mapper.OrderMapper.selectByUserId"
                fetchType="lazy"/>
</resultMap>
```

---

## 六、动态 SQL

### 6.1 if 条件

```xml
<select id="selectUserByCondition" resultType="User">
    SELECT * FROM users WHERE 1=1
    <if test="name != null and name != ''">
        AND name = #{name}
    </if>
    <if test="age != null">
        AND age = #{age}
    </if>
</select>
```

### 6.2 choose/when/otherwise（相当于 switch-case）

```xml
<select id="selectUserByChoose" resultType="User">
    SELECT * FROM users
    <where>
        <choose>
            <when test="name != null and name != ''">
                AND name = #{name}
            </when>
            <when test="email != null and email != ''">
                AND email = #{email}
            </when>
            <otherwise>
                AND status = 1
            </otherwise>
        </choose>
    </where>
</select>
```

### 6.3 where 标签

自动处理 AND/OR 前缀：

```xml
<select id="selectUserByCondition" resultType="User">
    SELECT * FROM users
    <where>
        <if test="name != null">
            AND name = #{name}
        </if>
        <if test="age != null">
            AND age = #{age}
        </if>
    </where>
</select>
```

### 6.4 set 标签（用于更新）

```xml
<update id="updateUser">
    UPDATE users
    <set>
        <if test="name != null">name = #{name},</if>
        <if test="email != null">email = #{email},</if>
        <if test="age != null">age = #{age},</if>
    </set>
    WHERE id = #{id}
</update>
```

### 6.5 foreach 循环

```xml
<!-- IN 查询 -->
<select id="selectByIds" resultType="User">
    SELECT * FROM users WHERE id IN
    <foreach collection="ids" item="id" open="(" separator="," close=")">
        #{id}
    </foreach>
</select>

<!-- 批量插入 -->
<insert id="batchInsert">
    INSERT INTO users (name, email, age) VALUES
    <foreach collection="users" item="user" separator=",">
        (#{user.name}, #{user.email}, #{user.age})
    </foreach>
</insert>
```

### 6.6 trim 标签

自定义 SQL 片段处理：

```xml
<trim prefix="WHERE" prefixOverrides="AND | OR">
    <if test="name != null">AND name = #{name}</if>
</trim>
```

### 6.7 bind 标签

```xml
<select id="selectUserLikeName" resultType="User">
    <bind name="pattern" value="'%' + name + '%'"/>
    SELECT * FROM users WHERE name LIKE #{pattern}
</select>
```

---

## 七、缓存机制

### 7.1 一级缓存（默认开启）

- **作用域**：SqlSession 级别
- **生命周期**：SqlSession 关闭或清空时失效
- **说明**：同一个 SqlSession 中执行相同的 SQL，会从缓存中获取结果

### 7.2 二级缓存

需要手动配置，作用域是 Mapper 级别：

```xml
<!-- mybatis-config.xml 开启二级缓存 -->
<settings>
    <setting name="cacheEnabled" value="true"/>
</settings>

<!-- Mapper XML 中启用缓存 -->
<mapper namespace="com.example.mapper.UserMapper">
    <cache/>
    <!-- 或自定义配置 -->
    <cache eviction="LRU" flushInterval="60000" size="512" readOnly="true"/>
</mapper>
```

**缓存属性说明：**
- `eviction`：回收策略（LRU、FIFO、SOFT、WEAK）
- `flushInterval`：刷新间隔（毫秒）
- `size`：缓存对象数量
- `readOnly`：是否只读

### 7.3 缓存使用注意事项

- 实体类必须实现 `Serializable` 接口
- 更新操作默认刷新缓存
- 分布式环境下建议使用 Redis 等外部缓存

---

## 八、插件与拦截器

### 8.1 自定义拦截器

```java
@Intercepts({
    @Signature(type = Executor.class, method = "query",
               args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
})
public class MyInterceptor implements Interceptor {
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 前置处理
        System.out.println("执行前...");

        // 执行目标方法
        Object result = invocation.proceed();

        // 后置处理
        System.out.println("执行后...");

        return result;
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 配置属性
    }
}
```

### 8.2 注册拦截器

```xml
<plugins>
    <plugin interceptor="com.example.interceptor.MyInterceptor">
        <property name="someProperty" value="100"/>
    </plugin>
</plugins>
```

### 8.3 常用插件

- **PageHelper**：分页插件
- **MyBatis-Plus**：增强工具

---

## 九、MyBatis 与 Spring 集成

### 9.1 Spring Boot 集成

**依赖：**
```xml
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>3.0.2</version>
</dependency>
```

**配置文件 (application.yml)：**
```yaml
mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.example.model
  configuration:
    map-underscore-to-camel-case: true
    cache-enabled: true

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/test
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### 9.2 Mapper 扫描配置

```java
@MapperScan("com.example.mapper")
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 9.3 Mapper 接口

```java
@Mapper
public interface UserMapper {
    @Select("SELECT * FROM users WHERE id = #{id}")
    User selectById(Integer id);
}
```

### 9.4 注解方式 vs XML 方式

| 方式 | 优点            | 缺点            | 适用场景          |
|------|-----------------|-----------------|-------------------|
| 注解 | 简洁方便        | 复杂 SQL 不友好 | 简单 CRUD         |
| XML  | 灵活、SQL 易维护 | 配置稍繁琐      | 复杂 SQL、动态 SQL |

---

## 十、MyBatis-Plus 增强

### 10.1 简介

MyBatis-Plus 是 MyBatis 的增强工具，简化开发：

```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
    <version>3.5.4</version>
</dependency>
```

### 10.2 BaseMapper 内置方法

```java
public interface UserMapper extends BaseMapper<User> {
    // 自动拥有以下方法：
    // selectById, selectBatchIds, selectByMap
    // insert, updateById, deleteById, deleteBatchIds
    // selectList, selectPage, selectCount 等
}
```

### 10.3 条件构造器

```java
// 查询
QueryWrapper<User> wrapper = new QueryWrapper<>();
wrapper.eq("name", "张三")
       .gt("age", 18)
       .orderByDesc("create_time");
List<User> users = userMapper.selectList(wrapper);

// 更新
UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
updateWrapper.eq("id", 1).set("name", "李四");
userMapper.update(null, updateWrapper);
```

### 10.4 Service 层封装

```java
public interface UserService extends IService<User> {
    // 自动拥有 CRUD 方法
}

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {
}
```

---

## 十一、最佳实践

### 11.1 命名规范

- Mapper 接口命名：`XxxMapper`
- XML 文件命名：`XxxMapper.xml`
- namespace：完整包名 + Mapper 接口名

### 11.2 配置优化

```yaml
mybatis:
  configuration:
    map-underscore-to-camel-case: true  # 驼峰映射
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl  # SQL 日志
    default-executor-type: SIMPLE  # 执行器类型
```

### 11.3 批量操作

```java
// 使用 ExecutorType.BATCH
try (SqlSession session = sqlSessionFactory.openSession(ExecutorType.BATCH)) {
    UserMapper mapper = session.getMapper(UserMapper.class);
    for (User user : users) {
        mapper.insert(user);
    }
    session.commit();
}
```

### 11.4 分页实现

```java
// 使用 PageHelper
PageHelper.startPage(pageNum, pageSize);
List<User> users = userMapper.selectAll();
PageInfo<User> pageInfo = new PageInfo<>(users);
```

### 11.5 事务管理

```java
@Transactional
public void createUser(User user) {
    userMapper.insert(user);
    // 其他操作...
}
```

---

## 十二、常见问题与解决方案

### 12.1 字段名与属性名不一致

**解决方案：** 使用 resultMap 或开启驼峰映射

### 12.2 SQL 注入风险

**解决方案：** 优先使用 `#{}`，避免使用 `${}`

### 12.3 空指针异常

**解决方案：** 动态 SQL 中添加 null 判断

### 12.4 缓存不一致

**解决方案：** 合理设置缓存刷新策略或使用分布式缓存

---

## 十三、总结对比表

| 特性     | MyBatis           | MyBatis-Plus       |
|----------|-------------------|--------------------|
| SQL 编写 | 手动编写          | 简单操作自动生成   |
| CRUD     | 需要配置          | 内置 BaseMapper    |
| 分页     | 需插件            | 内置分页           |
| 条件构造 | 动态 SQL          | LambdaQueryWrapper |
| 代码生成 | 需第三方          | 内置代码生成器     |
| 学习成本 | 低                | 低                 |
| 适用场景 | 复杂 SQL 多的项目 | 快速开发、CRUD 为主 |

---

## 十四、参考资源

- **官方文档**：https://mybatis.org/mybatis-3/
- **MyBatis-Plus 官方**：https://baomidou.com/
- **GitHub**：https://github.com/mybatis/mybatis-3
- **Spring Boot 集成**：https://mybatis.org/spring-boot-starter/

---
