#!/usr/bin/env bash

set -euo pipefail

# 仅在 test1 不存在时创建索引，防止覆盖现有测试数据。
base_url="${ES_URL:-http://127.0.0.1:9200}"
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
data_dir="${script_dir}/../test-data/test1"
: "${ELASTIC_PASSWORD:?请先设置 ELASTIC_PASSWORD}"

if curl -fsS -u "elastic:${ELASTIC_PASSWORD}" -I \
  "${base_url}/test1" >/dev/null; then
  echo "test1 已存在，未执行恢复" >&2
  exit 1
fi

curl -fsS -u "elastic:${ELASTIC_PASSWORD}" \
  -H 'Content-Type: application/json' \
  -X PUT "${base_url}/test1" \
  --data-binary "@${data_dir}/index.json"
curl -fsS -u "elastic:${ELASTIC_PASSWORD}" \
  -H 'Content-Type: application/x-ndjson' \
  -X POST "${base_url}/_bulk?refresh=wait_for" \
  --data-binary "@${data_dir}/documents.ndjson"
curl -fsS -u "elastic:${ELASTIC_PASSWORD}" \
  "${base_url}/test1/_count?pretty"
