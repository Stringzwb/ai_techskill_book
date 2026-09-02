CREATE TABLE IF NOT EXISTS `tech_english_sentence_pattern` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '句式框架主键',
    `pattern_text` VARCHAR(500) NOT NULL COMMENT '用于展示的英文可复用句式框架',
    `normalized_pattern` VARCHAR(500) NOT NULL COMMENT '小写并折叠空白后的去重键',
    `pattern_explanation` VARCHAR(1000) NULL COMMENT '句式框架学习解析',
    `createtime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updatetime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `createby` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人',
    `updateby` BIGINT NOT NULL DEFAULT 0 COMMENT '更新人',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常、1已删除',
    `reserved1` VARCHAR(255) NULL COMMENT '冗余字段1',
    `reserved2` VARCHAR(255) NULL COMMENT '冗余字段2',
    `reserved3` VARCHAR(255) NULL COMMENT '冗余字段3',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tech_english_sentence_pattern_normalized_deleted` (`normalized_pattern`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='技术英语可复用句式框架';

CREATE TABLE IF NOT EXISTS `tech_english_sentence_pattern_corpus` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '句式框架与语料关联主键',
    `sentence_pattern_id` BIGINT NOT NULL COMMENT '句式框架ID',
    `corpus_id` BIGINT NOT NULL COMMENT '句子语料ID',
    `createtime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updatetime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `createby` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人',
    `updateby` BIGINT NOT NULL DEFAULT 0 COMMENT '更新人',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常、1已删除',
    `reserved1` VARCHAR(255) NULL COMMENT '冗余字段1',
    `reserved2` VARCHAR(255) NULL COMMENT '冗余字段2',
    `reserved3` VARCHAR(255) NULL COMMENT '冗余字段3',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tech_english_sentence_pattern_corpus` (`sentence_pattern_id`, `corpus_id`),
    KEY `idx_tech_english_sentence_pattern_corpus_corpus` (`corpus_id`, `deleted`, `id`),
    CONSTRAINT `fk_tech_english_sentence_pattern_corpus_pattern`
        FOREIGN KEY (`sentence_pattern_id`) REFERENCES `tech_english_sentence_pattern` (`id`),
    CONSTRAINT `fk_tech_english_sentence_pattern_corpus_corpus`
        FOREIGN KEY (`corpus_id`) REFERENCES `tech_english_corpus` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='技术英语句式框架与句子语料关联';

INSERT INTO tech_english_sentence_pattern
    (pattern_text, normalized_pattern, pattern_explanation, createby, updateby, deleted)
SELECT MIN(source.sentence_pattern),
       source.normalized_pattern,
       MAX(source.sentence_pattern_explanation),
       0,
       0,
       0
FROM (
    SELECT sentence_pattern,
           sentence_pattern_explanation,
           LOWER(TRIM(REGEXP_REPLACE(sentence_pattern, '[[:space:]]+', ' '))) AS normalized_pattern
    FROM tech_english_corpus
    WHERE corpus_type = 'SENTENCE'
      AND deleted = 0
      AND sentence_pattern IS NOT NULL
      AND TRIM(sentence_pattern) <> ''
) AS source
GROUP BY source.normalized_pattern
ON DUPLICATE KEY UPDATE
    pattern_explanation = COALESCE(NULLIF(pattern_explanation, ''), VALUES(pattern_explanation)),
    deleted = 0,
    updatetime = CURRENT_TIMESTAMP;

INSERT INTO tech_english_sentence_pattern_corpus
    (sentence_pattern_id, corpus_id, createby, updateby, deleted)
SELECT pattern.id, corpus.id, corpus.createby, corpus.updateby, 0
FROM tech_english_corpus AS corpus
INNER JOIN tech_english_sentence_pattern AS pattern
    ON pattern.normalized_pattern = LOWER(TRIM(REGEXP_REPLACE(corpus.sentence_pattern, '[[:space:]]+', ' ')))
   AND pattern.deleted = 0
WHERE corpus.corpus_type = 'SENTENCE'
  AND corpus.deleted = 0
  AND corpus.sentence_pattern IS NOT NULL
  AND TRIM(corpus.sentence_pattern) <> ''
ON DUPLICATE KEY UPDATE
    deleted = 0,
    updatetime = CURRENT_TIMESTAMP;
