package com.aitechskill.book.english.domain.entity;

import com.aitechskill.book.common.domain.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 可复用的技术英语句式框架。
 */
@Getter
@Setter
@TableName("tech_english_sentence_pattern")
public class TechEnglishSentencePatternEntity extends BaseEntity {

    /** 句式框架主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 保留展示格式的英文句式框架。 */
    private String patternText;

    /** 小写、折叠空白后的稳定去重键。 */
    private String normalizedPattern;

    /** 句式框架解析。 */
    private String patternExplanation;
}
