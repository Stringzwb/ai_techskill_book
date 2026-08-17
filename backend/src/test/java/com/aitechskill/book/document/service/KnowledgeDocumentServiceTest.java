package com.aitechskill.book.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.aitechskill.book.document.domain.DocumentTagRecord;
import com.aitechskill.book.document.domain.entity.KnowledgeDocumentEntity;
import com.aitechskill.book.document.domain.response.DocumentDetailResponse;
import com.aitechskill.book.document.domain.response.DocumentPageResponse;
import com.aitechskill.book.document.mapper.KnowledgeDocumentMapper;
import com.aitechskill.book.storage.domain.StoredObjectContent;
import com.aitechskill.book.storage.service.ObjectStorageService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 主平台知识文档服务测试。
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeDocumentServiceTest {

    @Mock
    private KnowledgeDocumentMapper documentMapper;
    @Mock
    private ObjectStorageService objectStorageService;

    private KnowledgeDocumentService service;

    @BeforeEach
    void setUp() {
        service = new KnowledgeDocumentService(documentMapper, objectStorageService);
    }

    /** 验证列表结果包含分页、阅读时长和关联标签。 */
    @Test
    void searchesPublishedDocumentsWithTags() {
        KnowledgeDocumentEntity document = document(12L, "Spring Boot 启动流程", 1800L);
        given(documentMapper.countPublished("启动", 3L)).willReturn(1L);
        given(documentMapper.selectPublishedPage("启动", 3L, 0L, 12)).willReturn(List.of(document));
        given(documentMapper.selectTagsByDocumentIds(List.of(12L)))
                .willReturn(List.of(new DocumentTagRecord(12L, 3L, "自动配置", 3)));

        DocumentPageResponse response = service.search(" 启动 ", 3L, 1, 12);

        assertThat(response.total()).isEqualTo(1);
        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.title()).isEqualTo("Spring Boot 启动流程");
            assertThat(item.readingMinutes()).isEqualTo(2);
            assertThat(item.tags()).singleElement().satisfies(tag -> assertThat(tag.name()).isEqualTo("自动配置"));
        });
    }

    /** 验证详情正文通过对象存储读取且不会返回对象键。 */
    @Test
    void readsMarkdownFromPrivateStorage() {
        KnowledgeDocumentEntity document = document(12L, "Spring Boot 启动流程", 120L);
        document.setMarkdownObjectKey("prod/article/2026/08/owner/body.md");
        byte[] markdown = "# 启动流程".getBytes(StandardCharsets.UTF_8);
        given(documentMapper.selectPublishedById(12L)).willReturn(document);
        given(documentMapper.selectTagsByDocumentIds(List.of(12L))).willReturn(List.of());
        given(objectStorageService.get(document.getMarkdownObjectKey()))
                .willReturn(new StoredObjectContent(markdown, "text/markdown", markdown.length));

        DocumentDetailResponse response = service.getPublishedDocument(12L);

        assertThat(response.markdown()).isEqualTo("# 启动流程");
        verify(objectStorageService).get("prod/article/2026/08/owner/body.md");
    }

    /** 创建用于服务测试的文档实体。 */
    private KnowledgeDocumentEntity document(long id, String title, long markdownSize) {
        KnowledgeDocumentEntity document = new KnowledgeDocumentEntity();
        document.setId(id);
        document.setTitle(title);
        document.setSummary("测试摘要");
        document.setStatus("PUBLISHED");
        document.setMarkdownObjectKey("private-object-key");
        document.setMarkdownSize(markdownSize);
        document.setPublishedAt(LocalDateTime.of(2026, 8, 17, 12, 0));
        document.setUpdatetime(LocalDateTime.of(2026, 8, 17, 12, 0));
        return document;
    }
}
