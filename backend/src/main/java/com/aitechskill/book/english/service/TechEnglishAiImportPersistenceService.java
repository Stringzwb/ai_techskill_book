package com.aitechskill.book.english.service;

import com.aitechskill.book.common.exception.BusinessException;
import com.aitechskill.book.english.domain.ai.TechEnglishAutoImportPayload;
import com.aitechskill.book.english.domain.ai.TechEnglishSentenceImportPayload;
import com.aitechskill.book.english.domain.ai.TechEnglishVocabularyImportPayload;
import com.aitechskill.book.english.domain.entity.TechEnglishCorpusEntity;
import com.aitechskill.book.english.domain.entity.TechEnglishVocabularyExampleEntity;
import com.aitechskill.book.english.domain.response.TechEnglishCorpusDetailResponse;
import com.aitechskill.book.english.domain.response.TechEnglishKeyVocabularyResponse;
import com.aitechskill.book.english.domain.response.TechEnglishPatternExampleResponse;
import com.aitechskill.book.english.mapper.TechEnglishCorpusMapper;
import com.aitechskill.book.english.mapper.TechEnglishVocabularyExampleMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.aitechskill.book.storage.domain.StoredObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 在单个数据库事务中保存 AI 识别的技术英语语料。
 */
@Service
public class TechEnglishAiImportPersistenceService {

    private static final int MAX_ITEMS_PER_IMPORT = 100;

    private final TechEnglishCorpusMapper corpusMapper;
    private final TechEnglishVocabularyExampleMapper vocabularyExampleMapper;
    private final TechEnglishCorpusService corpusService;
    private final ObjectMapper objectMapper;

    public TechEnglishAiImportPersistenceService(
            TechEnglishCorpusMapper corpusMapper,
            TechEnglishVocabularyExampleMapper vocabularyExampleMapper,
            TechEnglishCorpusService corpusService,
            ObjectMapper objectMapper) {
        this.corpusMapper = corpusMapper;
        this.vocabularyExampleMapper = vocabularyExampleMapper;
        this.corpusService = corpusService;
        this.objectMapper = objectMapper;
    }

    /** 在同一事务中幂等保存 AI 自动分类后的生词和句子。 */
    @Transactional
    public List<TechEnglishCorpusDetailResponse> saveAuto(
            String batchUuid,
            TechEnglishAutoImportPayload payload,
            List<StoredObject> images,
            Map<String, List<Long>> itemTagAssignments,
            String sourceName,
            String scenario,
            int exampleCount,
            long userId) {
        List<TechEnglishCorpusEntity> existing = corpusMapper.selectList(
                Wrappers.<TechEnglishCorpusEntity>lambdaQuery()
                        .eq(TechEnglishCorpusEntity::getImportBatchUuid, batchUuid)
                        .eq(TechEnglishCorpusEntity::getCreateby, userId)
                        .eq(TechEnglishCorpusEntity::getDeleted, 0));
        if (!existing.isEmpty()) {
            return existing.stream().map(item -> corpusService.getPublishedCorpus(item.getId())).toList();
        }
        List<TechEnglishVocabularyImportPayload.Item> vocabularyItems = payload.vocabulary() == null
                || payload.vocabulary().items() == null ? List.of() : payload.vocabulary().items();
        List<TechEnglishSentenceImportPayload.Item> sentenceItems = payload.sentences() == null
                || payload.sentences().items() == null ? List.of() : payload.sentences().items();
        int totalItems = vocabularyItems.size() + sentenceItems.size();
        if (totalItems == 0) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "TECH_ENGLISH_AI_EMPTY",
                    "未从截图中识别到可入库的语料");
        }
        if (totalItems > MAX_ITEMS_PER_IMPORT) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "TECH_ENGLISH_AI_TOO_MANY_ITEMS",
                    "单次识别结果过多，请拆分截图");
        }
        List<TechEnglishCorpusDetailResponse> created = new ArrayList<>();
        if (!vocabularyItems.isEmpty()) {
            created.addAll(saveVocabulary(
                    batchUuid,
                    payload.vocabulary(),
                    images,
                    itemTagAssignments,
                    sourceName,
                    scenario,
                    exampleCount,
                    userId));
        }
        if (!sentenceItems.isEmpty()) {
            created.addAll(saveSentences(
                    batchUuid,
                    payload.sentences(),
                    images,
                    itemTagAssignments,
                    sourceName,
                    scenario,
                    exampleCount,
                    userId));
        }
        return List.copyOf(created);
    }

    /** 保存生词本识别结果。 */
    @Transactional
    public List<TechEnglishCorpusDetailResponse> saveVocabulary(
            String batchUuid,
            TechEnglishVocabularyImportPayload payload,
            List<StoredObject> images,
            Map<String, List<Long>> itemTagAssignments,
            String sourceName,
            String scenario,
            int exampleCount,
            long userId) {
        List<TechEnglishVocabularyImportPayload.Item> items = requireItems(payload.items());
        List<Long> createdIds = new ArrayList<>();
        Set<String> importedWords = new HashSet<>();
        for (TechEnglishVocabularyImportPayload.Item item : items) {
            String word = trimToNull(item.word(), 200);
            if (word == null || !importedWords.add(word.toLowerCase(Locale.ROOT))) {
                continue;
            }
            int imageIndex = requireImageIndex(item.sourceImageIndex(), images.size());
            List<Long> tagIds = requireTagIds(
                    itemTagAssignments,
                    TechEnglishAiRecognitionItemKey.create("VOCABULARY", imageIndex, word));
            List<TechEnglishVocabularyImportPayload.Example> examples = normalizeVocabularyExamples(
                    item.examples(), exampleCount);
            TechEnglishCorpusEntity corpus = baseCorpus(
                    "VOCABULARY", word, word, batchUuid, imageIndex, images,
                    sourceName, scenario, userId);
            corpus.setPartOfSpeech(trimToNull(item.partOfSpeech(), 64));
            corpus.setBritishPhonetic(trimToNull(item.britishPhonetic(), 120));
            corpus.setAmericanPhonetic(trimToNull(item.americanPhonetic(), 120));
            corpus.setPhonetic(firstText(corpus.getBritishPhonetic(), corpus.getAmericanPhonetic()));
            corpus.setTranslationText(trimToNull(item.meaning(), 5000));
            corpus.setTranslationStatus(StringUtils.hasText(corpus.getTranslationText()) ? "READY" : "NONE");
            corpusMapper.insert(corpus);
            if (!tagIds.isEmpty()) corpusMapper.insertTagLinks(corpus.getId(), tagIds);
            saveVocabularyExamples(corpus.getId(), examples, userId);
            createdIds.add(corpus.getId());
        }
        return loadCreated(createdIds);
    }

    /** 保存经典句子识别结果。 */
    @Transactional
    public List<TechEnglishCorpusDetailResponse> saveSentences(
            String batchUuid,
            TechEnglishSentenceImportPayload payload,
            List<StoredObject> images,
            Map<String, List<Long>> itemTagAssignments,
            String sourceName,
            String scenario,
            int exampleCount,
            long userId) {
        List<TechEnglishSentenceImportPayload.Item> items = requireItems(payload.items());
        List<Long> createdIds = new ArrayList<>();
        Set<String> importedSentences = new HashSet<>();
        for (TechEnglishSentenceImportPayload.Item item : items) {
            String sentence = trimToNull(item.sentence(), 20_000);
            if (sentence == null || !importedSentences.add(sentence.toLowerCase(Locale.ROOT))) {
                continue;
            }
            int imageIndex = requireImageIndex(item.sourceImageIndex(), images.size());
            List<Long> tagIds = requireTagIds(
                    itemTagAssignments,
                    TechEnglishAiRecognitionItemKey.create("SENTENCE", imageIndex, sentence));
            List<TechEnglishKeyVocabularyResponse> keyVocabulary = normalizeKeyVocabulary(item.keyVocabulary());
            List<TechEnglishPatternExampleResponse> patternExamples = normalizePatternExamples(
                    item.patternExamples(), exampleCount);
            TechEnglishCorpusEntity corpus = baseCorpus(
                    "SENTENCE", abbreviate(sentence, 160), sentence, batchUuid, imageIndex, images,
                    sourceName, scenario, userId);
            corpus.setTranslationText(trimToNull(item.translation(), 5000));
            corpus.setTranslationStatus(StringUtils.hasText(corpus.getTranslationText()) ? "READY" : "NONE");
            corpus.setSentencePattern(trimToNull(item.classicPattern(), 500));
            corpus.setSentencePatternExplanation(trimToNull(item.patternExplanation(), 1000));
            corpus.setExplanation(corpus.getSentencePatternExplanation());
            corpus.setKeyVocabularyJson(toJson(keyVocabulary));
            corpus.setPatternExamplesJson(toJson(patternExamples));
            corpusMapper.insert(corpus);
            if (!tagIds.isEmpty()) corpusMapper.insertTagLinks(corpus.getId(), tagIds);
            createdIds.add(corpus.getId());
        }
        return loadCreated(createdIds);
    }

    /** 创建 AI 导入语料的通用字段。 */
    private TechEnglishCorpusEntity baseCorpus(
            String corpusType,
            String title,
            String englishText,
            String batchUuid,
            int imageIndex,
            List<StoredObject> images,
            String sourceName,
            String scenario,
            long userId) {
        TechEnglishCorpusEntity corpus = new TechEnglishCorpusEntity();
        corpus.setCorpusUuid(UUID.randomUUID().toString());
        corpus.setCorpusType(corpusType);
        corpus.setTitle(abbreviate(title, 160));
        corpus.setEnglishText(englishText);
        corpus.setImageObjectKey(images.get(imageIndex - 1).objectKey());
        corpus.setImageAlt(sourceName + "截图 " + imageIndex);
        corpus.setSourceName(sourceName);
        corpus.setScenario(trimToNull(scenario, 80));
        corpus.setDifficulty("INTERMEDIATE");
        corpus.setAiReviewStatus("REVIEWED");
        corpus.setAiReviewNotes("AI 截图识别导入");
        corpus.setIndexStatus("NOT_INDEXED");
        corpus.setContentVersion(1L);
        corpus.setStatus("PUBLISHED");
        corpus.setPublishedAt(LocalDateTime.now());
        corpus.setImportBatchUuid(batchUuid);
        corpus.setSourceImageIndex(imageIndex);
        corpus.setCreateby(userId);
        corpus.setUpdateby(userId);
        return corpus;
    }

    /** 保存词汇例句。 */
    private void saveVocabularyExamples(
            long vocabularyCorpusId,
            List<TechEnglishVocabularyImportPayload.Example> examples,
            long userId) {
        for (int index = 0; index < examples.size(); index += 1) {
            TechEnglishVocabularyImportPayload.Example item = examples.get(index);
            TechEnglishVocabularyExampleEntity example = new TechEnglishVocabularyExampleEntity();
            example.setVocabularyCorpusId(vocabularyCorpusId);
            example.setEnglishText(item.englishText());
            example.setTranslationText(item.translationText());
            example.setSortOrder(index + 1);
            example.setCreateby(userId);
            example.setUpdateby(userId);
            vocabularyExampleMapper.insert(example);
        }
    }

    /** 清洗词汇例句并按用户设置限制数量。 */
    private List<TechEnglishVocabularyImportPayload.Example> normalizeVocabularyExamples(
            List<TechEnglishVocabularyImportPayload.Example> examples,
            int exampleCount) {
        if (examples == null || exampleCount <= 0) {
            return List.of();
        }
        return examples.stream()
                .filter(item -> item != null && StringUtils.hasText(item.englishText()))
                .limit(exampleCount)
                .map(item -> new TechEnglishVocabularyImportPayload.Example(
                        trimToNull(item.englishText(), 2000),
                        trimToNull(item.translationText(), 1000)))
                .toList();
    }

    /** 清洗句子重点词汇。 */
    private List<TechEnglishKeyVocabularyResponse> normalizeKeyVocabulary(
            List<TechEnglishSentenceImportPayload.KeyVocabulary> vocabulary) {
        if (vocabulary == null) {
            return List.of();
        }
        return vocabulary.stream()
                .filter(item -> item != null && StringUtils.hasText(item.word()))
                .limit(20)
                .map(item -> new TechEnglishKeyVocabularyResponse(
                        trimToNull(item.word(), 200),
                        trimToNull(item.partOfSpeech(), 64),
                        trimToNull(item.meaning(), 1000)))
                .toList();
    }

    /** 清洗句式例句并按用户设置限制数量。 */
    private List<TechEnglishPatternExampleResponse> normalizePatternExamples(
            List<TechEnglishSentenceImportPayload.PatternExample> examples,
            int exampleCount) {
        if (examples == null || exampleCount <= 0) {
            return List.of();
        }
        return examples.stream()
                .filter(item -> item != null && StringUtils.hasText(item.englishText()))
                .limit(exampleCount)
                .map(item -> new TechEnglishPatternExampleResponse(
                        trimToNull(item.englishText(), 2000),
                        trimToNull(item.translationText(), 1000)))
                .toList();
    }

    /** 校验识别结果数量。 */
    private <T> List<T> requireItems(List<T> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "TECH_ENGLISH_AI_EMPTY", "未从截图中识别到可入库的语料");
        }
        if (items.size() > MAX_ITEMS_PER_IMPORT) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "TECH_ENGLISH_AI_TOO_MANY_ITEMS", "单次识别结果过多，请拆分截图");
        }
        return items;
    }

    /** 校验已选择的知识标签。 */
    private void validateTagIds(List<Long> tagIds) {
        if (tagIds == null || tagIds.size() > 20
                || corpusMapper.countActiveTags(tagIds) != tagIds.size()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "TECH_ENGLISH_TAG_INVALID", "请选择有效的知识标签");
        }
    }

    /** 读取并校验单条语料的可选标签。 */
    private List<Long> requireTagIds(Map<String, List<Long>> itemTagAssignments, String itemKey) {
        List<Long> tagIds = itemTagAssignments.get(itemKey);
        if (tagIds == null || tagIds.isEmpty()) return List.of();
        validateTagIds(tagIds);
        return tagIds;
    }

    /** 校验 AI 返回的来源截图序号。 */
    private int requireImageIndex(Integer value, int imageCount) {
        if (value == null || value < 1 || value > imageCount) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "TECH_ENGLISH_AI_IMAGE_INDEX_INVALID", "AI 返回的截图序号无效");
        }
        return value;
    }

    /** 将列表序列化为 MySQL JSON 字段。 */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "TECH_ENGLISH_AI_JSON_ERROR", "AI 识别结果无法保存", exception);
        }
    }

    /** 读取已创建的语料详情。 */
    private List<TechEnglishCorpusDetailResponse> loadCreated(List<Long> ids) {
        if (ids.isEmpty()) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "TECH_ENGLISH_AI_EMPTY", "未从截图中识别到可入库的语料");
        }
        return ids.stream().map(corpusService::getPublishedCorpus).toList();
    }

    /** 返回第一个有内容的字符串。 */
    private String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    /** 清理空白并限制字段长度。 */
    private String trimToNull(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return abbreviate(value.trim(), maxLength);
    }

    /** 截断过长文本。 */
    private String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }
}
