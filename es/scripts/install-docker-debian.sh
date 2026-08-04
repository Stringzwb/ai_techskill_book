#!/usr/bin/env bash

set -euo pipefail

# 脚本仅适用于 Debian 12，安装 Docker 官方软件源和 Compose 插件。
if [[ "$(. /etc/os-release && printf '%s' "${ID}:${VERSION_ID}")" != "debian:12" ]]; then
  echo "仅支持 Debian 12" >&2
  exit 1
fi

apt-get update
apt-get install -y ca-certificates curl
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/debian/gpg \
  -o /etc/apt/keyrings/docker.asc
chmod a+r /etc/apt/keyrings/docker.asc
install -m 0644 "$(dirname "$0")/../config/docker.list" \
  /etc/apt/sources.list.d/docker.list

apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io \
  docker-buildx-plugin docker-compose-plugin
systemctl enable --now docker

docker --version
docker compose version
