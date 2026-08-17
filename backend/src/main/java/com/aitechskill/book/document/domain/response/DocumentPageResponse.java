package com.aitechskill.book.document.domain.response;

import java.util.List;

/**
 * 已发布文档分页响应。
 *
 * @param total 筛选后的文档总数
 * @param page 当前页码
 * @param size 每页数量
 * @param totalPages 总页数
 * @param items 当前页文档
 */
public record DocumentPageResponse(
        long total,
        int page,
        int size,
        int totalPages,
        List<DocumentSummaryResponse> items) {
}
