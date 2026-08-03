# 技术岗 AI 知识库

面向技术岗位学习与能力进阶的全栈知识库。后端使用 Java 17 + Spring Boot，前端使用 Vue 3 + Vite，数据存储使用 MySQL，生产环境由 Nginx 提供静态资源和 API 反向代理。

## 目录结构

```text
backend/                Spring Boot API
frontend/               Vue 3 + Vite 首页
```

## 本地开发

后端需要 Java 17+、Maven 3.8+ 和可访问的 MySQL 8：

```bash
cd backend
export DB_PASSWORD='请使用本机真实密码，不要写入仓库'
mvn spring-boot:run
```

前端需要 Node.js 18+：

```bash
cd frontend
npm install
npm run dev
```

开发服务器会把 `/api` 代理到 `http://127.0.0.1:8080`。

## 生产构建

```bash
cd backend && mvn clean test package
cd ../frontend && npm ci && npm run build
```

构建结果：

- 后端：`backend/target/ai-techskill-book-backend.jar`
- 前端：`frontend/dist/`

真实数据库密码、Nginx、systemd 与服务器部署文件均不保存在本仓库，由部署环境单独管理。
