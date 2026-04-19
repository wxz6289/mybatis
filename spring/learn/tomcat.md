# Tomcat 核心总结

## 1. Tomcat 简介

- Apache Tomcat 是一个开源 Java Web 容器，主要实现 Servlet、JSP、EL、WebSocket 等规范。
- 它不是完整的 Java EE 应用服务器，而是轻量级的 Web 应用容器，适合部署 Spring、Spring MVC、Spring Boot、Struts 等 Web 应用。

## 2. Tomcat 核心组件

- `Catalina`
  - Tomcat 的核心 Servlet 容器实现。
  - 负责请求解析、Servlet 调度、生命周期管理、容器层级结构。

- 容器层次结构
  - `Server`：Tomcat 实例顶层。
  - `Service`：把一个或多个 `Connector` 和一个 `Engine` 组合在一起。
  - `Connector`：负责接收客户端请求（HTTP、AJP、APR）。
  - `Engine`：处理请求的顶层容器，包含多个 `Host`。
  - `Host`：虚拟主机，对应一个域名或主机名。
  - `Context`：单个 Web 应用，对应 `webapp`。
  - `Wrapper`：代表单个 Servlet。

- 层级关系
  - `Engine` -> `Host` -> `Context` -> `Wrapper`

## 3. 重要配置文件与目录

- 目录结构
  - `bin/`：`startup.sh`、`shutdown.sh`、`catalina.sh` 等启动、停止脚本。
  - `conf/`：配置文件目录。
  - `webapps/`：默认部署的 Web 应用目录。
  - `logs/`：日志目录。
  - `work/`：JSP 编译后的中间文件。
  - `temp/`：临时文件。
  - `lib/`：Tomcat 运行依赖 Jar 包。

- 核心配置文件
  - `conf/server.xml`：核心服务器与 Connector 配置。
  - `conf/web.xml`：全局默认 Servlet/JSP 配置。
  - `conf/context.xml`：默认 Context 配置。
  - `META-INF/context.xml`：单个应用级 Context 配置。
  - `conf/tomcat-users.xml`：管理账号与角色配置。

## 4. 请求处理流程

1. `Connector` 接收客户端请求。
2. 请求封装为 `Request` 对象。
3. 请求进入 `Pipeline`，经过一系列 `Valve` 处理。
4. `Mapper` 定位对应的 `Context` 和 `Wrapper`。
5. 调用目标 `Servlet.service()` 方法。
6. 响应返回并通过 `Connector` 发回客户端。

- `Valve`
  - 类似过滤器，可插入处理链中。
  - 用于日志、认证、访问控制、压缩等功能。

- `Realm`
  - 认证与授权组件。
  - 常见类型：`MemoryRealm`、`JDBCRealm`、`DataSourceRealm`、`JNDIRealm`。

## 5. Servlet 与 JSP 运行机制

- Servlet 生命周期
  - 加载 -> 初始化 `init()` -> 请求处理 `service()` -> 销毁 `destroy()`。
- JSP 实际上被编译为 Servlet。
- 每个 Web 应用有独立类加载器，避免类冲突和类资源污染。

## 6. 常见配置要点

- `Connector` 参数
  - `port`、`protocol`、`maxThreads`、`acceptCount`
  - `connectionTimeout`、`maxConnections`
  - `compression`、`useSendfile`、`redirectPort`

- `Host` / `Context` 参数
  - `appBase`、`autoDeploy`、`deployOnStartup`
  - `docBase`、`path`、`reloadable`

- 数据源与 JNDI
  - JNDI 数据源可以在 `context.xml` 或 `server.xml` 中配置。
  - 适用于数据库连接池管理。

- 安全配置
  - `security-constraint`
  - `login-config`
  - `realm`
  - Cookie、Session、CSRF 等应用层安全也需要配合。

## 7. 性能调优方向

- 线程池调优
  - `maxThreads`：最大处理线程数。
  - `minSpareThreads`：保留线程数。

- 连接参数
  - `acceptCount`：等待队列长度。
  - `connectionTimeout`：连接超时时间。
  - `keepAliveTimeout`：保持活动连接超时。

- 静态资源优化
  - 启用 `sendfile`、`compression`。
  - 常与 Nginx/Apache 反向代理配合使用，减轻 Tomcat 负载。

- JVM 与 GC
  - Tomcat 依赖 JVM 运行，JVM 堆、GC 策略对性能影响大。

## 8. 部署与管理

- 启动方式
  - `bin/startup.sh` / `bin/shutdown.sh`
  - `catalina.sh start` / `catalina.sh stop`

- 部署方式
  - 直接将应用目录放入 `webapps/`
  - 部署 WAR 包
  - `META-INF/context.xml` 或 `conf/Catalina/localhost/*.xml` 定义应用 Context

- 管理工具
  - `manager` 应用：部署、停止、重载、查看状态
  - `host-manager`：管理虚拟主机
  - JMX 监控
  - 日志文件：`catalina.out`、`localhost.log`、`manager.log`

## 9. 常见使用场景

- 本地开发与调试 Servlet/JSP 应用。
- 生产环境部署 Spring/Spring Boot 应用。
- 与 Nginx/Apache 反向代理协同工作。
- 集群部署、Session 复制、负载均衡。
- 嵌入式 Tomcat 在 Spring Boot 中作为默认容器。

## 10. 复习重点

- Tomcat 核心是 Catalina 容器。
- 最重要的配置文件是 `server.xml`。
- 核心结构：`Server` -> `Service` -> `Connector` -> `Engine` -> `Host` -> `Context`。
- `Connector` 和线程池调优直接影响吞吐与并发。
- `Valve`、`Realm`、`Context` 是扩展和安全机制的关键。
- 类加载隔离、部署方式与生命周期管理是 Tomcat 运行基础。

`HttpServletRequest`、`HttpServletResponse`、ServletContext 是 Servlet API 的核心接口，分别代表客户端请求、服务器响应和应用上下文环境。理解它们的作用和使用方法对于开发基于 Tomcat 的 Web 应用至关重要。
`DispatcherServlet` 是 Spring MVC 的核心组件，负责将 HTTP 请求分发到相应的处理器（Controller）进行处理。它充当了前端控制器的角色，协调请求的处理流程，包括视图解析、数据绑定和异常处理等功能。DispatcherServlet 的设计使得 Spring MVC 能够实现高度灵活和可扩展的 Web 应用开发。

BS vs CS 模式：
- BS（Browser-Server）模式：客户端通过浏览器访问服务器，服务器处理请求并返回结果，适用于 Web 应用。
- CS（Client-Server）模式：客户端是一个独立的应用程序，直接与服务器通信，适用于桌面应用或移动应用。
