package com.aitechskill.book.english.domain.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * AI 根据截图内容自动分类后的统一返回。
 *
 * @param templateType 自动分类模板版本
 * @param vocabulary 按默认生词配置输出的内容
 * @param sentences 按默认句子配置输出的内容
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TechEnglishAutoImportPayload(
        String templateType,
        TechEnglishVocabularyImportPayload vocabulary,
        TechEnglishSentenceImportPayload sentences) {
}
