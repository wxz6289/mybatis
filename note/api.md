# API 接口文档

## 基础信息

- **Base URL**: `http://localhost:8080`
- **统一响应格式**: 
  ```json
  {
    "code": 0,           // 0表示成功，非0表示失败
    "message": "success", // 响应消息
    "data": {},          // 响应数据
    "timestamp": 1234567890 // 时间戳
  }
  ```

---

## 一、部门管理 (`/api/depts`)

### 1.1 查询部门列表（分页+条件查询）

**接口**: `GET /api/depts`

**请求参数**:

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| page | int | 否 | 1 | 页码，从1开始 |
| size | int | 否 | 10 | 每页大小 |
| name | String | 否 | - | 部门名称（模糊查询） |
| createdTimeStart | LocalDateTime | 否 | - | 创建时间开始 |
| createdTimeEnd | LocalDateTime | 否 | - | 创建时间结束 |

**请求示例**:
```bash
# 基本分页查询
GET /api/depts?page=1&size=10

# 按名称模糊查询
GET /api/depts?page=1&size=10&name=技术

# 按时间范围查询
GET /api/depts?createdTimeStart=2024-01-01T00:00:00&createdTimeEnd=2024-12-31T23:59:59

# 组合查询
GET /api/depts?page=1&size=10&name=技术&createdTimeStart=2024-01-01T00:00:00
```

**响应示例**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "name": "技术部",
        "createdTime": "2024-01-01 10:00:00",
        "updatedTime": "2024-01-01 10:00:00"
      }
    ],
    "total": 100,
    "page": 1,
    "size": 10,
    "pages": 10
  }
}
```

### 1.2 添加部门

**接口**: `POST /api/depts`

**请求头**: `Content-Type: application/json`

**请求体**:
```json
{
  "name": "技术部"
}
```

**响应示例**:
```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

### 1.3 更新部门

**接口**: `PUT /api/depts/{id}`

**路径参数**:

| 参数名 | 类型 | 说明 |
|--------|------|------|
| id | Long | 部门ID |

**请求体**:
```json
{
  "name": "技术研发部"
}
```

**请求示例**:
```bash
PUT /api/depts/1
Content-Type: application/json

{"name": "技术研发部"}
```

### 1.4 删除部门

**接口**: `DELETE /api/depts/{id}`

**路径参数**:

| 参数名 | 类型 | 说明 |
|--------|------|------|
| id | Long | 部门ID |

**请求示例**:
```bash
DELETE /api/depts/1
```

---

## 二、用户管理 (`/api/users`)

### 2.1 分页查询用户列表

**接口**: `GET /api/users`

**请求参数**:

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| page | int | 否 | 1 | 页码，从1开始 |
| size | int | 否 | 10 | 每页大小，最大100 |

**请求示例**:
```bash
GET /api/users?page=1&size=10
```

**响应示例**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "name": "张三",
        "age": 25,
        "deptId": 1,
        "createdTime": "2024-01-01 10:00:00",
        "updatedTime": "2024-01-01 10:00:00"
      }
    ],
    "total": 50,
    "page": 1,
    "size": 10,
    "pages": 5
  }
}
```

### 2.2 条件搜索用户（对象参数）

**接口**: `GET /api/users/search`

**请求参数** (所有参数可选，可任意组合):

| 参数名 | 类型 | 说明 |
|--------|------|------|
| name | String | 用户名（模糊查询） |
| startAge | Integer | 最小年龄 |
| endAge | Integer | 最大年龄 |

**请求示例**:
```bash
# 按名称搜索
GET /api/users/search?name=张三

# 按年龄范围搜索
GET /api/users/search?startAge=20&endAge=30

# 组合搜索
GET /api/users/search?name=李&startAge=25&endAge=35
```

### 2.3 条件搜索用户（注解方式）

**接口**: `GET /api/users/search2`

**请求参数** (所有参数可选):

| 参数名 | 类型 | 说明 |
|--------|------|------|
| name | String | 用户名（模糊查询） |
| startAge | Integer | 最小年龄 |
| endAge | Integer | 最大年龄 |

**请求示例**:
```bash
GET /api/users/search2?name=张&startAge=20&endAge=30
```

### 2.4 添加用户

**接口**: `POST /api/users`

**请求头**: `Content-Type: application/json`

**请求体**:
```json
{
  "name": "张三",
  "age": 25,
  "deptId": 1
}
```

**注意**: `id`、`createdTime`、`updatedTime` 由系统自动生成，无需传递。

### 2.5 删除用户（支持单个和批量）

**接口**: `DELETE /api/users` 或 `DELETE /api/users/{pathIds}`

**方式一：路径参数（推荐）**

| 参数名 | 类型 | 说明 |
|--------|------|------|
| pathIds | String | 用户ID，多个用逗号分隔 |

**请求示例**:
```bash
# 单个删除
DELETE /api/users/1

# 批量删除
DELETE /api/users/1,2,3
```

**方式二：查询参数**

| 参数名 | 类型 | 说明 |
|--------|------|------|
| ids | List<Long> | 用户ID列表 |

**请求示例**:
```bash
DELETE /api/users?ids=1,2,3
```

---

## 三、文件管理 (`/api/files`)

### 3.1 上传单个文件

**接口**: `POST /api/files/upload`

**请求头**: `Content-Type: multipart/form-data`

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| file | MultipartFile | 是 | 上传的文件 |

**请求示例**:
```bash
curl -X POST http://localhost:8080/api/files/upload \
  -F "file=@/path/to/photo.jpg"
```

**响应示例**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "originalName": "photo.jpg",
    "storedName": "a1b2c3d4e5f6g7h8.jpg",
    "size": 102400,
    "contentType": "image/jpeg",
    "url": "/api/file/view/a1b2c3d4e5f6g7h8.jpg",
    "extension": "jpg"
  }
}
```

### 3.2 批量上传文件

**接口**: `POST /api/files/upload/batch`

**请求头**: `Content-Type: multipart/form-data`

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| files | MultipartFile[] | 是 | 上传的文件数组 |

**请求示例**:
```bash
curl -X POST http://localhost:8080/api/files/upload/batch \
  -F "files=@file1.jpg" \
  -F "files=@file2.png" \
  -F "files=@file3.pdf"
```

**响应示例**:
```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "originalName": "file1.jpg",
      "storedName": "abc123.jpg",
      "size": 102400,
      "contentType": "image/jpeg",
      "url": "/api/file/view/abc123.jpg",
      "extension": "jpg"
    },
    {
      "originalName": "file2.png",
      "storedName": "def456.png",
      "size": 204800,
      "contentType": "image/png",
      "url": "/api/file/view/def456.png",
      "extension": "png"
    }
  ]
}
```

### 3.3 分页查询文件列表

**接口**: `GET /api/files/list`

**请求参数**:

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| page | int | 否 | 1 | 页码 |
| size | int | 否 | 10 | 每页大小 |

**请求示例**:
```bash
GET /api/files/list?page=1&size=10
```

**响应示例**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "originalName": "photo.jpg",
        "storedName": "abc123.jpg",
        "fileSize": 102400,
        "contentType": "image/jpeg",
        "extension": "jpg",
        "filePath": "/path/to/uploads/2026/05/09/abc123.jpg",
        "fileUrl": "/api/file/view/abc123.jpg",
        "uploadedBy": null,
        "uploadTime": "2026-05-09 10:00:00",
        "description": null,
        "deleted": 0
      }
    ],
    "total": 50,
    "page": 1,
    "size": 10,
    "pages": 5
  }
}
```

### 3.4 获取文件信息

**接口**: `GET /api/files/info/{id}`

**路径参数**:

| 参数名 | 类型 | 说明 |
|--------|------|------|
| id | Long | 文件ID |

**请求示例**:
```bash
GET /api/files/info/1
```

### 3.5 搜索文件

**接口**: `GET /api/files/search`

**请求参数** (所有参数可选):

| 参数名 | 类型 | 说明 |
|--------|------|------|
| name | String | 原始文件名（模糊查询） |
| contentType | String | 文件MIME类型 |
| page | int | 页码，默认1 |
| size | int | 每页大小，默认10 |

**请求示例**:
```bash
# 按名称搜索
GET /api/files/search?name=报告

# 按类型搜索
GET /api/files/search?contentType=image/jpeg

# 组合搜索
GET /api/files/search?name=报告&contentType=application/pdf&page=1&size=10
```

### 3.6 删除文件

**接口**: `DELETE /api/files/delete/{id}`

**路径参数**:

| 参数名 | 类型 | 说明 |
|--------|------|------|
| id | Long | 文件ID |

**请求示例**:
```bash
DELETE /api/files/delete/1
```

**注意**: 采用逻辑删除，数据库中保留记录。

### 3.7 批量删除文件

**接口**: `DELETE /api/files/batch`

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| ids | List<Long> | 是 | 文件ID列表 |

**请求示例**:
```bash
DELETE /api/files/batch?ids=1,2,3
```

---

## 四、文件访问 (`/api/file`)

> **注意**: 这些接口返回的是文件二进制流，不是JSON格式。

### 4.1 在线查看文件

**接口**: `GET /api/file/view/{filename}`

**路径参数**:

| 参数名 | 类型 | 说明 |
|--------|------|------|
| filename | String | 存储文件名（UUID格式） |

**请求示例**:
```bash
# 浏览器直接访问
GET http://localhost:8080/api/file/view/abc123.jpg

# 在HTML中使用
<img src="http://localhost:8080/api/file/view/abc123.jpg" />
```

**响应**: 
- 图片类型：直接在浏览器显示
- PDF类型：浏览器内置预览或下载
- 其他类型：根据浏览器支持情况处理

**Content-Type**: 根据文件扩展名自动设置
- `.jpg/.jpeg`: `image/jpeg`
- `.png`: `image/png`
- `.gif`: `image/gif`
- `.pdf`: `application/pdf`
- `.doc/.docx`: Word文档类型
- `.xls/.xlsx`: Excel文档类型
- 其他: `application/octet-stream`

### 4.2 下载文件（强制下载）

**接口**: `GET /api/file/download/{filename}`

**路径参数**:

| 参数名 | 类型 | 说明 |
|--------|------|------|
| filename | String | 存储文件名（UUID格式） |

**请求示例**:
```bash
# 命令行下载
curl -O http://localhost:8080/api/file/download/report.pdf

# 浏览器访问会触发下载
GET http://localhost:8080/api/file/download/report.pdf
```

**响应**: 
- Content-Type: `application/octet-stream`
- Content-Disposition: `attachment; filename="xxx"`
- 浏览器会弹出下载对话框

---

## 五、静态资源

### 5.1 直接访问上传文件

**接口**: `GET /uploads/{datePath}/{filename}`

**说明**: 通过WebConfig配置的静态资源映射，可以直接访问上传的文件。

**请求示例**:
```bash
GET http://localhost:8080/uploads/2026/05/09/abc123.jpg
```

**优势**: 
- 不经过Controller，性能更好
- 适合CDN加速
- 可以配置缓存策略

---

## 六、支持的上传文件类型

### 图片
- JPEG (.jpg, .jpeg)
- PNG (.png)
- GIF (.gif)
- WebP (.webp)

### 文档
- PDF (.pdf)
- Word (.doc, .docx)
- Excel (.xls, .xlsx)
- 文本 (.txt)

### 文件大小限制
- 单个文件最大: 10MB
- 请求总大小最大: 50MB

---

## 七、错误码说明

| 错误码 | 说明 |
|--------|------|
| 0 | 成功 |
| 500 | 服务器内部错误 |
| 400 | 请求参数错误 |
| 404 | 资源不存在 |

**错误响应示例**:
```json
{
  "code": 500,
  "message": "文件不存在",
  "data": null,
  "timestamp": 1715234567890
}
```

---

## 八、注意事项

1. **分页参数**: 页码从1开始，不是从0开始
2. **日期时间格式**: `yyyy-MM-dd HH:mm:ss`，时区为GMT+8
3. **文件上传**: 必须使用 `multipart/form-data` 格式
4. **文件访问**: `/api/file/view/` 用于在线查看，`/api/file/download/` 用于强制下载
5. **逻辑删除**: 文件和用户的删除都是逻辑删除，数据库中保留记录
6. **参数优先级**: 用户删除接口中，路径参数优先于查询参数
7. **全局响应包装**: 除 `ResponseEntity` 类型外，所有返回值自动包装为 `Result` 格式

---

## 九、快速测试

### cURL 示例

```bash
# 1. 上传文件
curl -X POST http://localhost:8080/api/files/upload \
  -F "file=@photo.jpg"

# 2. 查看文件列表
curl http://localhost:8080/api/files/list?page=1&size=10

# 3. 查看文件
curl http://localhost:8080/api/file/view/abc123.jpg -o downloaded.jpg

# 4. 添加用户
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"张三","age":25,"deptId":1}'

# 5. 删除用户
curl -X DELETE http://localhost:8080/api/users/1,2,3

# 6. 查询部门
curl "http://localhost:8080/api/depts?page=1&size=10&name=技术"
```

---

**文档版本**: v1.0  
**最后更新**: 2026-05-09
