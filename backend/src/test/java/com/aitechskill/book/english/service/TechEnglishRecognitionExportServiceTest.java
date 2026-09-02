package com.aitechskill.book.english.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.aitechskill.book.english.domain.TechEnglishRecognitionExport;
import com.aitechskill.book.english.domain.response.TechEnglishAiRecognitionItemResponse;
import com.aitechskill.book.english.domain.response.TechEnglishRecognitionHistoryDetailResponse;
import com.aitechskill.book.english.domain.response.TechEnglishRecognitionHistoryTaskResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 技术英语识图记录导出测试。
 */
@ExtendWith(MockitoExtension.class)
class TechEnglishRecognitionExportServiceTest {

    @Mock
    private TechEnglishAiRecognitionRecordService recordService;

    /** HTML 导出应保留识别内容，并提供可切换的紧凑、卡片与阅读视图。 */
    @Test
    void exportsHtmlWithoutTreatingCssPercentAsFormatSpecifier() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 9, 1, 20, 0, 0);
        TechEnglishAiRecognitionItemResponse item = new TechEnglishAiRecognitionItemResponse(
                "item-1", 1, "VOCABULARY", "resilient <word>", "adjective", "有韧性的",
                "/r/", "/r/", null, null, List.of(), List.of());
        TechEnglishRecognitionHistoryTaskResponse task = new TechEnglishRecognitionHistoryTaskResponse(
                "batch-1", "RECOGNIZED", 1, 1, 1, 1, null, null,
                createdAt, createdAt, null, List.of(item));
        TechEnglishRecognitionHistoryDetailResponse detail = new TechEnglishRecognitionHistoryDetailResponse(
                "session-1", "RECOGNIZED", "测试来源", "测试场景", 1, 1,
                createdAt, createdAt, List.of(task));
        given(recordService.detail(7L, "session-1")).willReturn(detail);

        TechEnglishRecognitionExport export = new TechEnglishRecognitionExportService(recordService)
                .export(7L, "session-1", "html");
        String html = new String(export.content(), java.nio.charset.StandardCharsets.UTF_8);

        assertThat(export.contentType()).isEqualTo("text/html;charset=UTF-8");
        assertThat(html)
                .contains("body class=\"view-compact\"")
                .contains("data-view=\"compact\"")
                .contains("data-view=\"cards\"")
                .contains("data-view=\"reading\"")
                .contains("来源：测试来源")
                .contains("resilient &lt;word&gt;")
                .doesNotContain("MissingFormatWidthException");
    }
}
