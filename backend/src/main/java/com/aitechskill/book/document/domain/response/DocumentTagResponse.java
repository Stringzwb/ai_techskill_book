package com.aitechskill.book.document.domain.response;

/**
 * 文档标签摘要。
 *
 * @param id 标签主键
 * @param name 标签名称
 * @param level 标签层级
 */
public record DocumentTagResponse(long id, String name, int level) {
}
