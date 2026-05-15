# YAML（yml）格式与语法总结

> 适用场景：配置文件（如 Spring Boot 的 `application.yml`）、CI/CD 配置、K8s 资源描述等。  
> YAML 目标是“**可读性优先**”，本质是键值映射（Map）、列表（List）、标量（String/Number/Boolean/Null）的组合。

---

## 1. YAML 基础规则

1. **用缩进表示层级**，不使用 `{}` 和 `[]`（虽然部分写法支持）。
2. **缩进必须统一**，通常用 **2 个空格**（不要混用 Tab）。
3. **键值分隔符**是冒号加空格：`key: value`。
4. `#` 开头是注释。
5. 文件后缀常见为 `.yaml` 或 `.yml`，两者等价。

示例：

```yaml
server:
  port: 8080
  servlet:
    context-path: /api
```

---

## 2. 三大核心数据结构

## 2.1 对象（Map / Dictionary）

```yaml
person:
  name: Alice
  age: 18
```

等价 JSON：

```json
{
  "person": {
    "name": "Alice",
    "age": 18
  }
}
```

## 2.2 数组（List）

```yaml
servers:
  - host: 10.0.0.1
    port: 8080
  - host: 10.0.0.2
    port: 8081
```

也可以是简单标量列表：

```yaml
tags:
  - java
  - spring
  - mybatis
```

## 2.3 标量（Scalar）

- 字符串：`name: "dk"` 或 `name: dk`
- 数字：`count: 10`
- 布尔：`enabled: true`
- 空值：`remark: null` 或 `remark: ~`

---

## 3. 字符串与多行文本

## 3.1 普通字符串

```yaml
title: hello
quote: "hello: world"
path: 'C:\temp\data'
```

- 双引号支持转义（如 `\n`）。
- 单引号更“原样”，转义规则更少。

## 3.2 多行字符串（重点）

### `|` 保留换行

```yaml
desc: |
  line1
  line2
```

结果近似：

```
line1
line2
```

### `>` 折叠换行（换行变空格）

```yaml
summary: >
  this is line1
  this is line2
```

结果近似：

```
this is line1 this is line2
```

---

## 4. 多文档与引用能力

## 4.1 多文档分隔

同一个 yml 文件可包含多个文档，用 `---` 分隔：

```yaml
spring:
  profiles:
    active: dev
---
spring:
  config:
    activate:
      on-profile: dev
server:
  port: 8081
```

## 4.2 锚点与别名（复用片段）

```yaml
default: &default
  timeout: 30
  retries: 3

serviceA:
  <<: *default
  url: http://a.example.com

serviceB:
  <<: *default
  url: http://b.example.com
```

- `&name`：定义锚点
- `*name`：引用锚点
- `<<:`：合并映射（merge key）

---

## 5. 常见易错点（高频）

1. **Tab 缩进**：YAML 对 Tab 很敏感，推荐只用空格。
2. **冒号后少空格**：`key:value` 可能解析异常，写成 `key: value`。
3. **布尔与数字误解析**：`on/off/yes/no` 在某些解析器中可能被当布尔；版本号如 `1.0` 可能被当数字。  
   - 解决：必要时加引号，如 `"on"`、`"1.0"`。
4. **前导零数字**：如 `00123` 可能被当八进制或丢失前导零。  
   - 解决：编号、邮编、ID 等统一按字符串处理：`"00123"`。
5. **特殊字符**：值中有 `:`, `#`, `{}`, `[]`, `,`, `&`, `*` 等，建议加引号。
6. **层级错位**：同级键缩进必须一致，否则会导致结构变化甚至启动失败。

---

## 6. Spring Boot / MyBatis 场景建议

示例：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/demo?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: ${DB_PASSWORD:root}
    driver-class-name: com.mysql.cj.jdbc.Driver

mybatis:
  mapper-locations: classpath:/mapper/**/*.xml
  type-aliases-package: com.dk.learn.entity
  configuration:
    map-underscore-to-camel-case: true
```

建议：

1. **敏感信息不要明文写死**（密码、AK/SK），优先环境变量或密钥管理。
2. 使用占位符默认值：`${ENV_NAME:defaultValue}`，提高本地可启动性。
3. 配置项命名尽量与框架约定一致（kebab-case 常见）。
4. 环境隔离：`application-dev.yml`、`application-prod.yml` + profile 激活。

---

## 7. YAML 最佳实践（可直接执行）

1. **统一风格**
   - 统一 2 空格缩进。
   - 冒号后保留一个空格。
   - 同类配置放在一起，按模块分组。

2. **可读优先，避免“技巧滥用”**
   - 锚点/别名虽强大，但过度使用会降低可读性。
   - 复杂逻辑不要塞进 yml，配置只存“声明式数据”。

3. **环境与密钥分离**
   - 通用配置放主文件。
   - 环境差异放 profile 文件。
   - 密钥从环境变量、配置中心、Vault 等注入。

4. **显式字符串化高风险字段**
   - 版本号、开关字面值、前导零编码、日期样式等，建议加引号避免隐式类型转换。

5. **增加注释但不过量**
   - 对“为什么这么配”写注释，不必对显而易见项重复解释。

6. **配置校验与格式检查**
   - 在 CI 加 YAML Lint（如 `yamllint`）。
   - 应用启动阶段启用配置绑定校验（如 Spring `@ConfigurationProperties` + `@Validated`）。

7. **避免重复，适度复用**
   - 重复多的片段可提取锚点，或通过配置中心模板化。

---

## 8. 一份推荐模板

```yaml
app:
  name: mybatis-demo
  env: ${APP_ENV:dev}

server:
  port: ${SERVER_PORT:8080}

spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/demo}
    username: ${DB_USER:root}
    password: ${DB_PASSWORD:root}

mybatis:
  mapper-locations: classpath:/mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true
```

---

## 9. 速记

- YAML = **缩进 + 键值 + 列表**。
- 易错点主要是：**缩进、类型隐式转换、特殊字符**。
- 最佳实践核心：**可读性、可维护性、环境隔离、安全合规**。
