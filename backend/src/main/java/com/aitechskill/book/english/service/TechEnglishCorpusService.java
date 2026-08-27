package com.aitechskill.book.english.service;

import com.aitechskill.book.common.exception.BusinessException;
import com.aitechskill.book.document.domain.DocumentTagRecord;
import com.aitechskill.book.document.domain.response.DocumentTagResponse;
import com.aitechskill.book.english.domain.entity.TechEnglishCorpusEntity;
import com.aitechskill.book.english.domain.response.TechEnglishCorpusDetailResponse;
import com.aitechskill.book.english.domain.response.TechEnglishCorpusPageResponse;
import com.aitechskill.book.english.domain.response.TechEnglishCorpusSummaryResponse;
import com.aitechskill.book.english.mapper.TechEnglishCorpusMapper;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 主平台技术英语语料检索与阅读服务。
 */
@Service
public class TechEnglishCorpusService {

    private static final int DEFAULT_PAGE_SIZE = 12;
    private static final int MAX_PAGE_SIZE = 50;

    private final TechEnglishCorpusMapper corpusMapper;

    public TechEnglishCorpusService(TechEnglishCorpusMapper corpusMapper) {
        this.corpusMapper = corpusMapper;
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
                corpus.getImageUrl(),
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
                corpus.getImageUrl(),
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
}
