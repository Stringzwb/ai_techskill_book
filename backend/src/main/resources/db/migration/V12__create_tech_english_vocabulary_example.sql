CREATE TABLE IF NOT EXISTS `tech_english_vocabulary_example` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '词汇例句主键',
    `vocabulary_corpus_id` BIGINT NOT NULL COMMENT '词汇语料ID',
    `sentence_corpus_id` BIGINT NULL COMMENT '自动同步生成的句子语料ID',
    `english_text` TEXT NOT NULL COMMENT '英文例句',
    `translation_text` VARCHAR(1000) NULL COMMENT '例句释义',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
    `createtime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updatetime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `createby` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人',
    `updateby` BIGINT NOT NULL DEFAULT 0 COMMENT '更新人',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常、1已删除',
    `reserved1` VARCHAR(255) NULL COMMENT '冗余字段1',
    `reserved2` VARCHAR(255) NULL COMMENT '冗余字段2',
    `reserved3` VARCHAR(255) NULL COMMENT '冗余字段3',
    PRIMARY KEY (`id`),
    KEY `idx_tech_english_vocab_example_vocab` (`vocabulary_corpus_id`, `deleted`, `sort_order`, `id`),
    KEY `idx_tech_english_vocab_example_sentence` (`sentence_corpus_id`, `deleted`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='技术英语词汇例句';
