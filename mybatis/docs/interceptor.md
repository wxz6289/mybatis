# 拦截器（Interceptor）核心内容与最佳实践

在 Java 生态里「拦截器」一词常指两类不同机制，本仓库文档路径在 **MyBatis** 下，故 **两者都说明**，避免混用：

| 名称 | 规范 / 框架 | 拦截位置 |
|------|----------------|----------|
| **`HandlerInterceptor`** | **Spring MVC** | `DispatcherServlet` 与 **Controller（Handler）** 之间 |
| **`Interceptor`（插件）** | **MyBatis** | **Executor / StatementHandler / ParameterHandler / ResultSetHandler** 调用链上 |

二者与 **Servlet `Filter`** 的关系见 [`filter.md`](./filter.md)。

---

## 目录

1. [Spring MVC：`HandlerInterceptor`](#1-spring-mvchandlerinterceptor)
2. [Spring：注册、匹配与顺序](#2-spring注册匹配与顺序)
3. [MyBatis：`Interceptor` 插件](#3-mybatisinterceptor-插件)
4. [MyBatis：签名、`Invocation` 与链式调用](#4-mybatis签名invocation-与链式调用)
5. [Filter、HandlerInterceptor、MyBatis Interceptor 对照](#5-filterhandlerinterceptormybatis-interceptor-对照)
6. [Spring：全局异常处理（与拦截器的关系）](#6-spring全局异常处理与拦截器的关系)
7. [最佳实践](#7-最佳实践)
8. [参考](#8-参考)

---

## 1. Spring MVC：`HandlerInterceptor`

接口（Spring Framework）：`org.springframework.web.servlet.HandlerInterceptor`

| 方法 | 调用时机 | 典型用途 |
|------|----------|----------|
| **`preHandle`** | Controller **执行前** | 登录校验、幂等 token、解析并注入上下文 |
| **`postHandle`** | Controller **返回后**、**视图渲染前** | 向 `ModelAndView` 补公共属性（注意 REST 常无 `ModelAndView`） |
| **`afterCompletion`** | **整个请求结束**（含视图渲染）之后 | 释放资源、清理 `ThreadLocal`、记慢请求日志 |

### 1.1 `preHandle` 返回值

- 返回 **`true`**：继续执行后续 Interceptor 与 **Handler**。
- 返回 **`false`**：**中断**后续链，**不会**再调用该 Handler；需自行设置响应（状态码、body），否则客户端可能收到空或异常响应。

### 1.2 与异常的关系

- 若 **`preHandle` 已返回 `true`**，之后链路中抛异常，**`postHandle` 通常不会调用**（Handler 未正常完成）；**`afterCompletion` 仍会调用**，且第四个参数 **`ex`** 可能非空，适合统一清理与审计。

### 1.3 `HandlerMethod` 与 REST

在 `preHandle` 中可将 `handler` 转为 **`HandlerMethod`** 读取 `@RequestMapping`、注解元数据，做细粒度鉴权。纯 **`@RestController`** 时 **`postHandle`** 往往用处有限（无视图），更多逻辑放在 **`preHandle` / `afterCompletion`**；**统一异常与错误 JSON** 见 [§6 Spring：全局异常处理](#6-spring全局异常处理与拦截器的关系)。

---

## 2. Spring：注册、匹配与顺序

### 2.1 注册方式

实现 **`WebMvcConfigurer.addInterceptors`**，向 **`InterceptorRegistry`** 添加拦截器并配置：

- **`addPathPatterns`**：要拦截的路径（Ant 风格或 PathPattern，视 Spring 版本）。 `*` 仅一级  `**` 多级路径
- **`excludePathPatterns`**：排除静态资源、健康检查、登录接口等。


### 2.2 顺序

- **`order(int)`** 或 **`InterceptorRegistration#order`**：数值 **越小越先** 执行 `preHandle`；`postHandle` / `afterCompletion` 以 **相反顺序** 调用（洋葱模型）。

### 2.3 与异步 MVC

异步请求、 **`Callable` / `DeferredResult`** 下，拦截器与线程的绑定关系更复杂；需阅读当前 Spring 版本文档中 **Async** 与 **`HandlerInterceptor`** 的说明，避免在错误的线程清理 `ThreadLocal`。

### 2.4 Spring Boot 3 / Jakarta

包名为 **`jakarta.servlet.*`**；`HandlerInterceptor` 仍属 **`org.springframework.web.servlet`**。

---

## 3. MyBatis：`Interceptor` 插件

包：`org.apache.ibatis.plugin.Interceptor`

MyBatis 在内部创建 **Executor、StatementHandler、ParameterHandler、ResultSetHandler** 时，会用 **JDK 动态代理** 包一层插件链。你的 **`Interceptor`** 在指定方法的 **`invoke` 前后** 执行。

**用途举例**：分页改写 SQL、多租户拼接 `tenant_id`、慢 SQL 日志、加密/脱敏参数或结果、禁止全表更新等。

### 3.1 定义插件

- 实现 **`Interceptor`**：`Object intercept(Invocation invocation) throws Throwable`。
- 类上标注 **`@Intercepts`**，内含多个 **`@Signature`**：
  - **`type`**：要拦截的四大接口之一。
  - **`method`**：方法名，如 `query`、`update`、`prepare`。
  - **`args`**：参数类型数组，**必须与运行时一致**，否则匹配失败。

### 3.2 注册方式

- **XML**：`<plugins><plugin interceptor="全限定类名"/></plugins>`。
- **Spring Boot**：`@Bean` 返回你的 `Interceptor` 实现，或 **`SqlSessionFactoryBean.setPlugins`**；若用 **mybatis-spring-boot-starter**，常见为 **`@Bean` + 自动注册**（以当前 starter 文档为准）。

### 3.3 多个插件顺序

**XML 中 `<plugin>` 声明顺序** 或 Spring 中 Bean 注册顺序会影响 **代理嵌套顺序**，进而影响 SQL 改写结果（例如先分页再租户与先租户再分页可能不同）。**应在项目内固定顺序并文档化**。

---

## 4. MyBatis：签名、`Invocation` 与链式调用

### 4.1 `Invocation`

- **`getTarget()`**：被代理对象。
- **`getMethod()` / `getArgs()`**：反射调用信息。
- **`proceed()`**：调用链 **下一环**（最终到达真实 JDBC 操作）。**必须**在合适位置调用，除非明确短路（极少）。

### 4.2 改写 SQL 的常见入口

- 通过 **`StatementHandler`**（常为 **`RoutingStatementHandler` → `PreparedStatementHandler`**）取 **`BoundSql`**，改 **`sql`** 字符串。
- 使用 **`MetaObject.forObject(handler, ...)`** 包装 target，便于取 **`delegate.boundSql`** 等嵌套属性（注意不同 MyBatis 版本与插件组合下 **delegate** 层级）。

### 4.3 `Plugin.wrap`

工具方法 **`Plugin.wrap(target, interceptor)`** 按 `@Signature` 判断是否需要为 `target` 创建代理；自己写代理时也应遵循规范，避免对非目标类型误包。

### 4.4 注意点

- **只拦声明的方法**：`@Signature` 写错则 **完全不生效**，且不易在编译期发现。
- **性能**：每条语句多一层代理与可能的 SQL 解析，避免在 `intercept` 里做重计算或 N+1 额外查询。
- **与 MyBatis-Plus**：内置分页、多租户等也是拦截器体系，**避免重复改写 SQL** 导致语法错误。

---

## 5. Filter、HandlerInterceptor、MyBatis Interceptor 对照

| 维度 | Servlet **Filter** | Spring **HandlerInterceptor** | MyBatis **Interceptor** |
|------|-------------------|-------------------------------|-------------------------|
| **规范** | Jakarta Servlet | Spring MVC | MyBatis |
| **边界** | 整个 Web 应用 URL（可调 `dispatcherTypes`） | **进入 DispatcherServlet 之后**、映射到 Handler 的请求 | **SQL 执行链** |
| **能否拦静态资源** | 能（视 mapping） | 通常 **不经过** Spring MVC 的不会拦 | 不涉及 |
| **能否改 SQL** | 否 | 否 | **能** |
| **典型用途** | 编码、认证、CORS、全局限流 | 登录、权限、幂等、操作日志 | 分页、租户、审计 SQL |

**选型**：Web 入口用 **Filter**；与 **Controller 映射、Handler 注解** 强相关用 **`HandlerInterceptor`**；与 **JDBC/MyBatis 语句** 强相关用 **MyBatis `Interceptor`**。

---

## 6. Spring：全局异常处理（与拦截器的关系）

全局异常处理解决的是：**Controller / 参数解析 / 消息转换** 等环节抛出的异常，如何统一转成 HTTP 状态码与响应体。**它不是 `HandlerInterceptor` 的替代品**，而是 **DispatcherServlet 内 `HandlerExceptionResolver` 链** 上的机制，与拦截器 **配合使用**。

### 6.1 调用顺序（概念）

1. **`preHandle`** 依次执行。  
2. **Handler（Controller）** 执行；其间可能抛异常。  
3. 若未短路：正常则 **`postHandle`**；无论成功或失败，只要 `preHandle` 曾为 `true`，最终会 **`afterCompletion(request, response, handler, ex)`**（`ex` 可能来自 Controller 或视图层）。  
4. **异常发生时**：Spring MVC 使用 **`HandlerExceptionResolver`**（含对 **`@ControllerAdvice`** 的解析）尝试把异常解析为 **`ModelAndView` / `ResponseEntity` / ProblemDetail** 等响应；**解析成功后** 对客户端而言是一次「正常错误响应」，**`afterCompletion` 仍会带上该异常**（具体以当前 Spring 版本行为为准，用于清理与日志）。

要点：**在 `afterCompletion` 里不要再次向 `response` 写 body**（通常已提交或已由异常处理器写好）；这里只做 **资源释放、ThreadLocal 清理、审计日志**。

### 6.2 推荐方式：`@ControllerAdvice` / `@RestControllerAdvice`

- 使用 **`@ExceptionHandler`** 按异常类型分支处理，返回 **`ResponseEntity<ErrorBody>`** 或 Spring 6 的 **`ProblemDetail`**（RFC 7807 风格）。  
- 多个 `@ControllerAdvice` 时可用 **`@Order`** 控制优先级（**数值越小越优先**）。  
- **细粒度**：可为某包或某 Controller 写「局部」`@ControllerAdvice(assignableTypes = …)` / `basePackages`（以 Spring 文档为准）。

### 6.3 与 `HandlerInterceptor` 的分工

| 场景 | 建议放在哪里 |
|------|----------------|
| 业务校验失败、可预期领域异常 | **`@ExceptionHandler`**，统一 body 与错误码 |
| 鉴权失败、未登录 | **`preHandle` 返回 `false`** 并写 401，或抛 **Spring Security** 异常由异常体系处理（二选一并团队统一） |
| 请求结束必做的清理（关流、remove ThreadLocal） | **`afterCompletion`**，根据 **`ex` 是否为空** 打不同级别日志 |
| 在进入 Controller 之前且无法映射为「某个 Handler」的异常 | 多在 **Filter** 或容器层处理；**`@ExceptionHandler` 管不到 Filter 里抛的异常**（除非被包装后再次进入 MVC） |

### 6.4 与 MyBatis / 数据访问异常

- Mapper 或 `SqlSession` 抛出的 **`PersistenceException`**、`DuplicateKeyException` 等常在 **Service 边界** 被包装为领域异常；在 **`@ExceptionHandler`** 中映射为 **409 / 400** 等，**避免**把 JDBC 原文直接返回给客户端。  
- **不要在 MyBatis `Interceptor` 里吞掉异常又返回 null** 冒充成功；应 **`throw`** 或让框架按异常处理。

### 6.5 Spring Boot 与「兜底」

- 未被任何 **`HandlerExceptionResolver`** 处理的异常，可能落到 **Spring Boot 的 `/error` 机制**（`BasicErrorController`、可自定义 **`ErrorController`** / **`ErrorAttributes`**）。  
- **生产环境**：关闭或限制 **Whitelabel** 暴露的堆栈与敏感字段；错误体字段与 **ProblemDetail** 对齐，便于前端与监控解析。

### 6.6 小结

- **统一 JSON 错误体、HTTP 状态、国际化消息**：优先 **`@RestControllerAdvice`**。  
- **`afterCompletion`**：清理与观测，**不重复**写响应。  
- **`preHandle` 返回 `false`**：属于「未进入 Handler 的短路」，**不会**走针对 Controller 的 `@ExceptionHandler`（除非你在 `preHandle` 内主动抛异常并交给上层，一般不推荐）。

---

## 7. 最佳实践

### 7.1 Spring `HandlerInterceptor`

1. **`preHandle` 做重逻辑**：鉴权、限流、解析 Token；失败时 **明确写响应** 并 `return false`。
2. **`ThreadLocal` / 用户上下文**：在 `preHandle` 设置，在 **`afterCompletion` 务必 `remove`**，避免线程池复用泄漏。
3. **不要**在拦截器里直接 **`SqlSession` 操作数据库**，除非极简单且团队约定；业务仍走 **Service**。
4. **排除路径**写全：Swagger、Actuator、静态资源、OPTIONS 预检等。
5. **与 Spring Security**：Security Filter 链在前；**`HandlerInterceptor`** 适合业务级细粒度规则，避免与 Security 职责重复打架。
6. **REST 为主**时，公共响应头、异常体更多用 **`Filter`** 或 **`@ControllerAdvice`** 统一处理，`postHandle` 仅在有需要时再用。

### 7.2 MyBatis `Interceptor`

1. **最小拦截面**：`@Signature` 精确到 **具体 type + method + args**，避免拦整个 `Executor`。
2. **先 `proceed()` 再包装结果** 或 **先改 BoundSql 再 `proceed()`**，逻辑要单一可读；复杂改写加单元测试覆盖多种 Mapper。
3. **顺序文档化**：分页、租户、乐观锁等插件 **顺序敏感**。
4. **禁止**在插件里 **`System.out` 打大对象** 或打印完整 SQL 到生产（脱敏、采样）。
5. **分页**：优先成熟库（如 **PageHelper**）或框架内置；自研时注意 **count 查询** 与 **方言**（MySQL/PostgreSQL）。
6. **升级 MyBatis**：大版本升级后重新验证 **delegate 结构** 与 **`@Signature`** 是否仍匹配。

---

## 8. 参考

- Spring：`HandlerInterceptor`、[WebMvcConfigurer](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-config/interceptors.html)
- Spring MVC 异常处理：[ExceptionHandler](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-exceptionhandler.html)、[ControllerAdvice](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-advice.html)
- MyBatis：[Plugins](https://mybatis.org/mybatis-3/configuration.html#plugins)
- 同目录：[Filter](./filter.md)、[Servlet](./servlet.md)

---

## 一句话总结

**Spring 的 `HandlerInterceptor` 拦在 Web MVC 的 Handler 前后，适合鉴权与请求级上下文；统一错误响应交给 `@RestControllerAdvice` + `@ExceptionHandler`，在 `afterCompletion` 做清理与日志；MyBatis 的 `Interceptor` 拦在 JDBC 调用链上，适合分页、多租户与 SQL 级横切；与 Servlet `Filter` 分层不同，按边界选型并注意顺序、线程与 `proceed()` 链完整性。**
