# Spring Boot 4.x HTTPS配置说明

## 重要更新

由于Spring Boot 4.x的架构变化，HTTP到HTTPS的自动重定向功能需要通过外部方式实现。

## 当前配置状态

### ✅ 已实现的功能

1. **SSL/HTTPS支持** - 完全支持
   - 自签名证书生成
   - PKCS12格式证书
   - 灵活的配置选项

2. **HTTPS监听** - 完全支持
   - 可配置HTTPS端口（默认8443）
   - 支持环境变量配置

### ⚠️ 需要外部配置的功能

**HTTP到HTTPS重定向** - 需要通过以下方式之一实现：

## 解决方案

### 方案一：使用Nginx反向代理（推荐）

#### 1. 安装Nginx

```bash
# macOS
brew install nginx

# Ubuntu/Debian
sudo apt-get install nginx

# CentOS/RHEL
sudo yum install nginx
```

#### 2. 配置Nginx

创建配置文件 `/etc/nginx/conf.d/mybatis-learn.conf`：

```nginx
# HTTP服务器 - 重定向到HTTPS
server {
    listen 80;
    server_name localhost;
    
    # 重定向所有HTTP请求到HTTPS
    return 301 https://$host:8443$request_uri;
}

# HTTPS服务器
server {
    listen 8443 ssl;
    server_name localhost;
    
    # SSL证书配置（如果使用Nginx终止SSL）
    # ssl_certificate /path/to/cert.pem;
    # ssl_certificate_key /path/to/key.pem;
    
    location / {
        # 转发到Spring Boot应用
        proxy_pass http://localhost:8443;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

#### 3. 启动Nginx

```bash
# 测试配置
sudo nginx -t

# 启动Nginx
sudo nginx

# 重启Nginx
sudo nginx -s reload
```

#### 4. 配置Spring Boot

在 `application.yaml` 中只启用HTTPS：

```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:certificates/mybatis-learn.p12
    key-store-password: changeit
  port: 8443
```

### 方案二：开发环境简化方案

开发环境中可以只使用HTTPS，不进行HTTP重定向：

#### 1. 启用HTTPS

```bash
export SSL_ENABLED=true
export SERVER_PORT=8443
```

#### 2. 直接访问HTTPS

```
https://localhost:8443
```

浏览器会显示安全警告（使用自签名证书），点击"继续访问"即可。

### 方案三：使用Apache HTTPD

```apache
# 启用mod_rewrite
LoadModule rewrite_module modules/mod_rewrite.so

# HTTP虚拟主机 - 重定向到HTTPS
<VirtualHost *:80>
    ServerName localhost
    
    RewriteEngine On
    RewriteRule ^(.*)$ https://%{HTTP_HOST}:8443$1 [R=301,L]
</VirtualHost>

# HTTPS虚拟主机
<VirtualHost *:8443>
    ServerName localhost
    
    SSLEngine on
    SSLCertificateFile /path/to/cert.pem
    SSLCertificateKeyFile /path/to/key.pem
    
    ProxyPreserveHost On
    ProxyPass / http://localhost:8443/
    ProxyPassReverse / http://localhost:8443/
</VirtualHost>
```

## 为什么Spring Boot 4.x移除了内置重定向？

Spring Boot 4.x进行了架构调整：

1. **模块化设计** - 将Web服务器相关功能更加模块化
2. **云原生支持** - 推荐使用云平台的负载均衡器处理重定向
3. **安全性** - 鼓励使用专业的反向代理处理SSL终止
4. **性能** - 外部反向代理通常性能更好

## 生产环境最佳实践

### 推荐的架构

```
客户端 → Nginx/HAProxy (SSL终止) → Spring Boot应用 (内部HTTP)
```

#### 优点：
- ✅ SSL证书管理更简单
- ✅ 可以集中处理多个应用的SSL
- ✅ 更好的性能和缓存
- ✅ 更容易实现负载均衡

#### Nginx配置示例（SSL终止）：

```nginx
server {
    listen 80;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name example.com;
    
    ssl_certificate /etc/letsencrypt/live/example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/example.com/privkey.pem;
    
    location / {
        # 转发到Spring Boot（内部使用HTTP）
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

#### Spring Boot配置：

```yaml
server:
  port: 8080  # 内部使用HTTP
  # 不需要配置SSL，由Nginx处理
```

## 快速测试

### 测试HTTPS是否工作

```bash
# 使用curl测试（跳过证书验证）
curl -k https://localhost:8443/api/test/public

# 应该返回成功响应
```

### 测试HTTP重定向（如果配置了Nginx）

```bash
# 测试HTTP重定向
curl -I http://localhost:80

# 应该返回 301 重定向到 https://localhost:8443
```

## 常见问题

### Q1: 为什么不直接使用Spring Boot的重定向？

**A**: Spring Boot 4.x改变了架构，推荐使用外部反向代理。这样有以下优势：
- 更好的性能
- 更灵活的配置
- 更容易管理多个应用
- 符合云原生最佳实践

### Q2: 开发环境必须配置重定向吗？

**A**: 不是必须的。开发环境中可以直接使用HTTPS访问：
```
https://localhost:8443
```

### Q3: 如何让浏览器信任自签名证书？

**A**: 参考 [HTTPS_GUIDE.md](HTTPS_GUIDE.md) 中的"信任自签名证书"部分。

### Q4: 生产环境需要什么证书？

**A**: 建议使用正式的SSL证书：
- Let's Encrypt（免费，推荐）
- 阿里云SSL证书
- 腾讯云SSL证书
- Comodo、DigiCert等商业证书

## 总结

虽然Spring Boot 4.x不再提供内置的HTTP重定向功能，但通过外部反向代理可以实现更强大、更灵活的配置。

**开发环境**：直接使用HTTPS即可
**生产环境**：使用Nginx等反向代理处理SSL和重定向

这种方式更符合现代Web应用的最佳实践。
