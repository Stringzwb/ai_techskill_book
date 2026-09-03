#!/usr/bin/env bash

set -euo pipefail

# 固定生产目录，避免误操作其他路径。
PROJECT_DIR="/home/ai_techskill_book"
BACKEND_DIR="${PROJECT_DIR}/backend"
FRONTEND_DIR="${PROJECT_DIR}/frontend"
RUNTIME_DIR="/opt/ai-techskill-book"
WEB_ROOT="/var/www/ai-techskill-book"
NGINX_CONFIG="/etc/nginx/conf.d/ai-techskill-book.conf"
SERVICE_NAME="ai-techskill-book.service"
EXPECTED_PUBLIC_IP="38.22.90.174"
EXPECTED_GIT_REMOTE="https://github.com/Stringzwb/ai_techskill_book.git"
EXPECTED_GIT_SSH_REMOTE="git@github.com:Stringzwb/ai_techskill_book.git"
CERT_DIR="/etc/letsencrypt/live/${EXPECTED_PUBLIC_IP}"
CHINESE_FONT_DIR="/usr/local/share/fonts/ai-techskill-book"
CHINESE_FONT_FILE="${CHINESE_FONT_DIR}/wqy-microhei.ttc"
CHINESE_FONT_URL="https://cdn.jsdelivr.net/gh/anthonyfok/fonts-wqy-microhei@cd82defe33ec0e86e628329f1b63049ef562c8e5/wqy-microhei.ttc"
CHINESE_FONT_SHA256="e4bca8df123ce01b104780f576ea1a58b9a5ff1662a91124b6d3180cb6c88212"
IPA_FONT_FILE="/usr/share/fonts/dejavu-sans-fonts/DejaVuSans.ttf"
DEPLOY_TIME="$(date +%Y%m%d_%H%M%S)"
BACKUP_DIR="${RUNTIME_DIR}/backups/${DEPLOY_TIME}"

fail() {
  echo "部署前校验失败：$*" >&2
  exit 1
}

require_command() {
  command -v "$1" > /dev/null 2>&1 || fail "缺少命令 $1"
}

# 生产部署必须由 root 执行。
if [[ "$(id -u)" -ne 0 ]]; then
  echo "请使用 root 用户执行部署脚本" >&2
  exit 1
fi

require_command git
require_command mvn
require_command npm
require_command curl
require_command systemctl
require_command nginx
require_command sha256sum

# 部署脚本只允许在本项目生产机执行，避免 SSH 工具误连到其他服务器。
[[ -d "${PROJECT_DIR}/.git" ]] || fail "未找到 ${PROJECT_DIR}/.git"
PRIMARY_IP="$(ip route get 1.1.1.1 2>/dev/null | sed -n 's/.* src \([0-9.]*\).*/\1/p' | head -1 || true)"
ALL_IPS="$(hostname -I 2>/dev/null || true)"
if [[ "${PRIMARY_IP}" != "${EXPECTED_PUBLIC_IP}" && " ${ALL_IPS} " != *" ${EXPECTED_PUBLIC_IP} "* ]]; then
  fail "当前主机 IP 不包含 ${EXPECTED_PUBLIC_IP}，PRIMARY_IP=${PRIMARY_IP:-unknown}"
fi

CURRENT_BRANCH="$(git -C "${PROJECT_DIR}" rev-parse --abbrev-ref HEAD)"
[[ "${CURRENT_BRANCH}" == "main" ]] || fail "当前分支是 ${CURRENT_BRANCH}，预期 main"

REMOTE_URL="$(git -C "${PROJECT_DIR}" remote get-url origin)"
if [[ "${REMOTE_URL}" != "${EXPECTED_GIT_REMOTE}" && "${REMOTE_URL}" != "${EXPECTED_GIT_SSH_REMOTE}" ]]; then
  fail "origin remote 不匹配预期仓库"
fi

git -C "${PROJECT_DIR}" diff --quiet || fail "工作区存在未提交的 tracked 修改"
git -C "${PROJECT_DIR}" diff --cached --quiet || fail "暂存区存在未提交修改"
systemctl list-unit-files "${SERVICE_NAME}" --no-legend | grep -q "^${SERVICE_NAME}[[:space:]]" \
  || fail "未找到 ${SERVICE_NAME}"
[[ -s "${CERT_DIR}/fullchain.pem" ]] || fail "证书不存在：${CERT_DIR}/fullchain.pem"
[[ -x /opt/certbot/bin/certbot ]] || fail "certbot 不存在或不可执行"

# PDFBox 2.x 不能正确子集化 CentOS 默认的 CFF-in-TTC 字体。
# 固定下载经校验的 TrueType 中文字体，避免 PDF/PNG 中文变成 ####。
if [[ ! -s "${CHINESE_FONT_FILE}" ]] \
    || ! printf '%s  %s\n' "${CHINESE_FONT_SHA256}" "${CHINESE_FONT_FILE}" | sha256sum -c - > /dev/null 2>&1; then
  CHINESE_FONT_TEMP="$(mktemp /tmp/ai-techskill-book-font.XXXXXX.ttc)"
  curl -fsSL --retry 3 --connect-timeout 10 --max-time 180 \
    "${CHINESE_FONT_URL}" -o "${CHINESE_FONT_TEMP}"
  printf '%s  %s\n' "${CHINESE_FONT_SHA256}" "${CHINESE_FONT_TEMP}" | sha256sum -c - \
    || fail "中文字体校验失败"
  install -d -o root -g root -m 755 "${CHINESE_FONT_DIR}"
  install -o root -g root -m 644 "${CHINESE_FONT_TEMP}" "${CHINESE_FONT_FILE}"
  rm -f "${CHINESE_FONT_TEMP}"
fi
[[ -s "${CHINESE_FONT_FILE}" ]] || fail "未找到中文字体：${CHINESE_FONT_FILE}"
[[ -s "${IPA_FONT_FILE}" ]] || fail "未找到 IPA 字体：${IPA_FONT_FILE}"

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
install -o root -g root -m 644 "${PROJECT_DIR}/deploy/nginx/ai-techskill-book.conf" "${NGINX_CONFIG}"
install -o root -g root -m 644 "${PROJECT_DIR}/deploy/systemd/certbot-renew-ai-techskill-book.service" /etc/systemd/system/certbot-renew-ai-techskill-book.service
install -o root -g root -m 644 "${PROJECT_DIR}/deploy/systemd/certbot-renew-ai-techskill-book.timer" /etc/systemd/system/certbot-renew-ai-techskill-book.timer
systemctl daemon-reload
systemctl enable --now certbot-renew-ai-techskill-book.timer
if ! nginx -t; then
  if [[ -f "${BACKUP_DIR}/ai-techskill-book.conf" ]]; then
    cp -a "${BACKUP_DIR}/ai-techskill-book.conf" "${NGINX_CONFIG}"
  fi
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
systemctl restart "${SERVICE_NAME}"
systemctl restart nginx.service

# 最多等待 30 秒，避免应用正常启动期间误报部署失败。
BACKEND_READY=0
for _ in {1..30}; do
  if curl -fs http://127.0.0.1:8080/actuator/health > /dev/null 2>&1; then
    BACKEND_READY=1
    break
  fi
  sleep 1
done
if [[ "${BACKEND_READY}" -ne 1 ]]; then
  systemctl status "${SERVICE_NAME}" --no-pager
  journalctl -u "${SERVICE_NAME}" -n 80 --no-pager
  exit 1
fi

curl -fsS http://127.0.0.1:8080/actuator/health
curl -fsS http://127.0.0.1:8080/api/ping
curl -fsSI --resolve "${EXPECTED_PUBLIC_IP}:443:127.0.0.1" "https://${EXPECTED_PUBLIC_IP}/"
systemctl is-active redis.service
systemctl is-active certbot-renew-ai-techskill-book.timer

echo "部署完成，备份目录：${BACKUP_DIR}"
