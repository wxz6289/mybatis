# Spring Boot 特性总结

Spring Boot 是基于 Spring 的快速开发框架，目标是“约定优于配置”，让我们更快搭建可上线的应用。

## 1. 自动配置（Auto Configuration）

- 根据依赖和环境自动配置 Bean，减少大量 XML/JavaConfig。
- 典型场景：引入 `spring-boot-starter-web` 后，自动配置 MVC、内嵌服务器等。

## 2. 起步依赖（Starter）

- 通过一组“场景化依赖”简化依赖管理。
- 常见：`spring-boot-starter-web`、`spring-boot-starter-data-jpa`、`spring-boot-starter-security`。

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
