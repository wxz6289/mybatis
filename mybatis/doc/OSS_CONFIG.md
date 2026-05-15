# 阿里云OSS文件存储配置说明

## 功能概述

本项目支持两种文件存储方式：
1. **本地存储**：文件存储在服务器本地文件系统
2. **阿里云OSS存储**：文件存储到阿里云对象存储服务

## 配置步骤

### 1. 获取阿里云OSS credentials

1. 登录阿里云控制台
2. 进入 Access Key 管理页面
3. 创建或获取 AccessKey ID 和 AccessKey Secret
4. 创建 OSS Bucket（存储空间）

### 2. 配置 application.yaml

在 `src/main/resources/application.yaml` 中配置以下参数：

```yaml
# 文件上传配置
file:
  upload:
    storage-type: oss  # 设置为 oss 启用阿里云OSS存储，local 为本地存储

# 阿里云OSS配置
aliyun:
  oss:
    endpoint: oss-cn-hangzhou.aliyuncs.com  # OSS Endpoint，根据您的地域修改
    access-key-id: your-access-key-id        # 替换为您的AccessKey ID
    access-key-secret: your-access-key-secret # 替换为您的AccessKey Secret
    bucket-name: your-bucket-name            # 替换为您的Bucket名称
    domain: https://cdn.example.com          # 可选，自定义加速域名
```

### 3. 常见Endpoint参考

| 地域 | Endpoint |
|------|----------|
| 华东1（杭州） | oss-cn-hangzhou.aliyuncs.com |
| 华东2（上海） | oss-cn-shanghai.aliyuncs.com |
| 华北1（青岛） | oss-cn-qingdao.aliyuncs.com |
| 华北2（北京） | oss-cn-beijing.aliyuncs.com |
| 华南1（深圳） | oss-cn-shenzhen.aliyuncs.com |

更多地域请参考：https://help.aliyun.com/document_detail/31837.html

## 使用方式

### 切换存储方式

只需修改配置文件中的 `storage-type` 即可：

- `storage-type: local` - 使用本地存储
- `storage-type: oss` - 使用阿里云OSS存储

### API接口

文件上传接口保持不变：

```bash
# 单个文件上传
POST /api/files/upload

# 批量文件上传
POST /api/files/upload/batch
```

## 文件存储结构

### OSS存储结构
```
bucket-name/
├── 2026/
│   └── 05/
│       └── 11/
│           ├── uuid1.jpg
│           ├── uuid2.png
│           └── ...
```

### 返回结果示例
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "originalName": "test.jpg",
    "storedName": "2026/05/11/abc123def456.jpg",
    "size": 102400,
    "contentType": "image/jpeg",
    "extension": "jpg",
    "url": "https://your-bucket.oss-cn-hangzhou.aliyuncs.com/2026/05/11/abc123def456.jpg"
  }
}
```

## 注意事项

1. **安全性**：不要将 AccessKey Secret 提交到代码仓库，建议使用环境变量
2. **费用**：OSS存储会产生费用，请注意控制用量
3. **权限**：确保Bucket权限设置正确，建议设置为私有读写
4. **CDN加速**：如需加速访问，可配置CDN并设置domain参数

## 环境变量配置（推荐）

为了安全起见，建议使用环境变量配置敏感信息：

```yaml
aliyun:
  oss:
    endpoint: ${OSS_ENDPOINT:oss-cn-hangzhou.aliyuncs.com}
    access-key-id: ${OSS_ACCESS_KEY_ID}
    access-key-secret: ${OSS_ACCESS_KEY_SECRET}
    bucket-name: ${OSS_BUCKET_NAME}
    domain: ${OSS_DOMAIN:}
```

启动时设置环境变量：
```bash
export OSS_ACCESS_KEY_ID=your-access-key-id
export OSS_ACCESS_KEY_SECRET=your-access-key-secret
export OSS_BUCKET_NAME=your-bucket-name
export OSS_ENDPOINT=oss-cn-hangzhou.aliyuncs.com
```
