ALTER TABLE `tech_english_corpus`
    ADD COLUMN `scenario_tags` TEXT NULL COMMENT '固定场景标签 JSON' AFTER `scenario`;

UPDATE `tech_english_corpus`
SET `corpus_type` = 'PHRASE',
    `title` = COALESCE(NULLIF(`title`, ''), NULLIF(`image_alt`, ''), '短语语料'),
    `english_text` = COALESCE(NULLIF(`english_text`, ''), NULLIF(`image_alt`, ''), `title`, '短语语料')
WHERE `corpus_type` = 'IMAGE'
  AND `deleted` = 0;
