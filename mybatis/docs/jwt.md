# JWT（JSON Web Token）详解与最佳实践

**JWT** 在 [RFC 7519](https://datatracker.ietf.org/doc/html/rfc7519) 中定义，通常指 **JWS（JSON Web Signature）** 的 **Compact Serialization**：`Header.Payload.Signature` 三段 Base64URL 编码，由点号连接。广泛用于 **Access Token**、服务间传递的身份声明等。

> 与 **MyBatis** 无直接耦合：JWT 属于 **认证/授权层**；业务层校验通过后，再在持久层用 `sub` 等 claim 做数据权限控制即可。

相关规范（按需深入）：

- [RFC 7519](https://datatracker.ietf.org/doc/html/rfc7519) — JWT  
- [RFC 7515](https://datatracker.ietf.org/doc/html/rfc7515) — JWS（签名）  
- [RFC 7516](https://datatracker.ietf.org/doc/html/rfc7516) — JWE（加密，与「仅签名的 JWT」不同）  
- [RFC 8725](https://datatracker.ietf.org/doc/html/rfc8725) — JSON Web Signature / Encryption 使用建议  

---

## 目录

1. [结构与安全边界](#1-结构与安全边界)
2. [Header（JOSE）](#2-headerjose)
3. [Payload（Claims）](#3-payloadclaims)
4. [Signature 与算法选型](#4-signature-与算法选型)
5. [校验流程（验证方必读）](#5-校验流程验证方必读)
6. [JWE 与「敏感数据进 JWT」](#6-jwe-与敏感数据进-jwt)
7. [存储与传输](#7-存储与传输)
8. [Refresh Token 与吊销](#8-refresh-token-与吊销)
9. [常见攻击与防御](#9-常见攻击与防御)
10. [何时不适合用 JWT](#10-何时不适合用-jwt)
11. [Java / Spring 生态提示](#11-java--spring-生态提示)
12. [最佳实践清单](#12-最佳实践清单)
13. [参考](#13-参考)

---

## 1. 结构与安全边界

```
Base64URL(UTF8(header)) . Base64URL(UTF8(payload)) . signature
```

| 部分 | 内容 | 机密性 |
|------|------|--------|
| **Header** | 算法、`typ` 等 | **无保密**：任何人可 Base64URL 解码查看 |
| **Payload** | Claims（声明） | **无保密**：仅 **Base64**，不是加密；勿放密码、明文 PII |
| **Signature** | 对 `header.payload` 的 MAC 或私钥签名 | 保证 **完整性 + 来源可信**（在密钥未泄露前提下） |

结论：**签名 JWT 防篡改，不防泄露；Payload 默认视为公开。**

---

## 2. Header（JOSE）

常见字段：

| 字段 | 含义 |
|------|------|
| **`alg`** | 签名算法，如 `HS256`、`RS256`、`ES256` |
| **`typ`** | 常为 `JWT` |
| **`kid`** | Key ID，便于轮换时选对公钥 |

**验证方必须**：根据本端策略选择允许的 `alg` 集合，**禁止**盲目信任客户端传来的 `alg`（见 [RFC 8725](https://datatracker.ietf.org/doc/html/rfc8725) 与下文攻击面）。

---

## 3. Payload（Claims）

### 3.1 Registered Claims（RFC 7519）

| Claim | 含义 | 实践建议 |
|-------|------|----------|
| **`iss`** | Issuer，签发方标识 | 校验是否与配置的 Issuer 一致，防错发 Token |
| **`sub`** | Subject，主体（常为用户 ID） | 稳定唯一；勿用可预测序列 |
| **`aud`** | Audience，受众 | **必须校验**，否则 Token 可能被挪用到别的 API |
| **`exp`** | 过期时间（秒级 Unix 时间戳） | 必设；留时钟偏差容忍（如 ±60s） |
| **`nbf`** | Not Before | 可选 |
| **`iat`** | Issued At | 可选；可结合 `max_age` 检测重放窗口 |
| **`jti`** | JWT ID | 唯一 ID，便于 **一次性使用** 或黑名单 |

### 3.2 Public / Private Claims

自定义 claim 命名建议避免与标准冲突；公开含义的放 Public，仅内部用的仍假定 **Payload 可被看到**。

---

## 4. Signature 与算法选型

| 类型 | 算法示例 | 特点 |
|------|----------|------|
| **对称 HMAC** | `HS256` | 签发与校验共用**同一密钥**；适合单服务；密钥分发难 |
| **非对称 RSA/EC** | `RS256`、`ES256` | **私钥签发、公钥校验**；网关与多微服务只配公钥，**密钥泄露面更小** |

**建议**：多服务、OAuth2 授权服务器 + 多资源服务器场景，优先 **RS256/ES256** + **`kid` 轮换**。

**禁止**：在生产使用 **`none`** 算法；验证库必须显式禁用或拒绝。

---

## 5. 校验流程（验证方必读）

完整校验应包括但不限于：

1. **格式**：三段、Base64URL 合法。  
2. **算法**：仅允许配置白名单中的 `alg`；拒绝 `none`。  
3. **签名**：用对称密钥或 **JWKS 公钥**（按 `kid`）验证 MAC/签名。  
4. **`exp` / `nbf`**：当前时间在允许窗口内。  
5. **`iss` / `aud`**：与当前服务配置一致。  
6. **（可选）`jti`**：查缓存/Redis 是否已使用或已吊销。

**不要**只做「解码 JSON 看 sub」而不验签。

---

## 6. JWE 与「敏感数据进 JWT」

- **JWS（常见 JWT）**：只签名，**Payload 明文可读**。  
- **JWE**：加密后再封装，适合「必须放在客户端携带且不能明文」的极少数场景；实现与密钥管理更复杂。

一般业务：**敏感数据放服务端**（Session、DB），Token 里只放 **标识符与必要元数据**；需要保密传输用 **TLS**，而不是把 JWT 当加密容器滥用。

---

## 7. 存储与传输

| 方式 | 优点 | 风险 |
|------|------|------|
| **`Authorization: Bearer`** | 不触发浏览器 Cookie 的跨站默认行为 | 需前端安全存储；防 XSS |
| **HttpOnly + Secure Cookie** | JS 读不到，降低 XSS 窃取 | 若同站 Cookie，需 **CSRF** 防护（SameSite、CSRF Token、双提交等） |
| **localStorage / sessionStorage** | 实现简单 | **XSS 即可盗 Token**；高敏感应用应避免长期存 Access Token |

**传输**：仅 HTTPS；禁止在 URL 查询串长期附带 JWT（Referer、日志泄露）。

---

## 8. Refresh Token 与吊销

| 目标 | 常见做法 |
|------|----------|
| **缩短 Access Token 寿命** | `exp` 如 5～15 分钟（按业务调） |
| **Refresh Token** | 更长寿命、**轮换**（每次刷新发新 RT、旧 RT 作废）、可存 HttpOnly Cookie 或安全存储 |
| **登出 / 吊销** | 短 AT + 服务端维护 **jti/会话黑名单**、或 **用户级 `token_version`**（改版本则旧 JWT 全部失效） |
| **即时权限变更** | 仅靠 JWT 无法「立刻缩小权限」；需 **短 exp**、**每次敏感操作查库/权限服务**，或混合 Session |

---

## 9. 常见攻击与防御

| 问题 | 说明 | 防御 |
|------|------|------|
| **算法混淆（RS256→HS256）** | 攻击者把 `alg` 改成 `HS256`，用**公钥**当 HMAC 密钥伪造签名 | 固定预期算法；或严格按密钥类型解析 `alg`（见 RFC 8725） |
| **`none` / 弱算法** | 无签名或弱 MAC | 拒绝 `none`；禁止弱密钥与过短 HMAC |
| **Kid 注入 / 路径遍历** | 恶意 `kid` 指向攻击者控制的 JWKS 路径 | 白名单 `kid`、固定 JWKS URL |
| **重放** | 截获仍在有效期内的 Token 重复使用 | 短 `exp`；敏感操作用 **jti 一次性** 或 **mTLS** |
| **XSS** | 窃取 Bearer 或 Cookie | CSP、输入输出编码、HttpOnly、短 AT |
| **CSRF** | Cookie 中的 JWT 被跨站请求携带 | SameSite、CSRF Token、改用 Header 发 AT |

---

## 10. 何时不适合用 JWT

- 需要**任意时刻立即吊销**且不能接受短 `exp` 或额外存储。  
- Token 体积极大、每次请求头膨胀明显。  
- 团队无法保证**验签、iss/aud/exp、算法白名单**一致实现。

此时 **Opaque Token + 授权服务器的 Token Introspection（RFC 7662）** 或 **服务端 Session** 更合适。

---

## 11. Java / Spring 生态提示

- **签发/校验**：常用库如 **Nimbus JOSE + JWT**、**jjwt**；注意版本与 CVE。  
- **Spring Authorization Server / Spring Security OAuth2 Resource Server**：资源服务器用 **JWT Decoder + JWKS URI**，由框架处理签名与 `issuer` 校验；业务代码从 `Authentication` 取 `sub`。  
- **与 MyBatis**：在 Service 层用 `sub` 或自定义 claim 做 **数据范围过滤**（如 `WHERE tenant_id = ?`），不要在 Mapper XML 里硬编码用户标识来源。

---

## 12. 最佳实践清单

1. **必验签**；**白名单 `alg`**；禁用 **`none`**。  
2. **必校 `exp`、`aud`、`iss`**；时钟偏差容忍。  
3. **Access Token 短寿命**；敏感能力配合 **服务端权限检查**。  
4. **Payload 不放秘密**；需要保密用 TLS + 服务端存储，或评估 **JWE**。  
5. **多服务优先非对称密钥** + **JWKS + kid 轮换**。  
6. **Refresh 轮换**；登出使 RT 失效；必要时 **jti / 版本号** 吊销链。  
7. **优先 HttpOnly Cookie 或 Header**；谨慎 **localStorage**。  
8. **HTTPS Only**；不把 JWT 放 URL。  
9. 网关与微服务 **统一校验规范**（同一套 JWKS 与 claim 策略）。  
10. 依赖 **RFC 8725** 做代码评审清单。

---

## 13. 参考

- [RFC 7519 — JWT](https://datatracker.ietf.org/doc/html/rfc7519)  
- [RFC 8725 — JWT Best Current Practices for JWT](https://datatracker.ietf.org/doc/html/rfc8725)  
- [OWASP JWT Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html)（Java 向，通用思想可借鉴）

---

## 一句话总结

**JWT 是带签名的可验证声明载体，不是加密保险箱：验签、控算法、短寿命、校 iss/aud/exp、慎存客户端；吊销与实时权限靠短 AT + Refresh 轮换或服务端状态配合。**
