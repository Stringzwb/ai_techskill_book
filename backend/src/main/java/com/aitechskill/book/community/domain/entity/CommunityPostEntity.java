package com.aitechskill.book.community.domain.entity;

import com.aitechskill.book.common.domain.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @TableName("knowledge_post")
public class CommunityPostEntity extends BaseEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long authorId;
    private String postType;
    private String title;
    private String markdownContent;
    private String linkUrl;
    private String linkDomain;
    private String linkComplianceStatus;
    private Integer commentCount;
    private LocalDateTime publishedAt;
}
