package com.aitechskill.book.common.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 数据库实体审计基类。
 */
@Getter
@Setter
public abstract class BaseEntity {

    /** 创建时间。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createtime;

    /** 更新时间。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatetime;

    /** 创建人。 */
    @TableField(fill = FieldFill.INSERT)
    private Long createby;

    /** 更新人。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateby;

    /** 逻辑删除标记。 */
    @TableLogic
    private Integer deleted;

    /** 冗余字段1。 */
    private String reserved1;

    /** 冗余字段2。 */
    private String reserved2;

    /** 冗余字段3。 */
    private String reserved3;
}
