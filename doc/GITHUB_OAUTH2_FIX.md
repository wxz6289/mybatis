# GitHub OAuth2 登录问题修复说明

## 已修复的问题

### 问题1：解析AccessToken响应失败 ✅

**错误信息**：
```json
{"code":500,"message":"登录失败: 解析AccessToken响应失败","timestamp":1778747153624}
```

**原因**：GitHub返回form-urlencoded格式，代码尝试用JSON解析

**解决**：添加平台区分处理和容错解析机制

---

### 问题2：401 Unauthorized from GET https://api.github.com/user ✅

**错误信息**：
```json
{"code":500,"message":"登录失败: 401 Unauthorized from GET https://api.github.com/user","timestamp":1778747943225}
```

**原因**：获取用户信息时，没有正确传递Access Token。GitHub API要求通过`Authorization` header传递Token，而不是query参数。

**解决**：为GitHub添加特殊的header认证方式

## 修复方案

### 修复1：Token端点响应格式处理

**修改前**：
```java
// ❌ 所有平台都使用JSON格式
Map<String, String> body = new HashMap<>();
body.put("client_id", config.getClientId());
// ...

String response = webClient.post()
    .uri(config.getTokenUrl())
    .contentType(MediaType.APPLICATION_JSON)  // 统一使用JSON
    .bodyValue(body)
    .retrieve()
    .bodyToMono(String.class)
    .block();
```

**修改后**：
```java
// ✅ 根据平台使用不同格式
if (platform == OAuth2Platform.GITHUB) {
    // GitHub使用form-urlencoded格式
    org.springframework.util.LinkedMultiValueMap<String, String> map = 
        new org.springframework.util.LinkedMultiValueMap<>();
    map.add("client_id", config.getClientId());
    // ...
    
    response = webClient.post()
        .uri(config.getTokenUrl())
        .header("Accept", "application/json")  // 请求JSON响应
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)  // 发送表单格式
        .bodyValue(map)
        .retrieve()
        .bodyToMono(String.class)
        .block();
} else {
    // 其他平台使用JSON格式
    Map<String, String> body = new HashMap<>();
    // ...
}
```

### 修复2：用户信息API认证方式

**问题原因**：
- GitHub API要求通过`Authorization: Bearer TOKEN` header传递token
- 原始代码使用query参数`?access_token=TOKEN`，导致401错误

**修改前**：
```java
// ❌ 所有平台都使用query参数
String response = webClient.get()
    .uri(config.getUserInfoUrl(), uriBuilder -> 
        uriBuilder.queryParam("access_token", accessToken).build())
    .retrieve()
    .bodyToMono(String.class)
    .block();
```

**修改后**：
```java
// ✅ 根据平台使用不同认证方式
if (platform == OAuth2Platform.GITHUB) {
    // GitHub使用Authorization header
    response = webClient.get()
        .uri(config.getUserInfoUrl())
        .header("Authorization", "Bearer " + accessToken)
        .header("Accept", "application/vnd.github.v3+json")
        .retrieve()
        .bodyToMono(String.class)
        .block();
} else {
    // 其他平台使用query参数
    response = webClient.get()
        .uri(config.getUserInfoUrl(), uriBuilder -> 
            uriBuilder.queryParam("access_token", accessToken).build())
        .retrieve()
        .bodyToMono(String.class)
        .block();
}
```

### 2. 添加容错解析机制

```java
try {
    // 尝试解析为JSON
    return objectMapper.readValue(response, Map.class);
} catch (Exception e) {
    // 如果JSON解析失败，尝试解析form-urlencoded格式
    log.warn("JSON解析失败，尝试解析form-urlencoded格式");
    return parseFormUrlEncoded(response);
}
```

### 3. 实现form-urlencoded解析器

```java
private Map<String, Object> parseFormUrlEncoded(String response) {
    Map<String, Object> result = new HashMap<>();
    String[] pairs = response.split("&");
    for (String pair : pairs) {
        String[] keyValue = pair.split("=", 2);
        if (keyValue.length == 2) {
            String key = java.net.URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
            String value = java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
            result.put(key, value);
        }
    }
    return result;
}
```

## 修改的文件

- [OAuth2Client.java](file:///Users/dreamerking/learn/java/src/main/java/com/dk/learn/common/util/OAuth2Client.java)
  - ✅ 添加平台判断逻辑（Token端点）
  - ✅ 支持form-urlencoded请求和响应
  - ✅ 添加容错解析机制
  - ✅ GitHub用户信息API使用Authorization header
  - ✅ 增强日志记录

## GitHub OAuth2配置

### 1. 创建GitHub OAuth App

1. 访问 https://github.com/settings/developers
2. 点击 **New OAuth App**
3. 填写应用信息：
   - **Application name**: MyBatis Learn
   - **Homepage URL**: http://localhost:8080
   - **Authorization callback URL**: http://localhost:8080/api/oauth2/callback/github

### 2. 获取凭据

创建后会获得：
- **Client ID**: xxxxxxxxxxxxxx
- **Client Secret**: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

### 3. 配置环境变量

```bash
export GITHUB_CLIENT_ID=your_client_id
export GITHUB_CLIENT_SECRET=your_client_secret
```

### 4. 验证配置

在 `application.yaml` 中确认：

```yaml
oauth2:
  github:
    client-id: ${GITHUB_CLIENT_ID}
    client-secret: ${GITHUB_CLIENT_SECRET}
    redirect-uri: http://localhost:8080/api/oauth2/callback/github
    authorization-url: https://github.com/login/oauth/authorize
    token-url: https://github.com/login/oauth/access_token
    user-info-url: https://api.github.com/user
    scope: user:email
```

## 测试步骤

### 1. 启动应用

```bash
./mvnw spring-boot:run
```

### 2. 获取授权URL

```bash
curl https://localhost:8443/api/oauth2/authorize/github
```

应该返回：
```json
{
  "code": 200,
  "data": "https://github.com/login/oauth/authorize?client_id=...&redirect_uri=...&response_type=code&scope=user:email&state=..."
}
```

### 3. 浏览器授权

1. 在浏览器中访问返回的授权URL
2. 使用GitHub账号登录
3. 点击 **Authorize** 授权

### 4. 观察回调

授权成功后会重定向到：
```
http://localhost:8080/api/oauth2/callback/github?code=AUTHORIZATION_CODE&state=STATE
```

### 5. 查看日志

应该看到类似以下日志：

```
INFO  - 获取GitHub平台AccessToken响应: {"access_token":"gho_xxx","token_type":"bearer","scope":"user:email"}
INFO  - 获取用户信息响应: {"login":"username","id":123456,...}
INFO  - 解析GitHub平台用户信息: {"login":"username","id":123456,...}
INFO  - 解析成功 - OpenID: 123456, Nickname: username
```

## GitHub API说明

### Token端点

**URL**: `https://github.com/login/oauth/access_token`

**请求方法**: POST

**Content-Type**: `application/x-www-form-urlencoded`

**请求参数**:
- `client_id`: 应用的Client ID
- `client_secret`: 应用的Client Secret
- `code`: 授权码
- `redirect_uri`: 回调地址
- `grant_type`: authorization_code

**响应格式** (默认):
```
access_token=gho_xxxxxxxxxxxx&token_type=bearer&scope=user
```

**响应格式** (添加Accept header):
```json
{
  "access_token": "gho_xxxxxxxxxxxx",
  "token_type": "bearer",
  "scope": "user"
}
```

### 用户信息端点

**URL**: `https://api.github.com/user`

**请求方法**: GET

**Headers**:
- `Authorization: Bearer ACCESS_TOKEN` ✅ 必需
- `Accept: application/vnd.github.v3+json` ✅ 推荐

**响应示例**:
```json
{
  "login": "octocat",
  "id": 1,
  "avatar_url": "https://github.com/images/error/octocat_happy.gif",
  "gravatar_id": "",
  "url": "https://api.github.com/users/octocat",
  "html_url": "https://github.com/octocat",
  "type": "User",
  "name": "monalisa octocat",
  "company": "GitHub",
  "blog": "https://github.com/blog",
  "location": "San Francisco",
  "email": "octocat@github.com",
  "hireable": null,
  "bio": "There once was...",
  "twitter_username": "monatheoctocat",
  "public_repos": 2,
  "public_gists": 1,
  "followers": 20,
  "following": 0,
  "created_at": "2008-01-14T04:33:35Z",
  "updated_at": "2008-01-14T04:33:35Z"
}
```

**重要提示**：
- ❌ **不要**使用query参数传递token（`?access_token=xxx`）
- ✅ **必须**使用`Authorization: Bearer TOKEN` header

## 常见问题

### 问题1：bad_verification_code

**原因**：授权码已过期或已被使用

**解决**：
1. 授权码只能使用一次
2. 授权码有效期约10分钟
3. 重新获取授权码

### 问题2：incorrect_client_credentials

**原因**：Client ID或Client Secret不正确

**解决**：
1. 检查GitHub应用设置
2. 确认环境变量设置正确
3. 重启应用

### 问题3：incorrect_redirect_uri

**原因**：回调地址不匹配

**解决**：
1. 检查application.yaml中的redirect-uri
2. 在GitHub应用设置中添加相同的URI
3. 确保完全一致（包括协议、域名、端口）

### 问题4：解析AccessToken响应失败 ✅ 已修复

**原因**：响应格式不是JSON

**解决**：✅ 已修复，现在支持form-urlencoded格式

### 问题5：401 Unauthorized from GET https://api.github.com/user ✅ 已修复

**原因**：没有正确使用Authorization header传递token

**解决**：✅ 已修复，现在GitHub使用Bearer token认证

## 调试技巧

### 1. 使用curl测试Token端点

```bash
curl -X POST https://github.com/login/oauth/access_token \
  -H "Accept: application/json" \
  -d "client_id=YOUR_CLIENT_ID" \
  -d "client_secret=YOUR_CLIENT_SECRET" \
  -d "code=AUTHORIZATION_CODE" \
  -d "redirect_uri=http://localhost:8080/api/oauth2/callback/github" \
  -d "grant_type=authorization_code"
```

应该返回JSON格式的响应。

### 2. 测试用户信息端点

```bash
curl -H "Authorization: Bearer ACCESS_TOKEN" \
  -H "Accept: application/vnd.github.v3+json" \
  https://api.github.com/user
```

应该返回用户信息的JSON数据。

### 3. 启用详细日志

在 `application.yaml` 中：

```yaml
logging:
  level:
    com.dk.learn.common.util.OAuth2Client: DEBUG
    org.springframework.web.reactive.function.client.ExchangeFunctions: DEBUG
```

## 与其他平台的对比

| 平台 | Token请求格式 | Token响应格式 | 用户信息请求 |
|------|--------------|--------------|-------------|
| **GitHub** | form-urlencoded | JSON/form-urlencoded | Header认证 |
| **Google** | JSON | JSON | Query参数 |
| **微信** | JSON | JSON | Query参数 |
| **QQ** | JSON | JSON | Query参数 |
| **微博** | JSON | JSON | Query参数 |

## 最佳实践

### 1. 始终添加Accept header

```java
.header("Accept", "application/json")
```

这样GitHub会返回JSON格式，更容易解析。

### 2. 使用正确的Content-Type

```java
// GitHub
.contentType(MediaType.APPLICATION_FORM_URLENCODED)

// 其他平台
.contentType(MediaType.APPLICATION_JSON)
```

### 3. 添加容错机制

```java
try {
    return objectMapper.readValue(response, Map.class);
} catch (Exception e) {
    return parseFormUrlEncoded(response);
}
```

### 4. 详细的日志记录

```java
log.info("获取{}平台AccessToken响应: {}", platform.getName(), response);
```

## 总结

✅ **已修复的问题**：
1. GitHub Token端点返回格式处理（form-urlencoded）
2. GitHub用户信息API认证方式（Authorization header）
3. 添加了form-urlencoded解析支持
4. 实现了容错解析机制
5. 增强了日志记录

✅ **改进的功能**：
- 支持多种响应格式（JSON和form-urlencoded）
- 支持多种认证方式（Header和Query参数）
- 更健壮的错误处理
- 更好的调试体验
- 平台适配更加灵活

现在GitHub OAuth2登录应该可以正常工作了！🎉

## 相关文档

- [GitHub OAuth2文档](https://docs.github.com/en/developers/apps/building-oauth-apps/authorizing-oauth-apps)
- [GOOGLE_OAUTH2_TROUBLESHOOTING.md](GOOGLE_OAUTH2_TROUBLESHOOTING.md) - Google登录问题排查
- [OAUTH2_GUIDE.md](OAUTH2_GUIDE.md) - OAuth2完整指南
