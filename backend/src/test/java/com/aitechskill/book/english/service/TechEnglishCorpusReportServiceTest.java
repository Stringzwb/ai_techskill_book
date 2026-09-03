package com.aitechskill.book.english.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.aitechskill.book.english.domain.TechEnglishRecognitionExport;
import com.aitechskill.book.english.domain.response.TechEnglishCorpusDetailResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 技术英语语料报告导出测试。
 */
@ExtendWith(MockitoExtension.class)
class TechEnglishCorpusReportServiceTest {

    @Mock
    private TechEnglishCorpusService corpusService;

    /** PDF 导出应返回有效 PDF 文件内容。 */
    @Test
    void exportsSelectedCorpusAsPdf() throws Exception {
        given(corpusService.getPublishedCorpus(101L)).willReturn(detail());

        TechEnglishRecognitionExport export = new TechEnglishCorpusReportService(corpusService)
                .export(List.of(101L), "pdf");

        assertThat(export.contentType()).isEqualTo("application/pdf");
        assertThat(new String(export.content(), 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
        try (PDDocument document = PDDocument.load(export.content())) {
            assertThat(new PDFTextStripper().getText(document))
                    .contains("/ˌθerəˈpiː/")
                    .contains("/ˈθerəpi/ · ə ɜː θ ð ʃ ʒ ŋ æ ɪ ʊ ɔː ɑː ˈ ˌ");
        }
    }

    private TechEnglishCorpusDetailResponse detail() {
        return new TechEnglishCorpusDetailResponse(
                101L, "PHRASE", "zero downtime", "zero downtime", null,
                "noun phrase", "/ˌθerəˈpiː/", "/ˈθerəpi/ · ə ɜː θ ð ʃ ʒ ŋ æ ɪ ʊ ɔː ɑː ˈ ˌ",
                "deployment phrase", null, null,
                null, null, "薄荷阅读", null, "release", List.of(),
                "INTERMEDIATE", null, "零停机", null, null, List.of(),
                List.of(), null, null, List.of(), List.of());
    }
}
