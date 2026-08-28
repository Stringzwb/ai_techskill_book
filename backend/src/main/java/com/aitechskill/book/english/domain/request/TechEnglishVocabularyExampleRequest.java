package com.aitechskill.book.english.domain.request;

import jakarta.validation.constraints.Size;

/**
 * 主平台词汇语料例句轻收录请求。
 */
public record TechEnglishVocabularyExampleRequest(
        @Size(max = 2000) String englishText,
        @Size(max = 1000) String translationText) {
}
