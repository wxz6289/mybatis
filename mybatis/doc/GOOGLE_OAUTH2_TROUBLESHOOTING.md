# Google OAuth2 登录问题排查指南

## 问题现象

Google授权登录时显示错误：
```json
{
  "code": 500,
  "message": "登录失败: 解析用户信息失败",
  "timestamp": 1778746456233
}
```

## 已修复的问题

### 1. 空值处理增强

**问题原因**：Google返回的用户信息中某些字段可能为空或不存在，导致`NullPointerException`。

**修复方案**：为所有平台的用户信息解析添加了健壮的空值检查。

**修改前**：
```java
userInfo.setOpenId(jsonNode.get("sub").asText());  // ❌ 如果sub不存在会抛出异常
userInfo.setEmail(jsonNode.get("email").asText());
```

**修改后**：
```java
// ✅ 先检查字段是否存在
userInfo.setOpenId(jsonNode.has("sub") ? jsonNode.get("sub").asText() : null);
userInfo.setEmail(jsonNode.has("email") && !jsonNode.get("email").isNull() 
    ? jsonNode.get("email").asText() : null);
```

### 2. 日志记录增强

添加了详细的日志记录，方便排查问题：

```java
log.info("解析{}平台用户信息: {}", platform.getName(), jsonNode.toString());
log.info("解析成功 - OpenID: {}, Nickname: {}", userInfo.getOpenId(), userInfo.getNickname());
```

## 排查步骤

### 步骤1：检查应用日志

启动应用后，查看控制台日志输出：

```bash
./mvnw spring-boot:run
```

尝试Google登录后，查找类似以下的日志：

```
INFO  - 解析Google平台用户信息: {"sub":"123456789","email":"user@gmail.com","name":"John Doe","picture":"..."}
INFO  - 解析成功 - OpenID: 123456789, Nickname: John Doe
```

或者错误日志：

```
ERROR - 解析Google平台用户信息失败: ...
```

### 步骤2：验证Google API配置

#### 2.1 检查application.yaml配置

```yaml
oauth2:
  google:
    client-id: ${GOOGLE_CLIENT_ID:your_google_client_id}
    client-secret: ${GOOGLE_CLIENT_SECRET:your_google_client_secret}
    redirect-uri: http://localhost:8080/api/oauth2/callback/google
    authorization-url: https://accounts.google.com/o/oauth2/v2/auth
    token-url: https://oauth2.googleapis.com/token
    user-info-url: https://www.googleapis.com/oauth2/v3/userinfo
    scope: openid profile email
```

#### 2.2 确认环境变量

```bash
# 设置Google OAuth2凭据
export GOOGLE_CLIENT_ID=your_actual_client_id
export GOOGLE_CLIENT_SECRET=your_actual_client_secret
```

### 步骤3：检查Google Cloud Console配置

#### 3.1 创建OAuth2客户端ID

1. 访问 [Google Cloud Console](https://console.cloud.google.com/)
2. 选择或创建项目
3. 导航到 **API和服务** > **凭据**
4. 点击 **创建凭据** > **OAuth客户端ID**
5. 选择应用类型：**Web应用**

#### 3.2 配置授权重定向URI

在Google Cloud Console中添加正确的重定向URI：

```
http://localhost:8080/api/oauth2/callback/google
```

如果使用HTTPS：
```
https://localhost:8443/api/oauth2/callback/google
```

**重要**：必须与实际使用的回调地址完全一致（包括端口号）。

#### 3.3 启用Google+ API

确保已启用以下API：
- Google People API
- Google+ API（如果需要）

### 步骤4：测试OAuth2流程

#### 4.1 获取授权URL

```bash
curl http://localhost:8080/api/oauth2/authorize/google
```

应该返回：
```json
{
  "code": 200,
  "data": "https://accounts.google.com/o/oauth2/v2/auth?client_id=...&redirect_uri=...&response_type=code&scope=openid%20profile%20email&state=..."
}
```

#### 4.2 手动测试完整流程

1. 在浏览器中访问授权URL
2. 使用Google账号授权
3. 观察回调是否成功
4. 检查应用日志

### 步骤5：检查Scope权限

Google OAuth2支持的scope：

| Scope | 说明 | 返回字段 |
|-------|------|---------|
| `openid` | 必需，OpenID Connect | sub |
| `profile` | 用户基本信息 | name, picture |
| `email` | 邮箱地址 | email, email_verified |

**推荐配置**：
```yaml
scope: openid profile email
```

## 常见错误及解决方案

### 错误1：invalid_client

**原因**：Client ID或Client Secret不正确

**解决**：
1. 检查Google Cloud Console中的凭据
2. 确认环境变量设置正确
3. 重启应用使配置生效

### 错误2：redirect_uri_mismatch

**原因**：回调地址不匹配

**解决**：
1. 检查application.yaml中的redirect-uri
2. 在Google Cloud Console中添加相同的URI
3. 确保完全一致（包括协议、域名、端口、路径）

### 错误3：access_denied

**原因**：用户拒绝授权或scope配置错误

**解决**：
1. 确认scope配置正确
2. 检查Google Cloud Console中启用的API
3. 尝试重新授权

### 错误4：解析用户信息失败（当前问题）

**原因**：
- Google返回的JSON格式与预期不符
- 某些字段为空或不存在
- 网络请求失败

**解决**：
1. ✅ 已修复：添加了空值检查
2. 查看应用日志，确认返回的JSON内容
3. 检查网络连接是否正常

## Google返回的用户信息示例

### 成功响应

```json
{
  "sub": "123456789012345678901",
  "name": "John Doe",
  "given_name": "John",
  "family_name": "Doe",
  "picture": "https://lh3.googleusercontent.com/a/...",
  "email": "john.doe@gmail.com",
  "email_verified": true,
  "locale": "en"
}
```

### 关键字段说明

| 字段 | 类型 | 说明 | 是否必选 |
|------|------|------|---------|
| `sub` | String | Google用户唯一ID | ✅ 必需 |
| `name` | String | 用户全名 | ⚠️ 可能为空 |
| `email` | String | 邮箱地址 | ⚠️ 需要email scope |
| `picture` | String | 头像URL | ⚠️ 可能为空 |
| `email_verified` | Boolean | 邮箱是否验证 | ⚠️ 需要email scope |

## 调试技巧

### 1. 启用详细日志

在 `application.yaml` 中增加日志级别：

```yaml
logging:
  level:
    com.dk.learn.common.util.OAuth2Client: DEBUG
    org.springframework.web.client.RestTemplate: DEBUG
```

### 2. 使用Postman测试

#### 获取Authorization Code

访问：
```
https://accounts.google.com/o/oauth2/v2/auth?
  client_id=YOUR_CLIENT_ID&
  redirect_uri=http://localhost:8080/api/oauth2/callback/google&
  response_type=code&
  scope=openid%20profile%20email&
  state=test_state
```

#### 交换Access Token

```bash
POST https://oauth2.googleapis.com/token
Content-Type: application/x-www-form-urlencoded

code=AUTHORIZATION_CODE&
client_id=YOUR_CLIENT_ID&
client_secret=YOUR_CLIENT_SECRET&
redirect_uri=http://localhost:8080/api/oauth2/callback/google&
grant_type=authorization_code
```

#### 获取用户信息

```bash
GET https://www.googleapis.com/oauth2/v3/userinfo
Authorization: Bearer ACCESS_TOKEN
```

### 3. 检查JWT Token

Google返回的ID Token是JWT格式，可以解码查看内容：

```bash
# 使用jwt.io在线解码
# 或使用命令行工具
echo "YOUR_ID_TOKEN" | cut -d'.' -f2 | base64 -d
```

## 代码修改总结

### OAuth2Client.java

**主要改进**：

1. ✅ 所有平台添加空值检查
2. ✅ 增加详细日志记录
3. ✅ 提供更友好的错误消息
4. ✅ 设置默认值防止null

**修改的平台**：
- GitHub
- WeChat（微信）
- QQ
- Google ✅ 重点修复
- Weibo（微博）

## 测试清单

完成以下测试确保修复有效：

- [ ] 应用可以正常启动
- [ ] 可以获取Google授权URL
- [ ] Google授权页面正常显示
- [ ] 授权后可以成功回调
- [ ] 日志显示解析成功的用户信息
- [ ] 返回JWT Token
- [ ] 数据库创建了用户记录和绑定关系

## 下一步建议

1. ✅ 测试Google登录功能
2. 🔄 测试其他平台（GitHub、微信等）
3. 🔄 实现Token刷新机制
4. 🔄 添加用户信息更新逻辑
5. 🔄 完善错误处理和用户提示

## 相关文档

- [Google OAuth2文档](https://developers.google.com/identity/protocols/oauth2)
- [OpenID Connect文档](https://openid.net/specs/openid-connect-core-1_0.html)
- [OAUTH2_GUIDE.md](OAUTH2_GUIDE.md) - 项目OAuth2实现指南

## 联系支持

如果问题仍然存在，请提供：
1. 完整的应用日志
2. Google Cloud Console配置截图
3. 浏览器控制台错误信息
4. 网络请求的详细信息
