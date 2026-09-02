package com.aitechskill.book.english.service;

import com.aitechskill.book.common.exception.BusinessException;
import com.aitechskill.book.document.domain.DocumentTagRecord;
import com.aitechskill.book.document.domain.response.DocumentTagResponse;
import com.aitechskill.book.english.domain.TechEnglishScenarioTagCatalog;
import com.aitechskill.book.english.domain.TechEnglishImageContent;
import com.aitechskill.book.english.domain.entity.TechEnglishCorpusEntity;
import com.aitechskill.book.english.domain.entity.TechEnglishVocabularyExampleEntity;
import com.aitechskill.book.english.domain.request.TechEnglishCorpusCreateRequest;
import com.aitechskill.book.english.domain.request.TechEnglishCorpusMetadataUpdateRequest;
import com.aitechskill.book.english.domain.request.TechEnglishVocabularyExampleRequest;
import com.aitechskill.book.english.domain.response.TechEnglishCorpusDetailResponse;
import com.aitechskill.book.english.domain.response.TechEnglishCorpusPageResponse;
import com.aitechskill.book.english.domain.response.TechEnglishCorpusSummaryResponse;
import com.aitechskill.book.english.domain.response.TechEnglishKeyVocabularyResponse;
import com.aitechskill.book.english.domain.response.TechEnglishPatternExampleResponse;
import com.aitechskill.book.english.domain.response.TechEnglishVocabularyExampleResponse;
import com.aitechskill.book.english.mapper.TechEnglishCorpusMapper;
import com.aitechskill.book.english.mapper.TechEnglishVocabularyExampleMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 主平台技术英语语料检索与阅读服务。
 */
@Service
public class TechEnglishCorpusService {

    private static final int DEFAULT_PAGE_SIZE = 12;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_VOCABULARY_EXAMPLES = 20;

    private final TechEnglishCorpusMapper corpusMapper;
    private final TechEnglishVocabularyExampleMapper vocabularyExampleMapper;
    private final TechEnglishImageStorageService imageStorageService;
    private final ObjectMapper objectMapper;

    public TechEnglishCorpusService(
            TechEnglishCorpusMapper corpusMapper,
            TechEnglishVocabularyExampleMapper vocabularyExampleMapper,
            TechEnglishImageStorageService imageStorageService,
            ObjectMapper objectMapper) {
        this.corpusMapper = corpusMapper;
        this.vocabularyExampleMapper = vocabularyExampleMapper;
        this.imageStorageService = imageStorageService;
        this.objectMapper = objectMapper;
    }

    /** 查询已发布语料并附加知识标签摘要，多个标签按命中任一标签处理。 */
    @Transactional(readOnly = true)
    public TechEnglishCorpusPageResponse search(String keyword, String corpusType, List<Long> tagIds, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        String normalizedType = normalizeOptionalType(corpusType);
        List<Long> normalizedTagIds = normalizeFilterTagIds(tagIds);
        long total = corpusMapper.countPublished(normalizedKeyword, normalizedType, normalizedTagIds);
        List<TechEnglishCorpusEntity> corpus = total == 0
                ? List.of()
                : corpusMapper.selectPublishedPage(
                        normalizedKeyword,
                        normalizedType,
                        normalizedTagIds,
                        (long) (safePage - 1) * safeSize,
                        safeSize);
        return toPage(total, safePage, safeSize, corpus);
    }

    /** 查询已发布语料，支持页面将多个底层类型组合为一个学习分区。 */
    @Transactional(readOnly = true)
    public TechEnglishCorpusPageResponse search(
            String keyword,
            String corpusType,
            List<String> corpusTypes,
            List<Long> tagIds,
            int page,
            int size) {
        int safePage = Math.max(page, 1);
        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        String normalizedType = normalizeOptionalType(corpusType);
        List<String> normalizedTypes = normalizeOptionalTypes(corpusTypes);
        List<Long> normalizedTagIds = normalizeFilterTagIds(tagIds);
        long total = corpusMapper.countPublishedByTypes(normalizedKeyword, normalizedType, normalizedTypes, normalizedTagIds);
        List<TechEnglishCorpusEntity> corpus = total == 0
                ? List.of()
                : corpusMapper.selectPublishedPageByTypes(
                        normalizedKeyword,
                        normalizedType,
                        normalizedTypes,
                        normalizedTagIds,
                        (long) (safePage - 1) * safeSize,
                        safeSize);
        return toPage(total, safePage, safeSize, corpus);
    }

    private TechEnglishCorpusPageResponse toPage(
            long total,
            int page,
            int size,
            List<TechEnglishCorpusEntity> corpus) {
        Map<Long, List<DocumentTagResponse>> tagsByCorpus = loadTags(corpus);
        List<TechEnglishCorpusSummaryResponse> items = corpus.stream()
                .map(item -> toSummary(item, tagsByCorpus.getOrDefault(item.getId(), List.of())))
                .toList();
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new TechEnglishCorpusPageResponse(total, page, size, totalPages, items);
    }

    /** 清理语料库筛选标签，去重并忽略无效 ID。 */
    private List<Long> normalizeFilterTagIds(List<Long> tagIds) {
        if (tagIds == null) {
            return List.of();
        }
        return new ArrayList<>(new LinkedHashSet<>(tagIds).stream()
                .filter(tagId -> tagId != null && tagId > 0)
                .toList());
    }

    /** 读取一条已发布语料详情。 */
    @Transactional(readOnly = true)
    public TechEnglishCorpusDetailResponse getPublishedCorpus(long id) {
        TechEnglishCorpusEntity corpus = corpusMapper.selectPublishedById(id);
        if (corpus == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "TECH_ENGLISH_NOT_FOUND", "技术英语语料不存在或尚未发布");
        }
        List<DocumentTagResponse> tags = loadTags(List.of(corpus)).getOrDefault(id, List.of());
        List<TechEnglishVocabularyExampleResponse> examples = loadExamples(corpus);
        return toDetail(corpus, tags, examples);
    }

    /** 将用户主动选择的词汇例句保存为独立句子语料。 */
    @Transactional
    public TechEnglishCorpusDetailResponse saveVocabularyExampleAsSentence(
            long vocabularyCorpusId,
            long exampleId,
            long userId) {
        TechEnglishCorpusEntity vocabulary = corpusMapper.selectPublishedById(vocabularyCorpusId);
        if (vocabulary == null || !isLexicalType(vocabulary.getCorpusType())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "TECH_ENGLISH_VOCABULARY_NOT_FOUND", "技术英语词汇不存在或尚未发布");
        }
        TechEnglishVocabularyExampleEntity example = vocabularyExampleMapper
                .selectActiveByVocabularyAndIdForUpdate(vocabularyCorpusId, exampleId);
        if (example == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "TECH_ENGLISH_EXAMPLE_NOT_FOUND", "词汇例句不存在");
        }
        if (example.getSentenceCorpusId() != null) {
            return getPublishedCorpus(example.getSentenceCorpusId());
        }
        List<Long> tagIds = corpusMapper.selectTagsByCorpusIds(List.of(vocabularyCorpusId)).stream()
                .map(DocumentTagRecord::id)
                .toList();
        Long sentenceCorpusId = createSentenceFromExample(
                vocabulary,
                new TechEnglishVocabularyExampleRequest(example.getEnglishText(), example.getTranslationText()),
                tagIds,
                userId);
        example.setSentenceCorpusId(sentenceCorpusId);
        example.setUpdateby(userId);
        vocabularyExampleMapper.updateById(example);
        return getPublishedCorpus(sentenceCorpusId);
    }

    /** 从主平台轻表单直接收录并发布技术英语语料。 */
    @Transactional
    public TechEnglishCorpusDetailResponse create(TechEnglishCorpusCreateRequest request, MultipartFile imageFile, long userId) {
        String corpusType = normalizeRequiredType(request.corpusType());
        String difficulty = normalizeDifficulty(request.difficulty());
        validateTextLengths(request);
        List<Long> tagIds = validateTagIds(request.tagIds());
        List<TechEnglishVocabularyExampleRequest> examples = normalizeExamples(corpusType, request);
        validateTypeContent(corpusType, request, imageFile);
        String corpusUuid = UUID.randomUUID().toString();
        TechEnglishCorpusEntity corpus = new TechEnglishCorpusEntity();
        corpus.setCorpusUuid(corpusUuid);
        corpus.setCorpusType(corpusType);
        corpus.setTitle(resolveTitle(corpusType, request));
        corpus.setEnglishText(trimToNull(request.englishText()));
        corpus.setPhonetic(trimToNull(request.phonetic()));
        corpus.setExplanation(trimToNull(request.explanation()));
        corpus.setArticleMarkdown(trimToNull(request.articleMarkdown()));
        corpus.setImageUrl(null);
        corpus.setImageObjectKey(null);
        corpus.setImageAlt(trimToNull(request.imageAlt()));
        corpus.setSourceName(trimToNull(request.sourceName()));
        corpus.setSourceUrl(trimToNull(request.sourceUrl()));
        corpus.setScenario(trimToNull(request.scenario()));
        corpus.setScenarioTags(toJson(TechEnglishScenarioTagCatalog.normalize(request.scenarioTagCodes())));
        corpus.setDifficulty(difficulty);
        corpus.setTranslationText(trimToNull(request.translationText()));
        corpus.setTranslationStatus(StringUtils.hasText(request.translationText()) ? "READY" : "NONE");
        corpus.setAiReviewStatus("NOT_REQUIRED");
        corpus.setIndexStatus("NOT_INDEXED");
        corpus.setContentVersion(1L);
        corpus.setStatus("PUBLISHED");
        corpus.setPublishedAt(LocalDateTime.now());
        corpus.setCreateby(userId);
        corpus.setUpdateby(userId);
        corpusMapper.insert(corpus);
        if (!tagIds.isEmpty()) {
            corpusMapper.insertTagLinks(corpus.getId(), tagIds);
        }
        List<TechEnglishVocabularyExampleResponse> exampleResponses = saveVocabularyExamples(
                corpus,
                examples,
                tagIds,
                Boolean.TRUE.equals(request.syncExamplesToSentences()),
                userId);
        List<DocumentTagResponse> tags = loadTags(List.of(corpus)).getOrDefault(corpus.getId(), List.of());
        return toDetail(corpus, tags, exampleResponses);
    }

    /** 仅在详情页更新知识标签和固定场景标签。 */
    @Transactional
    public TechEnglishCorpusDetailResponse updateMetadata(
            long id,
            TechEnglishCorpusMetadataUpdateRequest request,
            long userId) {
        TechEnglishCorpusEntity corpus = corpusMapper.selectPublishedById(id);
        if (corpus == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "TECH_ENGLISH_NOT_FOUND", "技术英语语料不存在或尚未发布");
        }
        List<Long> tagIds = validateTagIds(request == null ? null : request.tagIds());
        List<String> scenarioTags = TechEnglishScenarioTagCatalog.normalize(
                request == null ? null : request.scenarioTagCodes());
        corpus.setScenarioTags(toJson(scenarioTags));
        corpus.setUpdateby(userId);
        corpusMapper.updateById(corpus);
        corpusMapper.deleteTagLinks(corpus.getId(), userId);
        if (!tagIds.isEmpty()) {
            corpusMapper.insertTagLinks(corpus.getId(), tagIds);
        }
        return getPublishedCorpus(corpus.getId());
    }

    /** 读取已发布图片语料文件。 */
    @Transactional(readOnly = true)
    public TechEnglishImageContent getPublishedImage(long id) {
        TechEnglishCorpusEntity corpus = corpusMapper.selectPublishedById(id);
        if (corpus == null || !StringUtils.hasText(corpus.getImageObjectKey())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "TECH_ENGLISH_IMAGE_NOT_FOUND", "图片语料不存在");
        }
        return imageStorageService.open(corpus.getImageObjectKey());
    }

    /** 批量加载并按语料主键分组知识标签。 */
    private Map<Long, List<DocumentTagResponse>> loadTags(List<TechEnglishCorpusEntity> corpus) {
        if (corpus.isEmpty()) {
            return Map.of();
        }
        List<Long> corpusIds = corpus.stream().map(TechEnglishCorpusEntity::getId).toList();
        return corpusMapper.selectTagsByCorpusIds(corpusIds).stream()
                .collect(Collectors.groupingBy(
                        DocumentTagRecord::documentId,
                        Collectors.mapping(
                                tag -> new DocumentTagResponse(tag.id(), tag.name(), tag.level()),
                                Collectors.toList())));
    }

    /** 加载词汇例句。 */
    private List<TechEnglishVocabularyExampleResponse> loadExamples(TechEnglishCorpusEntity corpus) {
        if (!isLexicalType(corpus.getCorpusType())) {
            return List.of();
        }
        return vocabularyExampleMapper.selectList(Wrappers.<TechEnglishVocabularyExampleEntity>lambdaQuery()
                        .eq(TechEnglishVocabularyExampleEntity::getVocabularyCorpusId, corpus.getId())
                        .eq(TechEnglishVocabularyExampleEntity::getDeleted, 0)
                        .orderByAsc(TechEnglishVocabularyExampleEntity::getSortOrder)
                        .orderByAsc(TechEnglishVocabularyExampleEntity::getId))
                .stream()
                .map(example -> new TechEnglishVocabularyExampleResponse(
                        example.getId(),
                        example.getSentenceCorpusId(),
                        example.getEnglishText(),
                        example.getTranslationText()))
                .toList();
    }

    /** 规范化可选语料类型。 */
    private String normalizeOptionalType(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!"VOCABULARY".equals(normalized)
                && !"PHRASE".equals(normalized)
                && !"PATTERN".equals(normalized)
                && !"SENTENCE".equals(normalized)
                && !"ARTICLE".equals(normalized)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "TECH_ENGLISH_TYPE_INVALID", "语料类型不合法");
        }
        return normalized;
    }

    /** 规范化组合筛选类型，并拒绝未知类型。 */
    private List<String> normalizeOptionalTypes(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(this::normalizeRequiredType)
                .distinct()
                .toList();
    }

    /** 规范化必填语料类型。 */
    private String normalizeRequiredType(String value) {
        String normalized = normalizeOptionalType(value);
        if (normalized == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "TECH_ENGLISH_TYPE_INVALID", "请选择语料类型");
        }
        return normalized;
    }

    /** 校验主站轻表单字段长度。 */
    private void validateTextLengths(TechEnglishCorpusCreateRequest request) {
        checkLength(request.title(), 160, "标题不能超过 160 字");
        checkLength(request.englishText(), 20_000, "英文内容不能超过 20000 字");
        checkLength(request.phonetic(), 120, "发音提示不能超过 120 字");
        checkLength(request.explanation(), 1000, "说明不能超过 1000 字");
        checkLength(request.articleMarkdown(), 50_000, "文章正文不能超过 50000 字");
        checkLength(request.imageAlt(), 300, "图片说明不能超过 300 字");
        checkLength(request.sourceName(), 120, "来源名称不能超过 120 字");
        checkLength(request.sourceUrl(), 2048, "来源链接不能超过 2048 字");
        checkLength(request.scenario(), 80, "场景不能超过 80 字");
        checkLength(request.translationText(), 5000, "中文参考不能超过 5000 字");
    }

    /** 校验可选文本长度。 */
    private void checkLength(String value, int maxLength, String message) {
        if (value != null && value.trim().length() > maxLength) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "TECH_ENGLISH_FIELD_TOO_LONG", message);
        }
    }

    /** 清洗和校验词汇例句。 */
    private List<TechEnglishVocabularyExampleRequest> normalizeExamples(String corpusType, TechEnglishCorpusCreateRequest request) {
        List<TechEnglishVocabularyExampleRequest> rawExamples = request.vocabularyExamples() == null
                ? List.of()
                : request.vocabularyExamples();
        List<TechEnglishVocabularyExampleRequest> examples = new ArrayList<>();
        for (TechEnglishVocabularyExampleRequest example : rawExamples) {
            if (example == null || !StringUtils.hasText(example.englishText())) {
                continue;
            }
            String englishText = example.englishText().trim();
            String translationText = trimToNull(example.translationText());
            if (englishText.length() > 2000) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "TECH_ENGLISH_EXAMPLE_TOO_LONG", "例句不能超过 2000 字");
            }
            if (translationText != null && translationText.length() > 1000) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "TECH_ENGLISH_EXAMPLE_TOO_LONG", "例句释义不能超过 1000 字");
            }
            examples.add(new TechEnglishVocabularyExampleRequest(englishText, translationText));
        }
        if (!isLexicalType(corpusType) && !examples.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "TECH_ENGLISH_EXAMPLE_UNEXPECTED", "只有词汇语料可以添加例句");
        }
        if (examples.size() > MAX_VOCABULARY_EXAMPLES) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "TECH_ENGLISH_EXAMPLE_TOO_MANY", "例句最多添加 20 组");
        }
        return examples;
    }

    /** 规范化难度，主站未填写时采用中级。 */
    private String normalizeDifficulty(String value) {
        if (!StringUtils.hasText(value)) {
            return "INTERMEDIATE";
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!"BEGINNER".equals(normalized)
                && !"INTERMEDIATE".equals(normalized)
                && !"ADVANCED".equals(normalized)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "TECH_ENGLISH_DIFFICULTY_INVALID", "难度不合法");
        }
        return normalized;
    }

    /** 校验标签数量和有效性，并去重保持用户选择顺序。 */
    private List<Long> validateTagIds(List<Long> tagIds) {
        if (tagIds == null) {
            return List.of();
        }
        LinkedHashSet<Long> uniqueIds = tagIds.stream()
                .filter(tagId -> tagId != null && tagId > 0)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (uniqueIds.size() > 20) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "TECH_ENGLISH_TAG_REQUIRED", "知识标签最多选择 20 个");
        }
        List<Long> normalized = List.copyOf(uniqueIds);
        if (normalized.isEmpty()) {
            return normalized;
        }
        if (corpusMapper.countActiveTags(normalized) != normalized.size()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "TECH_ENGLISH_TAG_INVALID", "存在不可用的知识标签");
        }
        return normalized;
    }

    /** 按语料类型校验主站轻表单的必要内容。 */
    private void validateTypeContent(String corpusType, TechEnglishCorpusCreateRequest request, MultipartFile imageFile) {
        if ((isLexicalType(corpusType) || "PATTERN".equals(corpusType) || "SENTENCE".equals(corpusType))
                && !StringUtils.hasText(request.englishText())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "TECH_ENGLISH_CONTENT_REQUIRED", "请填写英文内容");
        }
        if (imageFile != null && !imageFile.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "TECH_ENGLISH_IMAGE_UNEXPECTED", "语料不再支持按图片类型入库");
        }
        if ("ARTICLE".equals(corpusType)
                && !StringUtils.hasText(request.articleMarkdown())
                && !StringUtils.hasText(request.sourceUrl())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "TECH_ENGLISH_ARTICLE_REQUIRED", "请填写文章正文或文章链接");
        }
        if (StringUtils.hasText(request.sourceUrl())) {
            requireHttpUrl(request.sourceUrl(), "TECH_ENGLISH_SOURCE_URL_INVALID", "请填写有效的来源链接");
        }
    }

    /** 根据语料类型推导标题，减少主站轻表单输入。 */
    private String resolveTitle(String corpusType, TechEnglishCorpusCreateRequest request) {
        String explicitTitle = trimToNull(request.title());
        if (explicitTitle != null) {
            return explicitTitle;
        }
        if (isLexicalType(corpusType) || "PATTERN".equals(corpusType) || "SENTENCE".equals(corpusType)) {
            return abbreviate(trimToNull(request.englishText()), 160);
        }
        String sourceName = trimToNull(request.sourceName());
        if (sourceName != null) {
            return sourceName;
        }
        String articleText = trimToNull(request.articleMarkdown());
        if (articleText != null) {
            return abbreviate(articleText, 160);
        }
        return abbreviate(trimToNull(request.sourceUrl()), 160);
    }

    /** 保存词汇例句，并按需同步为句子语料。 */
    private List<TechEnglishVocabularyExampleResponse> saveVocabularyExamples(
            TechEnglishCorpusEntity vocabulary,
            List<TechEnglishVocabularyExampleRequest> examples,
            List<Long> tagIds,
            boolean syncExamplesToSentences,
            long userId) {
        if (!isLexicalType(vocabulary.getCorpusType()) || examples.isEmpty()) {
            return List.of();
        }
        List<TechEnglishVocabularyExampleResponse> responses = new ArrayList<>();
        for (int index = 0; index < examples.size(); index += 1) {
            TechEnglishVocabularyExampleRequest request = examples.get(index);
            Long sentenceCorpusId = syncExamplesToSentences
                    ? createSentenceFromExample(vocabulary, request, tagIds, userId)
                    : null;
            TechEnglishVocabularyExampleEntity example = new TechEnglishVocabularyExampleEntity();
            example.setVocabularyCorpusId(vocabulary.getId());
            example.setSentenceCorpusId(sentenceCorpusId);
            example.setEnglishText(request.englishText());
            example.setTranslationText(trimToNull(request.translationText()));
            example.setSortOrder(index + 1);
            example.setCreateby(userId);
            example.setUpdateby(userId);
            vocabularyExampleMapper.insert(example);
            responses.add(new TechEnglishVocabularyExampleResponse(
                    example.getId(),
                    sentenceCorpusId,
                    example.getEnglishText(),
                    example.getTranslationText()));
        }
        return responses;
    }

    /** 将词汇例句同步成一条独立句子语料。 */
    private Long createSentenceFromExample(
            TechEnglishCorpusEntity vocabulary,
            TechEnglishVocabularyExampleRequest example,
            List<Long> tagIds,
            long userId) {
        TechEnglishCorpusEntity sentence = new TechEnglishCorpusEntity();
        sentence.setCorpusUuid(UUID.randomUUID().toString());
        sentence.setCorpusType("SENTENCE");
        sentence.setTitle(abbreviate(example.englishText(), 160));
        sentence.setEnglishText(example.englishText());
        sentence.setExplanation("来自词汇「" + vocabulary.getEnglishText() + "」的例句");
        sentence.setScenario(vocabulary.getScenario());
        sentence.setScenarioTags(vocabulary.getScenarioTags());
        sentence.setDifficulty(vocabulary.getDifficulty());
        sentence.setTranslationText(trimToNull(example.translationText()));
        sentence.setTranslationStatus(StringUtils.hasText(example.translationText()) ? "READY" : "NONE");
        sentence.setAiReviewStatus("NOT_REQUIRED");
        sentence.setIndexStatus("NOT_INDEXED");
        sentence.setContentVersion(1L);
        sentence.setStatus("PUBLISHED");
        sentence.setPublishedAt(LocalDateTime.now());
        sentence.setCreateby(userId);
        sentence.setUpdateby(userId);
        corpusMapper.insert(sentence);
        if (!tagIds.isEmpty()) {
            corpusMapper.insertTagLinks(sentence.getId(), tagIds);
        }
        return sentence.getId();
    }

    /** 截断标题，避免超出数据库字段。 */
    private String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    /** 校验公网 HTTP/HTTPS 链接。 */
    private void requireHttpUrl(String value, String code, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, code, message);
        }
        try {
            URI uri = new URI(value.trim());
            String scheme = uri.getScheme();
            if (uri.getHost() == null
                    || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                throw new URISyntaxException(value, "unsupported scheme");
            }
        } catch (URISyntaxException exception) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, code, message);
        }
    }

    /** 将空白字符串转为空值，避免写入无意义空白。 */
    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /** 转换列表响应。 */
    private TechEnglishCorpusSummaryResponse toSummary(
            TechEnglishCorpusEntity corpus,
            List<DocumentTagResponse> tags) {
        return new TechEnglishCorpusSummaryResponse(
                corpus.getId(),
                corpus.getCorpusType(),
                corpus.getTitle(),
                corpus.getEnglishText(),
                corpus.getPhonetic(),
                corpus.getPartOfSpeech(),
                corpus.getBritishPhonetic(),
                corpus.getAmericanPhonetic(),
                corpus.getExplanation(),
                imageUrl(corpus),
                corpus.getImageAlt(),
                corpus.getScenario(),
                scenarioTagResponses(corpus.getScenarioTags()),
                corpus.getDifficulty(),
                corpus.getTags(),
                corpus.getTranslationText(),
                corpus.getSentencePattern(),
                corpus.getSentencePatternExplanation(),
                corpus.getPublishedAt(),
                tags);
    }

    /** 转换详情响应。 */
    private TechEnglishCorpusDetailResponse toDetail(
            TechEnglishCorpusEntity corpus,
            List<DocumentTagResponse> tags) {
        return toDetail(corpus, tags, loadExamples(corpus));
    }

    /** 转换详情响应。 */
    private TechEnglishCorpusDetailResponse toDetail(
            TechEnglishCorpusEntity corpus,
            List<DocumentTagResponse> tags,
            List<TechEnglishVocabularyExampleResponse> examples) {
        return new TechEnglishCorpusDetailResponse(
                corpus.getId(),
                corpus.getCorpusType(),
                corpus.getTitle(),
                corpus.getEnglishText(),
                corpus.getPhonetic(),
                corpus.getPartOfSpeech(),
                corpus.getBritishPhonetic(),
                corpus.getAmericanPhonetic(),
                corpus.getExplanation(),
                corpus.getArticleMarkdown(),
                imageUrl(corpus),
                corpus.getImageAlt(),
                corpus.getImportBatchUuid(),
                corpus.getSourceName(),
                corpus.getSourceUrl(),
                corpus.getScenario(),
                scenarioTagResponses(corpus.getScenarioTags()),
                corpus.getDifficulty(),
                corpus.getTags(),
                corpus.getTranslationText(),
                corpus.getSentencePattern(),
                corpus.getSentencePatternExplanation(),
                readJsonList(
                        corpus.getKeyVocabularyJson(),
                        new TypeReference<List<TechEnglishKeyVocabularyResponse>>() {}),
                readJsonList(
                        corpus.getPatternExamplesJson(),
                        new TypeReference<List<TechEnglishPatternExampleResponse>>() {}),
                corpus.getPublishedAt(),
                corpus.getUpdatetime(),
                tags,
                examples);
    }

    /** 判断是否为词汇或短语语料。 */
    private boolean isLexicalType(String corpusType) {
        return "VOCABULARY".equals(corpusType) || "PHRASE".equals(corpusType);
    }

    /** 将固定场景标签 JSON 转成展示对象。 */
    private List<com.aitechskill.book.english.domain.response.TechEnglishScenarioTagResponse> scenarioTagResponses(String json) {
        return TechEnglishScenarioTagCatalog.responsesOf(readJsonList(
                json,
                new TypeReference<List<String>>() {}));
    }

    /** 序列化 JSON 字段。 */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法序列化技术英语语料字段", exception);
        }
    }

    /** 返回前端可直接访问的图片地址。 */
    private String imageUrl(TechEnglishCorpusEntity corpus) {
        if (StringUtils.hasText(corpus.getImageObjectKey())) {
            return "/api/tech-english/corpus/" + corpus.getId() + "/image";
        }
        return corpus.getImageUrl();
    }

    /** 解析语料中的结构化 JSON 列表。 */
    private <T> List<T> readJsonList(String json, TypeReference<List<T>> type) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "TECH_ENGLISH_JSON_INVALID",
                    "语料结构化内容无法读取",
                    exception);
        }
    }
}
