# 第三方OAuth2登录 - 快速开始

## 第一步：初始化数据库

```bash
mysql -u king -pking123 mybatis < src/main/resources/sql/oauth2_tables.sql
```

## 第二步：配置第三方平台

### 选项A：使用GitHub测试（推荐）

1. 访问 https://github.com/settings/developers
2. 点击 "New OAuth App"
3. 填写：
   - Application name: Test App
   - Homepage URL: http://localhost:8080
   - Authorization callback URL: http://localhost:8080/api/oauth2/callback/github
4. 获取 Client ID 和 Client Secret
5. 在 `application.yaml` 中配置：
   ```yaml
   oauth2:
     github:
       client-id: YOUR_CLIENT_ID
       client-secret: YOUR_CLIENT_SECRET
   ```

### 选项B：使用环境变量

```bash
export GITHUB_CLIENT_ID=your_client_id
export GITHUB_CLIENT_SECRET=your_client_secret
```

## 第三步：刷新Maven依赖

**重要**：如果出现编译错误，请刷新Maven依赖

### IntelliJ IDEA
- 右键点击 `pom.xml` → Maven → Reload Project
- 或点击右侧Maven工具窗口的刷新按钮

### 命令行
```bash
./mvnw clean install
```

## 第四步：启动应用

```bash
./mvnw spring-boot:run
```

## 第五步：测试登录

### 方式一：浏览器测试

1. 在浏览器中访问：
   ```
   http://localhost:8080/api/oauth2/authorize/github
   ```

2. 会返回授权URL，复制并在浏览器中打开

3. 授权后会回调并返回JWT Token

### 方式二：cURL测试

```bash
# 1. 获取授权URL
curl http://localhost:8080/api/oauth2/authorize/github

# 2. 手动访问返回的URL进行授权

# 3. 授权后，从回调URL中获取code，然后：
curl "http://localhost:8080/api/oauth2/callback/github?code=YOUR_CODE"
```

## API快速参考

### 获取授权URL
```bash
GET /api/oauth2/authorize/{platform}
# platform: github, wechat, qq, google, weibo
```

### 登录回调
```bash
GET /api/oauth2/callback/{platform}?code=xxx
```

### 绑定账号（需要JWT Token）
```bash
POST /api/oauth2/bind/{platform}?code=xxx
Authorization: Bearer YOUR_JWT_TOKEN
```

### 查询绑定列表
```bash
GET /api/oauth2/bindings
Authorization: Bearer YOUR_JWT_TOKEN
```

### 解绑账号
```bash
DELETE /api/oauth2/unbind/{platform}
Authorization: Bearer YOUR_JWT_TOKEN
```

## 前端集成示例

### React/Vue示例

```javascript
// 1. 处理登录按钮点击
const handleLogin = async () => {
  const response = await fetch('http://localhost:8080/api/oauth2/authorize/github');
  const result = await response.json();
  
  if (result.code === 0) {
    // 重定向到授权页面
    window.location.href = result.data;
  }
};

// 2. 处理回调页面
useEffect(() => {
  const code = new URLSearchParams(window.location.search).get('code');
  
  if (code) {
    fetch(`http://localhost:8080/api/oauth2/callback/github?code=${code}`)
      .then(res => res.json())
      .then(result => {
        if (result.code === 0) {
          // 保存Token
          localStorage.setItem('token', result.data.token);
          // 跳转到首页
          navigate('/');
        }
      });
  }
}, []);

// 3. 使用Token访问API
const fetchData = async () => {
  const token = localStorage.getItem('token');
  const response = await fetch('http://localhost:8080/api/test/protected', {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  return response.json();
};
```

## 故障排查

### 问题1：编译错误
**现象**：IDE显示"无法解析符号 ObjectMapper"等错误

**解决**：
```bash
# 刷新Maven依赖
./mvnw clean install

# 或在IDE中重新导入项目
```

### 问题2：回调地址不匹配
**现象**：第三方平台提示redirect_uri错误

**解决**：检查application.yaml中的redirect-uri是否与平台配置一致

### 问题3：获取用户信息失败
**现象**：登录后报错

**解决**：
1. 检查client-id和client-secret是否正确
2. 检查scope权限是否足够
3. 查看日志获取详细错误信息

### 问题4：CORS跨域错误
**现象**：前端调用API时报跨域错误

**解决**：添加CORS配置
```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE");
    }
}
```

## 下一步

1. ✅ 完成上述步骤，确保GitHub登录正常工作
2. 📝 阅读 [OAUTH2_GUIDE.md](OAUTH2_GUIDE.md) 了解详细文档
3. 🔧 配置其他平台（微信、QQ等）
4. 🎨 完善前端界面
5. 🚀 部署到生产环境

## 注意事项

⚠️ **重要提醒**：
1. 不要将client_secret提交到Git
2. 生产环境必须使用HTTPS
3. 实现state参数验证防止CSRF攻击
4. 定期更新和维护平台配置
5. 监控登录失败情况

## 获取帮助

- 查看详细文档：[OAUTH2_GUIDE.md](OAUTH2_GUIDE.md)
- 查看实现总结：[OAUTH2_IMPLEMENTATION_SUMMARY.md](OAUTH2_IMPLEMENTATION_SUMMARY.md)
- 查看各平台官方文档获取最新配置信息
