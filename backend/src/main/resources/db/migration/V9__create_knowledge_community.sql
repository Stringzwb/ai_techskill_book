CREATE TABLE IF NOT EXISTS `knowledge_post` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `author_id` BIGINT NOT NULL,
    `post_type` VARCHAR(16) NOT NULL COMMENT 'QUESTION、IMAGE、LINK、FILE、VOTE',
    `title` VARCHAR(160) NOT NULL,
    `markdown_content` LONGTEXT NULL,
    `link_url` VARCHAR(2048) NULL,
    `link_domain` VARCHAR(255) NULL,
    `link_compliance_status` VARCHAR(16) NULL,
    `comment_count` INT NOT NULL DEFAULT 0,
    `published_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `createtime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updatetime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createby` BIGINT NOT NULL DEFAULT 0, `updateby` BIGINT NOT NULL DEFAULT 0,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    `reserved1` VARCHAR(255) NULL, `reserved2` VARCHAR(255) NULL, `reserved3` VARCHAR(255) NULL,
    PRIMARY KEY (`id`),
    KEY `idx_knowledge_post_feed` (`deleted`, `published_at`, `id`),
    KEY `idx_knowledge_post_author` (`author_id`, `deleted`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识社区帖子';

CREATE TABLE IF NOT EXISTS `knowledge_post_tag` (
    `id` BIGINT NOT NULL AUTO_INCREMENT, `post_id` BIGINT NOT NULL, `tag_id` BIGINT NOT NULL,
    `createtime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, `updatetime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createby` BIGINT NOT NULL DEFAULT 0, `updateby` BIGINT NOT NULL DEFAULT 0, `deleted` TINYINT NOT NULL DEFAULT 0,
    `reserved1` VARCHAR(255) NULL, `reserved2` VARCHAR(255) NULL, `reserved3` VARCHAR(255) NULL,
    PRIMARY KEY (`id`), UNIQUE KEY `uk_knowledge_post_tag` (`post_id`, `tag_id`),
    KEY `idx_knowledge_post_tag_tag` (`tag_id`, `deleted`, `post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='社区帖子标签关联';

CREATE TABLE IF NOT EXISTS `knowledge_post_attachment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT, `post_id` BIGINT NOT NULL, `attachment_type` VARCHAR(12) NOT NULL,
    `object_key` VARCHAR(500) NOT NULL, `original_name` VARCHAR(255) NOT NULL, `content_type` VARCHAR(120) NOT NULL,
    `extension` VARCHAR(12) NOT NULL, `size_bytes` BIGINT NOT NULL, `sort_order` INT NOT NULL DEFAULT 0,
    `createtime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, `updatetime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createby` BIGINT NOT NULL DEFAULT 0, `updateby` BIGINT NOT NULL DEFAULT 0, `deleted` TINYINT NOT NULL DEFAULT 0,
    `reserved1` VARCHAR(255) NULL, `reserved2` VARCHAR(255) NULL, `reserved3` VARCHAR(255) NULL,
    PRIMARY KEY (`id`), KEY `idx_knowledge_post_attachment_post` (`post_id`, `deleted`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='社区帖子附件';

CREATE TABLE IF NOT EXISTS `knowledge_post_vote` (
    `post_id` BIGINT NOT NULL, `question` VARCHAR(300) NOT NULL, `allow_multiple` TINYINT NOT NULL DEFAULT 0,
    `anonymous` TINYINT NOT NULL DEFAULT 0, `vote_count` INT NOT NULL DEFAULT 0,
    `createtime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, `updatetime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='社区投票';

CREATE TABLE IF NOT EXISTS `knowledge_post_vote_option` (
    `id` BIGINT NOT NULL AUTO_INCREMENT, `post_id` BIGINT NOT NULL, `option_text` VARCHAR(160) NOT NULL,
    `sort_order` INT NOT NULL DEFAULT 0, `vote_count` INT NOT NULL DEFAULT 0, PRIMARY KEY (`id`),
    KEY `idx_knowledge_post_vote_option_post` (`post_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='社区投票选项';

CREATE TABLE IF NOT EXISTS `knowledge_post_vote_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT, `post_id` BIGINT NOT NULL, `user_id` BIGINT NOT NULL,
    `createtime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (`id`),
    UNIQUE KEY `uk_knowledge_post_vote_user` (`post_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='社区用户投票记录';

CREATE TABLE IF NOT EXISTS `knowledge_post_vote_selection` (
    `id` BIGINT NOT NULL AUTO_INCREMENT, `vote_record_id` BIGINT NOT NULL, `option_id` BIGINT NOT NULL, PRIMARY KEY (`id`),
    UNIQUE KEY `uk_knowledge_post_vote_selection` (`vote_record_id`, `option_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='社区投票选择';

CREATE TABLE IF NOT EXISTS `knowledge_post_comment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT, `post_id` BIGINT NOT NULL, `parent_id` BIGINT NULL, `author_id` BIGINT NOT NULL,
    `markdown_content` TEXT NOT NULL,
    `createtime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, `updatetime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createby` BIGINT NOT NULL DEFAULT 0, `updateby` BIGINT NOT NULL DEFAULT 0, `deleted` TINYINT NOT NULL DEFAULT 0,
    `reserved1` VARCHAR(255) NULL, `reserved2` VARCHAR(255) NULL, `reserved3` VARCHAR(255) NULL,
    PRIMARY KEY (`id`), KEY `idx_knowledge_post_comment_post` (`post_id`, `deleted`, `createtime`, `id`),
    KEY `idx_knowledge_post_comment_parent` (`parent_id`, `deleted`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='社区帖子多层评论';
