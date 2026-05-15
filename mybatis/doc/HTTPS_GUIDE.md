# HTTPS配置指南

## 概述

本项目已支持HTTPS配置，包括：
- ✅ SSL/TLS证书配置
- ✅ HTTP自动重定向到HTTPS
- ✅ 自签名证书生成工具
- ✅ 灵活的环境变量配置

## 快速开始

### 方式一：使用自签名证书（开发环境推荐）

#### 1. 生成SSL证书

```bash
# 给脚本添加执行权限
chmod +x generate-ssl-cert.sh

# 运行脚本生成证书
./generate-ssl-cert.sh
```

证书将生成在：`src/main/resources/certificates/mybatis-learn.p12`

#### 2. 启用HTTPS

在 `application.yaml` 中修改：
```yaml
server:
  ssl:
    enabled: true  # 改为true启用HTTPS
```

或者使用环境变量：
```bash
export SSL_ENABLED=true
```

#### 3. 启动应用

```bash
./mvnw spring-boot:run
```

应用将在以下端口启动：
- HTTPS: https://localhost:8443
- HTTP: http://localhost:8080 (自动重定向到HTTPS)

### 方式二：使用现有证书（生产环境）

如果您已有SSL证书（如从Let's Encrypt、阿里云等获取），请按以下步骤配置：

#### 1. 转换证书格式

Spring Boot需要PKCS12格式的证书。如果您的证书是PEM格式，需要转换：

```bash
# 假设有以下文件：
# - server.crt (证书文件)
# - server.key (私钥文件)

# 转换为PKCS12格式
openssl pkcs12 -export \
  -in server.crt \
  -inkey server.key \
  -out mybatis-learn.p12 \
  -name mybatis-learn \
  -passout pass:your_password
```

#### 2. 放置证书文件

将生成的 `.p12` 文件放到：
```
src/main/resources/certificates/mybatis-learn.p12
```

#### 3. 配置证书信息

在 `application.yaml` 中配置：
```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:certificates/mybatis-learn.p12
    key-store-password: your_password
    key-store-type: PKCS12
    key-alias: mybatis-learn
```

## 配置说明

### application.yaml配置项

```yaml
server:
  # SSL/HTTPS配置
  ssl:
    enabled: ${SSL_ENABLED:false}              # 是否启用SSL
    key-store: classpath:certificates/mybatis-learn.p12  # 证书路径
    key-store-password: changeit               # 证书密码
    key-store-type: PKCS12                     # 证书类型
    key-alias: mybatis-learn                   # 证书别名
    protocol: TLS                              # 协议版本
  
  # 端口配置
  port: ${SERVER_PORT:8443}                    # HTTPS端口
  http-port: ${HTTP_PORT:8080}                 # HTTP端口（用于重定向）
```

### 环境变量配置

可以通过环境变量覆盖默认配置：

```bash
# 启用HTTPS
export SSL_ENABLED=true

# 自定义HTTPS端口
export SERVER_PORT=443

# 自定义HTTP端口
export HTTP_PORT=80

# 自定义证书密码
export KEY_STORE_PASSWORD=your_password
```

## 访问方式

### 启用HTTPS后

1. **HTTPS访问**（推荐）
   ```
   https://localhost:8443
   ```

2. **HTTP访问**（自动重定向）
   ```
   http://localhost:8080
   # 会自动重定向到 https://localhost:8443
   ```

### 浏览器证书警告

使用自签名证书时，浏览器会显示安全警告。解决方法：

#### Chrome/Edge
1. 点击"高级"
2. 点击"继续前往localhost（不安全）"

#### Firefox
1. 点击"高级"
2. 点击"接受风险并继续"

#### Safari
1. 点击"显示详细信息"
2. 点击"访问此网站"
3. 输入密码确认

### 信任自签名证书（可选）

如果要消除浏览器警告，可以将证书添加到系统信任列表：

#### macOS
```bash
# 导出证书
keytool -exportcert \
  -keystore src/main/resources/certificates/mybatis-learn.p12 \
  -storepass changeit \
  -alias mybatis-learn \
  -file mybatis-learn.cer

# 添加到钥匙串
sudo security add-trusted-cert \
  -d \
  -r trustRoot \
  -k /Library/Keychains/System.keychain \
  mybatis-learn.cer
```

#### Windows
1. 双击 `.cer` 证书文件
2. 点击"安装证书"
3. 选择"本地计算机"
4. 选择"将所有的证书都放入下列存储"
5. 点击"浏览"，选择"受信任的根证书颁发机构"
6. 完成导入

#### Linux
```bash
# Ubuntu/Debian
sudo cp mybatis-learn.cer /usr/local/share/ca-certificates/
sudo update-ca-certificates

# CentOS/RHEL
sudo cp mybatis-learn.cer /etc/pki/ca-trust/source/anchors/
sudo update-ca-trust extract
```

## API接口更新

启用HTTPS后，所有API接口URL需要更新：

### OAuth2回调地址

如果使用HTTPS，需要更新第三方平台的回调地址配置：

```yaml
oauth2:
  github:
    redirect-uri: https://localhost:8443/api/oauth2/callback/github
  # ... 其他平台类似
```

或者使用环境变量：
```bash
export OAUTH2_REDIRECT_URI=https://localhost:8443/api/oauth2/callback/github
```

### 前端调用

前端调用API时也需要使用HTTPS：

```javascript
// 之前
const API_BASE = 'http://localhost:8080';

// 启用HTTPS后
const API_BASE = 'https://localhost:8443';
```

## 生产环境建议

### 1. 使用正式SSL证书

生产环境建议使用正式的SSL证书，推荐：
- **Let's Encrypt**（免费，自动化）
- **阿里云SSL证书**
- **腾讯云SSL证书**
- **Comodo**
- **DigiCert**

### 2. 使用标准端口

```yaml
server:
  port: 443        # HTTPS标准端口
  http-port: 80    # HTTP标准端口
```

### 3. 强制HTTPS

在生产环境，应该禁用HTTP或强制重定向：

```java
@Configuration
public class SecurityConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptorAdapter() {
            @Override
            public boolean preHandle(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   Object handler) {
                if (!request.isSecure()) {
                    String httpsUrl = "https://" + request.getServerName() 
                                    + request.getRequestURI();
                    response.sendRedirect(httpsUrl);
                    return false;
                }
                return true;
            }
        });
    }
}
```

### 4. HSTS配置

启用HTTP严格传输安全（HSTS）：

```yaml
server:
  ssl:
    enabled: true
  # 添加HSTS头
  tomcat:
    additional-http-headers: |
      Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
```

### 5. 证书监控

设置证书过期提醒：
```bash
# 检查证书过期时间
keytool -list -v \
  -keystore src/main/resources/certificates/mybatis-learn.p12 \
  -storepass changeit | grep "直到"
```

## 故障排查

### 问题1：证书找不到

**错误信息**：
```
java.io.FileNotFoundException: class path resource [certificates/mybatis-learn.p12] cannot be resolved
```

**解决方案**：
1. 确认证书文件存在：`ls src/main/resources/certificates/`
2. 运行证书生成脚本：`./generate-ssl-cert.sh`
3. 检查文件名是否正确

### 问题2：证书密码错误

**错误信息**：
```
java.io.IOException: keystore password was incorrect
```

**解决方案**：
检查 `application.yaml` 中的密码是否与生成证书时设置的密码一致：
```yaml
server:
  ssl:
    key-store-password: changeit  # 确保证书密码正确
```

### 问题3：端口被占用

**错误信息**：
```
java.net.BindException: Address already in use
```

**解决方案**：
更换端口：
```bash
export SERVER_PORT=8444
export HTTP_PORT=8081
```

### 问题4：浏览器无法访问

**现象**：浏览器显示"连接被拒绝"或"无法建立安全连接"

**解决方案**：
1. 确认应用已启动
2. 检查防火墙设置
3. 尝试使用 `https://127.0.0.1:8443` 访问
4. 查看应用日志确认HTTPS已启用

### 问题5：OAuth2回调失败

**现象**：第三方登录回调时报错

**解决方案**：
1. 确保第三方平台配置的回调地址与实际一致
2. 如果使用HTTPS，回调地址也必须使用HTTPS
3. 检查redirect-uri配置：
   ```yaml
   oauth2:
     github:
       redirect-uri: https://localhost:8443/api/oauth2/callback/github
   ```

## 性能优化

### 1. 启用SSL会话缓存

```yaml
server:
  tomcat:
    mbeanregistry:
      enabled: true
    properties:
      server.ssl.session-cache-size: 1000
      server.ssl.session-timeout: 300
```

### 2. 使用HTTP/2

Spring Boot 3.x默认支持HTTP/2：
```yaml
server:
  http2:
    enabled: true
```

### 3. 调整线程池

```yaml
server:
  tomcat:
    threads:
      max: 200
      min-spare: 10
```

## 安全建议

### ⚠️ 重要安全提醒

1. **不要硬编码密码**
   ```yaml
   # ❌ 错误做法
   key-store-password: changeit
   
   # ✅ 正确做法
   key-store-password: ${KEY_STORE_PASSWORD}
   ```

2. **保护证书文件**
   ```bash
   # 设置文件权限
   chmod 600 src/main/resources/certificates/mybatis-learn.p12
   ```

3. **不要在Git中提交证书**
   确保 `.gitignore` 包含：
   ```
   src/main/resources/certificates/*.p12
   src/main/resources/certificates/*.jks
   ```

4. **定期更新证书**
   - 自签名证书：建议每年更新
   - Let's Encrypt：每90天自动更新
   - 商业证书：按有效期更新

5. **使用强加密算法**
   ```yaml
   server:
     ssl:
       protocols: TLSv1.2,TLSv1.3
       ciphers: >
         TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,
         TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,
         TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256
   ```

## 测试HTTPS

### 使用cURL测试

```bash
# 测试HTTPS连接（跳过证书验证）
curl -k https://localhost:8443/api/test/public

# 测试HTTP重定向
curl -I http://localhost:8080/api/test/public
# 应该返回 302 重定向到 HTTPS
```

### 使用OpenSSL测试

```bash
# 检查SSL证书
openssl s_client -connect localhost:8443

# 查看证书详情
echo | openssl s_client -connect localhost:8443 2>/dev/null | openssl x509 -noout -dates
```

### 在线测试工具

- **SSL Labs**: https://www.ssllabs.com/ssltest/
- **Qualys SSL Test**: https://ssltest.qualys.com/

## 总结

本项目已完整支持HTTPS配置，包括：
- ✅ 自签名证书生成工具
- ✅ HTTP自动重定向到HTTPS
- ✅ 灵活的配置选项
- ✅ 详细的使用文档

**开发环境**：使用自签名证书快速测试
**生产环境**：使用正式的SSL证书，遵循安全最佳实践

## 相关文档

- [Spring Boot SSL配置](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#web.server.embedded-container.ssl)
- [Let's Encrypt](https://letsencrypt.org/)
- [OWASP TLS Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Transport_Layer_Protection_Cheat_Sheet.html)
