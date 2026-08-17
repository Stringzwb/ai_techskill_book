package com.aitechskill.book.document.domain;

/**
 * 文档关联标签查询记录。
 *
 * @param documentId 文档主键
 * @param id 标签主键
 * @param name 标签名称
 * @param level 标签层级
 */
public record DocumentTagRecord(long documentId, long id, String name, int level) {
}
