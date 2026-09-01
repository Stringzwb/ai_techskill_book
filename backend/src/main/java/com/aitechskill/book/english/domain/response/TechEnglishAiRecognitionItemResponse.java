package com.aitechskill.book.english.domain.response;

import java.util.List;

/**
 * 等待用户确认的单条截图识别结果。
 *
 * @param itemKey 识图语料稳定标识
 * @param sourceImageIndex 来源截图序号
 * @param corpusType AI 自动判定的语料类型
 * @param englishText 生词或经典句子
 * @param partOfSpeech 词性
 * @param translationText 中文释义或翻译
 * @param britishPhonetic 英式音标
 * @param americanPhonetic 美式音标
 * @param sentencePattern 经典句式
 * @param sentencePatternExplanation 经典句式解析
 * @param keyVocabulary 重点词汇
 * @param examples 生词或经典句式例句
 */
public record TechEnglishAiRecognitionItemResponse(
        String itemKey,
        int sourceImageIndex,
        String corpusType,
        String englishText,
        String partOfSpeech,
        String translationText,
        String britishPhonetic,
        String americanPhonetic,
        String sentencePattern,
        String sentencePatternExplanation,
        List<TechEnglishKeyVocabularyResponse> keyVocabulary,
        List<TechEnglishPatternExampleResponse> examples) {
}
