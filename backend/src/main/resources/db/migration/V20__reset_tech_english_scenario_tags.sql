/* 场景标签改为生活/工作大类，历史技术模块标签不再符合新目录。 */
UPDATE tech_english_corpus
SET scenario_tags = '[]'
WHERE deleted = 0
  AND scenario_tags IS NOT NULL
  AND TRIM(scenario_tags) <> ''
  AND scenario_tags <> '[]';
