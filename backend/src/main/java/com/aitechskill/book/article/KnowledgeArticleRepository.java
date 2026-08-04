package com.aitechskill.book.article;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 知识专题数据访问接口。
 */
public interface KnowledgeArticleRepository extends JpaRepository<KnowledgeArticle, Long> {

    /** 查询最新六篇精选专题。 */
    List<KnowledgeArticle> findTop6ByFeaturedTrueOrderByCreatedAtDesc();

    /** 按分类统计专题数量。 */
    long countByCategory(String category);
}
