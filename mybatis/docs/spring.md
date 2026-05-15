# Spring 常用注解详解与最佳实践

> 适用范围：Spring Framework + Spring Boot（含 Web、MyBatis 常见项目）
> 目标：从“会用”到“用对”，理解注解职责边界与工程实践。

---

## 1. Spring 注解总览（先有全局图）

Spring 常用注解可以按职责分为：

1. **IoC 与 Bean 管理**：定义 Bean、自动装配、作用域、生命周期。
2. **Web 开发**：Controller、请求映射、参数绑定、返回体处理。
3. **配置与属性注入**：配置类、条件装配、外部配置绑定。
4. **事务管理**：声明式事务边界与回滚规则。
5. **AOP**：切面、切点、增强逻辑。
6. **数据访问与异常转换**：DAO 层标识与异常语义。
7. **校验**：请求参数与对象约束校验。
8. **异步、定时、缓存**：性能与任务编排。
9. **测试注解**：单元测试、切片测试、集成测试。

---

## 2. IoC / Bean 管理核心注解

## 2.1 `@Component`

- **作用**：将类标记为 Spring 容器管理的组件（Bean）。
- **典型场景**：通用组件、工具类适配器等。

```java
@Component
public class IdGenerator {
    public String nextId() { return java.util.UUID.randomUUID().toString(); }
}
```

## 2.2 `@Service` / `@Repository` / `@Controller` / `@RestController`

它们是 `@Component` 的语义化派生注解：

- `@Service`：业务层组件。
- `@Repository`：持久层组件，通常会触发数据访问异常转换。
- `@Controller`：MVC 控制器，返回视图。
- `@RestController`：`@Controller + @ResponseBody`，返回 JSON/XML 等响应体。

> 最佳实践：优先使用语义化注解，便于团队协作、架构分层与后续扩展。

## 2.3 `@Autowired` / `@Qualifier` / `@Primary` / `@Resource`

- `@Autowired`：按类型注入 Bean。
- `@Qualifier("beanName")`：同类型多实现时指定 Bean。
- `@Primary`：声明默认优先注入 Bean。
- `@Resource`（JSR-250）：默认按名称再按类型注入。

推荐写法（构造器注入）：

```java
@Service
public class UserService {
    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }
}
```

> 最佳实践：
> 1）优先**构造器注入**（便于不可变、测试友好）；
> 2）避免字段注入；
> 3）多实现冲突时优先 `@Qualifier` 明确依赖关系。

## 2.4 `@Bean` + `@Configuration`

- `@Configuration`：声明配置类。
- `@Bean`：在配置类中显式注册第三方对象或复杂初始化对象。

```java
@Configuration
public class AppConfig {
    @Bean
    public java.time.Clock systemClock() {
        return java.time.Clock.systemUTC();
    }
}
```

## 2.5 `@Scope` / `@Lazy` / `@DependsOn`

- `@Scope("singleton"|"prototype"|...)`：Bean 作用域。
- `@Lazy`：延迟初始化。
- `@DependsOn`：指定依赖初始化顺序。

> 最佳实践：默认使用单例；仅在确有必要时改变作用域与初始化顺序。

---

## 3. 配置与外部化属性注解

## 3.1 `@Value`

- **作用**：注入单个配置项。
- **注意**：适合少量简单配置，不适合大量配置对象。

```java
@Value("${server.port:8080}")
private int port;
```

## 3.2 `@ConfigurationProperties`

- **作用**：将同前缀配置批量绑定到 POJO（强烈推荐）。
- **优势**：结构化、可校验、易维护。

```java
@org.springframework.boot.context.properties.ConfigurationProperties(prefix = "app.storage")
@jakarta.validation.constraints.NotBlank
public class StorageProperties {
    private String endpoint;
    // getter/setter
}
```

常见搭配：

- `@EnableConfigurationProperties(StorageProperties.class)`
- 或在启动类加 `@ConfigurationPropertiesScan`

## 3.3 条件装配：`@Profile` / `@ConditionalOn...`（Boot）

- `@Profile("dev")`：按环境激活 Bean。
- `@ConditionalOnProperty` / `@ConditionalOnClass` / `@ConditionalOnMissingBean`：自动装配条件控制。

> 最佳实践：
>
> - 环境差异用 `@Profile` + `application-*.yml`；
> - 框架或 starter 开发中多用 `@ConditionalOn...`。

---

## 4. Web 开发常用注解（Spring MVC）

## 4.1 路由映射：`@RequestMapping` 家族

- `@RequestMapping`：通用映射（类级、方法级）。
- `@GetMapping` / `@PostMapping` / `@PutMapping` / `@DeleteMapping` / `@PatchMapping`：语义化 HTTP 方法映射。

```java
@RestController
@RequestMapping("/users")
public class UserController {
    @GetMapping("/{id}")
    public UserVO findById(@PathVariable Long id) { return null; }
}
```

## 4.2 参数绑定

- `@PathVariable`：路径参数。
- `@RequestParam`：查询参数或表单参数。
- `@RequestBody`：请求体 JSON 绑定。
- `@RequestHeader`：请求头。
- `@CookieValue`：Cookie 值。
- `@ModelAttribute`：表单对象绑定（MVC 场景常见）。

## 4.3 返回与状态

- `@ResponseBody`：直接写响应体（`@RestController` 已内置）。
- `ResponseEntity<T>`：精细控制状态码、响应头与响应体。
- `@ResponseStatus`：标注方法或异常对应 HTTP 状态码。

## 4.4 全局异常处理

- `@ControllerAdvice` / `@RestControllerAdvice`：全局异常处理与统一返回。
- `@ExceptionHandler`：处理特定异常。

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
    public java.util.Map<String, Object> handle(IllegalArgumentException e) {
        return java.util.Map.of("code", 400, "message", e.getMessage());
    }
}
```

> 最佳实践：统一错误码与响应结构，避免 controller 到处 `try-catch`。

---

## 5. 事务注解

## 5.1 `@Transactional`

- **作用**：声明事务边界（类或方法）。
- 常用属性：
  - `propagation`：传播行为（如 `REQUIRED`、`REQUIRES_NEW`）。
  - `isolation`：隔离级别。
  - `readOnly`：只读事务优化。
  - `rollbackFor`：指定回滚异常类型。
  - `timeout`：超时秒数。

```java
@Service
public class OrderService {
    @Transactional(rollbackFor = Exception.class)
    public void createOrder() {
        // 写库逻辑
    }
}
```

### 高频坑位

1. **同类内部方法调用**不会走代理，事务可能不生效。
2. 默认仅对 `RuntimeException` 回滚，受检异常需 `rollbackFor` 指定。
3. `private/final` 方法可能导致代理失效（取决于代理方式）。
4. 在事务中执行耗时远程调用会放大锁持有时间。

> 最佳实践：事务放在 **Service 层方法**；保持事务方法短小，避免“长事务”。

---

## 6. AOP 注解

## 6.1 切面定义

- `@Aspect`：声明切面类。
- `@EnableAspectJAutoProxy`：启用 AOP 自动代理（Boot 通常已自动配置）。

## 6.2 增强注解

- `@Before`：前置通知
- `@After`：后置通知（不论异常）
- `@AfterReturning`：返回后通知
- `@AfterThrowing`：异常通知
- `@Around`：环绕通知（最强，需手动执行 `proceed()`）

## 6.3 `@Pointcut`

- 抽取切点表达式，复用与可读性更好。

```java
@Aspect
@Component
public class LogAspect {
    @Pointcut("execution(* com.dk.learn.service..*(..))")
    public void serviceMethods() {}

    @Around("serviceMethods()")
    public Object around(org.aspectj.lang.ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return pjp.proceed();
        } finally {
            long cost = System.currentTimeMillis() - start;
            // 记录耗时日志
        }
    }
}
```

> 最佳实践：AOP 适合日志、监控、鉴权、限流等横切关注点，不要塞业务主流程。

---

## 7. 数据访问相关注解

## 7.1 `@Repository`

- 语义化标记 DAO 层；在某些场景下配合异常转换机制，把底层异常转换为 Spring 统一数据访问异常体系。

## 7.2 MyBatis 常见补充（非 Spring 核心，但项目高频）

- `@Mapper`：标记 MyBatis Mapper 接口。
- `@MapperScan("...")`：批量扫描 Mapper 包。

---

## 8. 校验注解（Bean Validation）

常见约束注解（`jakarta.validation.constraints`）：

- `@NotNull` / `@NotBlank` / `@NotEmpty`
- `@Min` / `@Max` / `@Positive`
- `@Size`
- `@Email`
- `@Pattern`

触发方式：

- 在 Controller 参数上加 `@Valid` 或 `@Validated`

```java
public record UserCreateReq(
    @NotBlank String username,
    @Email String email
) {}
```

```java
@PostMapping
public void create(@Valid @RequestBody UserCreateReq req) {}
```

> 最佳实践：校验尽量前置在入口层（Controller），Service 层保留业务规则校验。

---

## 9. 异步、定时、缓存注解

## 9.1 `@EnableAsync` + `@Async`

- 异步执行方法，避免阻塞主流程。
- 建议配置线程池，不要依赖默认线程池。

## 9.2 `@EnableScheduling` + `@Scheduled`

- 定时任务（`fixedRate`、`fixedDelay`、`cron`）。
- 生产环境注意幂等、防重入与分布式锁。

## 9.3 缓存：`@EnableCaching` + `@Cacheable` / `@CachePut` / `@CacheEvict`

- `@Cacheable`：先查缓存，不命中才执行方法并写入缓存。
- `@CachePut`：执行方法并更新缓存。
- `@CacheEvict`：删除缓存（可 `allEntries = true`）。

---

## 10. 测试常用注解（Boot）

- `@SpringBootTest`：加载完整 Spring 容器（集成测试）。
- `@WebMvcTest`：MVC 切片测试（只加载 Controller 相关）。
- `@DataJpaTest` / `@MybatisTest`（如项目引入）：数据层切片。
- `@MockBean`：向 Spring 容器注入 Mock。
- `@Transactional`（测试中）：每个测试用例后回滚数据库变更。

> 最佳实践：优先“切片测试 + 少量全量集成测试”，平衡速度与覆盖率。

---

## 11. 面向工程的注解最佳实践（重点）

1. **分层语义明确**
   - Controller 用 `@RestController`
   - Service 用 `@Service`
   - DAO 用 `@Repository` / `@Mapper`

2. **注入优先构造器，不用字段注入**
   - 依赖清晰、便于单测、可做 `final` 不可变设计。

3. **配置优先 `@ConfigurationProperties`**
   - 少用散落的 `@Value`，避免“魔法字符串配置地狱”。

4. **事务放 Service，控制粒度**
   - 不在 Controller/DAO 随意开事务。
   - 明确回滚策略与隔离级别，避免长事务。

5. **统一异常处理与返回模型**
   - `@RestControllerAdvice` + `@ExceptionHandler` 统一错误输出。

6. **AOP 做横切，不侵入主业务**
   - 日志、埋点、权限、限流适合 AOP；复杂业务不要绕切面。

7. **校验前置**
   - 请求入参必须 `@Valid`/`@Validated`，不把脏数据放进业务层。

8. **注解不要“堆砌”**
   - 每个注解都应有明确目的；过度注解会降低可维护性。

9. **文档化与规范化**
   - 团队统一命名、分层与注解使用规范，减少“同事猜代码”。

---

## 12. 快速记忆（面试版）

- **Bean 管理**：`@Component`、`@Service`、`@Repository`、`@Bean`、`@Configuration`
- **依赖注入**：`@Autowired`、`@Qualifier`、`@Primary`
- **Web**：`@RestController`、`@RequestMapping`、`@PathVariable`、`@RequestBody`
- **事务**：`@Transactional`
- **AOP**：`@Aspect`、`@Around`、`@Pointcut`
- **配置**：`@ConfigurationProperties`、`@Profile`
- **校验**：`@Valid`、`@Validated` + `@NotBlank/@Email`
- **增强能力**：`@Async`、`@Scheduled`、`@Cacheable`
- **测试**：`@SpringBootTest`、`@WebMvcTest`、`@MockBean`

---

## 13. 一句话总结

Spring 注解的本质是：**用声明式方式表达组件职责、装配关系、横切能力与运行规则**。
高质量实践的关键不是“会背注解名”，而是：**分层清晰、边界明确、默认合理、可维护可测试**。
