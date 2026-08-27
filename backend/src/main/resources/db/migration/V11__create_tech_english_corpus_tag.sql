CREATE TABLE IF NOT EXISTS `tech_english_corpus_tag` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '技术英语语料标签关联主键',
    `corpus_id` BIGINT NOT NULL COMMENT '技术英语语料主键',
    `tag_id` BIGINT NOT NULL COMMENT '知识标签主键',
    `createtime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updatetime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `createby` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人，0表示管理后台',
    `updateby` BIGINT NOT NULL DEFAULT 0 COMMENT '更新人，0表示管理后台',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常、1已删除',
    `reserved1` VARCHAR(255) NULL COMMENT '冗余字段1',
    `reserved2` VARCHAR(255) NULL COMMENT '冗余字段2',
    `reserved3` VARCHAR(255) NULL COMMENT '冗余字段3',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tech_english_corpus_tag` (`corpus_id`, `tag_id`),
    KEY `idx_tech_english_corpus_tag_tag` (`tag_id`, `deleted`, `corpus_id`),
    KEY `idx_tech_english_corpus_tag_corpus` (`corpus_id`, `deleted`, `tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='技术英语语料与知识标签关联';
