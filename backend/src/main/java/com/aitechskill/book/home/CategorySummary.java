package com.aitechskill.book.home;

/**
 * 首页知识分类摘要。
 */
public record CategorySummary(
        String name,
        String code,
        String description,
        long articleCount) {
}
