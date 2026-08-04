# 技术岗 AI 知识库

面向技术岗位学习与能力进阶的全栈知识库。后端使用 Java 17 + Spring Boot，前端使用 Vue 3 + Vite，数据存储使用 MySQL，用户会话存储在 Redis，头像等文件使用私有 S3 兼容对象存储，生产环境由 Nginx 提供静态资源和 API 反向代理。

## 目录结构

```text
backend/                Spring Boot API
frontend/               Vue 3 + Vite 首页
```

## 本地开发

后端需要 Java 17+、Maven 3.8+、MySQL 8 和 Redis 6+：

```bash
cd backend
export DB_PASSWORD='请使用本机真实密码，不要写入仓库'
export REDIS_PASSWORD='请使用本机真实密码，不要写入仓库'
# 需要测试文件上传时，再通过 STORAGE_* 环境变量启用本地对象存储连接。
mvn spring-boot:run
```

前端需要 Node.js 20.19+：

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

真实数据库密码和环境配置不保存在本仓库；Nginx 与 systemd 仅提交不含凭据的部署模板。

对象存储默认关闭。启用后，文件按 `环境/业务/年/月/归属ID/UUID.扩展名` 保存；存储桶保持私有，前端通过后端受控地址读取头像。

生产环境的用户登录必须通过 HTTPS，并将 `SESSION_SECURE_COOKIE` 设置为 `true`。

详细代码规范和上线流程见 `docs/代码规范.md` 与 `docs/部署说明.md`。
