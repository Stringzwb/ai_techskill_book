package com.aitechskill.book.english.domain.request;

import java.util.List;

/**
 * 确认入库时一条识图语料的独立标签选择。
 *
 * @param itemKey 识图语料稳定标识
 * @param tagIds 该语料的知识标签
 */
public record TechEnglishAiItemTagAssignment(String itemKey, List<Long> tagIds) {
}
