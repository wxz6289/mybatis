# Spring Reactive Stack 与 Servlet Stack 对比，及 Reactor 核心内容

## 1. 两套栈分别是什么

### 1.1 Servlet Stack（传统 Servlet / 阻塞式栈）

- **运行时**：基于 **Servlet 容器**（如 Tomcat、Jetty），底层多为 **阻塞式 I/O**。
- **Spring 门面**：**Spring MVC**（`@RestController`、`@RequestMapping` 等）。
- **线程模型**：典型为 **每请求一线程**（thread-per-request）。请求线程在执行 Controller、Service、JDBC 查询时会**阻塞等待**，直到调用返回。
- **调用栈**：同步、直观，与多数 Java 库（JDBC、阻塞 HTTP 客户端）天然契合。

### 1.2 Reactive Stack（响应式 / 非阻塞栈）

- **运行时**：基于 **Netty** 等支持 **非阻塞 I/O** 的运行时（Spring WebFlux 默认嵌入 Netty）。
- **Spring 门面**：**Spring WebFlux**（路由函数式风格或注解风格）、响应式数据访问（R2DBC、Reactive Mongo 等）。
- **线程模型**：少量 **事件循环线程** + 有限的 **工作线程池**；I/O 等待时不占用线程空转，适合高并发、高延迟下游场景。
- **编程模型**：基于 **Reactive Streams**（异步序列 + 背压），典型 API 为 **Project Reactor** 的 `Mono` / `Flux`。

一句话：**Servlet 栈偏“线程撑并发”，Reactive 栈偏“少量线程 + 非阻塞与异步流水线”。**

---

## 2. 核心差异对照

### 2.1 I/O 与线程

| 维度 | Servlet Stack | Reactive Stack |
| --- | --- | --- |
| **I/O 模型** | 阻塞 I/O 为主 | 非阻塞 I/O |
| **典型线程** | 每请求一线程，线程易成为瓶颈 | 少量线程处理大量连接 |
| **阻塞调用** | JDBC、阻塞 RestTemplate 等可直接用 | 阻塞调用会占满线程池，破坏模型；需用非阻塞驱动（R2DBC、WebClient） |
| **适用负载** | CPU 密集、业务同步、团队熟悉阻塞模型 | I/O 密集、大量并发连接、延迟较高的下游服务 |

### 2.2 API 与编程风格

| 维度 | Servlet Stack | Reactive Stack |
| --- | --- | --- |
| **控制器返回** | `String`、`Object`、`ResponseEntity`（同步返回值） | `Mono<T>`、`Flux<T>`（异步序列） |
| **数据访问** | JPA、JdbcTemplate、阻塞 Mongo 等 | R2DBC、Reactive Mongo、Reactive Redis 等 |
| **HTTP 客户端** | `RestTemplate`、阻塞 `HttpClient` | **WebClient**（响应式） |
| **学习成本** | 低（顺序代码） | 中高（链式算子、调度、错误传播） |

### 2.3 生态与兼容性

- **Servlet**：与现有 Java 生态 **最广**：几乎所有 SDK、监控、事务习惯都按阻塞模型设计。
- **Reactive**：需要 **端到端非阻塞** 才能发挥优势；中间混用阻塞调用会导致线程池耗尽或性能退化。
- **Spring 官方态度**：两套栈 **长期并存**；多数业务仍以 **Spring MVC + Servlet** 为主；Reactive 适合明确场景（网关、高并发代理、流式推送等）。

---

## 3. 何时选用哪一套（实践建议）

### 3.1 更适合 Servlet Stack

- 团队以同步 Spring MVC 为主，依赖大量阻塞式库（经典 JDBC、阻塞 MQ 客户端等）。
- 业务以 CRUD、内部接口为主，并发压力可控。
- 希望调试栈简单、招聘与资料成本低。

### 3.2 更适合 Reactive Stack

- **高并发、长连接**（SSE、WebSocket）、**网关聚合**多个下游 HTTP。
- 下游延迟高且希望用 **少量线程**扛大量等待。
- 愿意投入 **全链路非阻塞**（数据库用 R2DBC 等，避免混用阻塞 JDBC）。

### 3.3 常见误区

- **误区 1**：以为 WebFlux 一定更快。若仅在 Controller 里阻塞 JDBC，往往不如 MVC。
- **误区 2**：把 `Mono` 当成“异步注解魔法”。必须理解订阅（subscribe）与线程调度，否则会写出难以调试的代码。
- **误区 3**：忽略 **背压**。高速生产者 + 慢消费者时，需要背压或限速策略。

---

## 4. Project Reactor 是什么

**Project Reactor** 是 Spring WebFlux 底层采用的 **响应式库**，实现了 **Reactive Streams** 规范，提供两类异步序列类型：

- **`Mono<T>`**：表示 **0 或 1 个元素**的异步序列（类似 `Optional` 的异步版 + 完成/失败信号）。
- **`Flux<T>`**：表示 **0 到 N 个元素**的异步序列（类似异步 `Stream`）。

它们都是 **Publisher**（发布者），下游通过 **订阅（subscribe）** 触发执行并接收数据。

---

## 5. Reactive Streams 四个接口（Reactor 的基础契约）

规范定义四种角色（理解背压的关键）：

1. **Publisher**：发布数据（Reactor 里即 `Mono` / `Flux`）。
2. **Subscriber**：接收 `onNext` / `onError` / `onComplete`。
3. **Subscription**：订阅关系；`request(n)` 表示拉取 n 个元素，**背压**由此实现。
4. **Processor**：既是 Subscriber 又是 Publisher（中间环节）。

**背压（Backpressure）**：消费者通过 **`request(n)`** 告诉生产者“我还能处理多少”，避免生产者推送过快压垮消费者。Reactor 在运行时帮你协调多数场景；编写自定义 Subscriber 时需遵守约定。

---

## 6. Mono 与 Flux 的语义

### 6.1 Mono

- 适用于：单次 DB 查询返回一行、单次 HTTP 调用返回一个 body、异步计算单个结果。
- 结束状态：最多一个 `onNext`，然后 `onComplete`；也可能直接 `onError`。

### 6.2 Flux

- 适用于：列表流式返回、分页拉取、Kafka 消费多条、SSE 推送多条事件。
- 元素个数：0 到任意多个，最后 `onComplete` 或 `onError`。

---

## 7. 冷热发布（Cold vs Hot）

### 7.1 Cold（冷）

- **默认多数 Mono/Flux 工厂是冷的**：每次 **subscribe** 才会真正执行数据源逻辑（例如重新发起 HTTP、重新查库）。
- 多个订阅者通常会 **各自独立执行一遍**（除非后续使用 `cache`、`share` 等）。

### 7.2 Hot（热）

- 先有数据源在推送，订阅者中途加入只能收到 **订阅之后** 的数据（类似广播）。
- 典型：`replay`、`publish`、`share`、响应式消息中间件推送。

选型直觉：**Cold 适合按需计算；Hot 适合事件广播与实时流。**

---

## 8. 常用操作符（概念级）

Reactor 提供大量 **operators**，用于变换、过滤、合并、错误处理等：

- **映射**：`map`（同步一对一）、`flatMap`（一对一展开为新的 Publisher，常用于异步编排）。
- **过滤**：`filter`、`take`、`skip`。
- **组合**：`zip`、`merge`、`concat`、`combineLatest`。
- **缓冲与窗口**：`buffer`、`window`。
- **副作用**：`doOnNext`、`doOnError`、`doOnSubscribe`（用于日志、监控，不改变业务语义）。
- **空与默认值**：`defaultIfEmpty`、`switchIfEmpty`。

**`flatMap` 与 `map` 的区别（高频考点）**：

- `map`：元素变换，仍是一条 Mono/Flux 里的同步变换。
- `flatMap`：每个元素映射成 **另一个 Publisher**，再合并输出，适合 **异步串并联**。

---

## 9. 线程与调度（Scheduler）

响应式不等于“单线程”。Reactor 通过 **`publishOn`** / **`subscribeOn`** 切换执行线程：

- **`subscribeOn`**：影响 **整个链条订阅项上游**（数据源在哪个调度器上启动），通常只用一次。
- **`publishOn`**：影响 **该算子之后的下游**在哪个线程执行；可多次使用以分工。
- 内置调度器：`Schedulers.parallel()`、`Schedulers.boundedElastic()`（阻塞调用兜底时要谨慎）、`Schedulers.immediate()` 等。

**注意**：在响应式链中随意使用 **阻塞调用**（如 `block()`、阻塞 JDBC）会占用 `boundedElastic` 线程池，高负载下仍可能出问题；最佳实践仍是 **非阻塞驱动**。

---

## 10. 订阅与执行时机

- **声明** `Mono`/`Flux`（链式调用）**不会立即执行**，多数情况下要 **`subscribe()`** 或有 Spring WebFlux **订阅出口**（返回给框架）才真正触发。
- **Spring MVC 返回 Mono/Flux**：框架负责订阅，写入 HTTP 响应。
- 手写测试中忘记 `subscribe()`，常导致“没有任何事情发生”。

---

## 11. 错误处理

常见方式：

- **`onErrorReturn` / `onErrorResume`**：出错时返回兜底值或切换到备用 Publisher。
- **`doOnError`**：副作用记录日志。
- **`retry` / `retryWhen`**：按策略重试（注意与幂等性、风暴重试的配合）。

错误在响应式链中 **向下传播**；若未处理，最终会体现在订阅方的 `onError` 或框架的错误响应中。

---

## 12. Reactor 与 Spring WebFlux 的关系

- **WebFlux Controller** 返回 `Mono`/`Flux`，由框架完成订阅与响应写出。
- **WebClient** 发起调用返回 `Mono`/`Flux`，可与其它算子组合。
- **全局异常**：可用 `@ControllerAdvice` 配合响应式类型处理异常映射。

---

## 13. 小结

- **Servlet Stack**：阻塞 I/O + Spring MVC + 广泛生态；适合大多数传统业务。
- **Reactive Stack**：非阻塞 I/O + WebFlux + Reactor；适合高并发 I/O 与流式场景，但需 **全链路意识** 与非阻塞基础设施。
- **Reactor 核心**：`Mono`/`Flux`、Reactive Streams **背压**、丰富的 **operators**、**Scheduler** 控制线程、明确 **订阅驱动执行**。

掌握 **“异步序列 + 背压 + 调度 + 错误传播”**，才算理解 Spring Reactive 栈与 Reactor 的主干。
