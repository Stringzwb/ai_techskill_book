package com.aitechskill.book.document.service;

import com.aitechskill.book.common.exception.BusinessException;
import com.aitechskill.book.document.domain.DocumentTagRecord;
import com.aitechskill.book.document.domain.entity.KnowledgeDocumentEntity;
import com.aitechskill.book.document.domain.response.DocumentDetailResponse;
import com.aitechskill.book.document.domain.response.DocumentPageResponse;
import com.aitechskill.book.document.domain.response.DocumentSummaryResponse;
import com.aitechskill.book.document.domain.response.DocumentTagResponse;
import com.aitechskill.book.document.mapper.KnowledgeDocumentMapper;
import com.aitechskill.book.storage.domain.StoredObjectContent;
import com.aitechskill.book.storage.service.ObjectStorageService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 主平台知识文档检索与阅读服务。
 */
@Service
public class KnowledgeDocumentService {

    private static final int DEFAULT_PAGE_SIZE = 12;
    private static final int MAX_PAGE_SIZE = 50;

    private final KnowledgeDocumentMapper documentMapper;
    private final ObjectStorageService objectStorageService;

    public KnowledgeDocumentService(
            KnowledgeDocumentMapper documentMapper,
            ObjectStorageService objectStorageService) {
        this.documentMapper = documentMapper;
        this.objectStorageService = objectStorageService;
    }

    /** 查询已发布文档并附加标签摘要。 */
    @Transactional(readOnly = true)
    public DocumentPageResponse search(String keyword, Long tagId, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        Long normalizedTagId = tagId != null && tagId > 0 ? tagId : null;
        long total = documentMapper.countPublished(normalizedKeyword, normalizedTagId);
        List<KnowledgeDocumentEntity> documents = total == 0
                ? List.of()
                : documentMapper.selectPublishedPage(
                        normalizedKeyword,
                        normalizedTagId,
                        (long) (safePage - 1) * safeSize,
                        safeSize);
        Map<Long, List<DocumentTagResponse>> tagsByDocument = loadTags(documents);
        List<DocumentSummaryResponse> items = documents.stream()
                .map(document -> new DocumentSummaryResponse(
                        document.getId(),
                        document.getTitle(),
                        document.getSummary(),
                        readingMinutes(document.getMarkdownSize()),
                        document.getPublishedAt(),
                        tagsByDocument.getOrDefault(document.getId(), List.of())))
                .toList();
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / safeSize);
        return new DocumentPageResponse(total, safePage, safeSize, totalPages, items);
    }

    /** 读取一篇已发布文档的 Markdown 正文。 */
    @Transactional(readOnly = true)
    public DocumentDetailResponse getPublishedDocument(long id) {
        KnowledgeDocumentEntity document = documentMapper.selectPublishedById(id);
        if (document == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND", "文档不存在或尚未发布");
        }
        StoredObjectContent storedContent = objectStorageService.get(document.getMarkdownObjectKey());
        String markdown = new String(storedContent.content(), StandardCharsets.UTF_8);
        List<DocumentTagResponse> tags = loadTags(List.of(document)).getOrDefault(id, List.of());
        return new DocumentDetailResponse(
                document.getId(),
                document.getTitle(),
                document.getSummary(),
                markdown,
                readingMinutes(document.getMarkdownSize()),
                document.getPublishedAt(),
                document.getUpdatetime(),
                tags);
    }

    /** 批量加载并按文档主键分组标签。 */
    private Map<Long, List<DocumentTagResponse>> loadTags(List<KnowledgeDocumentEntity> documents) {
        if (documents.isEmpty()) {
            return Map.of();
        }
        List<Long> documentIds = documents.stream().map(KnowledgeDocumentEntity::getId).toList();
        return documentMapper.selectTagsByDocumentIds(documentIds).stream()
                .collect(Collectors.groupingBy(
                        DocumentTagRecord::documentId,
                        Collectors.mapping(
                                tag -> new DocumentTagResponse(tag.id(), tag.name(), tag.level()),
                                Collectors.toList())));
    }

    /** 按正文体积估算阅读时间，至少一分钟。 */
    private int readingMinutes(Long markdownSize) {
        long safeSize = markdownSize == null ? 0 : markdownSize;
        return Math.max(1, (int) Math.ceil(safeSize / 900.0));
    }
}
