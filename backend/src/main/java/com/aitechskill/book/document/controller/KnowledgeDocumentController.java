package com.aitechskill.book.document.controller;

import com.aitechskill.book.document.domain.response.DocumentDetailResponse;
import com.aitechskill.book.document.domain.response.DocumentPageResponse;
import com.aitechskill.book.document.service.KnowledgeDocumentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 主平台知识文档检索与阅读接口。
 */
@RestController
@RequestMapping("/api/documents")
public class KnowledgeDocumentController {

    private final KnowledgeDocumentService knowledgeDocumentService;

    public KnowledgeDocumentController(KnowledgeDocumentService knowledgeDocumentService) {
        this.knowledgeDocumentService = knowledgeDocumentService;
    }

    /** 查询已发布文档。 */
    @GetMapping
    public DocumentPageResponse search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long tagId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size) {
        return knowledgeDocumentService.search(keyword, tagId, page, size);
    }

    /** 读取一篇已发布文档。 */
    @GetMapping("/{id}")
    public DocumentDetailResponse getDocument(@PathVariable long id) {
        return knowledgeDocumentService.getPublishedDocument(id);
    }
}
