# Java Stream API 核心总结与最佳实践

## 1. Stream 是什么

- **Stream** 不是数据结构，不存储元素；它描述一种**惰性求值**的元素序列计算流水线。
- 数据来源可以是：`Collection.stream()`、`Arrays.stream()`、`Stream.of`、`IntStream.range`、`Files.lines`、`Pattern.splitAsStream` 等。
- 典型用途：对集合或数据源做**声明式**、**可组合**的过滤、映射、聚合，减少手写 for 循环。

一句话：**Stream = 数据源 + 零到多个中间操作 + 一个终止操作**，终止操作触发实际计算。

---

## 2. 流水线结构

### 2.1 三部分角色

1. **数据源（Source）**：集合、数组、IO、生成器函数等。
2. **中间操作（Intermediate）**：返回新的 Stream，**惰性**：定义处理步骤但不立即执行。
3. **终止操作（Terminal）**：触发整条流水线执行，产生结果或副作用（如 `collect`、`forEach`）。

### 2.2 惰性（Lazy Evaluation）

- 只有遇到 **终止操作** 时，中间操作才会真正执行（可能只在需要时推进元素，即 **短路** 也可能生效）。
- 好处：可以合并遍历、避免不必要计算；需注意：**重复遍历集合需要重新创建 Stream**（Stream 通常是一次性消费的）。

---

## 3. 中间操作（常用）

| 操作 | 作用 |
| --- | --- |
| `filter(Predicate)` | 保留满足条件的元素 |
| `map(Function)` | 一对一映射 |
| `flatMap(Function)` | 一对多展开再扁平化为单一流 |
| `distinct()` | 去重（依赖 `equals`，对象需注意规范） |
| `sorted()` / `sorted(Comparator)` | 排序 |
| `peek(Consumer)` | 调试或附带副作用（仅限谨慎使用） |
| `limit(n)` | 只取前 n 个 |
| `skip(n)` | 跳过前 n 个 |
| `takeWhile` / `dropWhile`（Java 9+） | 按谓词分段截取（有序流上语义清晰） |

**`map` 与 `flatMap`**

- `map`：`Stream<T>` → `Stream<R>`，每个元素对应一个结果。
- `flatMap`：每个元素映射为一个 `Stream`，再**扁平合并**成一个 `Stream`（适合列表嵌套、Optional 链等）。

---

## 4. 终止操作（常用）

| 操作 | 作用 |
| --- | --- |
| `forEach` / `forEachOrdered` | 遍历（顺序保证场景用后者） |
| `collect(Collector)` | 收集为集合、字符串、分组统计等 |
| `reduce` | 归约为单个值（结合律良好时适合） |
| `min` / `max` | 最值（返回 `Optional`） |
| `count` | 计数 |
| `anyMatch` / `allMatch` / `noneMatch` | 谓词匹配（可短路） |
| `findFirst` / `findAny` | 查找（并行时 `findAny` 更宽松） |
| `toArray` | 转为数组 |

---

## 5. Collectors 核心

`Collectors` 是 `collect` 最常用的工厂：

- **`toList()` / `toSet()` / `toCollection(Supplier)`**：收集到指定集合实现。
- **`joining()`**：拼接字符串，可指定分隔符与前缀后缀。
- **`groupingBy`**：按分类函数分组（可嵌套、可下游再 `mapping`、`counting` 等）。
- **`partitioningBy`**：按 `boolean` 二分（本质是特殊的 `groupingBy`）。
- **`mapping`**：下游再映射一层。
- **`summarizingInt/Long/Double`**：一次性得到 count、sum、min、max、average。
- **`reducing`**：自定义归约收集。

---

## 6. 基本类型流（避免装箱）

- **`IntStream` / `LongStream` / `DoubleStream`**：避免 `Stream<Integer>` 等装箱开销。
- 常见入口：`list.stream().mapToInt(User::getAge)`、`IntStream.range(0, n)`。
- 专有聚合：`sum()`、`average()`、`summaryStatistics()` 等。

---

## 7. Optional 与 Stream 的配合

- `Optional.stream()`（Java 9+）：把 `Optional` 转成 0 或 1 个元素的流，便于与其它流 **flatMap** 衔接。
- 避免 `Optional.get()` 裸用；终止结果若可能为空，用 `orElse`、`orElseGet`、`orElseThrow`。

---

## 8. 并行流（parallelStream / parallel）

- **原理**：`ForkJoinPool` 分解任务，在多核上并行处理。
- **适用**：数据量大、计算**无共享可变状态**、拆分成本低、操作结合律清晰。
- **风险**：错误使用会导致结果错误或更慢（拆箱、同步、错误数据源）。
- 默认 common pool 线程数有限，与业务线程池混用时需理解竞争。

---

## 9. 与集合迭代的关系

- **Stream 不修改数据源**（除非你在操作里故意修改外部对象，属于反模式）。
- 一次 Stream **遍历结束后不能重用**，需要重新 `stream()`。
- `Iterator` / 增强 for 仍适合简单单次遍历；Stream 适合链式转换与聚合。

---

## 10. 最佳实践

### 10.1 何时使用 Stream

- **适合**：过滤 + 映射 + 聚合、分组统计、链式可读性优于多层循环。
- **不必强求**：极简单的单次遍历、性能极度敏感且已 profile 证明循环更优时，保留 for 循环更清晰。

### 10.2 可读性

- 合理拆分为**若干中间变量**或**私有方法**返回 Stream，避免一行过长。
- **方法引用**在语义清晰时优先（`User::getName`）；否则显式 lambda 更易读。

### 10.3 副作用与纯度

- **中间步骤尽量避免依赖外部可变状态**（如 `sum` 累加到外部 `int[]`）。应倾向 `reduce`、`collect`、基本类型流聚合。
- `peek` 仅用于调试或明确文档化的副作用，生产代码慎用。

### 10.4 性能注意事项

- **不要为了 Stream 而 Stream**：小列表上额外对象开销可能超过收益。
- **`parallel()` 默认慎用**：先确认线程安全、拆分成本、是否已有并发瓶颈；多数 Web 请求内并行流收益有限且可能干扰公共线程池。
- 大数据量注意 **`boxed()`** 带来的装箱成本，优先 `mapToInt` 等。

### 10.5 正确性与契约

- `distinct`、`sorted`、分组键依赖 **`equals` / `hashCode`**，自定义类型需保证契约。
- `sorted` 若元素不可比较会运行时异常；需提供 `Comparator`。

### 10.6 资源与 IO

- **`Files.lines`**、`BufferedReader.lines` 等产生 Stream 时，注意配合 **try-with-resources** 或在终止操作中关闭（或使用专门 API），避免文件句柄泄漏。

### 10.7 空与安全

- 数据源可能为 `null` 时先判空或用 `Optional.ofNullable`，避免 `NullPointerException`。
- 终止结果为空的聚合使用 `Optional` 版本 API（如 `reduce` 的无初始值重载）。

---

## 11. 简短示例（惯用法）

```java
// 过滤 + 映射 + 收集
List<String> names = users.stream()
    .filter(u -> u.isActive())
    .map(User::getName)
    .collect(Collectors.toList());

// 分组统计
Map<Department, Long> countByDept = users.stream()
    .collect(Collectors.groupingBy(User::getDept, Collectors.counting()));

// 基本类型避免装箱
int totalAge = users.stream()
    .mapToInt(User::getAge)
    .sum();
```

---

## 12. 小结

- Stream 的核心是 **惰性流水线 + 函数式风格操作符 + 终止时一次性计算**。
- 熟练 **`map` / `flatMap` / `collect` / `Collectors`** 与 **基本类型流** 可覆盖大部分业务聚合场景。
- **最佳实践**的本质：**保证可读性、避免隐蔽副作用、谨慎并行、注意资源与空值**，在性能敏感处用 profiling 验证而非凭感觉优化。
