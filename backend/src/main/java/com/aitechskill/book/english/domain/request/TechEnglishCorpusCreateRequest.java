package com.aitechskill.book.english.domain.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 主平台技术英语语料轻收录请求。
 */
public record TechEnglishCorpusCreateRequest(
        @NotNull @Size(max = 20) String corpusType,
        @Size(max = 160) String title,
        @Size(max = 20000) String englishText,
        @Size(max = 120) String phonetic,
        @Size(max = 1000) String explanation,
        @Size(max = 50000) String articleMarkdown,
        @Size(max = 300) String imageAlt,
        @Size(max = 120) String sourceName,
        @Size(max = 2048) String sourceUrl,
        @Size(max = 80) String scenario,
        @Size(max = 16) String difficulty,
        @Size(max = 5000) String translationText,
        @NotNull @NotEmpty @Size(max = 20) List<@Positive Long> tagIds,
        List<TechEnglishVocabularyExampleRequest> vocabularyExamples,
        Boolean syncExamplesToSentences) {
}
