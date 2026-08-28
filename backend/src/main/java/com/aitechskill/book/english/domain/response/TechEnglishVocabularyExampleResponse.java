package com.aitechskill.book.english.domain.response;

/**
 * 技术英语词汇例句。
 *
 * @param id 例句主键
 * @param sentenceCorpusId 自动同步生成的句子语料ID
 * @param englishText 英文例句
 * @param translationText 例句释义
 */
public record TechEnglishVocabularyExampleResponse(
        long id,
        Long sentenceCorpusId,
        String englishText,
        String translationText) {
}
