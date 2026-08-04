#!/usr/bin/env bash

set -euo pipefail

# 固定生产目录，避免误操作其他路径。
PROJECT_DIR="/home/ai_techskill_book"
BACKEND_DIR="${PROJECT_DIR}/backend"
FRONTEND_DIR="${PROJECT_DIR}/frontend"
RUNTIME_DIR="/opt/ai-techskill-book"
WEB_ROOT="/var/www/ai-techskill-book"
NGINX_CONFIG="/etc/nginx/conf.d/ai-techskill-book.conf"
DEPLOY_TIME="$(date +%Y%m%d_%H%M%S)"
BACKUP_DIR="${RUNTIME_DIR}/backups/${DEPLOY_TIME}"

# 生产部署必须由 root 执行。
if [[ "$(id -u)" -ne 0 ]]; then
  echo "请使用 root 用户执行部署脚本" >&2
  exit 1
fi

# 只允许从远端 main 快进更新源码。
git -C "${PROJECT_DIR}" pull --ff-only origin main

# 先完成构建和测试，失败时不替换线上产物。
mvn -f "${BACKEND_DIR}/pom.xml" clean test package
npm --prefix "${FRONTEND_DIR}" ci
npm --prefix "${FRONTEND_DIR}" run build

# 保存可恢复的上一版产物。
mkdir -p "${BACKUP_DIR}"
if [[ -f "${RUNTIME_DIR}/app.jar" ]]; then
  cp -a "${RUNTIME_DIR}/app.jar" "${BACKUP_DIR}/app.jar"
fi
if [[ -d "${WEB_ROOT}" ]]; then
  tar -C "${WEB_ROOT}" -czf "${BACKUP_DIR}/frontend.tar.gz" .
fi
if [[ -f "${NGINX_CONFIG}" ]]; then
  cp -a "${NGINX_CONFIG}" "${BACKUP_DIR}/ai-techskill-book.conf"
fi

# 安装 HTTPS 配置和短期证书自动续期任务。
test -s /etc/letsencrypt/live/38.22.90.174/fullchain.pem
test -x /opt/certbot/bin/certbot
install -o root -g root -m 644 "${PROJECT_DIR}/deploy/nginx/ai-techskill-book.conf" "${NGINX_CONFIG}"
install -o root -g root -m 644 "${PROJECT_DIR}/deploy/systemd/certbot-renew-ai-techskill-book.service" /etc/systemd/system/certbot-renew-ai-techskill-book.service
install -o root -g root -m 644 "${PROJECT_DIR}/deploy/systemd/certbot-renew-ai-techskill-book.timer" /etc/systemd/system/certbot-renew-ai-techskill-book.timer
systemctl daemon-reload
systemctl enable --now certbot-renew-ai-techskill-book.timer
if ! nginx -t; then
  cp -a "${BACKUP_DIR}/ai-techskill-book.conf" "${NGINX_CONFIG}"
  nginx -t
  exit 1
fi

# 安装后端和前端新产物。
install -o aitech -g aitech -m 640 \
  "${BACKEND_DIR}/target/ai-techskill-book-backend.jar" \
  "${RUNTIME_DIR}/app.jar"
find "${WEB_ROOT}" -mindepth 1 -delete
cp -a "${FRONTEND_DIR}/dist/." "${WEB_ROOT}/"
chown -R root:nginx "${WEB_ROOT}"
find "${WEB_ROOT}" -type d -exec chmod 755 {} +
find "${WEB_ROOT}" -type f -exec chmod 644 {} +

# 重启服务并完成上线检查。
systemctl restart ai-techskill-book.service
systemctl restart nginx.service
curl -fsS http://127.0.0.1:8080/actuator/health
curl -fsS http://127.0.0.1:8080/api/ping
curl -fsSI --resolve 38.22.90.174:443:127.0.0.1 https://38.22.90.174/
systemctl is-active redis.service
systemctl is-active certbot-renew-ai-techskill-book.timer

echo "部署完成，备份目录：${BACKUP_DIR}"
