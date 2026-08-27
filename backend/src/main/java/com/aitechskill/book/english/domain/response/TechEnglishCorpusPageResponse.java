package com.aitechskill.book.english.domain.response;

import java.util.List;

/**
 * 主平台技术英语语料分页响应。
 *
 * @param total 总记录数
 * @param page 当前页码
 * @param size 每页数量
 * @param totalPages 总页数
 * @param items 当前页语料
 */
public record TechEnglishCorpusPageResponse(
        long total,
        int page,
        int size,
        int totalPages,
        List<TechEnglishCorpusSummaryResponse> items) {
}
