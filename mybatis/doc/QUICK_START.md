# 快速开始 - 用户认证系统

## 第一步：初始化数据库

执行SQL脚本创建必要的表：

```bash
mysql -u king -pking123 mybatis < src/main/resources/sql/auth_tables.sql
```

或者在MySQL客户端中执行 `src/main/resources/sql/auth_tables.sql` 文件中的SQL语句。

## 第二步：启动应用

```bash
./mvnw spring-boot:run
```

等待应用启动完成，看到类似以下日志：
```
Started LearnApplication in X.XXX seconds
```

## 第三步：测试功能

### 方式一：使用测试脚本（推荐）

```bash
./test-auth.sh
```

这将自动执行完整的测试流程。

### 方式二：手动测试

#### 1. 注册用户

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "123456",
    "email": "test@example.com",
    "phone": "13800138000",
    "name": "测试用户",
    "age": 25,
    "deptId": 1
  }'
```

响应示例：
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "type": "Bearer",
    "userId": 1,
    "username": "testuser"
  }
}
```

**保存返回的token值！**

#### 2. 登录

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "123456"
  }'
```

响应示例：
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "type": "Bearer",
    "userId": 1,
    "username": "testuser"
  }
}
```

**保存返回的token值！**

#### 3. 获取当前用户信息（需要Token）

将 `YOUR_TOKEN_HERE` 替换为实际的token：

```bash
curl http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

响应示例：
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "username": "testuser",
    "email": "test@example.com",
    "phone": "13800138000",
    "status": 1,
    "lastLoginTime": "2024-01-01 12:00:00",
    "createdTime": "2024-01-01 10:00:00",
    "updatedTime": "2024-01-01 12:00:00"
  }
}
```

#### 4. 访问受保护的接口（需要Token）

```bash
curl http://localhost:8080/api/test/protected \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

响应示例：
```json
{
  "code": 0,
  "message": "success",
  "data": "这是一个受保护的接口，您已成功认证！"
}
```

#### 5. 测试未授权访问（不需要Token）

```bash
curl http://localhost:8080/api/test/protected
```

响应示例（应该返回401）：
```json
{
  "code": 401,
  "message": "未授权访问",
  "data": null
}
```

#### 6. 退出登录（需要Token）

```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

响应示例：
```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

## 验证操作日志

登录后查看操作日志是否记录：

```bash
mysql -u king -pking123 mybatis -e "SELECT * FROM operation_log ORDER BY operation_time DESC LIMIT 10;"
```

应该能看到刚才的操作记录。

## 常见问题

### 1. 数据库连接失败

检查 `application-dev.yaml` 中的数据库配置是否正确：
- 用户名：`king`
- 密码：`king123`
- 数据库：`mybatis`

### 2. 表不存在

确保已执行 `auth_tables.sql` 脚本创建表。

### 3. Token无效

- 检查Token是否正确复制（不要遗漏任何字符）
- 检查Token是否过期（默认24小时）
- 确保Authorization头格式正确：`Bearer <token>`

### 4. 端口冲突

如果8080端口被占用，可以在 `application.yaml` 中修改：
```yaml
server:
  port: 8081
```

## 下一步

- 查看 [AUTH_GUIDE.md](AUTH_GUIDE.md) 了解详细的API文档
- 查看 [AUTH_IMPLEMENTATION_SUMMARY.md](AUTH_IMPLEMENTATION_SUMMARY.md) 了解实现细节
- 根据需求扩展功能（刷新Token、角色权限等）

## 安全提醒

⚠️ **生产环境部署前必须：**
1. 修改JWT密钥为更复杂的随机字符串
2. 使用HTTPS传输
3. 实施更强的密码策略
4. 添加限流保护
5. 定期清理日志
