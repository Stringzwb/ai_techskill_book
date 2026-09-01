package com.aitechskill.book.english.domain.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * 生词本截图的 AI 结构化返回。
 *
 * @param templateType 模板版本
 * @param items 识别到的词汇
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TechEnglishVocabularyImportPayload(
        String templateType,
        List<Item> items) {

    /** 单个词汇识别结果。 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            Integer sourceImageIndex,
            String word,
            String partOfSpeech,
            String meaning,
            String britishPhonetic,
            String americanPhonetic,
            List<Example> examples) {
    }

    /** 词汇例句。 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Example(
            String englishText,
            String translationText) {
    }
}
