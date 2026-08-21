package com.aitechskill.book.community.domain.entity;
import com.aitechskill.book.common.domain.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter @TableName("knowledge_post_tag") public class CommunityPostTagEntity extends BaseEntity { @TableId(type = IdType.AUTO) private Long id; private Long postId; private Long tagId; }
