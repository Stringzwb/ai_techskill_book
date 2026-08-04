#!/usr/bin/env bash

set -euo pipefail

# 默认在服务器上读取实际环境文件，也可通过第一个参数指定其他文件。
env_file="${1:-/opt/elasticsearch/.env}"
if [[ ! -r "$env_file" ]]; then
  echo "无法读取环境文件: $env_file" >&2
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "$env_file"
set +a

curl -fsS -u "elastic:${ELASTIC_PASSWORD}" \
  'http://127.0.0.1:9200/_cluster/health?pretty'
curl -fsS -o /dev/null -w 'kibana_http=%{http_code}\n' \
  'http://127.0.0.1:5601/login'
docker inspect --format '{{.Name}} {{.State.Status}} {{.State.Health.Status}}' \
  elasticsearch kibana
