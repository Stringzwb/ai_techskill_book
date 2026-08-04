ALTER TABLE `sys_user`
    ADD COLUMN `avatar_object_key` VARCHAR(500) NULL COMMENT '头像对象存储键，不包含存储桶和访问凭据' AFTER `avatar_url`,
    MODIFY COLUMN `avatar_url` VARCHAR(500) NOT NULL DEFAULT '/default-avatar.svg' COMMENT '头像受控访问地址，默认使用站内头像';
