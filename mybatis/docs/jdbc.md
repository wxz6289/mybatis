# JDBC 数据库操作：核心要点与实践指南

JDBC（Java Database Connectivity）是 Java 访问关系型数据库的标准 API。理解「谁负责什么、资源如何关闭、如何避免 SQL 注入与连接泄漏」后，再使用 Spring `JdbcTemplate`、MyBatis 等框架会更稳。

---

## 1. JDBC 在整体中的位置

| 层次 | 说明 |
|------|------|
| **应用代码** | 业务逻辑、事务边界 |
| **JDBC API** | `Connection` / `Statement` / `PreparedStatement` / `ResultSet` |
| **JDBC 驱动** | 各数据库厂商实现（如 MySQL Connector/J） |
| **数据库** | MySQL、PostgreSQL 等 |

框架（Spring JDBC、MyBatis）本质上仍通过 JDBC 与驱动通信；**资源与事务规则不变**。

---

## 2. 核心类型与职责（必须掌握）

| 类型 | 职责 |
|------|------|
| **`Driver` / `DriverManager`** | 根据 JDBC URL 加载驱动、建立连接（现代应用更常用 `DataSource`） |
| **`DataSource`** | 连接工厂；生产环境几乎都用连接池实现（如 HikariCP） |
| **`Connection`** | 到数据库的一条会话；控制事务（`commit` / `rollback`） |
| **`Statement`** | 执行静态 SQL；**不推荐**用于带用户输入的场景 |
| **`PreparedStatement`** | 预编译 + 参数绑定；**防 SQL 注入的首选** |
| **`CallableStatement`** | 调用存储过程 |
| **`ResultSet`** | 遍历查询结果；有游标与列访问 API |

**一句话**：用 `DataSource` 拿 `Connection`，用 `PreparedStatement` 绑参执行，用 `ResultSet` 读行，**用完立刻关闭**（或交给 try-with-resources）。

---

## 3. 典型工作流程

1. 注册/加载驱动（MySQL 8+ 通常可省略显式 `Class.forName`，驱动 JAR 在 classpath 即可）。  
2. 通过 `DataSource` 或 `DriverManager.getConnection(url, user, password)` 获取 `Connection`。  
3. 创建 `PreparedStatement`，用 `setString` / `setInt` 等绑定参数。  
4. 执行：`executeQuery`（查询）或 `executeUpdate`（增删改）。  
5. 若是查询，遍历 `ResultSet`。  
6. 若在事务中，显式 `commit` 或 `rollback`。  
7. **按相反顺序关闭**：`ResultSet` → `Statement` → `Connection`（try-with-resources 自动处理）。

---

## 4. 连接 URL 与驱动（实践要点）

### 4.1 MySQL 示例 URL

```text
jdbc:mysql://主机:3306/数据库名?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false
```

- **`serverTimezone`**：避免旧驱动与时区告警。  
- **`useSSL`**：生产环境一般应开启 SSL 并正确配置证书，本地可临时关闭。  
- **`allowPublicKeyRetrieval`**：部分环境连接 MySQL 8 认证时需要，按安全策略评估。

### 4.2 驱动类名（MySQL Connector/J）

```text
com.mysql.cj.jdbc.Driver
```

Spring Boot 在 classpath 上仅有 MySQL 驱动时，常可省略显式 `driver-class-name`，由自动配置推断。

---

## 5. `PreparedStatement`：核心中的核心

### 5.1 为什么必须用预编译 + 占位符

**错误示例（字符串拼接，存在 SQL 注入）**：

```java
String sql = "SELECT * FROM users WHERE name = '" + userName + "'";
```

**正确示例（占位符 `?`）**：

```java
String sql = "SELECT id, name FROM users WHERE name = ?";
try (PreparedStatement ps = conn.prepareStatement(sql)) {
    ps.setString(1, userName);
    try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
            long id = rs.getLong("id");
            String name = rs.getString("name");
        }
    }
}
```

### 5.2 `executeQuery` 与 `executeUpdate`

| 方法 | 用途 | 返回值 |
|------|------|--------|
| `executeQuery()` | `SELECT` | `ResultSet` |
| `executeUpdate()` | `INSERT` / `UPDATE` / `DELETE` / DDL | 受影响行数（`int`） |
| `execute()` | 通用，较少在业务中直接使用 | 是否返回 `ResultSet` 需再判断 |

### 5.3 `ResultSet` 访问习惯

- 列名：`rs.getLong("id")`（可读性好，改名需注意 SQL 别名）。  
- 列索引：`rs.getString(1)`（从 1 开始，易错但略省开销）。  
- 可空列：配合 `wasNull()` 或先判 `getObject` 是否为 null。

---

## 6. 事务（Transaction）

### 6.1 默认行为

- 新 `Connection` 默认 **`autoCommit = true`**：每条语句一个事务。  
- 多语句要原子性时，必须关闭自动提交，手动 `commit` / `rollback`。

### 6.2 典型事务代码骨架

```java
conn.setAutoCommit(false);
try {
    // 多条 executeUpdate / executeQuery
    conn.commit();
} catch (Exception e) {
    conn.rollback();
    throw e;
} finally {
    conn.setAutoCommit(true); // 归还连接池前建议恢复，避免污染池内连接状态
}
```

### 6.3 隔离级别（了解即可）

`Connection.setTransactionIsolation(int level)` 可设置读未提交、读已提交、可重复读、串行化等。  
实际项目中常与 Spring `@Transactional` 或数据源配置一起管理。

---

## 7. 资源管理：必须 try-with-resources

JDBC 对象实现 `AutoCloseable`，**务必**使用 try-with-resources，避免连接耗尽：

```java
try (Connection conn = dataSource.getConnection();
     PreparedStatement ps = conn.prepareStatement(sql)) {
    ps.setInt(1, id);
    try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
            return rs.getString("name");
        }
    }
}
return null;
```

**反模式**：只 `close()` 在 `finally` 里手写多层 if-null，易漏关 `ResultSet`。

---

## 8. 批量与性能（进阶实践）

### 8.1 批量更新

```java
ps.setString(1, "a");
ps.addBatch();
ps.setString(1, "b");
ps.addBatch();
int[] counts = ps.executeBatch();
```

注意：批量与事务结合时，通常在**同一事务**内 `executeBatch` 再 `commit`。

### 8.2 大结果集

- 默认可能一次性拉很多行到客户端；大数据量时用 `Statement.setFetchSize` 等（与驱动、游标类型有关）。  
- 报表类场景可考虑流式读取或分页 SQL（`LIMIT` / keyset pagination）。

---

## 9. 异常与诊断

| 类型 | 含义 |
|------|------|
| **`SQLException`** | SQL 错误、连接失败、超时等；可 `getErrorCode()` / `getSQLState()` |
| **`SQLTimeoutException`** | 查询或连接超时 |

实践：日志中记录 **SQL 状态码、绑定参数摘要（脱敏）**、**耗时**，便于排障。

---

## 10. 与「本仓库 Spring Boot」的衔接

本项目的 `application.yaml` 已配置 `spring.datasource` 与 HikariCP。在 Spring 中：

- **不要**在业务代码里到处 `new` 连接；注入 `DataSource` 或直接用 `JdbcTemplate` / MyBatis。  
- 事务优先用 **`@Transactional`**，由容器管理 `Connection` 与提交时机。  
- 手写 JDBC 时仍遵守：**预编译、关资源、事务边界清晰**。

---

## 11. 最佳实践清单（Checklist）

- [ ] 对外部输入一律使用 **`PreparedStatement` 占位符**，禁止拼接 SQL。  
- [ ] 所有 JDBC 资源使用 **try-with-resources**。  
- [ ] 多步写操作使用 **事务**，失败 **`rollback`**。  
- [ ] 连接来自 **连接池**（生产），避免每次 `DriverManager` 裸连。  
- [ ] SQL、参数、耗时、错误码写入日志，**敏感信息脱敏**。  
- [ ] 明确字符集与时区（尤其 MySQL URL 参数）。  
- [ ] 需要时设置 **超时**（`Statement.setQueryTimeout` 等），防止拖垮线程池。

---

## 12. 最小完整示例（仅作本地理解用）

```java
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserRepository {

    private final DataSource dataSource;

    public UserRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public String findNameById(long id) throws Exception {
        String sql = "SELECT name FROM users WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("name") : null;
            }
        }
    }
}
```

---

## 13. 一句话总结

**JDBC 的核心实践**可以概括为：**用连接池拿连接、用 `PreparedStatement` 绑参、用 try-with-resources 关资源、用事务包住多步写操作**。框架只是把这些步骤做得更安全、更省事；底层原则不变。
