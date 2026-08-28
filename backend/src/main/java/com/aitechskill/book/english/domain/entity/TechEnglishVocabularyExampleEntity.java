package com.aitechskill.book.english.domain.entity;

import com.aitechskill.book.common.domain.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 技术英语词汇例句。
 */
@Getter
@Setter
@TableName("tech_english_vocabulary_example")
public class TechEnglishVocabularyExampleEntity extends BaseEntity {

    /** 例句主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 词汇语料ID。 */
    private Long vocabularyCorpusId;

    /** 自动同步生成的句子语料ID。 */
    private Long sentenceCorpusId;

    /** 英文例句。 */
    private String englishText;

    /** 例句释义。 */
    private String translationText;

    /** 排序。 */
    private Integer sortOrder;
}
