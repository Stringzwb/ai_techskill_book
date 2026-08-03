package com.aitechskill.book.article;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeArticleRepository extends JpaRepository<KnowledgeArticle, Long> {

    List<KnowledgeArticle> findTop6ByFeaturedTrueOrderByCreatedAtDesc();

    long countByCategory(String category);
}
