# MyBatis 学习工程

本仓库**仅维护** `mybatis/` 目录：基于 Spring Boot 与 MyBatis 的示例与练习代码。

## 目录说明

- `mybatis/`：Maven 工程根目录（`pom.xml`、源码、配置与文档）。
- 根目录 `.gitignore`：忽略 IDE、环境变量与本地凭据归档等文件。

## 本地运行

进入 `mybatis/` 后按 `mybatis/.env.example` 配置环境变量，再执行：

```bash
cd mybatis
./mvnw spring-boot:run
```

详见 `mybatis/doc/QUICK_START.md`（若存在）。
