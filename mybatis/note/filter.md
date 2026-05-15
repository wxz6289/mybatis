# Java Servlet `Filter` 核心内容详解

**Filter（过滤器）** 是 Servlet 规范中的 Web 组件（`javax.servlet.Filter` / **`jakarta.servlet.Filter`**），在 **请求进入 Servlet 之前** 与 **响应返回客户端之前** 介入，可对请求/响应做横切处理。典型用途：字符编码、认证鉴权、日志、跨域、限流等。

> 与 **MyBatis**：Filter 工作在 **Web 容器 / Spring MVC 前置链**；MyBatis 在 **Service → Mapper** 的数据访问层。二者分层不同，但 Filter 里解析出的用户身份可写入 `ThreadLocal` / `SecurityContext`，供下游 Service 与 Mapper 使用。

官方与扩展阅读：

- [Jakarta Servlet 6.0 — Filter](https://jakarta.ee/specifications/servlet/6.0/jakarta-servlet-spec-6.0.html)（Filter 语义以当前所用 Servlet 版本为准）
- Spring：`DelegatingFilterProxy`、`OncePerRequestFilter`、`FilterRegistrationBean`

---

## 目录

1. [角色与调用位置](#1-角色与调用位置)
2. [接口与生命周期](#2-接口与生命周期)
3. [FilterChain（责任链）](#3-filterchain责任链)
4. [映射与顺序](#4-映射与顺序)
5. [Request / Response 包装](#5-request--response-包装)
6. [与 Servlet、Listener、Spring Interceptor 的对比](#6-与-servletlistenerspring-interceptor-的对比)
7. [Spring 中的典型用法](#7-spring-中的典型用法)
8. [线程安全与资源](#8-线程安全与资源)
9. [常见场景](#9-常见场景)
10. [注意点与最佳实践](#10-注意点与最佳实践)

---

## 1. 角色与调用位置

```
客户端
  → Web 容器
      → Filter1.doFilter → Filter2.doFilter → … → Servlet / DispatcherServlet
      ← … ← Filter2 ← Filter1 ←
  ← 客户端
```

- **入站**：按配置顺序执行各 Filter 的 `doFilter`；在 `chain.doFilter` 之前可做请求校验、改写。
- **出站**：`chain.doFilter` 返回后，同一 `doFilter` 方法内**后续代码**在「Servlet 已处理完」之后执行，可改写响应头/体（若未提交）。

Filter 由 **容器管理单例**（每个 `<filter>` 定义通常一个实例），**不是**每个请求 new 一个。

---

## 2. 接口与生命周期

```java
public interface Filter {
    default void init(FilterConfig filterConfig) throws ServletException {}
    void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException;
    default void destroy() {}
}
```

| 方法 | 时机 | 说明 |
|------|------|------|
| **`init(FilterConfig)`** | 容器启动或首次使用前 | 读初始化参数 `filterConfig.getInitParameter(...)`，准备可复用资源 |
| **`doFilter(...)`** | 每次匹配的请求 | 必须调用 **`chain.doFilter`**（或已包装后的 request/response），否则请求不会到达 Servlet；可不调用以**短路**（直接写响应） |
| **`destroy()`** | 容器卸载应用 | 释放 `init` 中申请的资源 |

---

## 3. FilterChain（责任链）

`FilterChain` 表示**剩余**链：当前 Filter 调用 `chain.doFilter` 时，容器把控制权交给**下一个** Filter 或目标 Servlet。

- **短路**：不调用 `chain.doFilter`，自行 `response.getWriter()` 等返回，后续 Filter 与 Servlet **不会**执行（需已设置合适状态码与内容类型）。
- **嵌套**：`doFilter` 内 `try { chain.doFilter } finally { ... }` 可保证无论 Servlet 是否抛异常，都能做清理或统一日志。

---

## 4. 映射与顺序

### 4.1 `web.xml`（节选概念）

- `<filter>` 定义名称、类、`<init-param>`。
- `<filter-mapping>`：`url-pattern`（如 `/*`、`/api/*`）、可选 `servlet-name`。
- **`<dispatcher>`**（Servlet 2.4+）：`REQUEST`（默认）、`FORWARD`、`INCLUDE`、`ERROR`、`ASYNC`，控制**何种派发**下触发 Filter；漏配会导致 **forward 进 Spring MVC 时不经过 Filter** 等问题。

### 4.2 顺序

- **`web.xml` 中 `<filter-mapping>` 的声明顺序** 决定链顺序（规范语义）。
- **Spring Boot**：`FilterRegistrationBean#setOrder`（数值越小越靠前）；或 `@Order` 配合 `Filter` Bean 注册方式（以 Boot 文档为准）。
- 同顺序时行为依赖容器/框架实现，**不要依赖未文档化的并列顺序**。

---

## 5. Request / Response 包装

可对 `HttpServletRequest` / `HttpServletResponse` 做 **Wrapper**（装饰器），再交给 `chain.doFilter(wrappedRequest, wrappedResponse)`：

| 包装类型 | 用途 |
|----------|------|
| **可重复读 Body** | 缓存 InputStream，供后面多次读取（注意内存上限） |
| **XSS 清洗** | 包装 `getParameter` 等（谨慎，易与合法内容冲突） |
| **响应缓冲 /  gzip** | 包装 `getOutputStream` / `getWriter` |

Spring Security 大量基于 **包装** 与 **链** 扩展安全语义。

---

## 6. 与 Servlet、Listener、Spring Interceptor 的对比

| 组件 | 规范 | 作用范围 | 典型时机 |
|------|------|----------|----------|
| **Listener** | Servlet | 应用/会话/请求**事件** | 上下文启动、Session 创建销毁 |
| **Filter** | Servlet | **所有匹配 URL 的 Web 请求**（含到达 DispatcherServlet 前） | 编码、认证、全局限流 |
| **Servlet** | Servlet | 映射到的 URL | 业务处理（或 Spring 前端控制器） |
| **HandlerInterceptor** | Spring MVC | **DispatcherServlet 内**、映射到 Handler 的请求 | 细粒度 MVC 前后置、仅 Spring Web 内 |

要点：**Filter 在 DispatcherServlet 之外/之前**（就调用栈而言先于 Spring MVC 的分发）；需要拦静态资源、错误页派发等时，Filter 更合适；仅关心 Controller 前后则用 Interceptor 更贴切。

---

## 7. Spring 中的典型用法

### 7.1 `@ServletComponentScan` + `@WebFilter`

在嵌入式 Tomcat 下扫描 `javax/jakarta.servlet.annotation.WebFilter`，适合简单 Demo；顺序控制较弱。

### 7.2 `FilterRegistrationBean`

显式注册 `Filter`、URL、`dispatcherTypes`、`order`，**生产环境推荐**。

### 7.3 `OncePerRequestFilter`（Spring）

保证**同一次请求**在多种派发（forward/include/async）下 `doFilterInternal` **只执行一次**（避免重复鉴权、重复写日志）。Spring Security 的过滤器基类多继承此类。

### 7.4 `DelegatingFilterProxy`

容器里注册的 Filter 名对应 Spring 容器中的 **`Filter` Bean**（名字通常 `springSecurityFilterChain`），把 **Servlet 生命周期** 与 **Spring Bean 生命周期** 衔接。

---

## 8. 线程安全与资源

- Filter 实例一般为 **单例**，`doFilter` 可能被多线程并发调用：**不要在实例字段存请求级状态**（除非用 `ThreadLocal` 且必须在 `finally` 中清理，避免线程池复用导致泄漏）。
- 请求级数据应放在 **`request` attribute** 或 Spring `RequestContextHolder`（仍在注意清理与异步传播）。
- **`init` 里创建的线程安全资源**（连接池客户端等）可共享；**每个请求打开的资源**在 `finally` 关闭。

---

## 9. 常见场景

| 场景 | 说明 |
|------|------|
| **字符编码** | `request.setCharacterEncoding("UTF-8")`，并设 `response` 编码与 Content-Type |
| **认证前置** | 解析 JWT / Session，失败直接 401，成功写入 `SecurityContext` 或 request attribute |
| **CORS** | 预检 OPTIONS 与响应头（也可用 Spring `@CrossOrigin` / `CorsFilter`） |
| **请求 ID** | 生成 `X-Request-Id`，便于日志关联 |
| **限流 / IP 黑名单** | 在进 Spring 前拒绝，减轻下游压力 |
| **HTTPS / HSTS** | 部分安全头也可在 Filter 或网关统一加 |

---

## 10. 注意点与最佳实践

1. **`chain.doFilter` 勿漏**：除非故意短路；漏调会导致空白或挂起。  
2. **异步 Servlet**：`asyncStarted()` 后行为与线程切换有关，需读规范；Spring Security 对异步有专门支持。  
3. **`dispatcherTypes`**：使用 `forward`/`error` 页面时，确认 Filter 是否应在这些派发下执行。  
4. **异常**：`doFilter` 中抛出的受检异常按 `ServletException`/`IOException` 处理；与全局异常处理器的交互取决于异常是否在 Servlet 内抛出。  
5. **响应已提交**：`chain.doFilter` 返回后若 `response.isCommitted()`，无法再改状态码或部分头。  
6. **顺序**：认证 Filter 应早于业务 Filter；Spring Security 链顺序由框架配置决定，勿随意打乱。  
7. **与 MyBatis**：Filter 只做 Web 边界；**事务**仍放在 Service + `@Transactional`；SQL 与权限条件在 Mapper/Service 体现，避免在 Filter 里直接拿 `SqlSession`。

---

## 一句话总结

**Filter 是 Servlet 规范中的请求/响应横切链：单例、`doFilter` 内通过 `FilterChain` 传递控制权；掌握映射、`dispatcherTypes`、顺序与包装类，并与 Spring 的 `OncePerRequestFilter`、Security 链分工配合，即可安全地做编码、认证与全局限流而不与 MyBatis 持久层耦合。**
