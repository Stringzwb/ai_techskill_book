package com.aitechskill.book.article;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "knowledge_articles")
public class KnowledgeArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 600)
    private String summary;

    @Column(nullable = false, length = 40)
    private String category;

    @Column(nullable = false, length = 20)
    private String difficulty;

    @Column(nullable = false)
    private int readMinutes;

    @Column(nullable = false, length = 240)
    private String tags;

    @Column(nullable = false)
    private boolean featured;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected KnowledgeArticle() {
    }

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

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getCategory() {
        return category;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public int getReadMinutes() {
        return readMinutes;
    }

    public String getTags() {
        return tags;
    }

    public boolean isFeatured() {
        return featured;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
