package com.aitechskill.book.english.service;

import com.aitechskill.book.common.exception.BusinessException;
import com.aitechskill.book.english.domain.TechEnglishRecognitionExport;
import com.aitechskill.book.english.domain.response.TechEnglishAiRecognitionItemResponse;
import com.aitechskill.book.english.domain.response.TechEnglishKeyVocabularyResponse;
import com.aitechskill.book.english.domain.response.TechEnglishPatternExampleResponse;
import com.aitechskill.book.english.domain.response.TechEnglishRecognitionHistoryDetailResponse;
import com.aitechskill.book.english.domain.response.TechEnglishRecognitionHistoryTaskResponse;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 将一次识图会话的词汇、句子和 AI 补充内容导出为 Markdown 或 HTML。
 */
@Service
public class TechEnglishRecognitionExportService {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TechEnglishAiRecognitionRecordService recordService;

    public TechEnglishRecognitionExportService(TechEnglishAiRecognitionRecordService recordService) {
        this.recordService = recordService;
    }

    /** 导出当前用户的指定识图会话。 */
    public TechEnglishRecognitionExport export(long userId, String sessionUuid, String format) {
        TechEnglishRecognitionHistoryDetailResponse detail = recordService.detail(userId, sessionUuid);
        String normalized = format == null ? "markdown" : format.trim().toLowerCase(Locale.ROOT);
        String safeSession = sessionUuid.replaceAll("[^a-zA-Z0-9-]", "");
        if ("markdown".equals(normalized) || "md".equals(normalized)) {
            return new TechEnglishRecognitionExport(
                    "tech-english-recognition-" + safeSession + ".md",
                    "text/markdown;charset=UTF-8",
                    markdown(detail.sourceName(), detail.scenario(), detail.createdAt(),
                            detail.imageCount(), detail.itemCount(), detail.status(), detail.tasks())
                            .getBytes(StandardCharsets.UTF_8));
        }
        if ("html".equals(normalized)) {
            return new TechEnglishRecognitionExport(
                    "tech-english-recognition-" + safeSession + ".html",
                    "text/html;charset=UTF-8",
                    html(detail.batchName(), detail.sourceName(), detail.scenario(), detail.createdAt(),
                            detail.imageCount(), detail.itemCount(), detail.status(), detail.tasks())
                            .getBytes(StandardCharsets.UTF_8));
        }
        if ("pdf".equals(normalized)) {
            return new TechEnglishRecognitionExport(
                    "tech-english-recognition-" + safeSession + ".pdf",
                    "application/pdf",
                    pdf(html(detail.batchName(), detail.sourceName(), detail.scenario(), detail.createdAt(),
                            detail.imageCount(), detail.itemCount(), detail.status(), detail.tasks())));
        }
        if ("image".equals(normalized) || "png".equals(normalized)) {
            return new TechEnglishRecognitionExport(
                    "tech-english-recognition-" + safeSession + ".png",
                    "image/png",
                    image(html(detail.batchName(), detail.sourceName(), detail.scenario(), detail.createdAt(),
                            detail.imageCount(), detail.itemCount(), detail.status(), detail.tasks())));
        }
        throw new BusinessException(HttpStatus.BAD_REQUEST,
                "TECH_ENGLISH_EXPORT_FORMAT_INVALID", "导出格式仅支持 Markdown、HTML、PDF 或图片");
    }

    /** 导出当前用户的一次识图批次。 */
    public TechEnglishRecognitionExport exportBatch(
            long userId,
            String sessionUuid,
            String batchUuid,
            String format) {
        TechEnglishRecognitionHistoryDetailResponse detail = recordService.detail(userId, sessionUuid);
        TechEnglishRecognitionHistoryTaskResponse task = detail.tasks().stream()
                .filter(item -> batchUuid.equals(item.batchUuid()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "TECH_ENGLISH_RECOGNITION_HISTORY_NOT_FOUND", "识图记录不存在"));
        String normalized = format == null ? "markdown" : format.trim().toLowerCase(Locale.ROOT);
        String safeSession = sessionUuid.replaceAll("[^a-zA-Z0-9-]", "");
        String safeBatch = batchUuid.replaceAll("[^a-zA-Z0-9-]", "");
        if ("markdown".equals(normalized) || "md".equals(normalized)) {
            return new TechEnglishRecognitionExport(
                    "tech-english-recognition-" + safeSession + "-" + safeBatch + ".md",
                    "text/markdown;charset=UTF-8",
                    markdown(detail.sourceName(), detail.scenario(), task.createdAt(),
                            task.imageCount(), task.itemCount(), task.status(), List.of(task))
                            .getBytes(StandardCharsets.UTF_8));
        }
        if ("html".equals(normalized)) {
            return new TechEnglishRecognitionExport(
                    "tech-english-recognition-" + safeSession + "-" + safeBatch + ".html",
                    "text/html;charset=UTF-8",
                    html(detail.batchName(), detail.sourceName(), detail.scenario(), task.createdAt(),
                            task.imageCount(), task.itemCount(), task.status(), List.of(task))
                            .getBytes(StandardCharsets.UTF_8));
        }
        if ("pdf".equals(normalized)) {
            return new TechEnglishRecognitionExport(
                    "tech-english-recognition-" + safeSession + "-" + safeBatch + ".pdf",
                    "application/pdf",
                    pdf(html(detail.batchName(), detail.sourceName(), detail.scenario(), task.createdAt(),
                            task.imageCount(), task.itemCount(), task.status(), List.of(task))));
        }
        if ("image".equals(normalized) || "png".equals(normalized)) {
            return new TechEnglishRecognitionExport(
                    "tech-english-recognition-" + safeSession + "-" + safeBatch + ".png",
                    "image/png",
                    image(html(detail.batchName(), detail.sourceName(), detail.scenario(), task.createdAt(),
                            task.imageCount(), task.itemCount(), task.status(), List.of(task))));
        }
        throw new BusinessException(HttpStatus.BAD_REQUEST,
                "TECH_ENGLISH_EXPORT_FORMAT_INVALID", "导出格式仅支持 Markdown、HTML、PDF 或图片");
    }

    /** 生成层次清晰的 Markdown 学习文档。 */
    private String markdown(String sourceName,
            String scenario,
            java.time.LocalDateTime createdAt,
            int imageCount,
            int itemCount,
            String status,
            List<TechEnglishRecognitionHistoryTaskResponse> tasks) {
        StringBuilder output = new StringBuilder();
        output.append("# 技术英语 AI 识图记录\n\n")
                .append("> 来源：").append(markdownText(sourceName))
                .append("｜创建时间：").append(createdAt.format(TIME_FORMATTER))
                .append("｜图片：").append(imageCount)
                .append("｜语料：").append(itemCount)
                .append("｜状态：").append(markdownText(status)).append("\n\n");
        if (StringUtils.hasText(scenario)) {
            output.append("**例句场景：** ").append(markdownText(scenario)).append("\n\n");
        }
        for (TechEnglishRecognitionHistoryTaskResponse task : tasks) {
            output.append("## 识别子任务 ").append(task.chunkIndex())
                    .append(" / ").append(task.chunkCount()).append("\n\n");
            if ("FAILED".equals(task.status())) {
                output.append("> 识别失败：").append(markdownText(task.errorMessage())).append("\n\n");
                continue;
            }
            int itemNumber = 1;
            for (TechEnglishAiRecognitionItemResponse item : task.items()) {
                output.append("### ").append(itemNumber).append(". ")
                        .append(markdownText(item.englishText())).append("\n\n");
                output.append("- **类型：** ")
                        .append(typeLabel(item.corpusType()))
                        .append("\n- **来源截图：** ").append(item.sourceImageIndex()).append("\n");
                if (!item.scenarioTags().isEmpty()) {
                    output.append("- **场景标签：** ")
                            .append(item.scenarioTags().stream()
                                    .map(tag -> markdownText(tag.label()))
                                    .collect(java.util.stream.Collectors.joining("、")))
                            .append("\n");
                }
                appendMarkdownField(output, "词性", item.partOfSpeech());
                appendMarkdownField(output, "英式音标", item.britishPhonetic());
                appendMarkdownField(output, "美式音标", item.americanPhonetic());
                appendMarkdownField(output, "释义 / 翻译", item.translationText());
                appendMarkdownField(output, "经典句式", item.sentencePattern());
                appendMarkdownField(output, "句式解析", item.sentencePatternExplanation());
                if (!item.keyVocabulary().isEmpty()) {
                    output.append("\n**重点词汇**\n\n");
                    for (TechEnglishKeyVocabularyResponse word : item.keyVocabulary()) {
                        output.append("- ").append(markdownText(word.word()));
                        if (StringUtils.hasText(word.partOfSpeech())) {
                            output.append(" · ").append(markdownText(word.partOfSpeech()));
                        }
                        if (StringUtils.hasText(word.meaning())) {
                            output.append(" — ").append(markdownText(word.meaning()));
                        }
                        output.append("\n");
                    }
                }
                if (!item.examples().isEmpty()) {
                    output.append("\n**AI 补充例句**\n\n");
                    for (TechEnglishPatternExampleResponse example : item.examples()) {
                        output.append("- ").append(markdownText(example.englishText()));
                        if (StringUtils.hasText(example.translationText())) {
                            output.append("  \n  ").append(markdownText(example.translationText()));
                        }
                        output.append("\n");
                    }
                }
                output.append("\n---\n\n");
                itemNumber += 1;
            }
        }
        return output.toString();
    }

    /** 生成可独立打开的紧凑 HTML 学习文档。 */
    private String html(String batchName,
            String sourceName,
            String scenario,
            java.time.LocalDateTime createdAt,
            int imageCount,
            int itemCount,
            String status,
            List<TechEnglishRecognitionHistoryTaskResponse> tasks) {
        StringBuilder cards = new StringBuilder();
        StringBuilder failures = new StringBuilder();
        List<TechEnglishAiRecognitionItemResponse> items = tasks.stream()
                .filter(task -> !"FAILED".equals(task.status()))
                .flatMap(task -> task.items().stream())
                .sorted(Comparator.<TechEnglishAiRecognitionItemResponse>comparingInt(
                                item -> typeOrder(item.corpusType()))
                        .thenComparingInt(TechEnglishAiRecognitionItemResponse::sourceImageIndex))
                .toList();
        int globalNumber = 1;
        for (TechEnglishRecognitionHistoryTaskResponse task : tasks) {
            if ("FAILED".equals(task.status())) {
                failures.append("<div class=\"failed\"><strong>第 ")
                        .append(task.chunkIndex()).append(" 组识别失败</strong><p>")
                        .append(htmlText(itemOrFallback(task.errorMessage()))).append("</p></div>");
            }
        }
        for (TechEnglishAiRecognitionItemResponse item : items) {
            cards.append("<article class=\"card\"><div class=\"card-head\"><span class=\"index\">")
                    .append(String.format("%02d", globalNumber)).append("</span><span class=\"badge ")
                    .append(typeClass(item.corpusType())).append("\">")
                    .append(htmlTypeLabel(item.corpusType()))
                    .append("</span><small>截图 ").append(item.sourceImageIndex()).append("</small></div>")
                    .append("<h2>").append(htmlText(item.englishText())).append("</h2>");
            if (!item.scenarioTags().isEmpty()) {
                cards.append("<div class=\"chips\">");
                item.scenarioTags().forEach(tag -> appendHtmlChip(cards, tag.label()));
                cards.append("</div>");
            }
            if (StringUtils.hasText(item.partOfSpeech())
                    || StringUtils.hasText(item.britishPhonetic())
                    || StringUtils.hasText(item.americanPhonetic())) {
                cards.append("<div class=\"chips\">");
                appendHtmlChip(cards, item.partOfSpeech());
                appendHtmlChip(cards, prefixed("英 ", item.britishPhonetic()));
                appendHtmlChip(cards, prefixed("美 ", item.americanPhonetic()));
                cards.append("</div>");
            }
            appendHtmlSection(cards, "释义 / 翻译", item.translationText());
            appendHtmlSection(cards, "经典句式", item.sentencePattern());
            appendHtmlSection(cards, "句式解析", item.sentencePatternExplanation());
            if (!item.keyVocabulary().isEmpty()) {
                cards.append("<div class=\"field\"><b>重点词汇</b><div class=\"keywords\">");
                for (TechEnglishKeyVocabularyResponse word : item.keyVocabulary()) {
                    cards.append("<span><b>").append(htmlText(word.word())).append("</b>");
                    if (StringUtils.hasText(word.partOfSpeech())) {
                        cards.append(" · ").append(htmlText(word.partOfSpeech()));
                    }
                    if (StringUtils.hasText(word.meaning())) {
                        cards.append(" · ").append(htmlText(word.meaning()));
                    }
                    cards.append("</span>");
                }
                cards.append("</div></div>");
            }
            if (!item.examples().isEmpty()) {
                cards.append("<div class=\"field\"><b>AI 补充例句</b><ol class=\"examples\">");
                for (TechEnglishPatternExampleResponse example : item.examples()) {
                    cards.append("<li><p>").append(htmlText(example.englishText())).append("</p>");
                    if (StringUtils.hasText(example.translationText())) {
                        cards.append("<small>").append(htmlText(example.translationText())).append("</small>");
                    }
                    cards.append("</li>");
                }
                cards.append("</ol></div>");
            }
            cards.append("</article>");
            globalNumber += 1;
        }
        String title = exportTitle(batchName, sourceName);
        String scenarioMeta = StringUtils.hasText(scenario)
                ? "<span>场景：" + htmlText(scenario) + "</span>"
                : "";
        return """
                <!DOCTYPE html><html lang="zh-CN"><head><meta charset="utf-8"/>
                <meta name="viewport" content="width=device-width,initial-scale=1"/>
                <title>__TITLE__</title><style>
                @page{size:A4;margin:10mm}
                :root{color-scheme:light;--ink:#172033;--muted:#647087;--paper:#f6f8fb;--line:#d9e1ec;--blue:#3157d5}
                *{box-sizing:border-box}body{margin:0;background:var(--paper);color:var(--ink);font:14px/1.55 -apple-system,BlinkMacSystemFont,"Segoe UI","PingFang SC",sans-serif}
                main{width:min(1180px,calc(100% - 28px));margin:16px auto 40px}.hero{padding:16px 18px;border-bottom:2px solid var(--ink)}.eyebrow{color:var(--blue);font-size:10px;font-weight:800;letter-spacing:.12em}.hero h1{margin:3px 0 5px;font-size:clamp(22px,4vw,32px);line-height:1.2;overflow-wrap:anywhere}.hero p{margin:0;color:var(--muted)}.meta{display:flex;gap:6px;flex-wrap:wrap;margin-top:10px}.meta span{padding:3px 7px;border:1px solid var(--line);border-radius:5px;background:#fff;color:var(--muted);font-size:12px}
                .grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:8px;margin-top:12px;align-items:start}.card{break-inside:avoid;padding:10px 12px;border:1px solid var(--line);border-radius:7px;background:#fff}.card-head{display:flex;align-items:center;gap:6px;color:var(--muted);font-size:11px}.card-head small{margin-left:auto}.index{color:#9aa7b9;font-weight:800}.badge{padding:2px 6px;border-radius:4px;font-size:10px;font-weight:800}.badge.word{color:#08765d;background:#d9f7ef}.badge.phrase{color:#8a4b07;background:#fff2cf}.badge.sentence{color:#294bb9;background:#e1e9ff}.card h2{margin:4px 0 3px;font:700 18px/1.25 Georgia,"Times New Roman",serif;overflow-wrap:anywhere}.chips,.keywords{display:flex;flex-wrap:wrap;gap:4px;margin-top:4px}.chips span,.keywords span{padding:2px 5px;border-radius:4px;background:#eef2f8;color:#44516a;font-size:11px}.field{margin-top:6px}.field>b{display:block;margin-bottom:1px;color:var(--muted);font-size:10px}.field p{margin:0;white-space:pre-line;overflow-wrap:anywhere}.keywords{margin-top:2px}.keywords span{background:#f3f6fa}.examples{margin:2px 0 0;padding-left:17px}.examples li{padding:1px 0}.examples p{margin:0;font-weight:650;overflow-wrap:anywhere}.examples small{color:var(--muted)}.failed{grid-column:1/-1;padding:9px 11px;border:1px solid #efc7c7;border-radius:7px;background:#fff3f3;color:#8b3131}.failed p{display:inline;margin-left:6px}
                @media(max-width:760px){main{width:min(100% - 18px,720px);margin-top:8px}.grid{grid-template-columns:1fr}.hero{padding:13px 2px}.failed{grid-column:auto}}
                @media print{body{background:#fff;color:#172033}.hero{padding-top:0}.grid{display:block;margin-top:8px}.card{margin:0 0 7px;box-shadow:none}.failed{margin-bottom:7px}}
                </style></head><body><main><header class="hero"><div class="eyebrow">AI SCREENSHOT RECOGNITION</div>
                <h1>__TITLE__</h1><p>来源：__SOURCE__ · __CREATED_AT__</p><div class="meta"><span>__IMAGE_COUNT__ 张截图</span><span>__ITEM_COUNT__ 条语料</span><span>状态：__STATUS__</span>__SCENARIO__</div></header><section class="grid">__FAILURES____CARDS__</section></main></body></html>
                """
                .replace("__TITLE__", htmlText(title))
                .replace("__SOURCE__", htmlText(sourceName))
                .replace("__CREATED_AT__", createdAt.format(TIME_FORMATTER))
                .replace("__IMAGE_COUNT__", String.valueOf(imageCount))
                .replace("__ITEM_COUNT__", String.valueOf(itemCount))
                .replace("__STATUS__", htmlText(status))
                .replace("__SCENARIO__", scenarioMeta)
                .replace("__FAILURES__", failures.toString())
                .replace("__CARDS__", cards.toString());
    }

    /** 将紧凑 HTML 渲染为 PDF，供下载和图片导出共用。 */
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
                    "TECH_ENGLISH_RECOGNITION_PDF_FAILED", "识图 PDF 生成失败", exception);
        }
    }

    /** 将同一份 PDF 页面拼接为一张 PNG，保证图片与 PDF/HTML 内容一致。 */
    private byte[] image(String html) {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdf(html)))) {
            PDFRenderer renderer = new PDFRenderer(document);
            List<BufferedImage> pages = new ArrayList<>();
            int width = 0;
            int height = 0;
            for (int index = 0; index < document.getNumberOfPages(); index++) {
                BufferedImage page = renderer.renderImageWithDPI(index, 96);
                pages.add(page);
                width = Math.max(width, page.getWidth());
                height = Math.addExact(height, page.getHeight());
            }
            if (pages.isEmpty()) {
                throw new IllegalStateException("识图 PDF 没有可渲染页面");
            }
            BufferedImage combined = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = combined.createGraphics();
            try {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, width, height);
                int top = 0;
                for (BufferedImage page : pages) {
                    graphics.drawImage(page, 0, top, null);
                    top += page.getHeight();
                }
            } finally {
                graphics.dispose();
            }
            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                if (!ImageIO.write(combined, "png", output)) {
                    throw new IllegalStateException("PNG 图片编码器不可用");
                }
                return output.toByteArray();
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "TECH_ENGLISH_RECOGNITION_IMAGE_FAILED", "识图图片生成失败", exception);
        }
    }

    /** 尽量注册常见中文字体，避免 PDF 和图片中文缺字。 */
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

    /** 返回导出页面的任务标题，兼容没有批次名称的历史记录。 */
    private String exportTitle(String batchName, String sourceName) {
        if (StringUtils.hasText(batchName)) {
            return batchName.trim();
        }
        if (StringUtils.hasText(sourceName)) {
            return sourceName.trim();
        }
        return "技术英语识图任务";
    }

    /** 将空失败信息转换为稳定的用户提示。 */
    private String itemOrFallback(String value) {
        return StringUtils.hasText(value) ? value : "AI 识别服务暂时不可用";
    }

    /** 导出排序：单词、词组、句子类。未知类型放到句子类末尾。 */
    private int typeOrder(String corpusType) {
        return "VOCABULARY".equals(corpusType) ? 0 : "PHRASE".equals(corpusType) ? 1 : 2;
    }

    /** HTML 使用更紧凑的中文类型标签。 */
    private String htmlTypeLabel(String corpusType) {
        return switch (corpusType) {
            case "PHRASE" -> "词组";
            case "VOCABULARY" -> "单词";
            default -> "句子";
        };
    }

    /** 追加 Markdown 字段。 */
    private void appendMarkdownField(StringBuilder output, String label, String value) {
        if (StringUtils.hasText(value)) {
            output.append("- **").append(label).append("：** ")
                    .append(markdownText(value)).append("\n");
        }
    }

    /** 追加 HTML 内容分区。 */
    private void appendHtmlSection(StringBuilder output, String label, String value) {
        if (StringUtils.hasText(value)) {
            output.append("<section><label>").append(label).append("</label><p>")
                    .append(htmlText(value)).append("</p></section>");
        }
    }

    /** 追加 HTML 音标或词性胶囊。 */
    private void appendHtmlChip(StringBuilder output, String value) {
        if (StringUtils.hasText(value)) {
            output.append("<span>").append(htmlText(value)).append("</span>");
        }
    }

    /** 有内容时增加展示前缀。 */
    private String prefixed(String prefix, String value) {
        return StringUtils.hasText(value) ? prefix + value : null;
    }

    /** 返回导出里的语料类型展示名称。 */
    private String typeLabel(String corpusType) {
        return switch (corpusType) {
            case "PHRASE" -> "PHRASE · 短语";
            case "PATTERN" -> "PATTERN · 句式";
            case "SENTENCE" -> "SENTENCE · 句子";
            case "ARTICLE" -> "ARTICLE · 文章";
            default -> "VOCABULARY · 词汇";
        };
    }

    /** 返回导出里的语料类型样式名。 */
    private String typeClass(String corpusType) {
        return switch (corpusType) {
            case "PHRASE" -> "phrase";
            case "PATTERN" -> "pattern";
            case "SENTENCE" -> "sentence";
            case "ARTICLE" -> "article";
            default -> "word";
        };
    }

    /** 转义 Markdown 行内特殊字符。 */
    private String markdownText(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("`", "\\`")
                .replace("*", "\\*")
                .replace("_", "\\_")
                .replace("\r", "")
                .replace("\n", " ");
    }

    /** 转义 HTML 文本。 */
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
