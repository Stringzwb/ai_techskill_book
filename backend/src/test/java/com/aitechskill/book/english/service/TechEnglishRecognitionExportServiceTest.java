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

    /** HTML 导出应使用任务名称标题，并保留紧凑两栏模板中的识别内容。 */
    @Test
    void exportsHtmlWithoutTreatingCssPercentAsFormatSpecifier() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 9, 1, 20, 0, 0);
        TechEnglishAiRecognitionItemResponse item = new TechEnglishAiRecognitionItemResponse(
                "item-1", 1, "VOCABULARY", "resilient <word>", "adjective", "有韧性的",
                "/r/", "/r/", null, null, List.of(), List.of(), List.of());
        TechEnglishRecognitionHistoryTaskResponse task = new TechEnglishRecognitionHistoryTaskResponse(
                "batch-1", "RECOGNIZED", 1, 1, 1, 1, null, null,
                createdAt, createdAt, null, List.of(item));
        TechEnglishRecognitionHistoryDetailResponse detail = new TechEnglishRecognitionHistoryDetailResponse(
                "session-1", "RECOGNIZED", "测试来源", "测试任务", "测试场景", 1, 1,
                createdAt, createdAt, List.of(task));
        given(recordService.detail(7L, "session-1")).willReturn(detail);

        TechEnglishRecognitionExport export = new TechEnglishRecognitionExportService(recordService)
                .export(7L, "session-1", "html");
        String html = new String(export.content(), java.nio.charset.StandardCharsets.UTF_8);

        assertThat(export.contentType()).isEqualTo("text/html;charset=UTF-8");
        assertThat(html)
                .contains("<title>测试任务</title>")
                .contains("<h1>测试任务</h1>")
                .contains("grid-template-columns:repeat(2")
                .contains("来源：测试来源")
                .contains("resilient &lt;word&gt;")
                .doesNotContain("view-switch")
                .doesNotContain("MissingFormatWidthException");
    }

    /** HTML 导出应跨识别分组按单词、词组、句子稳定排序。 */
    @Test
    void sortsHtmlItemsByCorpusType() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 9, 1, 20, 0, 0);
        TechEnglishAiRecognitionItemResponse sentence = item("sentence", 1, "SENTENCE", "a sentence");
        TechEnglishAiRecognitionItemResponse phrase = item("phrase", 1, "PHRASE", "a phrase");
        TechEnglishAiRecognitionItemResponse word = item("word", 1, "VOCABULARY", "a word");
        TechEnglishRecognitionHistoryTaskResponse task = new TechEnglishRecognitionHistoryTaskResponse(
                "batch-1", "RECOGNIZED", 1, 1, 3, 3, null, null,
                createdAt, createdAt, null, List.of(sentence, phrase, word));
        TechEnglishRecognitionHistoryDetailResponse detail = new TechEnglishRecognitionHistoryDetailResponse(
                "session-1", "RECOGNIZED", "测试来源", "排序任务", null, 3, 3,
                createdAt, createdAt, List.of(task));
        given(recordService.detail(7L, "session-1")).willReturn(detail);

        String html = new String(new TechEnglishRecognitionExportService(recordService)
                .export(7L, "session-1", "html").content(), java.nio.charset.StandardCharsets.UTF_8);

        assertThat(html.indexOf("a word")).isLessThan(html.indexOf("a phrase"));
        assertThat(html.indexOf("a phrase")).isLessThan(html.indexOf("a sentence"));
    }

    /** PDF 和图片导出应返回对应的可打开文件格式。 */
    @Test
    void exportsPdfAndPngFromTheSameHtmlTemplate() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 9, 1, 20, 0, 0);
        TechEnglishAiRecognitionItemResponse item = item("item-1", 1, "VOCABULARY", "resilient");
        TechEnglishRecognitionHistoryTaskResponse task = new TechEnglishRecognitionHistoryTaskResponse(
                "batch-1", "RECOGNIZED", 1, 1, 1, 1, null, null,
                createdAt, createdAt, null, List.of(item));
        TechEnglishRecognitionHistoryDetailResponse detail = new TechEnglishRecognitionHistoryDetailResponse(
                "session-1", "RECOGNIZED", "测试来源", "导出任务", null, 1, 1,
                createdAt, createdAt, List.of(task));
        given(recordService.detail(7L, "session-1")).willReturn(detail);
        TechEnglishRecognitionExportService service = new TechEnglishRecognitionExportService(recordService);

        TechEnglishRecognitionExport pdf = service.export(7L, "session-1", "pdf");
        TechEnglishRecognitionExport image = service.export(7L, "session-1", "image");

        assertThat(pdf.contentType()).isEqualTo("application/pdf");
        assertThat(new String(pdf.content(), 0, 4, java.nio.charset.StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
        assertThat(image.contentType()).isEqualTo("image/png");
        assertThat(new String(image.content(), 1, 3, java.nio.charset.StandardCharsets.ISO_8859_1)).isEqualTo("PNG");
    }

    private TechEnglishAiRecognitionItemResponse item(String key, int imageIndex, String type, String text) {
        return new TechEnglishAiRecognitionItemResponse(
                key, imageIndex, type, text, null, null, null, null, null, null,
                List.of(), List.of(), List.of());
    }
}
