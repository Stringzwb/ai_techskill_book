package com.aitechskill.book.english.domain.request;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 技术英语语料详情页标签更新请求。
 */
public record TechEnglishCorpusMetadataUpdateRequest(
        @Size(max = 20) List<@Positive Long> tagIds,
        @Size(max = 4) List<@Size(max = 64) String> scenarioTagCodes) {
}
