package com.aitechskill.book.english.service;

import com.aitechskill.book.common.exception.BusinessException;
import com.aitechskill.book.document.domain.DocumentTagRecord;
import com.aitechskill.book.document.domain.response.DocumentTagResponse;
import com.aitechskill.book.english.domain.TechEnglishImageContent;
import com.aitechskill.book.english.domain.entity.TechEnglishCorpusEntity;
import com.aitechskill.book.english.domain.request.TechEnglishCorpusCreateRequest;
import com.aitechskill.book.english.domain.response.TechEnglishCorpusDetailResponse;
import com.aitechskill.book.english.domain.response.TechEnglishCorpusPageResponse;
import com.aitechskill.book.english.domain.response.TechEnglishCorpusSummaryResponse;
import com.aitechskill.book.english.mapper.TechEnglishCorpusMapper;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
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

    private final TechEnglishCorpusMapper corpusMapper;
    private final TechEnglishImageStorageService imageStorageService;

    public TechEnglishCorpusService(
            TechEnglishCorpusMapper corpusMapper,
            TechEnglishImageStorageService imageStorageService) {
        this.corpusMapper = corpusMapper;
        this.imageStorageService = imageStorageService;
    }

    /** 查询已发布语料并附加知识标签摘要。 */
    @Transactional(readOnly = true)
    public TechEnglishCorpusPageResponse search(String keyword, String corpusType, Long tagId, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        String normalizedType = normalizeOptionalType(corpusType);
        Long normalizedTagId = tagId != null && tagId > 0 ? tagId : null;
        long total = corpusMapper.countPublished(normalizedKeyword, normalizedType, normalizedTagId);
        List<TechEnglishCorpusEntity> corpus = total == 0
                ? List.of()
                : corpusMapper.selectPublishedPage(
                        normalizedKeyword,
                        normalizedType,
                        normalizedTagId,
                        (long) (safePage - 1) * safeSize,
                        safeSize);
        Map<Long, List<DocumentTagResponse>> tagsByCorpus = loadTags(corpus);
        List<TechEnglishCorpusSummaryResponse> items = corpus.stream()
                .map(item -> toSummary(item, tagsByCorpus.getOrDefault(item.getId(), List.of())))
                .toList();
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / safeSize);
        return new TechEnglishCorpusPageResponse(total, safePage, safeSize, totalPages, items);
    }

    /** 读取一条已发布语料详情。 */
    @Transactional(readOnly = true)
    public TechEnglishCorpusDetailResponse getPublishedCorpus(long id) {
        TechEnglishCorpusEntity corpus = corpusMapper.selectPublishedById(id);
        if (corpus == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "TECH_ENGLISH_NOT_FOUND", "技术英语语料不存在或尚未发布");
        }
        List<DocumentTagResponse> tags = loadTags(List.of(corpus)).getOrDefault(id, List.of());
        return toDetail(corpus, tags);
    }

    /** 从主平台轻表单直接收录并发布技术英语语料。 */
    @Transactional
    public TechEnglishCorpusDetailResponse create(TechEnglishCorpusCreateRequest request, MultipartFile imageFile, long userId) {
        String corpusType = normalizeOptionalType(request.corpusType());
        String difficulty = normalizeDifficulty(request.difficulty());
        validateTextLengths(request);
        List<Long> tagIds = validateTagIds(request.tagIds());
        validateTypeContent(corpusType, request, imageFile);
        String corpusUuid = UUID.randomUUID().toString();
        var storedImage = "IMAGE".equals(corpusType) ? imageStorageService.save(userId, imageFile) : null;
        TechEnglishCorpusEntity corpus = new TechEnglishCorpusEntity();
        corpus.setCorpusUuid(corpusUuid);
        corpus.setCorpusType(corpusType);
        corpus.setTitle(trimToNull(request.title()));
        corpus.setEnglishText(trimToNull(request.englishText()));
        corpus.setPhonetic(trimToNull(request.phonetic()));
        corpus.setExplanation(trimToNull(request.explanation()));
        corpus.setArticleMarkdown(trimToNull(request.articleMarkdown()));
        corpus.setImageUrl(null);
        corpus.setImageObjectKey(storedImage == null ? null : storedImage.objectKey());
        corpus.setImageAlt(trimToNull(request.imageAlt()));
        corpus.setSourceName(trimToNull(request.sourceName()));
        corpus.setSourceUrl(trimToNull(request.sourceUrl()));
        corpus.setScenario(trimToNull(request.scenario()));
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
        corpusMapper.insertTagLinks(corpus.getId(), tagIds);
        List<DocumentTagResponse> tags = loadTags(List.of(corpus)).getOrDefault(corpus.getId(), List.of());
        return toDetail(corpus, tags);
    }

    /** 读取已发布图片语料文件。 */
    @Transactional(readOnly = true)
    public TechEnglishImageContent getPublishedImage(long id) {
        TechEnglishCorpusEntity corpus = corpusMapper.selectPublishedById(id);
        if (corpus == null || !"IMAGE".equals(corpus.getCorpusType()) || !StringUtils.hasText(corpus.getImageObjectKey())) {
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

    /** 规范化可选语料类型。 */
    private String normalizeOptionalType(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!"VOCABULARY".equals(normalized)
                && !"SENTENCE".equals(normalized)
                && !"IMAGE".equals(normalized)
                && !"ARTICLE".equals(normalized)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "TECH_ENGLISH_TYPE_INVALID", "语料类型不合法");
        }
        return normalized;
    }

    /** 校验主站轻表单字段长度和标题必填。 */
    private void validateTextLengths(TechEnglishCorpusCreateRequest request) {
        requireText(request.title(), 160, "TECH_ENGLISH_TITLE_REQUIRED", "请填写 160 字以内的标题");
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

    /** 校验必填文本。 */
    private void requireText(String value, int maxLength, String code, String message) {
        if (!StringUtils.hasText(value) || value.trim().length() > maxLength) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, code, message);
        }
    }

    /** 校验可选文本长度。 */
    private void checkLength(String value, int maxLength, String message) {
        if (value != null && value.trim().length() > maxLength) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "TECH_ENGLISH_FIELD_TOO_LONG", message);
        }
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
        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>(tagIds);
        if (uniqueIds.isEmpty() || uniqueIds.size() > 20) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "TECH_ENGLISH_TAG_REQUIRED", "请选择 1 到 20 个知识标签");
        }
        List<Long> normalized = List.copyOf(uniqueIds);
        if (corpusMapper.countActiveTags(normalized) != normalized.size()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "TECH_ENGLISH_TAG_INVALID", "存在不可用的知识标签");
        }
        return normalized;
    }

    /** 按语料类型校验主站轻表单的必要内容。 */
    private void validateTypeContent(String corpusType, TechEnglishCorpusCreateRequest request, MultipartFile imageFile) {
        if (("VOCABULARY".equals(corpusType) || "SENTENCE".equals(corpusType))
                && !StringUtils.hasText(request.englishText())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "TECH_ENGLISH_CONTENT_REQUIRED", "请填写英文内容");
        }
        if ("IMAGE".equals(corpusType)) {
            if (imageFile == null || imageFile.isEmpty()) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "TECH_ENGLISH_IMAGE_REQUIRED", "请上传图片文件");
            }
        } else if (imageFile != null && !imageFile.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "TECH_ENGLISH_IMAGE_UNEXPECTED", "只有图片语料可以上传图片文件");
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
                corpus.getExplanation(),
                imageUrl(corpus),
                corpus.getImageAlt(),
                corpus.getScenario(),
                corpus.getDifficulty(),
                corpus.getTags(),
                corpus.getTranslationText(),
                corpus.getPublishedAt(),
                tags);
    }

    /** 转换详情响应。 */
    private TechEnglishCorpusDetailResponse toDetail(
            TechEnglishCorpusEntity corpus,
            List<DocumentTagResponse> tags) {
        return new TechEnglishCorpusDetailResponse(
                corpus.getId(),
                corpus.getCorpusType(),
                corpus.getTitle(),
                corpus.getEnglishText(),
                corpus.getPhonetic(),
                corpus.getExplanation(),
                corpus.getArticleMarkdown(),
                imageUrl(corpus),
                corpus.getImageAlt(),
                corpus.getSourceName(),
                corpus.getSourceUrl(),
                corpus.getScenario(),
                corpus.getDifficulty(),
                corpus.getTags(),
                corpus.getTranslationText(),
                corpus.getPublishedAt(),
                corpus.getUpdatetime(),
                tags);
    }

    /** 返回前端可直接访问的图片地址。 */
    private String imageUrl(TechEnglishCorpusEntity corpus) {
        if (StringUtils.hasText(corpus.getImageObjectKey())) {
            return "/api/tech-english/corpus/" + corpus.getId() + "/image";
        }
        return corpus.getImageUrl();
    }
}
