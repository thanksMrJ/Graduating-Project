# Graduating-Project（前后端同仓）

| 目录 | 说明 |
|------|------|
| `frontend/` | Vue 3 + Vite 前端 |
| `backend/` | Spring Boot 后端（`medical-multimodal`） |

## 本地运行

**前端**

```bash
cd frontend
npm install
npm run dev
```

默认开发地址见 `frontend/vite.config.js`（当前为 `http://localhost:5174`）。

**后端**

```bash
cd backend
./mvnw spring-boot:run
```

默认 `http://localhost:8080`。

## 部署

- 前端：推送到 `main` 后由 GitHub Actions 构建 `frontend/` 并发布到 GitHub Pages。
- 后端：需自行部署到云服务器 / 容器等（本仓库 workflow 仅负责前端静态站点）。

更详细的 API 与数据库脚本见 `backend/docs/`。
