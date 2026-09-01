ALTER TABLE `tech_english_ai_recognition_record`
    ADD COLUMN `raw_result_json` LONGTEXT NULL COMMENT 'AI 原始返回内容JSON或文本' AFTER `result_json`,
    ADD COLUMN `source_images_json` JSON NULL COMMENT '识别批次已保存的原图对象元数据JSON' AFTER `raw_result_json`;
