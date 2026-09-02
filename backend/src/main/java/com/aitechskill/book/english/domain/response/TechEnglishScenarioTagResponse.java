package com.aitechskill.book.english.domain.response;

/**
 * 固定场景标签选项。
 *
 * @param code 标签代码
 * @param label 展示名称
 * @param description 标签说明
 */
public record TechEnglishScenarioTagResponse(
        String code,
        String label,
        String description) {
}
