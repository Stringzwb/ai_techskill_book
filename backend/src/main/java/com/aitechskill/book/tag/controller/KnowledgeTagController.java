package com.aitechskill.book.tag.controller;

import com.aitechskill.book.tag.domain.response.KnowledgeTagTreeResponse;
import com.aitechskill.book.tag.service.KnowledgeTagService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 主平台知识标签树公开查询接口。
 */
@RestController
@RequestMapping("/api/knowledge-tags")
public class KnowledgeTagController {

    private final KnowledgeTagService knowledgeTagService;

    public KnowledgeTagController(KnowledgeTagService knowledgeTagService) {
        this.knowledgeTagService = knowledgeTagService;
    }

    /**
     * 查询可供文档库选择的完整知识标签树。
     *
     * @return 最多三级的知识标签树
     */
    @GetMapping("/tree")
    public List<KnowledgeTagTreeResponse> getTree() {
        return knowledgeTagService.getTree();
    }
}
