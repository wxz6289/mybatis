# 常用会话跟踪技术总结、对比与最佳实践

> 本文中的「会话」指 **Web / 分布式应用中识别同一用户多次请求** 的机制（通常对应 `HttpSession`、登录态等）。  
> 与 **MyBatis `SqlSession`**（一次数据库会话/事务边界）不是同一概念：后者是持久层 API，不负责浏览器用户识别。

---

## 1. 常见技术一览

| 技术 | 典型做法 | 常见场景 |
|------|----------|----------|
| **服务端 Session + Cookie** | 服务端生成 `JSESSIONID`（或自定义名），值指向内存/Redis 中的会话数据；浏览器用 **Cookie** 自动携带 | 传统 Web、Spring Session |
| **Cookie 自包含（JWT 等）** | 将签名后的声明（claims）放在 Cookie 或 Header 中，服务端**不存**或只存黑名单 | 无状态 API、跨域 BFF |
| **Authorization Bearer** | Access Token 放在 `Authorization: Bearer <token>` | SPA、移动端、OpenAPI |
| **URL 重写（Session ID 拼在 URL）** | `;jsessionid=...` 或查询参数 | 历史兼容无 Cookie 环境；**不推荐** |
| **隐藏域 / 表单 Token** | 每次表单带隐藏字段；多用于 **CSRF Token**，一般不单独作为「整站会话」 | 表单提交防 CSRF |
| **客户端存储（localStorage / sessionStorage）** | 前端存 JWT 再在请求里带上 | SPA 常见；** XSS 风险高**，需配合策略 |
| **Sticky Session（会话黏连）** | 负载均衡把同一客户端固定到同一节点，节点本地内存存 Session | 无集中存储时的权宜之计 |

---

## 2. 原理简述

### 2.1 服务端 Session + Cookie（最经典）

1. 用户登录成功后，服务端创建会话对象，生成**随机、高熵**的会话标识（Session ID）。
2. 通过 **`Set-Cookie`** 把该 ID 发给浏览器（如 `JSESSIONID=...`）。
3. 后续同站请求浏览器**自动**带上 Cookie；服务端用 ID 查会话存储（内存、Redis、DB 等）。

**特点**：敏感数据在服务端，Cookie 里只有句柄；吊销会话、集中登出容易。

### 2.2 无状态 JWT（JSON Web Token）

登录后签发 **签名**（常 HMAC 或 RSA）的 Token，内含 `sub`、`exp` 等；服务端用密钥校验签名即可验证，**默认不在服务端存会话**（除非做 blocklist 或短期会话表）。

**特点**：易水平扩展、适合微服务；**吊销与即时失效**相对麻烦，往往依赖短 `exp` + Refresh Token 或服务端记录版本号。

### 2.3 Bearer Token（Header）

与 JWT 常结合使用：Token 放在 **Authorization** 头，避免浏览器自动带 Cookie 的跨站行为差异；适合 **API、移动端**。

### 2.4 URL 中的 Session ID

禁用 Cookie 时代的兼容方案。Session ID 暴露在 **Referer、日志、历史记录** 中，**泄露面大**，现代应用应避免。

### 2.5 Sticky Session

同一用户总打到同一台机器，Session 存在本机内存。**某台宕机会话丢失**；扩缩容时体验差。通常应升级为 **集中式 Session 存储（如 Redis）**。

---

## 3. 对比维度

| 维度 | 服务端 Session + Cookie | JWT / 自包含 Token（Cookie 或 Header） |
|------|-------------------------|----------------------------------------|
| **服务端存储** | 需要（或 Spring Session 等集中存储） | 可不存（纯无状态）或少量（黑名单/刷新令牌） |
| **水平扩展** | 需 **Redis 等共享存储** 或黏连 | 天然易扩展（校验签名即可） |
| **即时吊销 / 强制下线** | 删会话即可 | 需额外机制（短过期、Redis 黑名单、`token_version`） |
| **XSS** | Cookie 若 **HttpOnly** 则 JS 读不到，相对有利 | 若在 `localStorage` 易被 XSS 窃取；**HttpOnly Cookie 存 JWT** 可改善 |
| **CSRF** | Cookie 自动提交，需 **CSRF Token / SameSite** | 若仅 Header 发送，**CSRF 风险通常更低**；放 Cookie 时仍要注意 |
| **体积** | Cookie 很小 | Token 可能较大，注意 Header/Cookie 大小限制 |
| **移动端 / 第三方 API** | Cookie 行为依赖客户端 | Bearer 更统一 |

**URL Session**：安全性、隐私性最差，仅作了解，不作为新系统设计选项。

---

## 4. 最佳实践（结合 Java / Spring 常见栈）

### 4.1 Cookie 承载会话 ID 时

- **`HttpOnly`**：禁止 JavaScript 读取，降低 XSS 窃取会话 ID 的概率。
- **`Secure`**：仅 HTTPS 传输。
- **`SameSite`**：至少 `Lax`；跨站要求高安全时用 `Strict` 或配合 CSRF Token；跨站 SSO 需单独设计。
- **会话固定攻击**：登录成功后 **轮换 Session ID**（`request.changeSessionId()` 或等价）。
- **整站 HTTPS**，不要在明文 HTTP 上发会话 Cookie。

### 4.2 服务端 Session 架构

- 多实例部署：使用 **Redis / Hazelcast** 等 **集中式 Session**，避免仅靠 Sticky。
- 会话数据只放**必要字段**；大对象、权限列表可考虑缩短或重建。
- 设置合理 **超时**（空闲超时 + 绝对过期）；敏感操作可要求 **重新认证**。

### 4.3 使用 JWT 时

- **短 `exp`**（如 15 分钟～1 小时，按业务定）；Refresh Token **更长但需轮换与存储**（或设备绑定）。
- **密钥与算法**：禁止弱密钥；校验 `alg`、使用成熟库；敏感操作不放在可被客户端篡改的 payload 中且无服务端校验。
- **存储位置**：优先 **HttpOnly Cookie** 或仅内存 + Header；谨慎使用 `localStorage` 存长期访问令牌。
- 需要「立即作废」时：服务端维护 **jti 黑名单**、**用户级 token 版本号** 或改用短 JWT + Redis 会话混合模式。

### 4.4 API / SPA

- 优先 **Authorization: Bearer** 或 **BFF 写 HttpOnly Cookie**，由 BFF 换 Token，减少前端持有明文。
- **CORS** 白名单；勿 `Access-Control-Allow-Origin: *` 搭配携带凭证。

### 4.5 与 MyBatis 一起使用时

- **认证与会话**：在 **Servlet 过滤器 / Spring Security** 中完成；业务层从 `SecurityContext` 或 `HttpSession` 取当前用户。
- **MyBatis `SqlSession`**：按「一次请求一线程」或 Spring 管理的生命周期使用，**不要把 HttpSession 塞进 SqlSession**；多租户可在拦截器里根据当前用户设置数据源或 `tenant_id`。

---

## 5. 选型建议（简表）

| 需求 | 建议 |
|------|------|
| 传统服务端渲染、强管控登录态 | **服务端 Session + HttpOnly Cookie** + Redis（集群） |
| 纯 API、多端、微服务网关 | **短 JWT + Refresh** 或 **Opaque Token + Token Introspection（RFC 7662）** |
| 既要 SSR 又要 API | **BFF**：浏览器侧 HttpOnly Cookie，BFF 用 Bearer 调下游 |
| 极高即时吊销要求 | **服务端 Session** 或 **JWT + 服务端状态（版本号/黑名单）** 混合 |

---

## 6. 一句话总结

**默认优先「服务端 Session + 安全 Cookie（HttpOnly、Secure、SameSite）+ 集中存储」；API 与无状态扩展用短生命周期 Token 并谨慎处理吊销与 XSS；避免把会话 ID 放在 URL；集群勿依赖单机内存或长期 Sticky。**
