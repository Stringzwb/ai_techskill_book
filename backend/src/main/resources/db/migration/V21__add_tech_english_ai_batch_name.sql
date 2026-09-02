ALTER TABLE `tech_english_ai_recognition_record`
    ADD COLUMN `batch_name` VARCHAR(120) NULL COMMENT '用户填写的识图批次名称，仅用于识图记录' AFTER `session_uuid`;
