package com.aitechskill.book.article;

import java.util.Arrays;
import java.util.List;

public record ArticleSummary(
        Long id,
        String title,
        String summary,
        String category,
        String difficulty,
        int readMinutes,
        List<String> tags) {

    public static ArticleSummary from(KnowledgeArticle article) {
        List<String> tagList = Arrays.stream(article.getTags().split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
        return new ArticleSummary(
                article.getId(),
                article.getTitle(),
                article.getSummary(),
                article.getCategory(),
                article.getDifficulty(),
                article.getReadMinutes(),
                tagList);
    }
}
