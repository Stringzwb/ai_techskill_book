ALTER TABLE `tech_english_corpus`
    ADD COLUMN `part_of_speech` VARCHAR(64) NULL COMMENT '词性，例如 noun、verb、adj.' AFTER `phonetic`,
    ADD COLUMN `british_phonetic` VARCHAR(120) NULL COMMENT '英式音标' AFTER `part_of_speech`,
    ADD COLUMN `american_phonetic` VARCHAR(120) NULL COMMENT '美式音标' AFTER `british_phonetic`,
    ADD COLUMN `sentence_pattern` VARCHAR(500) NULL COMMENT '经典句式或句型模板' AFTER `translation_text`,
    ADD COLUMN `sentence_pattern_explanation` VARCHAR(1000) NULL COMMENT '经典句式解析' AFTER `sentence_pattern`,
    ADD COLUMN `key_vocabulary_json` JSON NULL COMMENT '句子重点词汇JSON' AFTER `sentence_pattern_explanation`,
    ADD COLUMN `pattern_examples_json` JSON NULL COMMENT '经典句式例句JSON' AFTER `key_vocabulary_json`,
    ADD COLUMN `import_batch_uuid` CHAR(36) NULL COMMENT 'AI截图导入批次标识' AFTER `pattern_examples_json`,
    ADD COLUMN `source_image_index` INT NULL COMMENT '导入批次中的来源截图序号，从1开始' AFTER `import_batch_uuid`,
    ADD KEY `idx_tech_english_corpus_import_batch` (`import_batch_uuid`, `deleted`, `id`);
