package com.aitechskill.book.community.domain.entity;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter @TableName("knowledge_post_vote") public class CommunityVoteEntity { @TableId private Long postId; private String question; private Boolean allowMultiple; private Boolean anonymous; private Integer voteCount; }
