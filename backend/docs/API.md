# 医疗多模态早筛系统 — REST API 设计说明

本文档与开题报告中的**多模态数据采集、任务管理与推理编排、单模态分析、多模态推理（大模型）**等模块对齐，并与当前 Vue 前端（`vue-project`）中的用户 / 管理员 / 病历分析任务数据结构保持一致，便于前后端联调与后续迭代。

- **Base URL（开发建议）**：`http://localhost:8080`
- **API 前缀**：`/api/v1`
- **在线 OpenAPI**：启动后端后访问 [Swagger UI](http://localhost:8080/swagger-ui.html)（springdoc；具体路径以启动日志为准）
- **统一响应体**（除文件下载等特殊接口外）：

```json
{
  "code": 0,
  "message": "ok",
  "data": {}
}
```

- `code === 0` 表示成功；非 0 表示失败，`message` 为可读说明。
- **鉴权（规划）**：用户端与管理员端使用 `Authorization: Bearer <token>`；当前后端尚未实现登录发 token 时，可先跳过该头，仅用于本地联调。

---

## 1. 健康检查

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/health` | 服务存活探测 |

**响应示例 `data`：**

```json
{
  "status": "UP",
  "service": "medical-multimodal",
  "timestamp": "2026-04-05T12:00:00Z"
}
```

---

## 2. 用户端 — 认证与账号

与前端 `appStore` 一致：普通用户使用 **姓名 + 密码** 注册与登录（后续可扩展为手机号等）。

### 2.1 注册

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/auth/register` | 新用户注册并可选自动登录 |

**请求体 JSON：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 是 | 姓名（当前与登录名一致，需唯一） |
| password | string | 是 | 密码（明文传输仅用于开发；生产应 HTTPS + 服务端哈希） |
| age | string/number | 否 | 年龄 |
| gender | string | 否 | 性别 |
| phone | string | 否 | 联系方式 |

**响应 `data`（示例）：**

```json
{
  "token": "<jwt 或 session token>",
  "user": {
    "id": "1",
    "name": "张三",
    "age": "28",
    "gender": "男",
    "phone": "13800000001"
  }
}
```

> 注意：响应中的 `user` **不应**包含 `password` 字段。

### 2.2 登录

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/auth/login` | 姓名 + 密码登录 |

**请求体：**

```json
{ "name": "张三", "password": "123456" }
```

**响应：** 同注册成功时的 `token` + `user`。

### 2.3 忘记密码 / 重置密码

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/auth/reset-password` | 按姓名重置密码（与前端 `resetUserPassword` 行为一致） |

**请求体：**

```json
{ "name": "张三", "newPassword": "654321" }
```

**响应 `data`：** 更新后的 `user`（无密码），可附带新 `token`。

### 2.4 登出（可选）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/auth/logout` | 服务端失效 token（若使用 JWT 黑名单或 session） |

---

## 3. 用户端 — 当前用户资料

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/me` | 获取当前登录用户 |
| PATCH | `/api/v1/me` | 部分更新资料（不允许改 `id`） |

**PATCH 请求体（字段均可选）：**

```json
{ "age": "29", "gender": "男", "phone": "13900000000" }
```

---

## 4. 病历分析任务（推理任务）— 与前端「分析 / 记录」对齐

对应前端 `UserAnalyze.vue` 与 `records`：`title`、`createdAt`、`status`、`detail`（内含文本报告、指标、影像描述及后续模型输出）。

### 4.1 创建任务（多模态采集 + 创建任务一步或分步）

**方案 A（与当前前端表单一致，推荐首期）**

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/records` | 创建分析任务（文本字段描述多模态输入） |

**请求体：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | string | 是 | 任务标题 |
| createdAt | string | 否 | `YYYY-MM-DD`，默认当天 |
| detail | object | 是 | 业务载荷 |
| detail.textReport | string | 是 | 病例报告文本 |
| detail.indicators | string | 否 | 结构化指标（文本或 JSON 字符串） |
| detail.imagingDesc | string | 否 | 影像描述或摘要 |
| detail.result | string | 否 | 占位；由推理完成后填充 |

**初始 `status`：** 服务端可设为 `待分析`，入队后可为 `分析中`，完成后 `已完成`；失败则为 `失败` 并附 `message`。

**响应 `data`：** 完整任务对象，例如：

```json
{
  "id": "r_xxx",
  "userId": "1",
  "createdAt": "2026-04-05",
  "title": "病历分析任务",
  "status": "待分析",
  "detail": {
    "textReport": "...",
    "indicators": "",
    "imagingDesc": "",
    "result": null
  }
}
```

### 4.2 查询当前用户的任务列表

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/records` | 当前登录用户的所有任务，建议按创建时间倒序 |

**查询参数（可选）：** `status`、`page`、`size`

### 4.3 查询单条任务

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/records/{id}` | 任务详情；仅能访问本人数据 |

### 4.4 启动 / 重试推理（任务编排）

与开题报告「任务管理与推理编排」一致：创建任务后由用户或系统自动触发流水线。

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/records/{id}/run` | 启动多模态推理（异步） |

**响应 `data`（示例）：**

```json
{
  "jobId": "job_xxx",
  "recordId": "r_xxx",
  "status": "QUEUED"
}
```

### 4.5 查询推理任务状态（可选）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/analysis-jobs/{jobId}` | 返回阶段：数据准备 / 单模态分析 / 多模态推理 / 完成 |

---

## 5. 多模态文件上传（与开题报告「多模态数据采集」扩展）

当前前端用文本字段模拟；后端可预留标准上传接口，便于接 CT/MRI、检验单附件等。

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/uploads/imaging` | `multipart/form-data`，字段 `file`；返回 `fileId`、`url` 或存储路径 |
| POST | `/api/v1/uploads/document` | 病历 PDF / 图片等 |

创建或更新 `record` 时可在 `detail` 中引用 `fileId` 列表，例如：

```json
"detail": {
  "imagingFileIds": ["f1", "f2"],
  "textReport": "..."
}
```

---

## 6. 单模态分析 & 多模态推理（内部 / 对外）

与开题报告技术路线一致：

- **单模态分析（小模型）**：影像（如 MONAI）、结构化指标（如 XGBoost/逻辑回归）— 可由 Python 推理服务或 Java 侧 HTTP 客户端调用。
- **多模态推理（大模型，如 Qwen2.5）**：汇总各模态结构化结果生成风险评估与解释文本。

可对前端**隐藏**内部步骤，仅暴露 `POST .../run` 与轮询任务状态；若需调试可增加：

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/internal/unimodal/image` | 仅内网或管理员：提交影像 `fileId`，返回结构化特征 JSON |
| POST | `/api/v1/internal/multimodal/infer` | 内网：汇总特征 + 文本，调用 LLM，返回 `riskLevel`、`summary`、`evidence` |

---

## 7. 管理员端

与前端 `AdminLogin.vue`（演示账号 `admin` / `123456`）、`AdminUsers.vue`、`AdminRecords.vue` 对齐。

### 7.1 管理员登录

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/admin/auth/login` | 账号密码，返回 `token` |

**请求体：**

```json
{ "username": "admin", "password": "123456" }
```

### 7.2 用户管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/admin/users` | 用户列表（支持 `keyword` 查询） |
| DELETE | `/api/v1/admin/users/{userId}` | 删除用户；**级联删除**其所有 `records` |

### 7.3 全量任务 / 记录管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/admin/records` | 所有用户的分析任务；支持 `keyword`、`status`、`page`、`size` |
| DELETE | `/api/v1/admin/records/{id}` | 删除指定任务 |

---

## 8. 错误码约定（建议）

| code | 含义 |
|------|------|
| 0 | 成功 |
| 400 | 参数错误 / 业务校验失败（如姓名已注册） |
| 401 | 未登录或 token 无效 |
| 403 | 无权限（如访问他人 record） |
| 404 | 资源不存在 |
| 409 | 冲突（如重复提交） |
| 500 | 服务器内部错误 |

---

## 9. 与 Vue 前端的联调提示

1. **开发环境 CORS**：后端已配置 `app.cors.allowed-origins` 指向 `http://localhost:5174`（与 `vite.config.js` 中 `server.port` 一致）。若更换端口，请同步修改 `application.yml`。
2. **Hash 路由**：前端使用 `createWebHashHistory`，请求后端 URL **不受** hash 影响，只需保证 `axios` / `fetch` 的 baseURL 指向 `http://localhost:8080`。
3. **管理员 token**：前端使用 `localStorage.admin_token`；后端发 token 后，建议统一为 Bearer 方式，前端在请求拦截器中附加即可。

---

## 10. 配置与运行（后端）

- 默认激活 **`dev`**：内存 H2，JPA `ddl-auto: update`，H2 控制台路径 `/h2-console`。
- 切换 **MySQL**：`SPRING_PROFILES_ACTIVE=prod`，并设置环境变量 `MYSQL_URL`、`MYSQL_USER`、`MYSQL_PASSWORD`（见 `application.yml`）。

```bash
cd medical-multimodal
./mvnw spring-boot:run
```

验证：`GET http://localhost:8080/api/v1/health`

---

文档版本：v1（与开题报告及当前前端数据模型同步，后续实现接口时可再细化字段校验与 OpenAPI 注解）。
