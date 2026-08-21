package com.aitechskill.book.bootstrap;

import com.aitechskill.book.document.domain.entity.KnowledgeDocumentEntity;
import com.aitechskill.book.document.mapper.KnowledgeDocumentMapper;
import com.aitechskill.book.storage.service.ObjectStorageService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 将历史对象存储 Markdown 一次性迁移到 MySQL。
 *
 * <p>仅在显式开启配置时运行，迁移完成后应关闭该配置。</p>
 */
@Component
@Order(10)
@ConditionalOnProperty(name = "app.document.migrate-object-storage-on-startup", havingValue = "true")
public class MarkdownStorageMigrationRunner implements CommandLineRunner {

    private final KnowledgeDocumentMapper documentMapper;
    private final ObjectStorageService objectStorageService;

    public MarkdownStorageMigrationRunner(
            KnowledgeDocumentMapper documentMapper,
            ObjectStorageService objectStorageService) {
        this.documentMapper = documentMapper;
        this.objectStorageService = objectStorageService;
    }

    /** 迁移所有仍未写入数据库的历史正文。 */
    @Override
    @Transactional
    public void run(String... args) {
        List<KnowledgeDocumentEntity> documents = documentMapper.selectDocumentsNeedingMigration();
        for (KnowledgeDocumentEntity document : documents) {
            byte[] content = objectStorageService.get(document.getMarkdownObjectKey()).content();
            String markdown = new String(content, StandardCharsets.UTF_8);
            documentMapper.updateMigratedMarkdown(document.getId(), markdown, content.length);
            String taskType = "PUBLISHED".equals(document.getStatus()) ? "UPSERT" : "DELETE";
            documentMapper.insertIndexTaskIfAbsent(
                    document.getId(),
                    document.getContentVersion() == null ? 1L : document.getContentVersion(),
                    taskType);
        }
    }
}
