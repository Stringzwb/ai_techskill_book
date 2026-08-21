package com.aitechskill.book.document.mapper;

import com.aitechskill.book.document.domain.DocumentTagRecord;
import com.aitechskill.book.document.domain.entity.KnowledgeDocumentEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Param;

/**
 * 已发布知识文档数据访问接口。
 */
public interface KnowledgeDocumentMapper extends BaseMapper<KnowledgeDocumentEntity> {

    /** 查询符合关键词和标签条件的已发布文档。 */
    List<KnowledgeDocumentEntity> selectPublishedPage(
            @Param("keyword") String keyword,
            @Param("tagId") Long tagId,
            @Param("offset") long offset,
            @Param("size") int size);

    /** 统计符合关键词和标签条件的已发布文档。 */
    long countPublished(@Param("keyword") String keyword, @Param("tagId") Long tagId);

    /** 查询一篇已发布文档。 */
    KnowledgeDocumentEntity selectPublishedById(@Param("id") long id);

    /** 批量查询文档关联标签。 */
    List<DocumentTagRecord> selectTagsByDocumentIds(@Param("documentIds") List<Long> documentIds);

    /** 查询仍需从对象存储迁移正文的文档。 */
    List<KnowledgeDocumentEntity> selectDocumentsNeedingMigration();

    /** 写入历史正文并创建对应的索引任务。 */
    int updateMigratedMarkdown(
            @Param("id") long id,
            @Param("markdownContent") String markdownContent,
            @Param("markdownSize") long markdownSize);

    /** 为文档创建幂等索引任务。 */
    int insertIndexTaskIfAbsent(
            @Param("documentId") long documentId,
            @Param("contentVersion") long contentVersion,
            @Param("taskType") String taskType);

    /** 查询索引任务对应的最新文档。 */
    KnowledgeDocumentEntity selectForIndex(@Param("id") long id);

    /** 原子抢占一个可处理的索引任务。 */
    int claimIndexTask(@Param("taskId") long taskId);

    /** 标记索引任务成功。 */
    int markIndexTaskSucceeded(@Param("taskId") long taskId);

    /** 标记索引任务失败并安排重试。 */
    int markIndexTaskFailed(
            @Param("taskId") long taskId,
            @Param("error") String error,
            @Param("availableAt") LocalDateTime availableAt);

    /** 仅在正文版本仍然匹配时更新文档索引状态。 */
    int markDocumentIndexSucceeded(
            @Param("documentId") long documentId,
            @Param("contentVersion") long contentVersion);

    /** 仅在正文版本仍然匹配时记录索引失败。 */
    int markDocumentIndexFailed(
            @Param("documentId") long documentId,
            @Param("contentVersion") long contentVersion,
            @Param("error") String error);
}
