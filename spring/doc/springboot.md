# Spring Boot 特性总结

[Spring Boot 官方文档](https://docs.spring.io/spring-boot/index.html)  
[Spring Initializr](https://start.spring.io/)

Spring Boot 是基于 Spring 的快速开发框架，目标是“约定优于配置”，让我们更快搭建可上线的应用。

## 1. 自动配置（Auto Configuration）

- 根据依赖和环境自动配置 Bean，减少大量 XML/JavaConfig。
- 典型场景：引入 `spring-boot-starter-web` 后，自动配置 MVC、内嵌服务器等。

## 2. 起步依赖（Starter）

- 通过一组**场景化启动器**聚合传递依赖，简化版本对齐与引入方式。
- 常见：`spring-boot-starter-web`、`spring-boot-starter-data-jpa`、`spring-boot-starter-security`。
- 更多概念、命名规则与选型见 **第 9 节**。

## 3. 内嵌服务器

- 默认支持 Tomcat，也可切换 Jetty、Undertow。
- 应用可直接 `java -jar` 运行，部署简单。

## 4. 外部化配置

- 支持 `application.yml` / `application.properties`。
- 支持多环境配置：`dev`、`test`、`prod`（`spring.profiles.active`）。
- 支持环境变量、命令行参数覆盖配置。

## 5. 生产级能力（Actuator）

- 提供健康检查、指标、线程、环境信息等端点。
- 便于监控系统状态并接入 Prometheus/Grafana。

## 6. 开发效率提升

- 与 Spring 生态无缝集成（MVC、Data、Security、Messaging）。
- `spring-boot-devtools` 支持热重启，提升开发体验。
- 测试支持完善：`@SpringBootTest`、切片测试等。

## 7. 快速构建与运维友好

- Maven/Gradle 插件支持打包可执行 Jar。
- 易于容器化与云部署，适合微服务和云原生场景。

## 8. 核心价值

- 快速启动项目。
- 降低样板代码与配置复杂度。
- 提供“可开发 + 可测试 + 可运维”的完整应用基础设施。

---

## 9. 场景启动器（Starter）总结

### 9.1 是什么

- **Starter** 不是单独的运行框架，而是 Maven/Gradle 里的 **聚合依赖（Bill of Materials 思想下的场景包）**。
- 在 `build.gradle` / `pom.xml` 里引入**一个** `spring-boot-starter-*`，即可按需拉取一组**已互相兼容版本**的传递依赖（如 Web 场景会带上 Spring MVC、内嵌 Tomcat、Jackson 等）。
- 中文常说的 **“场景启动器”** 即：按**使用场景**（Web、JPA、Security、消息等）打包好的起步依赖。

### 9.2 命名规则

- **官方场景启动器**：`spring-boot-starter-{场景名}`，由 Spring Boot 团队维护。  
  例如：`spring-boot-starter-web`、`spring-boot-starter-data-jpa`。
- **第三方扩展**：习惯命名为 `{项目}-spring-boot-starter` 或 `spring-boot-starter-{第三方名}`，由社区或厂商提供（如某些数据库、监控组件）。

### 9.3 与 BOM、版本管理的关系

- Spring Boot 通过 **`spring-boot-dependencies`**（BOM）统一管理大量依赖的**推荐版本**。
- 子模块引入 starter 时，**不必逐个声明**子依赖的版本（除非你要覆盖版本）；减少“依赖地狱”与冲突排查成本。
- 升级 Spring Boot 版本时，通常整批依赖版本随 BOM **一并迁移**，需做回归测试。

### 9.4 常见官方 Starter（速查）

| Starter | 典型用途 |
| --- | --- |
| `spring-boot-starter` | 核心能力（含日志、JUnit 等基础测试依赖），常与其它 starter 组合 |
| `spring-boot-starter-web` | Spring MVC + REST + 内嵌 Tomcat（Servlet 栈） |
| `spring-boot-starter-webflux` | WebFlux + Reactor + Netty（响应式栈） |
| `spring-boot-starter-data-jpa` | JPA/Hibernate、数据源抽象 |
| `spring-boot-starter-jdbc` | JDBC、`JdbcTemplate`，不含 JPA |
| `spring-boot-starter-data-r2dbc` | R2DBC 响应式数据库访问 |
| `spring-boot-starter-security` | Spring Security |
| `spring-boot-starter-validation` | Bean Validation（如 Hibernate Validator） |
| `spring-boot-starter-cache` | 缓存抽象 |
| `spring-boot-starter-data-redis` | Redis（Lettuce 等） |
| `spring-boot-starter-amqp` | RabbitMQ |
| `spring-boot-starter-kafka` | Kafka |
| `spring-boot-starter-mail` | 邮件发送 |
| `spring-boot-starter-actuator` | 监控与健康检查端点 |
| `spring-boot-starter-test` | 测试（JUnit、Mockito、AssertJ、Spring Test 等） |

按需引入即可；**不要**为了“省事”把无关 starter 全加上，否则会带来多余传递依赖与潜在冲突。

### 9.5 与自动配置的关系

- Starter 负责把**类路径**上需要的库拉进来；**自动配置**依据类路径与条件注解决定是否启用相应 Bean。
- 二者配合：`starter-web` 到位后，`DispatcherServlet`、Jackson 等才会在自动配置中装配（除非被排除或自定义覆盖）。

### 9.6 自定义场景启动器（扩展阅读）

- 团队可在内部封装 **`xxx-spring-boot-starter`**：把自己的 SDK + 默认配置 + `AutoConfiguration` 打成一枚依赖，业务项目一行引入即可。
- Spring Boot 3.x 使用 **`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`** 注册自动配置类（2.x 时代常见 `spring.factories`，迁移时需注意）。

### 9.7 最佳实践

1. **按场景最小引入**：只加当前模块需要的 starter，避免 fat classpath。
2. **统一 Boot 版本**：全项目或父 BOM 对齐 Spring Boot 版本，再谈 Cloud/其它生态对齐。
3. **排查冲突**：若出现版本不一致，在构建工具里查看**依赖树**（Gradle `dependencies` / Maven `dependency:tree`），用 `exclusions` 或 BOM 覆盖谨慎处理。
4. **安全与体积**：生产镜像与 Jar 只保留必要 starter；测试专用依赖（如 `spring-boot-starter-test`）使用 `test` 作用域。
5. **文档即契约**：内部自定义 starter 应写清**提供的自动配置、可配置项、与官方 starter 的组合方式**，避免“黑盒依赖”。

### 9.8 小结

- **场景启动器** = **场景化依赖聚合 + BOM 版本对齐** + 与 **自动配置** 联动。
- 使用原则是：**选对场景、少而准、会查依赖树、升级必回归**。
