package com.aitechskill.book.english.domain.entity;

import com.aitechskill.book.common.domain.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 技术英语语料实体。
 */
@Getter
@Setter
@TableName("tech_english_corpus")
public class TechEnglishCorpusEntity extends BaseEntity {

    /** 语料主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 语料稳定标识。 */
    private String corpusUuid;

    /** 语料类型。 */
    private String corpusType;

    /** 语料标题。 */
    private String title;

    /** 英文词汇、句子、图片说明或文章摘录。 */
    private String englishText;

    /** 音标或发音提示。 */
    private String phonetic;

    /** 词性。 */
    private String partOfSpeech;

    /** 英式音标。 */
    private String britishPhonetic;

    /** 美式音标。 */
    private String americanPhonetic;

    /** 人工说明。 */
    private String explanation;

    /** 英语文章 Markdown 正文。 */
    private String articleMarkdown;

    /** 图片访问地址。 */
    private String imageUrl;

    /** 图片对象键。 */
    private String imageObjectKey;

    /** 图片替代文本。 */
    private String imageAlt;

    /** 来源名称。 */
    private String sourceName;

    /** 来源链接。 */
    private String sourceUrl;

    /** 技术场景。 */
    private String scenario;

    /** 固定场景标签 JSON。 */
    private String scenarioTags;

    /** 难度。 */
    private String difficulty;

    /** 轻量标签。 */
    private String tags;

    /** 翻译文本。 */
    private String translationText;

    /** 经典句式。 */
    private String sentencePattern;

    /** 经典句式解析。 */
    private String sentencePatternExplanation;

    /** 句子重点词汇 JSON。 */
    private String keyVocabularyJson;

    /** 经典句式例句 JSON。 */
    private String patternExamplesJson;

    /** AI 截图导入批次标识。 */
    private String importBatchUuid;

    /** 来源截图序号。 */
    private Integer sourceImageIndex;

    /** 翻译状态。 */
    private String translationStatus;

    /** AI处理状态。 */
    private String aiReviewStatus;

    /** AI处理备注。 */
    private String aiReviewNotes;

    /** 搜索索引状态。 */
    private String indexStatus;

    /** 内容版本。 */
    private Long contentVersion;

    /** 发布状态。 */
    private String status;

    /** 最近发布时间。 */
    private LocalDateTime publishedAt;
}
