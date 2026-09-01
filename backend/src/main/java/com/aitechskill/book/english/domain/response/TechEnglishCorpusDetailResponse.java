package com.aitechskill.book.english.domain.response;

import com.aitechskill.book.document.domain.response.DocumentTagResponse;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 主平台技术英语语料详情。
 *
 * @param id 语料主键
 * @param corpusType 语料类型
 * @param title 语料标题
 * @param englishText 英文文本
 * @param phonetic 音标或发音提示
 * @param partOfSpeech 词性
 * @param britishPhonetic 英式音标
 * @param americanPhonetic 美式音标
 * @param explanation 人工说明
 * @param articleMarkdown 英语文章正文
 * @param imageUrl 图片访问地址
 * @param imageAlt 图片替代文本
 * @param importBatchUuid AI 截图导入批次标识
 * @param sourceName 来源名称
 * @param sourceUrl 来源链接
 * @param scenario 技术场景
 * @param difficulty 难度
 * @param tags 轻量标签
 * @param translationText 翻译文本
 * @param sentencePattern 经典句式
 * @param sentencePatternExplanation 经典句式解析
 * @param keyVocabulary 重点词汇
 * @param patternExamples 句式例句
 * @param publishedAt 发布时间
 * @param updatedAt 更新时间
 * @param knowledgeTags 关联知识标签
 * @param vocabularyExamples 词汇例句
 */
public record TechEnglishCorpusDetailResponse(
        long id,
        String corpusType,
        String title,
        String englishText,
        String phonetic,
        String partOfSpeech,
        String britishPhonetic,
        String americanPhonetic,
        String explanation,
        String articleMarkdown,
        String imageUrl,
        String imageAlt,
        String importBatchUuid,
        String sourceName,
        String sourceUrl,
        String scenario,
        String difficulty,
        String tags,
        String translationText,
        String sentencePattern,
        String sentencePatternExplanation,
        List<TechEnglishKeyVocabularyResponse> keyVocabulary,
        List<TechEnglishPatternExampleResponse> patternExamples,
        LocalDateTime publishedAt,
        LocalDateTime updatedAt,
        List<DocumentTagResponse> knowledgeTags,
        List<TechEnglishVocabularyExampleResponse> vocabularyExamples) {
}
