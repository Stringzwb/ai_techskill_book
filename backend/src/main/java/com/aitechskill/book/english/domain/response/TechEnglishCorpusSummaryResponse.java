package com.aitechskill.book.english.domain.response;

import com.aitechskill.book.document.domain.response.DocumentTagResponse;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 主平台技术英语语料列表项。
 *
 * @param id 语料主键
 * @param corpusType 语料类型
 * @param title 语料标题
 * @param englishText 英文文本摘要
 * @param phonetic 音标或发音提示
 * @param partOfSpeech 词性
 * @param britishPhonetic 英式音标
 * @param americanPhonetic 美式音标
 * @param explanation 人工说明
 * @param imageUrl 图片访问地址
 * @param imageAlt 图片替代文本
 * @param scenario 技术场景
 * @param difficulty 难度
 * @param tags 轻量标签
 * @param translationText 翻译文本
 * @param publishedAt 发布时间
 * @param knowledgeTags 关联知识标签
 */
public record TechEnglishCorpusSummaryResponse(
        long id,
        String corpusType,
        String title,
        String englishText,
        String phonetic,
        String partOfSpeech,
        String britishPhonetic,
        String americanPhonetic,
        String explanation,
        String imageUrl,
        String imageAlt,
        String scenario,
        String difficulty,
        String tags,
        String translationText,
        LocalDateTime publishedAt,
        List<DocumentTagResponse> knowledgeTags) {
}
