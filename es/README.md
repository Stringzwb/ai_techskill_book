# 数据服务器与 Elastic Stack 资料

本目录归档数据服务器 `38.95.75.32` 上的 Docker、Elasticsearch、Kibana 和测试索引资料。归档基准时间为 2026-08-04。

## 目录结构

```text
es/
├── compose.yaml                         服务器当前使用的 Compose 配置
├── .env.example                        不含真实凭据的环境变量模板
├── backups/                            历史 Compose 配置备份
├── config/                             Docker 软件源和 Elasticsearch 内核参数
├── docs/                               服务器现状、运维和部署记录
├── scripts/                            安装、检查和测试数据恢复脚本
├── test-data/test1/                    test1 索引定义和 32 条 AI 测试文档
└── secrets/                            真实凭据和本地接手信息，Git 忽略
```

## 当前服务

| 服务 | 版本 | 公网地址 | 端口映射 | 数据目录 |
|---|---|---|---|---|
| Elasticsearch | `8.19.0` | `http://38.95.75.32:9200` | `9200:9200` | `/srv/elasticsearch/data` |
| Kibana | `8.19.0` | `http://38.95.75.32:5601` | `5601:5601` | `/srv/kibana/data` |

Elasticsearch 为单节点集群并启用身份认证。Kibana 使用专用 service account token 连接 Elasticsearch，界面语言为简体中文。两个容器的重启策略均为 `unless-stopped`。

## 凭据与安全

真实 SSH 密码、Elasticsearch 密码、Kibana token 和持久化加密密钥仅保存在 `es/secrets/`，该目录已被 Git 忽略。可提交的 `.env.example` 只包含占位值。

当前 `9200` 和 `5601` 按业务要求通过公网 HTTP 同端口对外提供服务，尚未启用 TLS。接入敏感数据前应增加 HTTPS 或来源 IP 白名单。`9300` 只在容器网络中使用，不对外发布。

## 快速运维

```bash
cd /opt/elasticsearch
docker compose ps
docker compose up -d
docker compose logs --tail=100 elasticsearch kibana
```

完整运维命令见 `docs/运维说明.md`，服务器清单见 `docs/服务器现状.md`，`test1` 恢复方式见 `test-data/test1/README.md`。
