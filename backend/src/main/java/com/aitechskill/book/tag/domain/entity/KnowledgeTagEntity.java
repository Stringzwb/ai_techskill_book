package com.aitechskill.book.tag.domain.entity;

import com.aitechskill.book.common.domain.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 知识标签树实体。
 */
@Getter
@Setter
@TableName("knowledge_tag")
public class KnowledgeTagEntity extends BaseEntity {

    /** 标签主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标签名称。 */
    private String name;

    /** 父标签主键，0表示一级知识模块。 */
    private Long parentId;

    /** 标签层级，范围为1到3。 */
    private Integer level;

    /** 同级排序值。 */
    private Integer sortOrder;

    /** 标签说明。 */
    private String description;

    /** 从一级节点到当前节点的路径。 */
    private String tagPath;
}
