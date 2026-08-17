INSERT INTO `knowledge_tag` (`name`, `parent_id`, `level`, `sort_order`, `description`, `tag_path`)
SELECT 'Java 开发', 0, 1, 10, 'Java 服务端工程知识模块', '/'
WHERE NOT EXISTS (
    SELECT 1 FROM `knowledge_tag` WHERE `parent_id` = 0 AND `name` = 'Java 开发' AND `deleted` = 0
);

SET @java_module_id = (
    SELECT `id` FROM `knowledge_tag`
    WHERE `parent_id` = 0 AND `name` = 'Java 开发' AND `deleted` = 0
    ORDER BY `id` ASC LIMIT 1
);

UPDATE `knowledge_tag`
SET `tag_path` = CONCAT('/', `id`, '/')
WHERE `id` = @java_module_id;

INSERT INTO `knowledge_tag` (`name`, `parent_id`, `level`, `sort_order`, `description`, `tag_path`)
SELECT seed.`name`, @java_module_id, 2, seed.`sort_order`, seed.`description`, CONCAT('/', @java_module_id, '/')
FROM (
    SELECT 'Spring Boot' AS `name`, 10 AS `sort_order`, '应用开发与生产配置' AS `description`
    UNION ALL SELECT 'JVM 与性能', 20, '虚拟机、诊断与性能调优'
    UNION ALL SELECT '并发编程', 30, '线程、锁与异步编排'
) AS seed
WHERE @java_module_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `knowledge_tag` AS existing_tag
      WHERE existing_tag.`parent_id` = @java_module_id
        AND existing_tag.`name` = seed.`name`
        AND existing_tag.`deleted` = 0
  );

UPDATE `knowledge_tag` AS child_tag
JOIN `knowledge_tag` AS parent_tag ON parent_tag.`id` = child_tag.`parent_id`
SET child_tag.`tag_path` = CONCAT(parent_tag.`tag_path`, child_tag.`id`, '/')
WHERE child_tag.`parent_id` = @java_module_id
  AND child_tag.`deleted` = 0;

SET @spring_boot_id = (
    SELECT `id` FROM `knowledge_tag`
    WHERE `parent_id` = @java_module_id AND `name` = 'Spring Boot' AND `deleted` = 0
    ORDER BY `id` ASC LIMIT 1
);

INSERT INTO `knowledge_tag` (`name`, `parent_id`, `level`, `sort_order`, `description`, `tag_path`)
SELECT seed.`name`, @spring_boot_id, 3, seed.`sort_order`, seed.`description`, CONCAT('/', @java_module_id, '/', @spring_boot_id, '/')
FROM (
    SELECT '自动配置' AS `name`, 10 AS `sort_order`, '自动装配与条件注解' AS `description`
    UNION ALL SELECT 'Web 开发', 20, 'REST 接口与参数校验'
    UNION ALL SELECT '数据访问', 30, '事务、JPA 与 MyBatis'
) AS seed
WHERE @spring_boot_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `knowledge_tag` AS existing_tag
      WHERE existing_tag.`parent_id` = @spring_boot_id
        AND existing_tag.`name` = seed.`name`
        AND existing_tag.`deleted` = 0
  );

UPDATE `knowledge_tag` AS child_tag
JOIN `knowledge_tag` AS parent_tag ON parent_tag.`id` = child_tag.`parent_id`
SET child_tag.`tag_path` = CONCAT(parent_tag.`tag_path`, child_tag.`id`, '/')
WHERE child_tag.`parent_id` = @spring_boot_id
  AND child_tag.`deleted` = 0;
