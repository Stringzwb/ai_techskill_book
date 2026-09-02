package com.aitechskill.book.english.domain.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * 经典句子截图的 AI 结构化返回。
 *
 * @param templateType 模板版本
 * @param items 识别到的句子
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TechEnglishSentenceImportPayload(
        String templateType,
        List<Item> items) {

    /** 单个经典句子识别结果。 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            Integer sourceImageIndex,
            String sentence,
            String translation,
            List<KeyVocabulary> keyVocabulary,
            String classicPattern,
            String patternExplanation,
            List<String> scenarioTags,
            List<PatternExample> patternExamples) {
    }

    /** 句子重点词汇。 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KeyVocabulary(
            String word,
            String partOfSpeech,
            String meaning) {
    }

    /** 经典句式扩展例句。 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PatternExample(
            String englishText,
            String translationText) {
    }
}
