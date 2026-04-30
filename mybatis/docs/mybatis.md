# MyBatis 预编译SQL与参数占位符详解

## 1. 预编译SQL（Prepared Statement）

### 1.1 什么是预编译SQL？
预编译SQL是数据库提供的一种机制，它将SQL语句的执行分为两个阶段：
- **预编译阶段**：数据库解析SQL语句结构，生成执行计划
- **执行阶段**：将具体参数传递给已预编译的SQL语句执行

### 1.2 预编译的优势
- **防止SQL注入**：参数不会被当作SQL代码执行
- **性能优化**：相同结构的SQL只需编译一次，可重复使用执行计划
- **类型安全**：参数会进行类型检查和自动转换
- **减少解析开销**：避免了每次执行都重新解析SQL语句

## 2. `#{}` 和 `${}` 的核心区别

### 2.1 `#{}` - 预编译参数占位符（推荐使用）

#### 特点：
- 使用JDBC的PreparedStatement机制
- 生成`?`占位符，参数通过set方法传递
- 自动进行参数转义和类型处理
- **有效防止SQL注入攻击**

#### 示例：
```java
@Select("SELECT * FROM user WHERE id = #{id} AND name = #{name}")
User findUser(@Param("id") Long id, @Param("name") String name);

@Select("SELECT * FROM user LIMIT #{offset}, #{size}")
List<User> listPage(@Param("offset") int offset, @Param("size") int size);
```

#### 实际执行的SQL：
```sql
-- MyBatis会将 #{ } 替换为 ?
SELECT * FROM user WHERE id = ? AND name = ?
SELECT * FROM user LIMIT ?, ?
```

### 2.2 `${}` - 字符串直接替换（谨慎使用）

#### 特点：
- 直接进行字符串拼接，不经过预编译
- 参数原样插入到SQL语句中
- **存在SQL注入风险**
- 适用于动态表名、列名等场景

#### 示例：
```java
// 动态排序字段（需要手动校验安全性）
@Select("SELECT * FROM user ORDER BY ${orderByField} DESC")
List<User> listUsers(@Param("orderByField") String orderByField);

// 动态表名（需要严格控制）
@Select("SELECT * FROM ${tableName} WHERE id = #{id}")
Object findById(@Param("tableName") String tableName, @Param("id") Long id);
```

#### 实际执行的SQL：
```sql
-- ${ } 直接替换为变量值
SELECT * FROM user ORDER BY created_at DESC  -- 如果orderByField="created_at"
SELECT * FROM user_backup WHERE id = 1       -- 如果tableName="user_backup"
```

## 3. 对比分析表

| 对比维度 | `#{}` | `${}` |
|---------|-------|-------|
| **处理方式** | PreparedStatement预编译 | 字符串直接替换 |
| **SQL注入防护** | ✅ 完全防护 | ❌ 有风险 |
| **性能表现** | 更好（可复用执行计划） | 较差（每次重新编译） |
| **参数转义** | 自动转义特殊字符 | 不转义，原样插入 |
| **适用场景** | WHERE条件、INSERT值、UPDATE值等 | 动态表名、列名、ORDER BY字段 |
| **生成的SQL** | `WHERE id = ?` | `ORDER BY name DESC` |
| **安全性** | 高 | 低（需人工校验） |

## 4. 使用场景指南

### 4.1 推荐使用 `#{}` 的场景：
```java
// ✅ WHERE 条件
@Select("SELECT * FROM user WHERE age > #{minAge}")
List<User> findByAge(@Param("minAge") int minAge);

// ✅ INSERT 语句
@Insert("INSERT INTO user(name, email) VALUES(#{name}, #{email})")
void insertUser(@Param("name") String name, @Param("email") String email);

// ✅ UPDATE 语句
@Update("UPDATE user SET name = #{name} WHERE id = #{id}")
void updateUser(@Param("id") Long id, @Param("name") String name);

// ✅ LIKE 查询（配合CONCAT）
@Select("SELECT * FROM user WHERE name LIKE CONCAT('%', #{keyword}, '%')")
List<User> searchByName(@Param("keyword") String keyword);
```

### 4.2 谨慎使用 `${}` 的场景：
```java
// ⚠️ 动态排序（必须校验字段合法性）
@Select("SELECT * FROM user ORDER BY ${sortField} ${sortOrder}")
List<User> listSorted(@Param("sortField") String sortField, 
                      @Param("sortOrder") String sortOrder);

// ⚠️ 动态表名（必须白名单控制）
@Select("SELECT COUNT(*) FROM ${tableName}")
long countTable(@Param("tableName") String tableName);

// ⚠️ 动态列名（必须严格校验）
@Select("SELECT ${columnName} FROM user WHERE id = #{id}")
Object getColumnValue(@Param("columnName") String columnName, @Param("id") Long id);
```

## 5. 安全防护最佳实践

### 5.1 使用 `${}` 时的安全措施：
```java
// 方案1：白名单校验
public List<User> listSorted(String sortField, String sortOrder) {
    // 定义允许的排序字段
    List<String> allowedFields = Arrays.asList("name", "age", "created_at");
    if (!allowedFields.contains(sortField)) {
        throw new IllegalArgumentException("Invalid sort field");
    }
    
    // 限定排序方向
    if (!"ASC".equalsIgnoreCase(sortOrder) && !"DESC".equalsIgnoreCase(sortOrder)) {
        sortOrder = "DESC";
    }
    
    return userMapper.listSorted(sortField, sortOrder);
}

// 方案2：正则表达式校验
if (!sortField.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
    throw new IllegalArgumentException("Invalid field name");
}
```

### 5.2 绝对禁止的做法：
```java
// ❌ 危险：直接将用户输入拼接到SQL中
@Select("SELECT * FROM user WHERE name = '${name}'")
List<User> unsafeFind(@Param("name") String name);  // SQL注入风险！

// ❌ 危险：未校验的动态表名
@Select("SELECT * FROM ${userProvidedTable}")
List<Object> unsafeQuery(@Param("userProvidedTable") String table);  // 极高风险！
```

## 6. 实际案例分析

### 6.1 项目中的正确使用（来自UserMapper）：
```java
// ✅ 正确使用 #{ } 进行分页查询
@Select("SELECT * FROM user ORDER BY createdAt DESC LIMIT #{offset}, #{size}")
List<User> listPage(@Param("offset") int offset, @Param("size") int size);

// ✅ 正确使用 #{ } 进行删除操作
@Select("DELETE FROM user WHERE id = #{id}")
void removeUser(@Param("id") long id);
```

### 6.2 SQL注入攻击示例：
假设使用`${}`且未做校验：
```java
// 恶意输入：name = "' OR '1'='1"
@Select("SELECT * FROM user WHERE name = '${name}'")
// 生成的SQL：SELECT * FROM user WHERE name = '' OR '1'='1'
// 结果：返回所有用户数据！
```

使用`#{}`则安全：
```java
// 同样的恶意输入会被当作普通字符串处理
@Select("SELECT * FROM user WHERE name = #{name}")
// 生成的SQL：SELECT * FROM user WHERE name = ?
// 参数："' OR '1'='1" （作为字符串值，不会改变SQL逻辑）
```

## 7. 总结与建议

### 7.1 核心原则：
1. **默认使用 `#{}`**：95%的场景都应该使用`#{}`
2. **慎用 `${}`**：仅在必要时使用，且必须做好安全校验
3. **永远不要信任用户输入**：任何外部输入都需要验证和过滤

### 7.2 选择决策树：
```
需要使用参数？
├─ 是参数值（WHERE、INSERT、UPDATE等） → 使用 #{ }
└─ 是SQL结构（表名、列名、ORDER BY等）
   ├─ 可以枚举所有可能值 → 使用白名单 + ${ }
   └─ 无法预知所有值 → 重新设计业务逻辑，避免动态SQL结构
```

### 7.3 记忆口诀：
- **井号预编译防注入，美元拼接要谨慎**
- **参数取值用井号，表名列名美元替**
- **安全第一记心间，白名单校验不能少**

## 8. MyBatis可选参数传递详解

### 8.1 为什么需要可选参数？

在实际开发中，我们经常遇到多条件查询场景，用户可能只填写部分条件：
- 只按姓名搜索
- 只按年龄范围搜索  
- 同时按姓名和年龄搜索
- 不带任何条件（查询全部）

### 8.2 方案对比

#### 方案1：使用对象封装 + XML动态SQL（⭐ 强烈推荐）

**优点：**
- 参数清晰，易于维护
- 支持任意组合的可选条件
- SQL逻辑集中在XML中，便于优化
- 可以灵活添加分页、排序等

**实现步骤：**

**Step 1: 创建查询条件对象**
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserQuery {
    private String name;      // 姓名（模糊查询）
    private Integer startAge; // 起始年龄
    private Integer endAge;   // 结束年龄
    private Integer offset;   // 分页偏移量
    private Integer size;     // 每页大小
}
```

**Step 2: Mapper接口定义**

⚠️ **重要提示**：如果使用 `@Param` 注解，需要在XML中通过参数名访问属性；如果不使用 `@Param`，则直接访问属性。

```java
@Mapper
public interface UserMapper {
    /**
     * 方式1：不使用@Param（推荐）
     * XML中直接使用属性名：#{name}, #{startAge}
     */
    List<User> list(UserQuery query);
    
    /**
     * 方式2：使用@Param
     * XML中需要通过参数名访问：#{query.name}, #{query.startAge}
     */
    List<User> listWithParam(@Param("query") UserQuery query);
}
```

**方式1的XML（不使用@Param）：**
```xml
<select id="list" resultMap="userResultMap">
    SELECT * FROM user
    <where>
        <if test="name != null and name != ''">
            AND name LIKE CONCAT('%', #{name}, '%')
        </if>
        <if test="startAge != null">
            AND age &gt;= #{startAge}
        </if>
        <if test="endAge != null">
            AND age &lt;= #{endAge}
        </if>
    </where>
    ORDER BY createdAt DESC
    <if test="offset != null and size != null">
        LIMIT #{offset}, #{size}
    </if>
</select>
```

**方式2的XML（使用@Param）：**
```xml
<select id="listWithParam" resultMap="userResultMap">
    SELECT * FROM user
    <where>
        <!-- 注意：需要使用 query. 前缀 -->
        <if test="query.name != null and query.name != ''">
            AND name LIKE CONCAT('%', #{query.name}, '%')
        </if>
        <if test="query.startAge != null">
            AND age &gt;= #{query.startAge}
        </if>
        <if test="query.endAge != null">
            AND age &lt;= #{query.endAge}
        </if>
    </where>
    ORDER BY createdAt DESC
    <if test="query.offset != null and query.size != null">
        LIMIT #{query.offset}, #{query.size}
    </if>
</select>
```

**Step 4: 调用示例**
```java
// 查询所有用户
UserQuery query1 = new UserQuery();
List<User> allUsers = userMapper.list(query1);

// 只按姓名查询
UserQuery query2 = new UserQuery();
query2.setName("张三");
List<User> usersByName = userMapper.list(query2);

// 只按年龄范围查询
UserQuery query3 = new UserQuery();
query3.setStartAge(20);
query3.setEndAge(30);
List<User> usersByAge = userMapper.list(query3);

// 组合查询 + 分页
UserQuery query4 = new UserQuery();
query4.setName("李");
query4.setStartAge(25);
query4.setOffset(0);
query4.setSize(10);
List<User> users = userMapper.list(query4);
```

**生成的SQL示例：**
```sql
-- query1: 无条件
SELECT * FROM user ORDER BY createdAt DESC

-- query2: 只有姓名
SELECT * FROM user WHERE name LIKE '%张三%' ORDER BY createdAt DESC

-- query3: 只有年龄范围  
SELECT * FROM user WHERE age >= 20 AND age <= 30 ORDER BY createdAt DESC

-- query4: 组合条件 + 分页
SELECT * FROM user WHERE name LIKE '%李%' AND age >= 25 ORDER BY createdAt DESC LIMIT 0, 10
```

---

#### 方案2：注解方式 + `<script>`标签（适合简单场景）

**优点：**
- 不需要XML文件
- 适合简单的动态SQL

**缺点：**
- SQL写在注解中，复杂时难以阅读
- 特殊字符需要转义（如 `>` 写成 `&gt;`）

**实现：**
```java
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
```

**调用示例：**
```java
// 某些参数传null表示不使用该条件
List<User> users = userMapper.listWithAnnotation("张三", null, null);
List<User> users2 = userMapper.listWithAnnotation(null, 20, 30);
```

---

#### 方案3：使用Java代码构建SQL（SQL Provider）

**优点：**
- 完全使用Java代码，类型安全
- 适合非常复杂的动态SQL

**缺点：**
- 代码量较大
- 需要额外的Provider类

**实现：**
```java
public class UserSqlProvider {
    
    public String listWithProvider(UserQuery query) {
        SQL sql = new SQL();
        sql.SELECT("*");
        sql.FROM("user");
        
        if (query.getName() != null && !query.getName().isEmpty()) {
            sql.WHERE("name LIKE CONCAT('%', #{name}, '%')");
        }
        if (query.getStartAge() != null) {
            sql.WHERE("age >= #{startAge}");
        }
        if (query.getEndAge() != null) {
            sql.WHERE("age <= #{endAge}");
        }
        
        sql.ORDER_BY("createdAt DESC");
        
        if (query.getOffset() != null && query.getSize() != null) {
            // 注意：LIMIT不能直接用WHERE，需要特殊处理
        }
        
        return sql.toString();
    }
}

@Mapper
public interface UserMapper {
    @SelectProvider(type = UserSqlProvider.class, method = "listWithProvider")
    List<User> listWithProvider(UserQuery query);
}
```

---

### 8.3 动态SQL常用标签

| 标签 | 说明 | 示例 |
|------|------|------|
| `<if>` | 条件判断 | `<if test="name != null">...</if>` |
| `<where>` | 智能添加WHERE关键字，去除多余AND/OR | `<where><if test="...">...</if></where>` |
| `<set>` | 用于UPDATE，智能添加SET关键字 | `<set><if test="...">...</if></set>` |
| `<foreach>` | 循环遍历集合 | `<foreach collection="ids" item="id">...</foreach>` |
| `<choose>` | 多选一（类似switch） | `<choose><when>...</when><otherwise>...</otherwise></choose>` |
| `<trim>` | 自定义前后缀处理 | `<trim prefix="WHERE" prefixOverrides="AND">...</trim>` |

### 8.4 实战技巧

#### 技巧1：空字符串处理
```xml
<!-- 同时判断null和空字符串 -->
<if test="name != null and name != ''">
    AND name LIKE CONCAT('%', #{name}, '%')
</if>
```

#### 技巧2：IN查询
```xml
<select id="findByIds" resultMap="userResultMap">
    SELECT * FROM user
    WHERE id IN
    <foreach collection="ids" item="id" open="(" separator="," close=")">
        #{id}
    </foreach>
</select>
```

```java
List<User> findByIds(@Param("ids") List<Long> ids);
```

#### 技巧3：批量插入
```xml
<insert id="batchInsert">
    INSERT INTO user (name, age, deptId) VALUES
    <foreach collection="users" item="user" separator=",">
        (#{user.name}, #{user.age}, #{user.deptId})
    </foreach>
</insert>
```

```java
void batchInsert(@Param("users") List<User> users);
```

#### 技巧4：动态UPDATE
```xml
<update id="updateUser">
    UPDATE user
    <set>
        <if test="name != null">
            name = #{name},
        </if>
        <if test="age != null">
            age = #{age},
        </if>
        <if test="deptId != null">
            deptId = #{deptId},
        </if>
    </set>
    WHERE id = #{id}
</update>
```

### 8.5 最佳实践总结

1. **优先使用方案1（对象+XML）**：最灵活、最易维护
2. **使用包装类型**：用`Integer`而不是`int`，这样可以区分0和null
3. **合理使用`<where>`标签**：自动处理AND/OR和WHERE关键字
4. **注意特殊字符转义**：XML中 `>` 写成 `&gt;`，`<` 写成 `&lt;`
5. **避免过度动态**：如果条件固定，直接用静态SQL性能更好
6. **考虑添加索引**：动态查询的字段应该有合适的数据库索引

### 8.6 常见错误与解决方案

#### 错误1：Parameter 'xxx' not found

**错误信息：**
```
org.apache.ibatis.binding.BindingException: 
Parameter 'name' not found. Available parameters are [query, param1]
```

**原因：**
- Mapper接口使用了 `@Param("query")` 注解
- 但XML中直接使用了 `#{name}` 而不是 `#{query.name}`

**解决方案：**
```java
// Mapper接口
List<User> list(@Param("query") UserQuery query);
```

```xml
<!-- ❌ 错误写法 -->
<if test="name != null">
    AND name LIKE #{name}
</if>

<!-- ✅ 正确写法 -->
<if test="query.name != null">
    AND name LIKE #{query.name}
</if>
```

**或者去掉@Param注解：**
```java
// Mapper接口（不使用@Param）
List<User> list(UserQuery query);
```

```xml
<!-- XML中直接使用属性名 -->
<if test="name != null">
    AND name LIKE #{name}
</if>
```

---

#### 错误2：使用了废弃的parameterMap

**错误写法：**
```xml
<parameterMap id="query" type="com.dk.learn.entity.UserQuery"/>
<select id="list" parameterMap="query">
    ...
</select>
```

**说明：**
- `parameterMap` 是MyBatis旧版本的用法，已废弃
- 应该直接使用 `parameterType` 或不指定（MyBatis会自动推断）

**正确写法：**
```xml
<!-- 方式1：不指定parameterType（推荐） -->
<select id="list" resultMap="userResultMap">
    ...
</select>

<!-- 方式2：指定parameterType -->
<select id="list" resultMap="userResultMap" parameterType="com.dk.learn.entity.UserQuery">
    ...
</select>
```

---

#### 错误3：test表达式和#{}不一致

**错误示例：**
```xml
<!-- test中用了query.name，但#{}中用了name -->
<if test="query.name != null">
    AND name LIKE #{name}  <!-- ❌ 错误！ -->
</if>
```

**正确示例：**
```xml
<!-- 保持一致 -->
<if test="query.name != null">
    AND name LIKE #{query.name}  <!-- ✅ 正确 -->
</if>
```

---

#### 错误4：基本类型vs包装类型

**问题：**
```java
// 使用基本类型 int
private int startAge;
```

当 `startAge = 0` 时，`<if test="startAge != null">` 永远为true，无法实现可选参数。

**解决：**
```java
// 使用包装类型 Integer
private Integer startAge;
```

这样可以通过 `null` 来判断是否传入该参数。

## 8. MyBatis 动态 SQL 详解

动态 SQL 的核心目标是：根据参数条件动态拼接 SQL 片段，避免在 Java 代码中手写字符串拼接。  
常见于“可选查询条件、多字段更新、批量 IN 查询、复杂排序过滤”等场景。

### 8.1 动态 SQL 的常用标签总览

| 标签 | 作用 | 典型场景 |
|------|------|----------|
| `<if>` | 条件成立才拼接 SQL | 按需追加 `WHERE` 条件 |
| `<choose>/<when>/<otherwise>` | 多分支二选一/多选一 | 按优先级选择查询条件 |
| `<where>` | 自动处理 `WHERE` 和前导 `AND/OR` | 多条件组合查询 |
| `<set>` | 自动处理 `UPDATE SET` 逗号 | 动态更新非空字段 |
| `<trim>` | 自定义前后缀和裁剪规则 | 手动构建复杂片段 |
| `<foreach>` | 遍历集合生成 SQL | `IN (...)`、批量插入 |
| `<bind>` | 绑定 OGNL 表达式结果到变量 | 模糊查询关键字拼接 |
| `<script>` | 注解方式下启用动态 SQL | `@Select/@Update` 中使用 XML 风格标签 |

### 8.2 `<if>`：最基础的条件拼接

```xml
<select id="queryUsers" resultType="User">
  SELECT id, name, age, status
  FROM user
  WHERE 1 = 1
  <if test="name != null and name != ''">
    AND name = #{name}
  </if>
  <if test="minAge != null">
    AND age >= #{minAge}
  </if>
  <if test="status != null">
    AND status = #{status}
  </if>
</select>
```

说明：
- `test` 使用 OGNL 表达式，可访问参数对象字段、Map 键、`@Param` 名称。
- `WHERE 1=1` 虽常见，但更推荐配合 `<where>`，可读性更好。

### 8.3 `<where>` 和 `<trim>`：解决前导 `AND/OR`

`<where>` 会在有内容时自动添加 `WHERE`，并自动去除最前面的 `AND` / `OR`。

```xml
<select id="queryUsers2" resultType="User">
  SELECT * FROM user
  <where>
    <if test="name != null and name != ''">
      AND name = #{name}
    </if>
    <if test="email != null and email != ''">
      AND email = #{email}
    </if>
  </where>
</select>
```

`<trim>` 是更通用版本，可自定义规则：

```xml
<trim prefix="WHERE" prefixOverrides="AND |OR ">
  ...
</trim>
```

### 8.4 `<set>`：动态更新推荐写法

```xml
<update id="updateUserSelective">
  UPDATE user
  <set>
    <if test="name != null">name = #{name},</if>
    <if test="email != null">email = #{email},</if>
    <if test="age != null">age = #{age},</if>
    <if test="status != null">status = #{status},</if>
  </set>
  WHERE id = #{id}
</update>
```

说明：
- `<set>` 会自动补 `SET`，并清理最后一个多余逗号。
- 特别适合“按传入字段部分更新”的接口（Selective Update）。

### 8.5 `<choose>`：多条件优先级选择

```xml
<select id="queryByPriority" resultType="User">
  SELECT * FROM user
  <where>
    <choose>
      <when test="id != null">
        id = #{id}
      </when>
      <when test="email != null and email != ''">
        email = #{email}
      </when>
      <otherwise>
        status = 'ACTIVE'
      </otherwise>
    </choose>
  </where>
</select>
```

说明：
- 类似 Java 的 `if ... else if ... else`。
- 只会进入第一个满足条件的分支，避免多个条件同时生效导致结果异常。

### 8.6 `<foreach>`：集合遍历与批量 SQL

#### 场景1：`IN` 条件

```xml
<select id="listByIds" resultType="User">
  SELECT * FROM user
  WHERE id IN
  <foreach collection="ids" item="id" open="(" separator="," close=")">
    #{id}
  </foreach>
</select>
```

#### 场景2：批量插入

```xml
<insert id="batchInsertUsers">
  INSERT INTO user(name, email, age)
  VALUES
  <foreach collection="list" item="u" separator=",">
    (#{u.name}, #{u.email}, #{u.age})
  </foreach>
</insert>
```

说明：
- `collection` 常见取值：`list`、`array`、`ids`（取决于参数命名）。
- 空集合要提前处理，否则可能生成非法 SQL（如 `IN ()`）。

### 8.7 `<bind>`：预绑定变量，简化表达式

```xml
<select id="searchByKeyword" resultType="User">
  <bind name="kw" value="'%' + keyword + '%'" />
  SELECT * FROM user
  WHERE name LIKE #{kw}
</select>
```

说明：
- 用于把表达式结果绑定成局部变量，后续可用 `#{}` 安全引用。
- 比直接 `${}` 拼接更安全，推荐用于模糊查询参数处理。

### 8.8 注解方式动态 SQL：`<script>`

```java
@Select({
    "<script>",
    "SELECT * FROM user",
    "<where>",
    "  <if test='name != null and name != \"\"'>",
    "    AND name = #{name}",
    "  </if>",
    "  <if test='status != null'>",
    "    AND status = #{status}",
    "  </if>",
    "</where>",
    "</script>"
})
List<User> query(@Param("name") String name, @Param("status") Integer status);
```

说明：
- 注解复杂度高时可读性较差，复杂 SQL 仍建议使用 XML Mapper。
- 注解写法中的引号转义要特别小心（`\"\"`）。

### 8.9 动态 SQL 与 `#{}` / `${}` 的关系

- 动态 SQL 标签用于“控制 SQL 结构是否出现”。
- 真正填充值时优先 `#{}`，保持预编译与防注入能力。
- `${}` 仅用于无法参数化的结构片段（动态列名、动态排序、动态表名），并且必须白名单校验。

### 8.10 最佳实践与常见坑

最佳实践：
1. 复杂查询优先 XML 动态 SQL，不要在 Java 里做字符串拼接。
2. 所有参数值一律使用 `#{}`；`${}` 只给结构字段，且配白名单。
3. `WHERE` 条件统一用 `<where>`，更新语句统一用 `<set>`。
4. `IN` 查询前先判空集合；必要时直接返回空结果，避免无效 SQL。
5. 为动态 SQL 编写“参数组合测试”，覆盖空值、边界值、全条件和无条件场景。

常见坑：
- 忘记处理空字符串：导致条件误入（例如 `"   "`）。
- `foreach` 的 `collection` 名称写错：运行时报参数绑定异常。
- `choose` 使用不当：以为会叠加条件，实际只命中一个分支。
- 动态排序直接拼 `${sortField}`：未校验会造成 SQL 注入风险。

### 8.11 一段完整示例（条件查询 + 动态排序）

```xml
<select id="pageQuery" resultType="User">
  SELECT id, name, age, status, created_at
  FROM user
  <where>
    <if test="name != null and name != ''">
      AND name LIKE CONCAT('%', #{name}, '%')
    </if>
    <if test="minAge != null">
      AND age &gt;= #{minAge}
    </if>
    <if test="maxAge != null">
      AND age &lt;= #{maxAge}
    </if>
    <if test="status != null">
      AND status = #{status}
    </if>
  </where>
  ORDER BY ${sortField} ${sortOrder}
  LIMIT #{offset}, #{size}
</select>
```

配套建议：
- `sortField`、`sortOrder` 在 Service 层做白名单校验后再传入 Mapper。
- 其他筛选值全部走 `#{}`。

### 8.12 小结

- 动态 SQL 解决的是“SQL 结构随条件变化”的问题。
- `#{}` 解决的是“参数安全与预编译”的问题。
- 二者配合是 MyBatis 最常见、最稳妥的企业级写法。