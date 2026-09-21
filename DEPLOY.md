# 上线部署指南（Java + Spring Boot）

本项目基于 [yuyuanweb/ai-test](https://github.com/yuyuanweb/ai-test) 的 Java / Spring Boot 版，已做生产加固：密钥改环境变量、Docker 内网代理修正、Compose 健康检查与持久化卷。

## 你将得到什么

- 前端：Vue 3（浏览器访问）
- 后端：Spring Boot 3.5 + Spring AI（OpenRouter 多模型）
- 依赖：MySQL + Redis + RabbitMQ
- 一键：`docker compose up -d --build`

## 上线前准备

1. 安装 [Docker Desktop](https://www.docker.com/products/docker-desktop/)（Windows / Mac）或 Docker Engine（Linux）
2. **Mock / Live 自动切换**：`.env` 里填入真实 `OPENROUTER_API_KEY`（`sk-or-` 开头），并将 `AI_MOCK_ENABLED` 留空或设为 `false`，即可调用真实模型
3. 查看当前模式：`GET http://localhost:8123/api/system/ai-status`
4. **现阶段默认本地上传**：可不配腾讯云 COS，图片落到容器卷并通过 `/api/files/**` 访问
5. （可选）腾讯云 COS：配置真实密钥后可将 `STORAGE_LOCAL_ENABLED=false` 切到云存储

## 本地 / 服务器一键启动

```bash
# 1. 进入项目根目录
cd "C:\AI model test"

# 2. 准备环境变量
copy .env.example .env
# 改掉默认弱密码即可；大模型 Key 可后填

# 3. 构建并启动
docker compose up -d --build

# 4. 查看日志（首次 Maven 构建可能较久）
docker compose logs -f backend
```

启动成功后：

| 入口 | 地址 |
|------|------|
| 网站前台 | http://localhost:8090 |
| 后端健康检查 | http://localhost:8123/api/actuator/health |
| RabbitMQ 管理台 | http://localhost:15673 （账号见 .env） |

## 公网部署建议

1. 把项目放到云服务器，同样用 Docker Compose 启动
2. 用 Nginx / Caddy 反代到 `FRONTEND_PORT`（默认 8090），并配置 HTTPS
3. **不要**把 MySQL / Redis / RabbitMQ 端口暴露到公网（可在 `docker-compose.yml` 里删掉对应 `ports`）
4. `.env` 不要提交到 Git（已在 `.gitignore`）
5. 定期备份 `mysql-data` 卷

示例 Caddy 反代：

```
your-domain.com {
  reverse_proxy localhost:8090
}
```

## 本地开发（不走 Docker 全套）

需要本机已有 MySQL / Redis / RabbitMQ，或只 docker 起中间件：

```bash
docker compose up -d mysql redis rabbitmq
```

后端：

```bash
# 设置 OpenRouter Key 后启动
set OPENROUTER_API_KEY=sk-or-v1-xxx
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

前端：

```bash
cd frontend
npm install
npm run dev
```

## 常见问题

**backend 一直 unhealthy**  
看 `docker compose logs backend`。多数是 MySQL 未就绪或 `OPENROUTER_API_KEY` 为空。

**能打开页面但模型无不动**  
检查 `.env` 里 Key 是否有效、账户是否有余额。

**图片上传失败**  
配置腾讯云 COS 相关环境变量；未配置时核心对比功能仍可用。

**改密码后 Redis/MySQL 起不来**  
旧数据卷仍是旧密码。开发环境可 `docker compose down -v` 清空卷后重建（会丢数据）。

## 技术栈

- Java 21、Spring Boot 3.5、Spring AI、MyBatis-Flex
- Vue 3、Vite、Ant Design Vue、ECharts
- MySQL 8、Redis 7、RabbitMQ 3.12
