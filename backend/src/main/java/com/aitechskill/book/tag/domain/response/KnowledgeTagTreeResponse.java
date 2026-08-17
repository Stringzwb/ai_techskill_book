package com.aitechskill.book.tag.domain.response;

import java.util.List;

/**
 * 面向标签选择组件的知识标签树节点。
 *
 * @param id 标签主键
 * @param name 标签名称
 * @param level 标签层级
 * @param sortOrder 同级排序值
 * @param description 标签说明
 * @param children 子标签
 */
public record KnowledgeTagTreeResponse(
        long id,
        String name,
        int level,
        int sortOrder,
        String description,
        List<KnowledgeTagTreeResponse> children) {
}
