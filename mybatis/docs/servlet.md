# Java Servlet 核心内容与最佳实践

**Servlet** 是 Jakarta Servlet 规范（原 Java EE `javax.servlet`，现多为 **`jakarta.servlet`**）中的 **Web 组件**：由 **Servlet 容器**（如 Tomcat、Jetty、Undertow）加载，按 URL 映射接收 HTTP 请求并产生响应。Spring MVC 的 **`DispatcherServlet`** 本身也是一个 Servlet，应用里多数「业务 Servlet」已被 **Controller** 替代，但理解 Servlet 仍是掌握 Filter、Session、异步与部署模型的基础。

> 与 **MyBatis**：Servlet / Controller 属于 **Web 接入层**；MyBatis 在 **持久层**。业务与事务应落在 **Service**；Servlet 或 Controller 只做参数绑定、调用 Service、选择视图或序列化 JSON。

规范入口：[Jakarta Servlet Specification](https://jakarta.ee/specifications/servlet/)（版本以项目依赖为准，如 5.0 / 6.0）。

---

## 目录

1. [在整体架构中的位置](#1-在整体架构中的位置)
2. [核心类型与继承体系](#2-核心类型与继承体系)
3. [生命周期](#3-生命周期)
4. [请求处理：`service` 与 HTTP 动词方法](#4-请求处理service-与-http-动词方法)
5. [`ServletConfig` 与 `ServletContext`](#5-servletconfig-与-servletcontext)
6. [映射与路径语义](#6-映射与路径语义)
7. [请求转发与包含](#7-请求转发与包含)
8. [Session 与 Cookie（Servlet 视角）](#8-session-与-cookieservlet-视角)
9. [异步、Multipart、错误处理](#9-异步multipart错误处理)
10. [线程模型与资源管理](#10-线程模型与资源管理)
11. [与 Filter、Spring MVC 的关系](#11-与-filterspring-mvc-的关系)
12. [最佳实践](#12-最佳实践)
13. [参考](#13-参考)

---

## 1. 在整体架构中的位置

```
客户端 → 容器监听端口 → Filter 链 → Servlet.service() → （可选）转发/包含 → 响应
```

- **容器**负责：Socket、HTTP 解析、线程（或异步调度）、Servlet 与 Filter 生命周期、Session 管理（可持久化到 Redis 等由插件实现）。
- **Servlet**负责：一次请求的处理逻辑入口（或像 `DispatcherServlet` 那样再分发给 Handler）。

---

## 2. 核心类型与继承体系

| 类型 | 作用 |
|------|------|
| **`Servlet`** | 顶层接口：`init`、`service`、`destroy` |
| **`GenericServlet`** | 与协议无关的抽象基类，实现 `ServletConfig` 存取等 |
| **`HttpServlet`** | HTTP 专用：模板方法 **`service`** 根据方法分发到 **`doGet` / `doPost` / …** |

应用开发 **几乎总是** 继承 **`HttpServlet`**（或仅使用框架提供的 `DispatcherServlet`）。

---

## 3. 生命周期

| 阶段 | 方法 | 说明 |
|------|------|------|
| **加载与实例化** | 容器创建 **一个** Servlet 实例（每个 `<servlet>` 定义） | 时机可由 **`load-on-startup`** 控制：≥0 时随 Web 应用启动加载；未配置则首次请求再加载 |
| **初始化** | **`init(ServletConfig)`** | 只调一次；可读取 **初始化参数**、缓存只读配置；失败则 Servlet 不可用 |
| **服务** | **`service`**（`HttpServlet` 再分发到 `doXxx`） | **每次请求**在容器线程上调用（异步模式下另论） |
| **销毁** | **`destroy()`** | Web 应用停止或 Servlet 被卸载；释放 `init` 中占用的资源 |

**注意**：`init` 抛 `ServletException` 后该 Servlet 不会进入可用状态；不要在 `destroy` 里假设仍有活跃请求（应先优雅停机或依赖容器顺序）。

---

## 4. 请求处理：`service` 与 HTTP 动词方法

`HttpServlet.service` 根据 **`HttpServletRequest.getMethod()`** 调用：

- `doGet` / `doPost` / `doPut` / `doDelete` / `doHead` / `doOptions` / `doTrace`

常见约定：

- **GET**：幂等、可缓存、参数在查询串；**不应**改服务端状态。
- **POST**：提交表单、创建资源；非幂等。
- **PUT/PATCH**：更新；**DELETE**：删除（REST 风格时常用）。

若子类只重写 `doGet` 而客户端发 **POST**，默认父类可能返回 **405 Method Not Allowed**（取决于实现），故需按接口契约实现对应方法。

---

## 5. `ServletConfig` 与 `ServletContext`

| 对象 | 范围 | 典型用途 |
|------|------|----------|
| **`ServletConfig`** | **单个** Servlet | `getServletName()`、`getInitParameter(name)`、`getServletContext()` |
| **`ServletContext`** | **整个** Web 应用（WAR） | `getRealPath`、`getResourceAsStream`、**全局属性** `setAttribute`、上下文参数 `context-param`、注册 Servlet/Filter（Servlet 3.0+ `ServletContext` API） |

**不要**用 `ServletContext` 存 **每个用户** 的大量可变状态（应使用 **Session** 或外部存储）。

---

## 6. 映射与路径语义

### 6.1 声明方式

- **`web.xml`**：`<servlet>` + `<servlet-mapping>` + `url-pattern`。
- **注解**：`@WebServlet(urlPatterns = {"/api/hello"})`（需 **`@ServletComponentScan`** 或等价扫描，Spring Boot 常用）。

### 6.2 `url-pattern` 规则（规范级）

常见模式包括：**精确路径**、**路径前缀**（`/*`）、**扩展名**（`*.do`）等；**同一路径被多个 pattern 匹配时** 有**最长路径优先**等规则，复杂部署时应查当前 Servlet 版本规范，避免歧义。

### 6.3 常用 API（`HttpServletRequest`）

| API | 含义 |
|-----|------|
| **`getContextPath()`** | 应用上下文前缀，如 `/myapp` |
| **`getServletPath()`** | 匹配到 Servlet 的部分 |
| **`getPathInfo()`** | 映射剩余路径（取决于 mapping 类型，可为 `null`） |
| **`getRequestURI()` / `getRequestURL()`** | 原始 URI / 完整 URL |

路径拼接与重定向时注意 **context path**，避免硬编码漏前缀。

---

## 7. 请求转发与包含

- **`RequestDispatcher.forward`**：将**同一次请求**交给另一个 Web 组件（Servlet/JSP），**原 Servlet 不应再写响应**；URL 栏不变。
- **`include`**：把另一组件输出**并入**当前响应。

与 **重定向 `sendRedirect`** 区别：重定向是 **302/301** + 新 URL，浏览器再次发起 **新请求**（Session 仍通常保留，但无 POST body）。

---

## 8. 与 Session、Cookie（Servlet 视角）

- **`request.getSession(true)`**：无则创建 **HttpSession**；`false` 只获取已有。
- Session 默认以 **`JSESSIONID` Cookie**（或 URL 重写，不推荐）关联。
- **`response.addCookie`**：设置 **Set-Cookie**；属性如 **HttpOnly、Secure、SameSite** 在 Servlet API 较新版本中才有完整表达，旧版可借助 **Spring** 或 **容器配置**。

详见同目录 [`seesion.md`](./seesion.md)、[`jwt.md`](./jwt.md) 中的会话与 Token 实践。

---

## 9. 异步、Multipart、错误处理

### 9.1 异步 Servlet

`request.startAsync()` 后可在 **另一线程** 写响应；需遵守规范中 **线程安全** 与 **AsyncContext** 完成/超时/错误回调。Spring MVC **异步返回类型**（`DeferredResult`、`Callable`、WebFlux）建立在容器异步能力之上。

### 9.2 Multipart（文件上传）

Servlet 3.0+：`@MultipartConfig` 或 `web.xml` 等价配置；用 **`request.getParts()`** 等解析。Spring 常用 **`MultipartResolver`** 封装。

### 9.3 错误与异常

- **`response.sendError(sc, msg)`**：委托容器错误机制，可走 **`web.xml` `<error-page>`** 或 Spring Boot `ErrorController`。
- 业务 Servlet 内应 **避免吞掉异常** 又不写响应，导致**空白或挂起**。

---

## 10. 线程模型与资源管理

- **每个 Servlet 类通常只有一个实例**，**多请求并发**调用 `service` / `doXxx`：**禁止**在 Servlet **实例字段**存请求级状态（应用 **`request` attribute** 或栈上局部变量）。
- **连接、流、锁**：在 `finally` 中关闭；长事务应在 Service 层配合连接池与 **`@Transactional`**。
- **CPU 密集**任务不应长时间占用容器线程；应异步或丢给业务线程池并正确 `complete`。

---

## 11. 与 Filter、Spring MVC 的关系

- **Filter**：见 [`filter.md`](./filter.md)；在 Servlet **之前/之后** 环绕。
- **DispatcherServlet**：映射如 `*.do` 或 `/`；内部再用 **`HandlerMapping` / `HandlerAdapter`** 调用 `@Controller`。因此日常开发以 **Controller + Service + Mapper** 为主，**自定义 `HttpServlet` 较少**。
- 仍会在 **非 Spring** 项目、**SSE/下载/特殊协议**、或 **嵌入 Servlet 容器** 场景直接写 Servlet。

---

## 12. 最佳实践

1. **薄接入层**：Servlet / Controller 只做 **校验、绑定、调用 Service、返回视图或 DTO**；业务与 SQL 下沉。  
2. **字符集与 Content-Type**：统一在 **Filter** 或框架配置中设置 **UTF-8**，避免每 Servlet 重复。  
3. **幂等与动词**：GET 不修改状态；POST/PUT/DELETE 语义与 REST 文档一致。  
4. **路径与重定向**：使用 **`request.getContextPath()`** 拼 URL，避免部署到子上下文时断裂。  
5. **错误与日志**：统一异常映射；记录 **request id**；不向客户端暴露堆栈。  
6. **安全**：防 **路径穿越**（`getRealPath` 慎用）、**CSRF**（有 Session 的 Cookie 场景）、**安全响应头**（可由 Filter 或网关加）。  
7. **资源**：`InputStream`/`Reader` 及时关闭；大文件用流式，避免整读进内存。  
8. **与 MyBatis**：在 **Service** 开启事务并调用 Mapper；Servlet 内不要 **`SqlSessionFactory.openSession()`** 手写生命周期，除非极特殊且清楚后果。

---

## 13. 参考

- [Jakarta Servlet 规范索引](https://jakarta.ee/specifications/servlet/)  
- 同目录：[Filter](./filter.md)、[会话](./seesion.md)、[JWT](./jwt.md)

---

## 一句话总结

**Servlet 是容器管理的、单例多线程的 HTTP 请求入口：掌握生命周期、`HttpServlet` 方法分发、`ServletContext` 与路径语义，以及转发与异步边界；业务与持久化交给 Service 与 MyBatis，Servlet 层保持轻薄与安全一致。**
