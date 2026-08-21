ALTER TABLE `knowledge_document`
    ADD COLUMN `markdown_content` LONGTEXT NULL COMMENT 'Markdown正文，新的正文唯一来源' AFTER `summary`,
    MODIFY COLUMN `markdown_object_key` VARCHAR(500) NULL COMMENT '历史Markdown正文对象键，仅用于迁移和回退审计',
    ADD COLUMN `content_version` BIGINT NOT NULL DEFAULT 1 COMMENT '正文版本号，每次正文保存递增' AFTER `markdown_size`,
    ADD COLUMN `index_status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '索引状态：PENDING处理中、SUCCEEDED成功、FAILED失败' AFTER `content_version`,
    ADD COLUMN `index_error` VARCHAR(500) NULL COMMENT '最近一次索引错误摘要' AFTER `index_status`,
    ADD COLUMN `index_updated_at` DATETIME NULL COMMENT '最近一次索引处理时间' AFTER `index_error`;

CREATE TABLE IF NOT EXISTS `knowledge_document_index_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文档索引任务主键',
    `document_id` BIGINT NOT NULL COMMENT '知识文档主键',
    `content_version` BIGINT NOT NULL COMMENT '任务对应的正文版本',
    `task_type` VARCHAR(16) NOT NULL COMMENT '任务类型：UPSERT写入、DELETE删除',
    `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态：PENDING、DISPATCHED、PROCESSING、SUCCEEDED、FAILED',
    `retry_count` INT NOT NULL DEFAULT 0 COMMENT '已重试次数',
    `available_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '允许再次投递时间',
    `last_error` VARCHAR(500) NULL COMMENT '最近一次失败摘要',
    `createtime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updatetime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `createby` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人，0表示系统',
    `updateby` BIGINT NOT NULL DEFAULT 0 COMMENT '更新人，0表示系统',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常、1已删除',
    `reserved1` VARCHAR(255) NULL COMMENT '冗余字段1',
    `reserved2` VARCHAR(255) NULL COMMENT '冗余字段2',
    `reserved3` VARCHAR(255) NULL COMMENT '冗余字段3',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_knowledge_document_index_task` (`document_id`, `content_version`, `task_type`),
    KEY `idx_knowledge_document_index_task_dispatch` (`status`, `available_at`, `id`),
    KEY `idx_knowledge_document_index_task_document` (`document_id`, `content_version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识文档异步索引任务';
