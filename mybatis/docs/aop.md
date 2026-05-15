# AOP（面向切面编程）核心与最佳实践

**AOP（Aspect-Oriented Programming）** 把与业务主流程弱相关、却在多处重复出现的逻辑（日志、鉴权、事务、监控、缓存等）抽成**切面**，在**连接点**上按**切点**规则统一织入，从而减少模板代码、集中维护横切关注点。

在 Spring 生态中，日常所说的 AOP 多数指 **Spring AOP**：默认基于 **代理** 的运行时织入，与 **AspectJ 编译期织入** 不同。下文以 **Spring AOP + @AspectJ 注解风格** 为主。

---

## 目录

1. 为什么需要 AOP  
2. Spring AOP 与 AspectJ  
3. 核心概念对照表  
4. 代理机制与可见范围  
5. 通知类型与执行顺序  
6. 切点表达式（Pointcut）  
7. 多个切面与顺序  
8. 常见陷阱  
9. 与声明式事务、MyBatis 的配合  
10. 最佳实践清单  
11. 参考  

---

## 1. 为什么需要 AOP

没有 AOP 时，每个业务方法里都要写一遍「打日志、开事务、计时、权限判断」等，带来：

- 代码重复、可读性差  
- 横切逻辑变更要改大量文件  
- 容易遗漏或行为不一致  

AOP 把这些逻辑收敛到**少量切面类**，业务类只关注领域逻辑。

---

## 2. Spring AOP 与 AspectJ

| 对比项 | Spring AOP | AspectJ |
|--------|--------------|---------|
| 织入时机 | 运行时，通过 **JDK 动态代理** 或 **CGLIB** 生成代理对象 | 编译期或加载期织入字节码 |
| 能拦截的对象 | 必须是 **Spring 容器中的 Bean** 的**代理入口**调用 | 更细粒度，可拦截 `new`、私有方法等（视织入方式） |
| 典型使用 | `@Aspect` + `@Before` / `@Around` 等，与 Spring 集成简单 | `ajc` 编译、LTW 等，能力更强、构建更复杂 |

**结论**：一般业务与框架扩展用 **Spring AOP** 足够；需要非 Spring 管理对象或更细粒度时再评估 **AspectJ**。

---

## 3. 核心概念对照表

| 术语 | 含义 |
|------|------|
| **Aspect（切面）** | 横切关注点的模块化，通常是一个带 `@Aspect` 的类 |
| **Join Point（连接点）** | 程序执行过程中的一个点；在 Spring AOP 中主要是 **方法的执行** |
| **Pointcut（切点）** | 匹配一组连接点的谓词，如 `execution(* com.example.service..*(..))` |
| **Advice（通知）** | 在切点上要执行的逻辑：`@Before`、`@Around` 等 |
| **Introduction（引入）** | 为已有类型动态添加接口实现（使用相对较少） |
| **Weaving（织入）** | 把切面应用到目标对象产生代理的过程；Spring 多在容器创建 Bean 时完成 |
| **Advisor** | Spring 中的一对「切点 + 通知」的抽象；`@Aspect` 背后也会被解析为 Advisor |

---

## 4. 代理机制与可见范围

### 4.1 两种代理

- **JDK 动态代理**：目标类**实现接口**时，对 **接口方法** 生成代理。  
- **CGLIB**：基于子类代理，可代理**未实现接口的类**（不能代理 `final` 类，且对 `final` 方法无法增强）。

Spring Boot 2.x 起，对类代理常用 **CGLIB**（`spring.aop.proxy-target-class=true` 为常见默认），具体以项目配置与 Spring 版本为准。

### 4.2 自调用（Self-invocation）

只有通过 **Spring 注入的代理引用** 调用方法时，切面才会生效。在类内部 **`this.otherMethod()`** 走的是 **目标对象本身**，**不会**经过代理，因此 **AOP 不生效**。

**解决思路**：拆到另一个 Bean、通过 `AopContext.currentProxy()`（需开启 `exposeProxy`）、或避免在同类里绕开代理的调用链。

### 4.3 可见性与可拦截方法

在 **Spring AOP** 下，通常只有 **public** 方法作为对外连接点最稳妥（`protected` 在 CGLIB 下部分场景可用，但不建议依赖）。**private**、**final**、**static** 方法**不应**指望被 Spring AOP 增强。

---

## 5. 通知类型与执行顺序

| 注解 | 时机 |
|------|------|
| `@Before` | 目标方法执行前 |
| `@After` | 目标方法之后（**无论是否异常**，类似 finally 语义） |
| `@AfterReturning` | 方法**正常返回**后，可绑定返回值 |
| `@AfterThrowing` | 方法**抛出异常**后，可绑定异常类型 |
| `@Around` | 包裹整个方法：可在前后任意逻辑，**必须**在合适位置调用 `ProceedingJoinPoint.proceed()` |

**`@Around` 注意**：不调用 `proceed()` 则目标方法不执行；吞掉异常则外层 `@AfterThrowing` 可能收不到；可在 `try/finally` 里做资源与计时。

**同一切面内多通知**：Spring 5.2+ 可用 `@Order` 控制**切面类**顺序；同一 `@Aspect` 内多 `@Before` 等顺序建议合并为一个方法或拆成多个切面类并显式 `@Order`，避免依赖未文档化的声明顺序。

---

## 6. 切点表达式（Pointcut）

常用 **AspectJ 切点语言**写法（Spring AOP 支持其中子集，以官方文档为准）：

| 指示符 | 典型用途 |
|--------|----------|
| `execution(...)` | 按**方法签名**匹配，最常用 |
| `within(TypePattern)` | 按**类型范围**匹配某包或某类下所有方法 |
| `this(type)` / `target(type)` | 按代理对象类型 / 目标对象类型 |
| `args(...)` | 按**参数类型** |
| `@annotation(...)` | 方法上带某注解 |
| `@within(...)` / `@target(...)` | 类上注解或运行时目标类型带注解 |
| `bean(idOrName)` | 按 **Spring Bean 名称** |

**execution 简例**：

```text
execution(public * com.example.service.UserService.*(..))
execution(* com.example..service.*Service.*(..))
execution(@org.springframework.transaction.annotation.Transactional * *(..))
```

**组合**：`&&`、`||`、`!` 组合多个表达式；复杂切点可抽成 `@Pointcut` 方法复用。

**性能**：切点匹配在代理创建或每次调用时有成本，应避免过于宽泛的 `execution(* *(..))` 配重量级 `@Around`。

---

## 7. 多个切面与顺序

- 切面类上使用 **`@Order(整数)`**：数值**越小**越靠前（对 `@Before` 越先执行；对 `@After` 往往越后执行，呈「洋葱」模型）。  
- **声明式事务** `@Transactional` 也是通过 AOP 实现，通常应让**事务切面**在**最外层**或按团队约定顺序，避免「已提交事务后又做应回滚的逻辑」等错误组合。

---

## 8. 常见陷阱

1. **自调用导致切面不生效**：见上文 4.2。  
2. **在切面里再调同一 Service 且又命中切点**：可能递归或重复增强，需控制切点范围。  
3. **`@Around` 未 `proceed` 或错误处理异常**：业务不执行或异常语义被改变。  
4. **把大量业务写在切面里**：切面应薄，复杂规则仍应在 Service 或领域层。  
5. **忽略线程与异步**：`@Async` 后在新线程执行的方法若未经过同一套代理/上下文，**ThreadLocal**（如用户上下文）可能丢失，与 AOP 无直接关系但常一起出现。  
6. **与 `final` 类**：无法 CGLIB 子类化，可能导致无法创建代理或退化为仅接口 JDK 代理，需提前设计。

---

## 9. 与声明式事务、MyBatis 的配合

- **`@Transactional`** 由 Spring 事务 AOP 实现，与自定义切面一样是**代理**。自调用同样会导致**事务不生效**。  
- **事务边界**：放在 **Service** 的 public 方法上，内部调用 **Mapper / Repository**；不要在 Controller 上滥用长事务。  
- **只读事务**：查询密集可用 `@Transactional(readOnly = true)`，配合连接池与驱动行为优化（视数据库与配置而定）。  
- **MyBatis**：SQL 与 Mapper 保持纯粹；**审计字段填充**（如 `createdBy`）可用切面 + 注解或 `MetaObjectHandler`（若用 MyBatis-Plus）等，避免在 XML 里散落重复赋值逻辑。

---

## 10. 最佳实践清单

1. **切面要薄**：日志、鉴权、监控、幂等 token 校验等横切逻辑；核心业务留在 Service。  
2. **切点要准**：优先 `execution` / `@annotation` 精确到包与方法，避免全局 `*` 匹配。  
3. **优先能测的设计**：切面逻辑可单元测试；对关键 `@Around` 写集成测试验证 `proceed` 与异常路径。  
4. **顺序显式化**：多切面用 `@Order` 文档化；事务与自定义切面的相对顺序要团队统一。  
5. **避免在 `@Around` 里远程 IO**：与事务方法同线程时拉长持锁时间。  
6. **与 Spring Security / MDC**：在 `@After` / `finally` 中清理 **MDC**、**SecurityContext** 子线程副本等，防止线程池污染。  
7. **需要更细粒度时**：评估 **AspectJ** 或 **Filter / HandlerInterceptor** 等更合适的层次，而不是强行用方法级 AOP 解决所有问题。

---

## 11. 参考

- Spring Framework：[Aspect Oriented Programming with Spring](https://docs.spring.io/spring-framework/reference/core/aop.html)  
- Spring Framework：[Pointcut API](https://docs.spring.io/spring-framework/reference/core/aop-api/pointcuts.html)（与 `@AspectJ` 表达式对照）  
- 同目录：[Spring 注解](./spring.md)、[拦截器](./interceptor.md)

---

## 一句话总结

**Spring AOP 通过代理在方法连接点织入横切逻辑：弄清代理、自调用与切点范围，用 `@Around` 时保证 `proceed` 与异常语义，用 `@Order` 管理多切面，并把事务与核心业务边界放在 Service，与 MyBatis 持久层解耦。**
