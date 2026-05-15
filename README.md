# Spring Boot + MyBatis 示例

本仓库由父仓库 [`wxz6289/mybatis`](https://github.com/wxz6289/mybatis)（`learn/java` 聚合仓）以 **Git 子模块** 方式引用。

## 克隆（仅本仓库）

```bash
git clone git@github.com:wxz6289/mybatis-submodule.git
cd mybatis-submodule
```

## 配置与运行

复制 `.env.example` 为 `.env` 或导出环境变量后：

```bash
./mvnw spring-boot:run
```

## 与父仓库协作

在父仓库根目录：

```bash
git submodule update --init mybatis
```

修改本仓库并 `git push` 后，在父仓库执行 `git add mybatis && git commit` 以更新子模块指针。
