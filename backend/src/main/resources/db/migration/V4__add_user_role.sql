ALTER TABLE `sys_user`
    ADD COLUMN `user_role` VARCHAR(32) NOT NULL DEFAULT 'USER' COMMENT '用户角色：USER普通用户、SUPER_ADMIN超级管理员' AFTER `member_level`;
