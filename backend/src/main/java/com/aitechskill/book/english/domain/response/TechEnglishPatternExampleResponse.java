package com.aitechskill.book.english.domain.response;

/**
 * 经典句式的扩展例句。
 *
 * @param englishText 英文例句
 * @param translationText 中文翻译
 */
public record TechEnglishPatternExampleResponse(
        String englishText,
        String translationText) {
}
