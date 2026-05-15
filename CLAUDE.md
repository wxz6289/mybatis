# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 在此仓库中工作时提供指导。

## 构建与运行

```bash
# 构建并运行测试
./mvnw clean test

# 运行应用（需要本地 MySQL，见下文）
./mvnw spring-boot:run

# 运行单个测试类
./mvnw test -Dtest=LearnApplicationTests

# 运行单个测试方法
./mvnw test -Dtest=LearnApplicationTests#testGetUser
```

## 技术栈

- **Java 21**、Spring Boot 4.0.6、MyBatis 4.0.1
- MySQL Connector J + Druid 连接池（ali/druid-spring-boot-starter 1.2.20）
- PageHelper（com.github.pagehelper 2.1.1）
- Lombok 1.18.38（通过 maven-compiler-plugin 进行注解处理）
- 阿里云 OSS SDK（aliyun-sdk-oss 3.18.1）

## 数据库

需要本地 MySQL 实例。`application.yaml` 中的默认配置：
- 主机：`localhost:3306`，数据库：`mybatis`
- 用户名：`${DB_USERNAME:king}`，密码：`${DB_PASSWORD:king123}`
- 可通过环境变量 `DB_USERNAME` / `DB_PASSWORD` 覆盖

需要的表：`user`（id, name, age, deptId, createdAt, updatedAt）、`dept`（id, name, createdAt, updatedAt）、`file_info`（id, original_name, stored_name, file_size, content_type, extension, file_path, file_url, uploaded_by, upload_time, description, deleted）。

## 架构

`com.dk.learn` 下的标准分层 Spring Boot 结构：

```
controller/  →  REST 接口、请求映射、参数校验
service/     →  业务逻辑，委托给 mapper
mapper/      →  MyBatis Mapper 接口（@Mapper）
entity/      →  数据库实体（User, Dept, FileInfo）及查询/VO 对象
config/      →  @Configuration 配置类（Web, Jackson, OSS, 异常处理）
common/      →  page/（PageQuery, PageResult, PageBean）和 result/（Result<T>）
```

## MyBatis 使用模式

项目中同时展示了两种方式：

1. **XML 动态 SQL** — `UserMapper.list()` 和 `listByCondition` 在 `UserMapper.java` 中声明为方法签名，实际 SQL 在 `src/main/resources/com/dk/learn/mapper/UserMapper.xml` 中，使用 `<where>`、`<if>`、`<foreach>`、`<set>` 标签。`DeptMapper` 同理。这是复杂查询的主要方式。

2. **注解 SQL** — `UserMapper.listWithAnnotation()` 使用 `@Select("<script>...")` 直接在 Java 接口中嵌入动态 SQL。简单查询（`getUser`、`addUser`、`removeUser`）使用普通的 `@Select`/`@Insert`/`@Update` 注解。

## 统一返回结果模式

所有控制器响应应包装在 `Result<T>` 中（来自 `common/result/Result.java`）：
- `Result.ok(data)` — 成功，带数据
- `Result.error(msg)` / `Result.fail(code, msg)` — 错误

`ApiResponseBodyAdvice` 实现了 `ResponseBodyAdvice` 以确保所有响应统一包装（需确认是否已生效——如未生效，需在控制器中显式包装）。

## 分页

使用两种分页策略：

1. **手动 offset/size** — `UserMapper.listWithPagination` 将 `offset`/`size` 传入 SQL `LIMIT #{offset}, #{size}`。`PageQuery` 是一个 record，`of(page, size)` 方法将基于 1 的页码转换为 offset。`PageResult.of(records, total, pq)` 构建返回结果。

2. **PageHelper** — `DeptController` 使用 `PageHelper.startPage(page, size)`，然后用 `PageInfo` 包装结果。这是第三方方案，使用 `PageInfo.getTotal()`、`getPageNum()`、`getPages()` 来构建 `PageResult`。

## 文件上传系统

双存储后端，由 `application.yaml` 中的 `file.upload.storage-type` 控制：
- `local` — 文件保存到 `file.upload.path`（默认 `./uploads`）
- `oss` — 文件上传到阿里云 OSS，通过 `aliyun.oss.*` 属性配置

`FileService` 作为门面，委托给配置的存储后端。文件元数据始终通过 `FileInfoMapper` 持久化到 `file_info` 表。文件访问通过 `FileAccessController`（公开下载/预览），管理操作通过 `FileManageController`（上传/删除/列表）。

## 全局异常处理

`GlobalExceptionHandler`（@RestControllerAdvice）捕获：
- `IllegalArgumentException` → 400
- `MethodArgumentTypeMismatchException` → 400，并返回详细错误信息
- `Exception`（兜底）→ 500，记录堆栈日志