package com.aitechskill.book.document.domain.entity;

import com.aitechskill.book.common.domain.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * Markdown 知识文档实体。
 */
@Getter
@Setter
@TableName("knowledge_document")
public class KnowledgeDocumentEntity extends BaseEntity {

    /** 文档主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 文档稳定归属标识。 */
    private String documentUuid;

    /** 文档标题。 */
    private String title;

    /** 文档摘要。 */
    private String summary;

    /** 发布状态。 */
    private String status;

    /** Markdown 正文私有对象键。 */
    private String markdownObjectKey;

    /** Markdown 正文字节数。 */
    private Long markdownSize;

    /** 最近发布时间。 */
    private LocalDateTime publishedAt;
}
