package com.aitechskill.book.english.domain.response;

/**
 * 经典句子中的重点词汇。
 *
 * @param word 词汇
 * @param partOfSpeech 词性
 * @param meaning 中文释义
 */
public record TechEnglishKeyVocabularyResponse(
        String word,
        String partOfSpeech,
        String meaning) {
}
