# 第三方OAuth2登录使用指南

## 概述

本项目已实现完整的第三方OAuth2登录功能，支持以下平台：
- ✅ GitHub
- ✅ 微信（WeChat）
- ✅ QQ
- ✅ Google
- ✅ 微博（Weibo）

## 功能特性

1. **第三方登录** - 使用第三方账号直接登录系统
2. **账号绑定** - 将第三方账号绑定到现有账户
3. **账号解绑** - 解除第三方账号绑定
4. **自动注册** - 首次使用第三方登录自动创建账号
5. **Token管理** - 自动管理和刷新访问令牌

## 数据库初始化

执行SQL脚本创建第三方绑定表：

```bash
mysql -u king -pking123 mybatis < src/main/resources/sql/oauth2_tables.sql
```

## 配置第三方平台

### 1. GitHub配置

1. 访问 https://github.com/settings/developers
2. 点击 "New OAuth App"
3. 填写应用信息：
   - Application name: 你的应用名称
   - Homepage URL: http://localhost:8080
   - Authorization callback URL: http://localhost:8080/api/oauth2/callback/github
4. 获取 Client ID 和 Client Secret
5. 配置环境变量或application.yaml：
   ```yaml
   oauth2:
     github:
       client-id: YOUR_GITHUB_CLIENT_ID
       client-secret: YOUR_GITHUB_CLIENT_SECRET
   ```

### 2. 微信配置

1. 访问 https://open.weixin.qq.com/
2. 注册开发者账号并创建应用
3. 获取 AppID 和 AppSecret
4. 配置回调域名
5. 配置：
   ```yaml
   oauth2:
     wechat:
       client-id: YOUR_WECHAT_APP_ID
       client-secret: YOUR_WECHAT_APP_SECRET
   ```

### 3. QQ配置

1. 访问 https://connect.qq.com/
2. 创建应用
3. 获取 APP ID 和 APP Key
4. 配置回调地址
5. 配置：
   ```yaml
   oauth2:
     qq:
       client-id: YOUR_QQ_APP_ID
       client-secret: YOUR_QQ_APP_KEY
   ```

### 4. Google配置

1. 访问 https://console.cloud.google.com/
2. 创建项目和应用
3. 配置OAuth2同意屏幕
4. 创建OAuth2客户端ID
5. 配置：
   ```yaml
   oauth2:
     google:
       client-id: YOUR_GOOGLE_CLIENT_ID
       client-secret: YOUR_GOOGLE_CLIENT_SECRET
   ```

### 5. 微博配置

1. 访问 https://open.weibo.com/
2. 创建应用
3. 获取 App Key 和 App Secret
4. 配置授权回调页
5. 配置：
   ```yaml
   oauth2:
     weibo:
       client-id: YOUR_WEIBO_APP_KEY
       client-secret: YOUR_WEIBO_APP_SECRET
   ```

## API接口说明

### 1. 获取授权URL

**接口**: `GET /api/oauth2/authorize/{platform}`

**路径参数**:
- platform: 平台名称（github, wechat, qq, google, weibo）

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": "https://github.com/login/oauth/authorize?client_id=xxx&redirect_uri=xxx&response_type=code&scope=user:email&state=xxx"
}
```

**使用示例**:
```bash
curl http://localhost:8080/api/oauth2/authorize/github
```

### 2. 第三方登录回调

**接口**: `GET /api/oauth2/callback/{platform}`

**路径参数**:
- platform: 平台名称

**查询参数**:
- code: 授权码（由第三方平台回调时携带）
- state: 状态参数（可选）

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "type": "Bearer",
    "userId": 1,
    "username": "github_user"
  }
}
```

### 3. 绑定第三方账号

**接口**: `POST /api/oauth2/bind/{platform}`

**路径参数**:
- platform: 平台名称

**查询参数**:
- code: 授权码

**请求头**:
```
Authorization: Bearer YOUR_JWT_TOKEN
```

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

### 4. 解绑第三方账号

**接口**: `DELETE /api/oauth2/unbind/{platform}`

**路径参数**:
- platform: 平台名称

**请求头**:
```
Authorization: Bearer YOUR_JWT_TOKEN
```

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

### 5. 查询绑定列表

**接口**: `GET /api/oauth2/bindings`

**请求头**:
```
Authorization: Bearer YOUR_JWT_TOKEN
```

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 1,
      "userId": 1,
      "platform": "github",
      "openId": "12345678",
      "unionId": null,
      "createdAt": "2024-01-01 10:00:00",
      "updatedAt": "2024-01-01 10:00:00"
    }
  ]
}
```

## 使用流程

### 方式一：GitHub登录示例

#### 1. 前端重定向到授权页面

```javascript
// 获取授权URL
fetch('http://localhost:8080/api/oauth2/authorize/github')
  .then(response => response.json())
  .then(result => {
    // 重定向到GitHub授权页面
    window.location.href = result.data;
  });
```

#### 2. 用户授权后回调

用户在GitHub授权后，会回调到：
```
http://localhost:8080/api/oauth2/callback/github?code=AUTH_CODE&state=STATE
```

#### 3. 处理回调获取Token

```javascript
// 从URL中获取code参数
const urlParams = new URLSearchParams(window.location.search);
const code = urlParams.get('code');

// 使用code换取JWT Token
fetch(`http://localhost:8080/api/oauth2/callback/github?code=${code}`)
  .then(response => response.json())
  .then(result => {
    if (result.code === 0) {
      // 保存JWT Token
      localStorage.setItem('token', result.data.token);
      // 跳转到首页
      window.location.href = '/';
    }
  });
```

#### 4. 使用Token访问受保护接口

```javascript
fetch('http://localhost:8080/api/test/protected', {
  headers: {
    'Authorization': 'Bearer ' + localStorage.getItem('token')
  }
})
.then(response => response.json())
.then(data => console.log(data));
```

### 方式二：绑定第三方账号

```javascript
// 1. 先登录获取JWT Token
// 2. 获取授权URL
fetch('http://localhost:8080/api/oauth2/authorize/github')
  .then(response => response.json())
  .then(result => {
    window.location.href = result.data;
  });

// 3. 回调后绑定
const code = urlParams.get('code');
fetch(`http://localhost:8080/api/oauth2/bind/github?code=${code}`, {
  method: 'POST',
  headers: {
    'Authorization': 'Bearer ' + localStorage.getItem('token')
  }
})
.then(response => response.json())
.then(result => {
  console.log('绑定成功');
});
```

## cURL测试示例

### 1. 获取GitHub授权URL
```bash
curl http://localhost:8080/api/oauth2/authorize/github
```

### 2. 模拟回调（需要先从GitHub获取code）
```bash
curl "http://localhost:8080/api/oauth2/callback/github?code=YOUR_CODE_FROM_GITHUB"
```

### 3. 查询绑定列表
```bash
curl http://localhost:8080/api/oauth2/bindings \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 4. 解绑GitHub
```bash
curl -X DELETE http://localhost:8080/api/oauth2/unbind/github \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## 架构说明

### 核心组件

1. **实体类**
   - `OAuth2Platform`: 平台枚举
   - `OAuth2UserInfo`: 第三方用户信息
   - `UserThirdParty`: 第三方账号绑定

2. **配置类**
   - `OAuth2Config`: 平台配置

3. **工具类**
   - `OAuth2Client`: OAuth2客户端，处理授权、Token交换、用户信息获取

4. **Mapper层**
   - `UserThirdPartyMapper`: 第三方绑定数据访问

5. **Service层**
   - `OAuth2LoginService`: 第三方登录业务逻辑

6. **Controller层**
   - `OAuth2Controller`: 第三方登录API接口

### 登录流程

```
用户点击第三方登录
    ↓
前端请求 /api/oauth2/authorize/{platform}
    ↓
后端返回授权URL
    ↓
前端重定向到第三方平台授权页面
    ↓
用户授权
    ↓
第三方平台回调 /api/oauth2/callback/{platform}?code=xxx
    ↓
后端用code换取access_token
    ↓
后端用access_token获取用户信息
    ↓
检查是否已绑定
    ↓
已绑定 → 生成JWT Token返回
未绑定 → 创建新用户 → 建立绑定关系 → 生成JWT Token返回
```

## 安全建议

1. **State参数验证** - 生产环境必须验证state参数防止CSRF攻击
2. **HTTPS** - 生产环境必须使用HTTPS
3. **Token存储** - 安全存储JWT Token
4. **定期刷新** - 定期刷新第三方平台的access_token
5. **错误处理** - 妥善处理各种异常情况

## 扩展建议

1. 添加更多第三方平台（Facebook, Twitter, LinkedIn等）
2. 实现Token自动刷新机制
3. 添加账号合并功能
4. 实现单点登录（SSO）
5. 添加登录日志记录
6. 实现社交分享功能
7. 添加好友关系同步

## 常见问题

### 1. 回调地址不匹配

确保在第三方平台配置的回调地址与实际一致。

### 2. Scope权限不足

根据需求配置合适的scope，确保能获取所需用户信息。

### 3. CORS问题

前端跨域访问时需要配置CORS。

### 4. Token过期

定期检查并刷新第三方平台的access_token。

## 注意事项

⚠️ **重要提醒**：
1. 不要将client_secret提交到版本控制系统
2. 使用环境变量管理敏感配置
3. 生产环境务必启用HTTPS
4. 定期更新和维护第三方平台配置
5. 监控和记录登录失败情况
