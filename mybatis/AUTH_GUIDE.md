# 用户认证系统使用指南

## 概述

本项目已实现完整的用户认证系统，包括以下功能：
- 用户注册
- 用户登录
- JWT Token认证
- 身份验证拦截器
- 操作日志记录
- 退出登录

## 数据库初始化

在首次使用前，需要执行SQL脚本创建必要的表：

```bash
mysql -u king -pking123 mybatis < src/main/resources/sql/auth_tables.sql
```

或者手动执行 `src/main/resources/sql/auth_tables.sql` 中的SQL语句。

## API接口说明

### 1. 用户注册

**接口**: `POST /api/auth/register`

**请求体**:
```json
{
  "username": "testuser",
  "password": "123456",
  "email": "test@example.com",
  "phone": "13800138000",
  "name": "测试用户",
  "age": 25,
  "deptId": 1
}
```

**响应**:
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

### 2. 用户登录

**接口**: `POST /api/auth/login`

**请求体**:
```json
{
  "username": "testuser",
  "password": "123456",
  "rememberMe": false
}
```

**响应**:
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

### 3. 获取当前用户信息

**接口**: `GET /api/auth/me`

**请求头**:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**响应**:
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

### 4. 退出登录

**接口**: `POST /api/auth/logout`

**请求头**:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

### 5. 受保护的接口示例

**接口**: `GET /api/test/protected`

**请求头**:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": "这是一个受保护的接口，您已成功认证！"
}
```

### 6. 公开接口示例

**接口**: `GET /api/test/public`

**无需认证，直接访问**

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": "这是一个公开的接口，任何人都可以访问！"
}
```

## 使用流程

### 1. 注册用户

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "123456",
    "email": "test@example.com",
    "name": "测试用户",
    "age": 25,
    "deptId": 1
  }'
```

### 2. 登录获取Token

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "123456"
  }'
```

保存响应中的 `token` 值。

### 3. 访问受保护的接口

```bash
curl http://localhost:8080/api/test/protected \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 4. 获取当前用户信息

```bash
curl http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 5. 退出登录

```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

## 技术实现

### 核心组件

1. **实体类**
   - `UserAuth`: 用户认证实体
   - `LoginRequest`: 登录请求DTO
   - `RegisterRequest`: 注册请求DTO
   - `JwtResponse`: JWT响应DTO
   - `OperationLog`: 操作日志实体

2. **工具类**
   - `JwtUtil`: JWT令牌生成和验证
   - `PasswordUtil`: BCrypt密码加密

3. **Mapper层**
   - `UserAuthMapper`: 用户认证数据访问

4. **Service层**
   - `AuthService`: 认证业务逻辑
   - `OperationLogService`: 操作日志服务

5. **Controller层**
   - `AuthController`: 认证API接口

6. **拦截器**
   - `JwtAuthenticationInterceptor`: JWT认证拦截器
   - `OperationLogInterceptor`: 操作日志拦截器

7. **配置**
   - `WebConfig`: Web配置，注册拦截器

### 安全特性

1. **密码加密**: 使用BCrypt算法加密存储密码
2. **JWT认证**: 基于Token的无状态认证
3. **Token有效期**: 默认24小时（可配置）
4. **拦截器保护**: 自动验证受保护接口的Token
5. **操作日志**: 记录所有API请求的操作日志

## 配置说明

### JWT配置 (application.yaml)

```yaml
jwt:
  secret: mySecretKeyForJWTTokenGenerationAndValidation123456789  # JWT密钥
  expiration: 86400000  # Token有效期（毫秒），默认24小时
```

### 拦截器配置 (WebConfig)

可以在 `WebConfig` 中配置哪些接口需要认证，哪些接口公开访问：

```java
registry.addInterceptor(jwtAuthenticationInterceptor)
    .addPathPatterns("/api/**")  // 拦截所有/api开头的请求
    .excludePathPatterns(
        "/api/auth/login",    // 排除登录接口
        "/api/auth/register", // 排除注册接口
        "/api/test/public"    // 排除公开测试接口
    );
```

## 注意事项

1. **生产环境**: 请修改JWT密钥为更复杂的随机字符串
2. **HTTPS**: 生产环境建议使用HTTPS传输
3. **Token存储**: 前端应安全存储Token（如HttpOnly Cookie）
4. **密码策略**: 建议实施更强的密码策略
5. **限流**: 建议对登录接口实施限流保护
6. **日志清理**: 定期清理操作日志，避免数据库过大

## 扩展建议

1. 添加刷新Token机制
2. 实现记住我功能
3. 添加邮箱/手机验证码注册
4. 实现OAuth2第三方登录
5. 添加角色和权限管理
6. 实现账号锁定机制（多次登录失败）
7. 添加登录设备管理
8. 实现双因素认证（2FA）
