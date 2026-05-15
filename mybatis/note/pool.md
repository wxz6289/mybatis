# 数据库连接池：整理与总结

连接池的本质是：**预先创建并复用一组到数据库的 TCP 连接**，避免业务每次请求都「建连 → 鉴权 → 拆连」带来的高延迟与数据库端压力。

---

## 1. 为什么需要连接池

| 无连接池 | 有连接池 |
|----------|----------|
| 每次业务自己 `getConnection()`，底层往往新建物理连接 | 从池中「借」已就绪连接，用完归还 |
| 建连成本高（TLS、认证、会话初始化） | 多数请求只付「借还」成本 |
| 高并发下易打满数据库最大连接数 | 通过上限控制并发连接数，保护 DB |

**结论**：生产环境应始终通过 **实现了 `javax.sql.DataSource` 的连接池** 获取连接，而不是裸用 `DriverManager` 每次新建。

---

## 2. 核心概念（术语对齐）

| 概念 | 含义 |
|------|------|
| **池（Pool）** | 管理一组 `Connection` 的组件 |
| **最大连接数（`maximumPoolSize`）** | 池中允许同时存在的最大连接数，也是向 DB 申请连接的硬上限之一 |
| **最小空闲（`minimumIdle`）** | 池尽量保持的空闲连接数，用于应对突发流量（实现因池而异） |
| **连接超时（`connectionTimeout`）** | 从池里**借**连接时，等待可用连接的最长时间；超时抛异常 |
| **空闲回收（`idleTimeout`）** | 连接在池中空闲超过该时间可被回收（需小于或与 `maxLifetime` 配合理解） |
| **最大生命周期（`maxLifetime`）** | 单条连接最多存活多久后被关闭并重建，避免 DB/网络中间件对长连接的不一致状态 |
| **校验（Validation / keepalive）** | 借出前或周期性探测连接是否仍有效，避免拿到「僵尸连接」 |

不同连接池对上述参数命名略有差异，但语义相近。

---

## 3. 常见连接池实现（HikariCP、Druid、DBCP2、c3p0）

下面四种都是生产里**真实会碰到**的实现：都实现 `javax.sql.DataSource`（或包装为 `DataSource`），差别主要在**默认策略、监控能力、依赖体积与生态**。

### 3.1 总览对比

| 实现 | 生态 / 典型场景 | 优势 | 注意点 |
|------|-----------------|------|--------|
| **HikariCP** | Spring Boot 默认；轻量服务首选 | 性能好、依赖小、默认即合理 | 监控能力相对「朴素」，需配合 Actuator/APM |
| **Alibaba Druid** | 国内 Java 项目常见 | **内置监控页**、SQL 统计、防火墙等扩展多 | 功能多 = 配置与安全意识要求更高；与 Spring Boot 大版本要选对应 starter |
| **Apache Commons DBCP2** | 传统企业、Tomcat 周边 | 成熟、文档多、配置项清晰 | 默认参数未必最优，需按并发调优 |
| **c3p0（mchange）** | 老项目、遗留系统 | 出现早、资料多 | **新项目一般不推荐首选**；维护节奏与性能不如 Hikari |

---

### 3.2 HikariCP（`com.zaxxer:HikariCP`）

- **定位**：高性能、轻量连接池；Spring Boot 引入 JDBC 相关 starter 后**默认**使用。  
- **Maven**：通常**不必手写**，由 `spring-boot-starter-jdbc` 等传递引入；单独使用时坐标为 `com.zaxxer:HikariCP`。  
- **配置习惯（Spring Boot）**：`spring.datasource.hikari.*`（如 `maximum-pool-size`、`connection-timeout`、`max-lifetime`）。  
- **实践建议**：作为默认池时，重点调 **`maximum-pool-size` / `connection-timeout` / `max-lifetime`**，并结合监控看「等待连接」是否变长。

---

### 3.3 Alibaba Druid（`com.alibaba:druid` / Druid Spring Boot Starter）

- **定位**：连接池 + **可观测性**（内置监控、慢 SQL、统计等），适合希望「开箱即用看池与 SQL」的团队。  
- **Maven（示例）**：  
  - 仅核心池：`com.alibaba:druid`  
  - Spring Boot 集成：一般使用 **`druid-spring-boot-3-starter`**（面向 Spring Boot 3+；若你使用 Spring Boot 4，请以 [Druid 官方说明](https://github.com/alibaba/druid) 与当前大版本兼容矩阵为准）。  
- **配置习惯**：常见为 `spring.datasource.druid.*`（具体前缀以 starter 文档为准），并注意 **监控页、统计接口的鉴权**，避免默认暴露在生产公网。  
- **实践建议**：「功能多」要配套 **安全与最小权限**；不需要的插件/过滤器尽量关闭，降低攻击面。

---

### 3.4 Apache Commons DBCP2（`org.apache.commons:commons-dbcp2`）

- **定位**：Apache 经典连接池第二代（DBCP2），配置模型直观，常见于与 **Tomcat**、老框架或显式 `BasicDataSource` Bean 搭配。  
- **Maven**：

```xml
<dependency>
  <groupId>org.apache.commons</groupId>
  <artifactId>commons-dbcp2</artifactId>
</dependency>
```

- **Spring Boot**：可通过 `spring.datasource.type=org.apache.commons.dbcp2.BasicDataSource` 指定类型（需依赖在 classpath；属性名以当前 Boot 文档为准），并使用 `spring.datasource.*` 映射 URL、用户名密码及 DBCP2 专有属性。  
- **实践建议**：关注 **`maxTotal` / `maxIdle` / `minIdle` / `maxWaitMillis`** 等与 Hikari 命名不同的项，避免照搬 Hikari 参数名。

---

### 3.5 c3p0（`com.mchange:c3p0`）

- **定位**：出现时间很早的连接池，**遗留系统**里仍可能遇到。  
- **Maven**：

```xml
<dependency>
  <groupId>com.mchange</groupId>
  <artifactId>c3p0</artifactId>
</dependency>
```

- **典型类**：`com.mchange.v2.c3p0.ComboPooledDataSource`。  
- **实践建议**：新项目优先 **HikariCP / Druid**；若维护老代码，重点排查 **连接泄漏**、**池参数过大**、以及与新 JDK / 新驱动的兼容性。

---

### 3.6 在 Spring Boot 里如何「换池」

1. **默认**：引入 `spring-boot-starter-jdbc`（或包含 JDBC 的 starter）时，一般为 **HikariCP**，用 `spring.datasource.hikari.*` 调参。  
2. **换成 Druid**：引入对应 **Druid Spring Boot Starter**，按文档配置；不要与多个池 starter 混用导致 Bean 冲突。  
3. **换成 DBCP2 / c3p0**：把对应依赖加入 classpath，并通过 **`spring.datasource.type`** 指向具体 `DataSource` 实现类（或自己 `@Bean` 定义 `DataSource`），再配置该实现类支持的属性。

本仓库当前为 **Spring Boot + JDBC + MyBatis**，在未额外引入 Druid 等 starter 时，**默认即为 HikariCP**。

---

## 4. Spring Boot 中的配置方式

在 `application.yaml` / `application.properties` 中，以 **`spring.datasource.hikari.*`** 为前缀配置 HikariCP（属性名以当前 Spring Boot 文档为准）。若已切换到其他 `DataSource` 实现，请改用该实现对应的配置前缀或自定义 `DataSource` Bean。

**示例（可按流量调参）：**

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mybatis?serverTimezone=Asia/Shanghai
    username: your_user
    password: your_password
    hikari:
      pool-name: LearnPool
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

**参数实践含义简述：**

- **`maximum-pool-size`**：与数据库 `max_connections`、应用实例数、线程模型一起算总连接数，避免「多实例 × 每池很大」压垮 DB。  
- **`connection-timeout`**：借连接等太久应失败快速返回，而不是无限阻塞线程。  
- **`max-lifetime`**：略小于数据库或中间件对空闲连接、防火墙 NAT 的超时，减少「连接还在池里但已不可用」的概率。

> 说明：你当前仓库里的 `application.yaml` 若未写 `hikari` 段，则使用 Hikari 的**默认值**；上线前建议按环境显式配置并压测。

---

## 5. 池大小怎么估（工程经验）

没有万能公式，可按下面思路迭代：

1. **看数据库上限**：MySQL `max_connections`、云 RDS 规格限制。  
2. **看应用实例数**：总连接 ≈ `实例数 × maximumPoolSize`（若有多个服务连同一库要加总）。  
3. **看并发模型**：  
   - 线程池处理请求：池大小不必等于线程数，通常 **略大于平均并行访问 DB 的线程数** 即可；  
   - 过大：占用 DB 连接与内存，上下文切换与锁竞争也可能变差。  
4. **压测验证**：观察 RT、错误率、池等待时间、DB 端 active 连接与慢查询。

**起点建议（仅作初始值）**：中小型 Web 服务可从 **`maximum-pool-size` 10～20** 起，结合监控再调。

---

## 6. 与 JDBC 的关系（和 `docs/jdbc.md` 衔接）

- 业务代码仍使用 **`Connection` / `PreparedStatement` / `ResultSet`**。  
- 区别仅在于：`DataSource.getConnection()` 背后由**池**分配一条已建立或可复用的连接。  
- **仍必须** try-with-resources 或等价方式 `close()`，把连接**归还池**，而不是销毁池外再建。

---

## 7. 常见反模式与故障现象

| 反模式 | 现象 |
|--------|------|
| 借了连接不关闭（不 `close`） | 池耗尽，`connectionTimeout` 频繁超时 |
| 池过大 + 多实例 | DB `Too many connections`、整体抖动 |
| 从不校验 / `maxLifetime` 过长 | 偶发 `Communications link failure`、第一次查询失败重试才成功 |
| 在业务线程里执行极慢 SQL | 占满池中连接，拖垮整个服务 |

---

## 8. 观测与排障（建议）

- **应用侧**：池活跃数、等待队列、获取连接耗时、超时次数。  
- **数据库侧**：当前连接数、慢查询、锁等待。  
- **日志**：连接获取超时、SQL 异常与慢 SQL 摘要（注意脱敏）。

Spring Boot Actuator 等组件可在启用后暴露数据源/健康指标（按项目需要引入）。

---

## 9. 一句话总结

连接池用「**有限、复用、可配置生命周期**」的连接触发器，换取 **更低延迟、更稳的并发、对数据库更可控的压力**。调参核心是：**上限与实例数匹配 DB 能力、超时快速失败、用生命周期与校验避免僵尸连接**。
