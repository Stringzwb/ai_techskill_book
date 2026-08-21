package com.aitechskill.book.community.domain.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter @TableName("knowledge_post_vote_option") public class CommunityVoteOptionEntity { @TableId(type = IdType.AUTO) private Long id; private Long postId; private String optionText; private Integer sortOrder; private Integer voteCount; }
