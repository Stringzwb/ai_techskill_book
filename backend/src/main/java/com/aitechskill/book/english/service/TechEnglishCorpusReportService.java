package com.aitechskill.book.english.service;

import com.aitechskill.book.common.exception.BusinessException;
import com.aitechskill.book.document.domain.response.DocumentTagResponse;
import com.aitechskill.book.english.domain.TechEnglishRecognitionExport;
import com.aitechskill.book.english.domain.response.TechEnglishCorpusDetailResponse;
import com.aitechskill.book.english.domain.response.TechEnglishKeyVocabularyResponse;
import com.aitechskill.book.english.domain.response.TechEnglishPatternExampleResponse;
import com.aitechskill.book.english.domain.response.TechEnglishScenarioTagResponse;
import com.aitechskill.book.english.domain.response.TechEnglishVocabularyExampleResponse;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 根据用户选中的技术英语语料生成 HTML 或 PDF 学习报告。
 */
@Service
public class TechEnglishCorpusReportService {

    private static final int MAX_REPORT_ITEMS = 100;
    private static final DateTimeFormatter FILE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter DISPLAY_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TechEnglishCorpusService corpusService;

    public TechEnglishCorpusReportService(TechEnglishCorpusService corpusService) {
        this.corpusService = corpusService;
    }

    /** 导出选中语料。 */
    public TechEnglishRecognitionExport export(List<Long> ids, String format) {
        List<Long> normalizedIds = normalizeIds(ids);
        List<TechEnglishCorpusDetailResponse> corpus = normalizedIds.stream()
                .map(corpusService::getPublishedCorpus)
                .toList();
        String html = html(corpus);
        String normalizedFormat = format == null ? "html" : format.trim().toLowerCase(Locale.ROOT);
        String suffix = FILE_TIME_FORMATTER.format(LocalDateTime.now());
        if ("html".equals(normalizedFormat)) {
            return new TechEnglishRecognitionExport(
                    "tech-english-corpus-report-" + suffix + ".html",
                    "text/html;charset=UTF-8",
                    html.getBytes(StandardCharsets.UTF_8));
        }
        if ("pdf".equals(normalizedFormat)) {
            return new TechEnglishRecognitionExport(
                    "tech-english-corpus-report-" + suffix + ".pdf",
                    "application/pdf",
                    pdf(html));
        }
        throw new BusinessException(HttpStatus.BAD_REQUEST,
                "TECH_ENGLISH_REPORT_FORMAT_INVALID", "报告格式仅支持 HTML 或 PDF");
    }

    /** 限制报告规模，保持生成和下载可控。 */
    private List<Long> normalizeIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "TECH_ENGLISH_REPORT_IDS_REQUIRED", "请选择要生成报告的语料");
        }
        List<Long> normalized = new LinkedHashSet<>(ids).stream()
                .filter(id -> id != null && id > 0)
                .toList();
        if (normalized.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "TECH_ENGLISH_REPORT_IDS_REQUIRED", "请选择要生成报告的语料");
        }
        if (normalized.size() > MAX_REPORT_ITEMS) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "TECH_ENGLISH_REPORT_TOO_MANY", "单次最多选择 100 条语料生成报告");
        }
        return normalized;
    }

    /** 将报告 HTML 渲染成 PDF。 */
    private byte[] pdf(String html) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            registerFont(builder);
            builder.withHtmlContent(html, null);
            builder.toStream(output);
            builder.run();
            return output.toByteArray();
        } catch (Exception exception) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "TECH_ENGLISH_REPORT_PDF_FAILED", "PDF 报告生成失败", exception);
        }
    }

    /** 尽量注册常见中文字体，避免 PDF 中文缺字。 */
    private void registerFont(PdfRendererBuilder builder) {
        List<String> candidates = List.of(
                "/usr/share/fonts/opentype/noto/NotoSansCJKsc-Regular.otf",
                "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.otf",
                "/usr/share/fonts/truetype/noto/NotoSansSC-Regular.ttf",
                "/usr/share/fonts/truetype/wqy/wqy-microhei.ttf");
        for (String path : candidates) {
            File font = new File(path);
            if (font.isFile()) {
                builder.useFont(font, "Noto Sans CJK SC");
                return;
            }
        }
    }

    /** 生成适合浏览和打印的报告 HTML。 */
    private String html(List<TechEnglishCorpusDetailResponse> corpus) {
        StringBuilder cards = new StringBuilder();
        for (int index = 0; index < corpus.size(); index += 1) {
            appendCard(cards, index + 1, corpus.get(index));
        }
        return """
                <!doctype html><html lang="zh-CN"><head><meta charset="utf-8">
                <meta name="viewport" content="width=device-width,initial-scale=1">
                <title>技术英语语料报告</title><style>
                @page{size:A4;margin:14mm}
                :root{--ink:#202633;--muted:#667085;--line:#d8dee8;--paper:#f7f9fc;--green:#0f8f72;--blue:#2957c8;--rose:#b44765}
                *{box-sizing:border-box}body{margin:0;background:var(--paper);color:var(--ink);font:14px/1.62 "Noto Sans CJK SC","PingFang SC","Microsoft YaHei",Arial,sans-serif}main{width:min(980px,calc(100% - 28px));margin:24px auto 48px}
                .hero{padding:22px 0 14px;border-bottom:2px solid #202633}.hero small{color:var(--muted);font-size:12px;letter-spacing:.12em}.hero h1{margin:4px 0;font-size:34px;line-height:1.1;letter-spacing:0}.hero p{margin:0;color:var(--muted)}.summary{display:flex;gap:8px;flex-wrap:wrap;margin-top:12px}.summary span{padding:4px 9px;border:1px solid var(--line);border-radius:999px;background:#fff;font-size:12px}
                .card{break-inside:avoid;margin-top:12px;padding:15px 16px;border:1px solid var(--line);border-radius:8px;background:#fff}.card header{display:flex;align-items:center;gap:8px}.num{font-weight:800;color:#98a2b3}.badge{padding:2px 7px;border-radius:999px;font-size:11px;font-weight:800}.vocabulary{color:#145b49;background:#dbf5ee}.phrase{color:#8a4b07;background:#fff2cf}.sentence{color:#2148ae;background:#e7edff}.article{color:#913552;background:#ffe6ee}.tags{display:flex;gap:5px;flex-wrap:wrap;margin-left:auto}.tags span{padding:2px 6px;border-radius:6px;background:#f0f3f8;color:#475467;font-size:11px}
                h2{margin:9px 0 6px;font:700 22px/1.28 Georgia,"Times New Roman","Noto Sans CJK SC",serif;letter-spacing:0}.meta{display:flex;gap:6px;flex-wrap:wrap}.meta span{padding:2px 7px;border-radius:6px;background:#f4f6fa;color:#485467;font-size:12px}section{margin-top:10px}label{display:block;margin-bottom:2px;color:var(--muted);font-size:11px;font-weight:800}p{margin:0}.examples{margin:3px 0 0;padding-left:20px}.examples li{padding:2px 0}.keywords{display:flex;gap:5px;flex-wrap:wrap}.keywords span{padding:3px 7px;border-radius:6px;background:#f4f6fa}.footer{margin-top:18px;color:var(--muted);font-size:12px;text-align:right}
                @media print{body{background:#fff}main{width:100%;margin:0}.card{box-shadow:none}.hero{padding-top:0}}
                </style></head><body><main><header class="hero"><small>TECH ENGLISH CORPUS REPORT</small><h1>技术英语语料报告</h1><p>生成时间：__CREATED_AT__</p><div class="summary"><span>共 __COUNT__ 条语料</span><span>最多支持 100 条</span></div></header>__CARDS__<p class="footer">Generated by AI TechSkill Book</p></main></body></html>
                """
                .replace("__CREATED_AT__", DISPLAY_TIME_FORMATTER.format(LocalDateTime.now()))
                .replace("__COUNT__", String.valueOf(corpus.size()))
                .replace("__CARDS__", cards.toString());
    }

    /** 追加单条语料卡片。 */
    private void appendCard(StringBuilder output, int index, TechEnglishCorpusDetailResponse item) {
        output.append("<article class=\"card\"><header><span class=\"num\">")
                .append(String.format("%02d", index)).append("</span><span class=\"badge ")
                .append(typeClass(item.corpusType())).append("\">")
                .append(typeLabel(item.corpusType())).append("</span><div class=\"tags\">");
        for (TechEnglishScenarioTagResponse tag : item.scenarioTags()) {
            output.append("<span>").append(htmlText(tag.label())).append("</span>");
        }
        for (DocumentTagResponse tag : item.knowledgeTags()) {
            output.append("<span>").append(htmlText(tag.name())).append("</span>");
        }
        output.append("</div></header><h2>")
                .append(htmlText(firstText(item.englishText(), item.title()))).append("</h2>");
        appendMeta(output, item);
        appendSection(output, "释义 / 翻译", firstText(item.translationText(), item.explanation()));
        appendSection(output, "句式框架", item.sentencePattern());
        appendSection(output, "句式解析", item.sentencePatternExplanation());
        if (!item.keyVocabulary().isEmpty()) {
            output.append("<section><label>重点词汇</label><div class=\"keywords\">");
            for (TechEnglishKeyVocabularyResponse word : item.keyVocabulary()) {
                output.append("<span><b>").append(htmlText(word.word())).append("</b>");
                if (StringUtils.hasText(word.partOfSpeech())) {
                    output.append(" · ").append(htmlText(word.partOfSpeech()));
                }
                if (StringUtils.hasText(word.meaning())) {
                    output.append(" · ").append(htmlText(word.meaning()));
                }
                output.append("</span>");
            }
            output.append("</div></section>");
        }
        if (!item.vocabularyExamples().isEmpty()) {
            output.append("<section><label>例句</label><ol class=\"examples\">");
            for (TechEnglishVocabularyExampleResponse example : item.vocabularyExamples()) {
                appendExample(output, example.englishText(), example.translationText());
            }
            output.append("</ol></section>");
        }
        if (!item.patternExamples().isEmpty()) {
            output.append("<section><label>句式例句</label><ol class=\"examples\">");
            for (TechEnglishPatternExampleResponse example : item.patternExamples()) {
                appendExample(output, example.englishText(), example.translationText());
            }
            output.append("</ol></section>");
        }
        output.append("</article>");
    }

    /** 追加音标、场景和来源等紧凑信息。 */
    private void appendMeta(StringBuilder output, TechEnglishCorpusDetailResponse item) {
        output.append("<div class=\"meta\">");
        appendChip(output, item.partOfSpeech());
        appendChip(output, prefixed("英 ", item.britishPhonetic()));
        appendChip(output, prefixed("美 ", item.americanPhonetic()));
        appendChip(output, prefixed("场景 ", item.scenario()));
        appendChip(output, item.sourceName());
        output.append("</div>");
    }

    private void appendSection(StringBuilder output, String label, String value) {
        if (StringUtils.hasText(value)) {
            output.append("<section><label>").append(htmlText(label)).append("</label><p>")
                    .append(htmlText(value)).append("</p></section>");
        }
    }

    private void appendExample(StringBuilder output, String englishText, String translationText) {
        output.append("<li><p>").append(htmlText(englishText)).append("</p>");
        if (StringUtils.hasText(translationText)) {
            output.append("<small>").append(htmlText(translationText)).append("</small>");
        }
        output.append("</li>");
    }

    private void appendChip(StringBuilder output, String value) {
        if (StringUtils.hasText(value)) {
            output.append("<span>").append(htmlText(value)).append("</span>");
        }
    }

    private String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private String prefixed(String prefix, String value) {
        return StringUtils.hasText(value) ? prefix + value : null;
    }

    private String typeClass(String corpusType) {
        return switch (corpusType) {
            case "PHRASE" -> "phrase";
            case "SENTENCE" -> "sentence";
            case "ARTICLE" -> "article";
            default -> "vocabulary";
        };
    }

    private String typeLabel(String corpusType) {
        return switch (corpusType) {
            case "PHRASE" -> "PHRASE · 短语";
            case "SENTENCE" -> "PATTERN · 句式";
            case "ARTICLE" -> "ARTICLE · 文章";
            default -> "VOCABULARY · 词汇";
        };
    }

    private String htmlText(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
