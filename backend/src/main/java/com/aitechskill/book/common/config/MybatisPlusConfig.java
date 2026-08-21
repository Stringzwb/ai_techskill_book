package com.aitechskill.book.common.config;

import com.aitechskill.book.auth.utils.UserContextHolder;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import java.time.LocalDateTime;
import org.apache.ibatis.reflection.MetaObject;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 与自动审计配置。
 */
@Configuration
@MapperScan({
        "com.aitechskill.book.user.mapper",
        "com.aitechskill.book.tag.mapper",
        "com.aitechskill.book.document.mapper",
        "com.aitechskill.book.community.mapper"
})
public class MybatisPlusConfig {

    /**
     * 创建实体审计字段填充器。
     *
     * @return 审计字段填充器
     */
    @Bean
    MetaObjectHandler auditMetaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                LocalDateTime now = LocalDateTime.now();
                Long operatorId = UserContextHolder.currentUserId().orElse(0L);
                strictInsertFill(metaObject, "createtime", LocalDateTime.class, now);
                strictInsertFill(metaObject, "updatetime", LocalDateTime.class, now);
                strictInsertFill(metaObject, "createby", Long.class, operatorId);
                strictInsertFill(metaObject, "updateby", Long.class, operatorId);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                Long operatorId = UserContextHolder.currentUserId().orElse(0L);
                strictUpdateFill(metaObject, "updatetime", LocalDateTime.class, LocalDateTime.now());
                strictUpdateFill(metaObject, "updateby", Long.class, operatorId);
            }
        };
    }
}
