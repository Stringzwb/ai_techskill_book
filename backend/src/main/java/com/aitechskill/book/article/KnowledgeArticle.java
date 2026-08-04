package com.aitechskill.book.article;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;

/**
 * 知识专题实体。
 */
@Entity
@Getter
@Table(name = "knowledge_articles")
public class KnowledgeArticle {

    /** 专题主键。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 专题标题。 */
    @Column(nullable = false, length = 160)
    private String title;

    /** 专题摘要。 */
    @Column(nullable = false, length = 600)
    private String summary;

    /** 所属分类。 */
    @Column(nullable = false, length = 40)
    private String category;

    /** 难度级别。 */
    @Column(nullable = false, length = 20)
    private String difficulty;

    /** 预计阅读分钟数。 */
    @Column(nullable = false)
    private int readMinutes;

    /** 逗号分隔的标签。 */
    @Column(nullable = false, length = 240)
    private String tags;

    /** 是否精选。 */
    @Column(nullable = false)
    private boolean featured;

    /** 创建时间。 */
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected KnowledgeArticle() {
    }

    /**
     * 创建知识专题。
     */
    public KnowledgeArticle(
            String title,
            String summary,
            String category,
            String difficulty,
            int readMinutes,
            String tags,
            boolean featured) {
        this.title = title;
        this.summary = summary;
        this.category = category;
        this.difficulty = difficulty;
        this.readMinutes = readMinutes;
        this.tags = tags;
        this.featured = featured;
        this.createdAt = Instant.now();
    }
}
