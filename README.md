# Spring Boot + MyBatis 示例

本仓库由父聚合仓 **[`wxz6289/java`](https://github.com/wxz6289/java)** 以 **Git 子模块** 方式引用目录 `mybatis/`。

## 克隆（仅本仓库）

```bash
git clone git@github.com:wxz6289/mybatis.git
cd mybatis
```

## 配置与运行

复制 `.env.example` 为 `.env` 或导出环境变量后：

```bash
./mvnw spring-boot:run
```

## 与父仓库协作

在父仓库根目录：

```bash
git clone --recurse-submodules git@github.com:wxz6289/java.git
cd java
# 或已克隆后：
git submodule update --init mybatis
```

修改本仓库并 `git push` 后，在父仓库执行 `git add mybatis && git commit` 以更新子模块指针。
