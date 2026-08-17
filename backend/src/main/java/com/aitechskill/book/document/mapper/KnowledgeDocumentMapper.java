package com.aitechskill.book.document.mapper;

import com.aitechskill.book.document.domain.DocumentTagRecord;
import com.aitechskill.book.document.domain.entity.KnowledgeDocumentEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
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
}
