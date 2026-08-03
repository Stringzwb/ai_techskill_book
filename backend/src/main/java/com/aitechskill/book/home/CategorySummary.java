package com.aitechskill.book.home;

public record CategorySummary(
        String name,
        String code,
        String description,
        long articleCount) {
}
