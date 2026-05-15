# Java Lambda 表达式核心总结与最佳实践

## 1. Lambda 是什么

- **Lambda 表达式**是 Java 8 引入的语法，用于表示**匿名函数**（没有名字的函数式实现）。
- 本质上是 **函数式接口（Functional Interface）** 单抽象方法（SAM）的**简洁实现**，编译器会生成合成方法或 invokedynamic 调用。
- 典型用途：传给 `Stream`、`Optional`、`CompletableFuture`、`Thread`、`Executor`、Spring `@Bean` 工厂等需要“一块行为”的 API。

语法骨架：

```java
(参数列表) -> { 方法体 }
```

单行可省略大括号与 `return`（表达式直接作为返回值）。

---

## 2. 函数式接口（Functional Interface）

### 2.1 定义

- 只有一个 **抽象方法** 的接口（可另有 `default`/`static` 方法）。
- 建议标注 **`@FunctionalInterface`**：编译期校验，并在文档中表明用途。

### 2.2 `java.util.function` 包（最常用）

| 接口 | 抽象方法 | 用途 |
| --- | --- | --- |
| `Predicate<T>` | `boolean test(T t)` | 断言、过滤 |
| `Function<T,R>` | `R apply(T t)` | 映射 T → R |
| `Consumer<T>` | `void accept(T t)` | 消费，无返回值 |
| `Supplier<T>` | `T get()` | 供应，无参产出 |
| `UnaryOperator<T>` | `T apply(T t)` | 一元运算，入参出参同型 |
| `BinaryOperator<T>` | `T apply(T l, T r)` | 二元归约（同型） |
| `BiFunction<T,U,R>` | `R apply(T t, U u)` | 双入映射 |
| `BiConsumer<T,U>` | `void accept(T t, U u)` | 双入消费 |

基本类型特化（避免装箱）：如 `IntPredicate`、`ToIntFunction`、`ObjIntConsumer` 等。

---

## 3. Lambda 语法要点

### 3.1 参数

- 类型可省略，由目标类型推断（target typing）。
- 单参数可省略括号：`x -> x + 1`。
- 无参数：`() -> 42`。
- 多参数：`(a, b) -> a.compareTo(b)`。

### 3.2 方法体

- 单表达式：无需 `{}`，值即返回值。
- 多语句：使用 `{}`，需要返回值时显式 `return`。

### 3.3 目标类型（Target Typing）

Lambda **不能脱离上下文类型单独存在**，必须能推断出实现的是哪个函数式接口，例如：

```java
Predicate<String> p = s -> s.isEmpty();
Function<String, Integer> f = String::length;
```

---

## 4. 方法引用（Method Reference）

当 Lambda 仅调用已有方法时，可用 **`类名::方法名`** 或 **`实例::方法名`** 简化：

| 形式 | 示例 | 说明 |
| --- | --- | --- |
| 静态方法 | `Integer::parseInt` | `s -> Integer.parseInt(s)` |
| 实例方法（特定对象） | `System.out::println` | 绑定接收者 |
| 实例方法（第一个参数作接收者） | `String::length` | `s -> s.length()` |
| 构造方法 | `ArrayList::new` | `() -> new ArrayList<>()` |

**何时用方法引用**：语义一目了然时优先；参数映射复杂时 Lambda 更清晰。

---

## 5. 变量捕获与 effectively final

- Lambda **只能读取**外层局部变量与参数，且这些变量必须是 **effectively final**（赋值后未再修改）。
- **实例字段、静态字段**可读写（仍要注意线程安全）。
- 不能在 Lambda 里对外层局部变量 **赋值**（语言限制）。

目的：避免隐蔽的可变共享状态，保证闭包语义清晰。

---

## 6. Lambda 与匿名内部类

| 维度 | Lambda | 匿名类 |
| --- | --- | --- |
| 语法 | 简洁 | 冗长 |
| `this` 含义 | 外层 enclosing 实例 | 匿名类自身 |
| 可额外声明字段 | 否（除非用块级技巧） | 可以 |
| 对接口/类数量 | 仅函数式接口 SAM | 可实现多方法 |
| 调试栈信息 | 有时合成名较难读 | 类名固定 |

需要访问外围 `this` 的多层语义或实现多个方法时，匿名类仍有用武之地。

---

## 7. 异常与 Lambda

- 函数式接口抽象方法**未声明检查异常**时，Lambda 内**不能直接抛出**受检异常（除非包装）。
- 常见做法：
  - 在 Lambda 内 try-catch 并转为运行时异常；
  - 使用自定义函数式接口 `throws Exception`（打破标准接口签名）；
  - 提取为具名方法，在方法上声明 throws。

---

## 8. 泛型与类型推断

- 钻石运算符与泛型推断：`List.of()`、`Collectors.toList()` 等与 Lambda 配合良好。
- 复杂嵌套时编译器可能推断失败，需显式类型：`(String s) -> s.length()` 或对变量声明接口类型。

---

## 9. 最佳实践

### 9.1 可读性优先

- Lambda **不宜过长**；超过若干行应提取为 **私有方法** 或 **具名类**。
- 复杂条件用 **具名方法** + 方法引用，比嵌套 Lambda 更易测。

### 9.2 副作用与纯函数倾向

- 在 `Stream`、`parallelStream` 中，优先 **无副作用** 的映射与过滤；副作用集中在 `forEach` 或明确边界。
- 避免在 Lambda 里修改共享可变集合以外的**外部计数器**等模式（易错且不利于并行）。

### 9.3 调试与测试

- 业务复杂逻辑尽量落在 **可单测的普通方法** 中，Lambda 只做薄包装。
- 日志：复杂 Lambda 内打断点有时不如抽方法清晰。

### 9.4 性能直觉

- Lambda 创建本身有成本但通常可忽略；热点在算法与数据结构。
- **并行流** + 捕获过多变量或同步块可能抵消收益（参见 `stream.md`）。

### 9.5 API 设计

- 对外暴露 API 时，参数类型优先使用 **`java.util.function`** 标准接口，便于与 Stream、Optional 生态组合。
- 若必须抛出受检异常，定义自己的 `@FunctionalInterface` 并声明 throws，文档写清楚。

### 9.6 序列化与框架

- Lambda 序列化行为与合成类相关，**分布式或某些框架**若要求稳定签名，慎用匿名 Lambda 作为可序列化回调；必要时使用 **具名类** 或 **方法引用**（依具体 JDK/框架版本行为为准）。

---

## 10. 常见误区

- **误区 1**：认为 Lambda 总是更快。性能取决于算法，不是语法糖。
- **误区 2**：在 Lambda 里修改外层局部变量。编译禁止；若用数组或集合 hack，可读性与线程安全差。
- **误区 3**：到处方法引用。参数意图不清晰时反而降低可读性。
- **误区 4**：忽略受检异常传播，到处 `RuntimeException` 包装却无统一错误模型。

---

## 11. 最小示例对照

```java
// Lambda
list.forEach(s -> System.out.println(s));

// 方法引用等价
list.forEach(System.out::println);

// Predicate 组合
Predicate<String> notEmpty = s -> !s.isBlank();
Predicate<String> shortOk = s -> s.length() < 100;
Predicate<String> valid = notEmpty.and(shortOk);
```

---

## 12. 小结

- Lambda 是 **函数式接口的语法糖**，与 **Stream / Optional / CompletableFuture** 等 API 配套使用威力最大。
- 掌握 **`java.util.function`**、**方法引用**、**effectively final** 与 **异常策略**，即可避免多数坑。
- **最佳实践**核心：**短、纯、可测试**；复杂逻辑下沉到具名方法；对外 API 优先标准函数式接口。
