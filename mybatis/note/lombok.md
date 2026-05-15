# Lombok 用法总结（Java）

[Lombok](https://projectlombok.org/) 通过**编译期注解处理**生成样板代码（getter/setter、构造器、equals/hashCode、Builder、日志变量等），减少手写重复代码。理解「生成了什么、和哪些框架有交互、IDE 如何识别」后使用会更稳。

---

## 1. 工作原理（核心）

- 在 **`javac` 编译阶段**，Lombok 作为 **Annotation Processor** 读取注解，向 AST 注入方法/字段等。  
- **源码里看不到**生成的方法，但 **`.class` 里存在**对应字节码。  
- 因此：**IDE 必须开启注解处理 / 安装 Lombok 支持**，否则会出现「找不到符号」的红线或无法跳转（与 Maven 能否编译成功不一定一致）。

---

## 2. 项目接入（Maven）

### 2.1 推荐写法（`provided` + 编译器注解处理路径）

在 `pom.xml` 中：

```xml
<dependencies>
  <dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <scope>provided</scope>
  </dependency>
</dependencies>

<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-compiler-plugin</artifactId>
      <configuration>
        <annotationProcessorPaths>
          <path>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${lombok.version}</version>
          </path>
        </annotationProcessorPaths>
      </configuration>
    </plugin>
  </plugins>
</build>
```

说明：

- **`provided`**：运行时不需要把 Lombok 打进业务包（生成代码已在 class 中）。  
- **`${lombok.version}`**：若使用 Spring Boot Parent，通常 BOM 会管理 Lombok 版本；也可在 `<properties>` 中显式指定。  
- 若未配置 `annotationProcessorPaths`，部分环境下仍能编译，但**显式声明更可靠**（尤其多模块、增量编译）。

### 2.2 Spring Boot 项目注意点

- `spring-boot-starter-parent` 往往已带 Lombok 版本管理；**仍需**在 `dependencies` 里声明 `lombok` 依赖才会生效。  
- 本仓库 `pom.xml` 若尚未加入 Lombok，按上节添加即可。

---

## 3. IDE 与编辑器

| 环境 | 建议 |
|------|------|
| **IntelliJ IDEA** | 安装 **Lombok 插件**（新版本常内置）；Settings → Build → Compiler → **Enable annotation processing** |
| **VS Code / Cursor** | 使用 **Language Support for Java** 等扩展；确保 `java.jdt.ls.lombokSupport.enabled`（或等价设置）开启；Maven 项目刷新 |
| **Eclipse** | 使用 Lombok 提供的 `lombok.jar` 安装器或按官方文档配置 |

现象对照：

- **Maven 能编译，IDE 全红**：多半是 IDE 未启用注解处理或未识别 Lombok。  
- **IDE 正常，Maven 报错**：检查 `maven-compiler-plugin` 与 `annotationProcessorPaths`。

---

## 4. 常用注解速查（按用途分组）

### 4.1 Getter / Setter

| 注解 | 作用 |
|------|------|
| `@Getter` / `@Setter` | 类或字段级生成访问器；可 `AccessLevel.PROTECTED` 等 |
| `@Getter(lazy = true)` | 懒加载 getter（适合昂贵初始化，线程安全由 Lombok 生成实现保证） |

### 4.2 构造器

| 注解 | 作用 |
|------|------|
| `@NoArgsConstructor` | 无参构造 |
| `@AllArgsConstructor` | 全参构造（所有字段） |
| `@RequiredArgsConstructor` | 为 **`final` 字段** 与 **`@NonNull` 字段** 生成构造器；Spring **构造器注入**常用 |

### 4.3 equals / hashCode / toString

| 注解 | 作用 |
|------|------|
| `@EqualsAndHashCode` | 生成 `equals`/`hashCode`；可 `onlyExplicitlyIncluded = true` + `@EqualsAndHashCode.Include` 精确控制参与字段 |
| `@ToString` | 生成 `toString`；可 `exclude`、`of` 指定字段 |

**继承场景**：子类若参与相等性比较，需理解父类字段是否应纳入；可用 `callSuper = true`（谨慎，父类需有合理实现）。

### 4.4 「数据类」组合

| 注解 | 等价大致包含 | 典型用途 |
|------|----------------|----------|
| `@Data` | `@Getter`、`@Setter`、`@RequiredArgsConstructor`、`@ToString`、`@EqualsAndHashCode` | DTO、简单实体（可变） |
| `@Value` | 不可变类：`final` 字段、`@Getter`、`@AllArgsConstructor`、`@EqualsAndHashCode`、`@ToString` | 值对象、不可变 DTO |

注意：`@Data` 会为**所有字段**生成 setter（除非字段级控制），不适合强调不可变语义的场景。

### 4.5 Builder 模式

| 注解 | 作用 |
|------|------|
| `@Builder` | 生成 Builder：`User.builder().name("a").build()` |
| `@SuperBuilder` | 解决 **继承链** 上的 Builder（普通 `@Builder` 与继承组合时易踩坑） |

常与 `@AllArgsConstructor(access = AccessLevel.PRIVATE)` + 自定义静态工厂配合，避免外部误用构造器。

### 4.6 资源与异常样板

| 注解 | 作用 |
|------|------|
| `@Cleanup` | 在作用域结束自动 `close()`（更推荐 Java try-with-resources） |
| `@SneakyThrows` | 把受检异常包装为未检查抛出（**慎用**，破坏显式 throws 语义） |

### 4.7 日志

| 注解 | 生成字段 |
|------|----------|
| `@Slf4j` | `private static final Logger log = LoggerFactory.getLogger(Class.class);` |
| `@CommonsLog` / `@Log4j2` 等 | 对应其他日志门面或实现 |

Spring Boot 默认常用 **SLF4J**，`@Slf4j` 最普遍。

### 4.8 其他实用注解

| 注解 | 作用 |
|------|------|
| `@NonNull` | 参数/字段非空声明；与 `@RequiredArgsConstructor`、空检查生成相关 |
| `@With` | 生成 `withXxx` 拷贝方法（不可变更新风格） |
| `@UtilityClass` | 工具类：构造器私有、方法静态化 |
| `@Accessors(chain = true)` | setter 链式返回 `this`（与某些序列化/Bean 规范需注意兼容性） |

---

## 5. 与主流框架一起用时的要点

### 5.1 Spring

- **构造器注入**：`@RequiredArgsConstructor` + `final` 依赖字段，简洁且利于测试。  
- **配置属性**：`@ConfigurationProperties` 常与标准 getter/setter 配合；链式 setter 需确认绑定器是否支持。

### 5.2 Jackson（JSON）

- 反序列化需要 **无参构造 + setter** 或 **构造器 + `@JsonCreator`** 等；`@Data` + 全参构造时要留意 Jackson 版本与 `@Jacksonized`（Lombok 与 Jackson 组合时的 Builder 支持）等文档。  
- **不变对象**：优先 `@Value` + 不可变策略，或为字段写 `@JsonProperty`。

### 5.3 JPA / MyBatis

- **JPA 实体**：通常需要 **无参构造**（`@NoArgsConstructor`）与 **getter/setter**；`@Data` 的 `equals/hashCode` 包含懒加载代理字段时可能踩坑，实体上更推荐显式 `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` 或只用业务主键参与。  
- **MyBatis**：映射结果集到对象时，需要符合属性访问约定；Lombok 生成的 getter/setter 一般无问题。

---

## 6. `lombok.config`（团队规范）

可在项目根或包路径上放置 `lombok.config` 统一风格，例如：

```properties
lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Qualifier
lombok.addLombokGeneratedAnnotation = true
```

常见用途：

- 控制哪些第三方注解在生成方法上**保留**（如 Spring 的 `@Qualifier`）。  
- 让生成代码带 **`@Generated`**，便于覆盖率工具忽略。

---

## 7. 反模式与风险（实践必看）

1. **实体滥用 `@Data`**：特别是 JPA 中 `toString`/`equals` 触达关联字段引发 N+1 或懒加载异常。  
2. **继承 + `@EqualsAndHashCode` / `@Builder`**：优先考虑组合；若必须继承，查文档用 `@SuperBuilder` 或手写。  
3. **`@SneakyThrows` 滥用**：隐藏受检异常，接口契约变差。  
4. **公开 API 库**：若发布给外部使用的 jar，考虑提供 **源码级 API** 或 **delombok** 后的源码，降低使用者对 Lombok 的强制依赖。  
5. **调试困惑**：栈里方法来自生成代码，必要时 **Delombok** 查看等价 Java。

---

## 8. Delombok（查看生成结果）

需要审查生成代码或脱离 Lombok 时：

```bash
java -jar lombok.jar delombok src -d delombok-src
```

或使用构建插件按官方文档集成。适合迁移、合规审计、对外发布源码。

---

## 9. 推荐实践清单（Checklist）

- [ ] Maven：`lombok` **provided** + `maven-compiler-plugin` **annotationProcessorPaths**  
- [ ] IDE：**启用注解处理**，并安装/启用 Lombok 支持  
- [ ] 实体/DTO：明确 **可变 vs 不可变**；实体慎用 `@Data`  
- [ ] `equals`/`hashCode`：**只包含业务相关字段**；注意继承与关联  
- [ ] 日志：优先 `@Slf4j`  
- [ ] 团队：必要时引入 **`lombok.config`** 统一行为  

---

## 10. 一句话总结

Lombok 的价值是 **用注解在编译期安全生成样板代码**；要把它用好，关键是 **搞清生成了什么、框架是否依赖反射/代理字段、以及 IDE 与 Maven 的注解处理是否一致**。
