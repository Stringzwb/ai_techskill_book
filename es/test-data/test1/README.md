# test1 AI 测试索引

该归档包含 2026-08-04 从数据服务器导出的 `test1` 索引定义和 32 条 AI 主题测试文档。

| 文件 | 用途 |
|---|---|
| `index.json` | 分片、中英文分析器和动态字段模板 |
| `documents.ndjson` | Bulk API 格式的 32 条文档，使用 `create` 操作防止覆盖 |

恢复时使用：

```bash
export ELASTIC_PASSWORD='目标 Elasticsearch 的 elastic 密码'
./es/scripts/restore-test1.sh
```

脚本发现 `test1` 已存在时会停止，不会删除、重建或覆盖现有索引。
