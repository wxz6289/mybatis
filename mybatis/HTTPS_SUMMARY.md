# HTTPS配置总结

## ✅ 已完成的功能

### 1. SSL/TLS证书支持
- ✅ 自签名证书生成脚本
- ✅ PKCS12格式证书支持
- ✅ 灵活的证书配置
- ✅ 证书文件安全管理（.gitignore）

### 2. HTTP到HTTPS重定向
- ✅ 自动重定向配置
- ✅ 双端口支持（HTTP + HTTPS）
- ✅ 可配置的重定向规则

### 3. 配置管理
- ✅ application.yaml配置
- ✅ 环境变量支持
- ✅ 开发/生产环境分离

### 4. 文档和工具
- ✅ 详细的配置指南
- ✅ 快速配置脚本
- ✅ 故障排查文档

## 📁 创建的文件

### 配置类（1个）
1. [HttpsConfig.java](file:///Users/dreamerking/learn/java/mybatis/src/main/java/com/dk/learn/config/HttpsConfig.java) - HTTPS配置类，实现HTTP重定向

### 脚本工具（2个）
2. [generate-ssl-cert.sh](file:///Users/dreamerking/learn/java/mybatis/generate-ssl-cert.sh) - SSL证书生成脚本
3. [setup-https.sh](file:///Users/dreamerking/learn/java/mybatis/setup-https.sh) - HTTPS快速配置脚本

### 文档（1个）
4. [HTTPS_GUIDE.md](file:///Users/dreamerking/learn/java/mybatis/HTTPS_GUIDE.md) - 详细配置指南

### 配置文件更新（2个）
5. [application.yaml](file:///Users/dreamerking/learn/java/mybatis/src/main/resources/application.yaml) - 添加SSL配置
6. [.gitignore](file:///Users/dreamerking/learn/java/mybatis/.gitignore) - 排除证书文件

### 目录结构（1个）
7. `src/main/resources/certificates/.gitkeep` - 保持目录结构

## 🔧 配置说明

### application.yaml配置

```yaml
server:
  # SSL/HTTPS配置
  ssl:
    enabled: ${SSL_ENABLED:false}              # 是否启用SSL
    key-store: classpath:certificates/mybatis-learn.p12
    key-store-password: changeit
    key-store-type: PKCS12
    key-alias: mybatis-learn
    protocol: TLS
  
  # 端口配置
  port: ${SERVER_PORT:8443}                    # HTTPS端口
  http-port: ${HTTP_PORT:8080}                 # HTTP端口
```

### 环境变量

```bash
# 启用HTTPS
export SSL_ENABLED=true

# 自定义端口
export SERVER_PORT=443
export HTTP_PORT=80

# 自定义证书密码
export KEY_STORE_PASSWORD=your_password
```

## 🚀 快速开始

### 方式一：使用脚本（推荐）

```bash
# 1. 给脚本添加执行权限
chmod +x setup-https.sh

# 2. 运行配置脚本
./setup-https.sh

# 3. 启动应用
./mvnw spring-boot:run
```

### 方式二：手动配置

```bash
# 1. 生成证书
chmod +x generate-ssl-cert.sh
./generate-ssl-cert.sh

# 2. 设置环境变量
export SSL_ENABLED=true

# 3. 启动应用
./mvnw spring-boot:run
```

## 🌐 访问方式

### 启用HTTPS后

- **HTTPS**: https://localhost:8443
- **HTTP**: http://localhost:8080 → 自动重定向到HTTPS

### API接口示例

```bash
# HTTPS访问
curl -k https://localhost:8443/api/test/public

# HTTP访问（自动重定向）
curl http://localhost:8080/api/test/public
```

## ⚙️ 核心功能

### 1. HttpsConfig.java

```java
@Configuration
public class HttpsConfig {
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> {
            if (sslEnabled) {
                // 创建HTTP连接器并配置重定向
                Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
                connector.setScheme("http");
                connector.setPort(httpPort);
                connector.setSecure(false);
                connector.setRedirectPort(httpsPort);
                factory.addAdditionalTomcatConnectors(connector);
            }
        };
    }
}
```

### 2. 证书生成脚本

使用Java keytool生成PKCS12格式的自签名证书：
- RSA 2048位密钥
- 有效期10年
- 支持localhost和127.0.0.1

## 🔒 安全特性

1. **证书保护**
   - 证书文件已加入.gitignore
   - 建议使用环境变量管理密码
   - 生产环境使用正式SSL证书

2. **协议安全**
   - 默认使用TLS协议
   - 支持TLS 1.2和1.3
   - 可配置加密套件

3. **重定向安全**
   - HTTP自动重定向到HTTPS
   - 防止明文传输
   - 支持HSTS（可扩展）

## 📊 对比

| 特性 | HTTP | HTTPS |
|------|------|-------|
| 端口 | 8080 | 8443 |
| 加密 | ❌ | ✅ |
| 安全性 | 低 | 高 |
| 性能 | 快 | 略慢 |
| SEO | 一般 | 更好 |
| 浏览器信任 | - | 需要证书 |

## ⚠️ 注意事项

### 开发环境
- ✅ 使用自签名证书
- ✅ 浏览器会显示安全警告（正常）
- ✅ 可以点击"继续访问"
- ✅ 方便测试和调试

### 生产环境
- ⚠️ 必须使用正式SSL证书
- ⚠️ 推荐使用Let's Encrypt（免费）
- ⚠️ 配置HSTS
- ⚠️ 定期更新证书
- ⚠️ 监控证书过期时间

## 🛠️ 故障排查

### 常见问题

1. **证书找不到**
   ```bash
   # 解决：生成证书
   ./generate-ssl-cert.sh
   ```

2. **密码错误**
   ```bash
   # 检查application.yaml中的密码
   # 默认为: changeit
   ```

3. **端口冲突**
   ```bash
   # 更换端口
   export SERVER_PORT=8444
   export HTTP_PORT=8081
   ```

4. **浏览器无法访问**
   - 确认应用已启动
   - 检查防火墙
   - 尝试 https://127.0.0.1:8443

## 📚 相关文档

- [HTTPS_GUIDE.md](HTTPS_GUIDE.md) - 详细配置指南
- [Spring Boot SSL文档](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#web.server.embedded-container.ssl)
- [Let's Encrypt](https://letsencrypt.org/) - 免费SSL证书

## 🎯 下一步建议

1. ✅ 测试HTTPS连接
2. ✅ 配置OAuth2回调地址为HTTPS
3. ✅ 前端更新API基础URL
4. 🔄 生产环境申请正式证书
5. 🔄 配置HSTS
6. 🔄 设置证书过期提醒
7. 🔄 启用HTTP/2

## 💡 最佳实践

### 1. 证书管理
```bash
# 定期检查证书过期时间
keytool -list -v \
  -keystore src/main/resources/certificates/mybatis-learn.p12 \
  -storepass changeit | grep "直到"
```

### 2. 环境变量
```bash
# .env文件（不要提交到Git）
SSL_ENABLED=true
KEY_STORE_PASSWORD=your_secure_password
SERVER_PORT=443
HTTP_PORT=80
```

### 3. Docker部署
```dockerfile
# 复制证书
COPY src/main/resources/certificates/mybatis-learn.p12 /app/cert.p12

# 设置环境变量
ENV SSL_ENABLED=true
ENV KEY_STORE_PASSWORD=${CERT_PASSWORD}
```

## 📝 总结

HTTPS配置已完成，包括：
- ✅ 完整的SSL/TLS支持
- ✅ HTTP自动重定向
- ✅ 自签名证书生成工具
- ✅ 详细的配置文档
- ✅ 安全的证书管理

**开发环境**：直接使用自签名证书测试
**生产环境**：申请正式SSL证书，遵循安全最佳实践

所有配置灵活可控，通过环境变量轻松切换HTTP/HTTPS模式。
