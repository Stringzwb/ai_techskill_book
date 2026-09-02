package com.aitechskill.book.english.service;

import com.aitechskill.book.common.exception.BusinessException;
import com.aitechskill.book.english.domain.TechEnglishRecognitionExport;
import com.aitechskill.book.english.domain.response.TechEnglishAiRecognitionItemResponse;
import com.aitechskill.book.english.domain.response.TechEnglishKeyVocabularyResponse;
import com.aitechskill.book.english.domain.response.TechEnglishPatternExampleResponse;
import com.aitechskill.book.english.domain.response.TechEnglishRecognitionHistoryDetailResponse;
import com.aitechskill.book.english.domain.response.TechEnglishRecognitionHistoryTaskResponse;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
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
                    html(detail.sourceName(), detail.scenario(), detail.createdAt(),
                            detail.imageCount(), detail.itemCount(), detail.status(), detail.tasks())
                            .getBytes(StandardCharsets.UTF_8));
        }
        throw new BusinessException(HttpStatus.BAD_REQUEST,
                "TECH_ENGLISH_EXPORT_FORMAT_INVALID", "导出格式仅支持 Markdown 或 HTML");
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
                    html(detail.sourceName(), detail.scenario(), task.createdAt(),
                            task.imageCount(), task.itemCount(), task.status(), List.of(task))
                            .getBytes(StandardCharsets.UTF_8));
        }
        throw new BusinessException(HttpStatus.BAD_REQUEST,
                "TECH_ENGLISH_EXPORT_FORMAT_INVALID", "导出格式仅支持 Markdown 或 HTML");
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

    /** 生成可独立打开的精美 HTML 学习文档。 */
    private String html(String sourceName,
            String scenario,
            java.time.LocalDateTime createdAt,
            int imageCount,
            int itemCount,
            String status,
            List<TechEnglishRecognitionHistoryTaskResponse> tasks) {
        StringBuilder cards = new StringBuilder();
        int globalNumber = 1;
        for (TechEnglishRecognitionHistoryTaskResponse task : tasks) {
            if ("FAILED".equals(task.status())) {
                cards.append("<section class=\"failed\"><strong>第 ")
                        .append(task.chunkIndex()).append(" 组识别失败</strong><p>")
                        .append(htmlText(task.errorMessage())).append("</p></section>");
                continue;
            }
            for (TechEnglishAiRecognitionItemResponse item : task.items()) {
                cards.append("<article class=\"card\"><header><span class=\"index\">")
                        .append(String.format("%02d", globalNumber)).append("</span><span class=\"badge ")
                        .append(typeClass(item.corpusType())).append("\">")
                        .append(typeLabel(item.corpusType()))
                        .append("</span><small>截图 ").append(item.sourceImageIndex()).append("</small></header>")
                        .append("<h2>").append(htmlText(item.englishText())).append("</h2>");
                if (!item.scenarioTags().isEmpty()) {
                    cards.append("<div class=\"pronunciation\">");
                    item.scenarioTags().forEach(tag -> appendHtmlChip(cards, tag.label()));
                    cards.append("</div>");
                }
                if (StringUtils.hasText(item.partOfSpeech())
                        || StringUtils.hasText(item.britishPhonetic())
                        || StringUtils.hasText(item.americanPhonetic())) {
                    cards.append("<div class=\"pronunciation\">");
                    appendHtmlChip(cards, item.partOfSpeech());
                    appendHtmlChip(cards, prefixed("英 ", item.britishPhonetic()));
                    appendHtmlChip(cards, prefixed("美 ", item.americanPhonetic()));
                    cards.append("</div>");
                }
                appendHtmlSection(cards, "释义 / 翻译", item.translationText());
                appendHtmlSection(cards, "经典句式", item.sentencePattern());
                appendHtmlSection(cards, "句式解析", item.sentencePatternExplanation());
                if (!item.keyVocabulary().isEmpty()) {
                    cards.append("<section><label>重点词汇</label><div class=\"keywords\">");
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
                    cards.append("</div></section>");
                }
                if (!item.examples().isEmpty()) {
                    cards.append("<section><label>AI 补充例句</label><ol class=\"examples\">");
                    for (TechEnglishPatternExampleResponse example : item.examples()) {
                        cards.append("<li><p>").append(htmlText(example.englishText())).append("</p>");
                        if (StringUtils.hasText(example.translationText())) {
                            cards.append("<small>").append(htmlText(example.translationText())).append("</small>");
                        }
                        cards.append("</li>");
                    }
                    cards.append("</ol></section>");
                }
                cards.append("</article>");
                globalNumber += 1;
            }
        }
        return """
                <!doctype html><html lang="zh-CN"><head><meta charset="utf-8">
                <meta name="viewport" content="width=device-width,initial-scale=1">
                <title>技术英语 AI 识图记录</title><style>
                :root{color-scheme:light;--ink:#172033;--muted:#647087;--paper:#f4f7fb;--line:#dce4ef;--blue:#3157d5;--mint:#0c9b79}
                *{box-sizing:border-box}body{margin:0;background:radial-gradient(circle at 10% 0,#dce7ff 0,transparent 34%),var(--paper);color:var(--ink);font:15px/1.7 -apple-system,BlinkMacSystemFont,"Segoe UI","PingFang SC",sans-serif}
                main{width:min(900px,calc(100% - 32px));margin:30px auto 64px}.hero{padding:30px;border-radius:22px;color:#fff;background:linear-gradient(135deg,#17233e,#3157d5 58%,#0c9b79);box-shadow:0 20px 54px #294da82b}.hero small{letter-spacing:.16em;opacity:.8}.hero h1{margin:8px 0 9px;font-size:clamp(27px,5vw,44px);line-height:1.1}.hero p{margin:0;opacity:.86}.stats{display:flex;gap:7px;flex-wrap:wrap;margin-top:18px}.stats span{padding:5px 9px;border:1px solid #ffffff42;border-radius:999px;background:#ffffff14;font-size:13px}
                .view-switch{display:flex;gap:5px;flex-wrap:wrap;margin-top:12px}.view-switch button{padding:5px 9px;color:#dce8ff;border:1px solid #ffffff42;border-radius:999px;background:#ffffff12;font:700 12px/1 inherit;cursor:pointer}.view-switch button:hover,.view-switch button.active{color:#17233e;border-color:#fff;background:#fff}
                .grid{display:grid;gap:9px;margin-top:14px}.card{padding:16px 18px;border:1px solid var(--line);border-radius:14px;background:#fffffff2;box-shadow:0 5px 18px #26334b0d}.card header{display:flex;align-items:center;gap:8px}.card header small{margin-left:auto;color:var(--muted);font-size:12px}.index{font-weight:800;color:#99a6bc}.badge{padding:3px 7px;border-radius:999px;font-size:10px;font-weight:800;letter-spacing:.05em}.badge.word{color:#08765d;background:#d9f7ef}.badge.phrase{color:#8a4b07;background:#fff2cf}.badge.sentence{color:#294bb9;background:#e1e9ff}.card h2{margin:8px 0 5px;font:700 clamp(18px,3.5vw,27px)/1.28 Georgia,"Times New Roman",serif}.pronunciation{display:flex;flex-wrap:wrap;gap:6px;margin:7px 0}.pronunciation span,.keywords span{padding:3px 7px;border-radius:7px;background:#eef2f8;color:#44516a;font-size:13px}.card section{margin-top:11px}.card label{display:block;margin-bottom:3px;color:var(--muted);font-size:10px;font-weight:800;letter-spacing:.12em;text-transform:uppercase}.card section p{margin:0}.keywords{display:flex;flex-wrap:wrap;gap:6px}.examples{margin:5px 0 0;padding-left:20px}.examples li{padding:3px 0}.examples p{font-weight:650}.examples small{color:var(--muted)}.failed{padding:16px;border:1px solid #f0c6c6;border-radius:14px;background:#fff3f3;color:#8b3131}
                body.view-cards main{width:min(980px,calc(100% - 32px));margin-top:42px}body.view-cards .grid{gap:18px;margin-top:24px}body.view-cards .card{padding:26px;border-radius:22px;box-shadow:0 10px 34px #26334b12}body.view-cards .card h2{margin:14px 0 8px;font-size:clamp(22px,4vw,34px)}body.view-cards .card section{margin-top:18px}body.view-cards .pronunciation{gap:8px;margin:10px 0}body.view-cards .pronunciation span,body.view-cards .keywords span{padding:5px 10px;border-radius:10px}body.view-cards .examples li{padding:6px 0}
                body.view-reading main{width:min(740px,calc(100% - 32px));margin-top:24px}body.view-reading .hero{padding:26px 0;color:var(--ink);border-radius:0;border-bottom:2px solid var(--ink);background:transparent;box-shadow:none}body.view-reading .hero small,body.view-reading .hero p{color:var(--muted);opacity:1}body.view-reading .stats span,body.view-reading .view-switch button{color:var(--ink);border-color:var(--line);background:transparent}body.view-reading .view-switch button.active{color:#fff;border-color:var(--ink);background:var(--ink)}body.view-reading .grid{gap:0;margin-top:18px}body.view-reading .card{padding:20px 0;border:0;border-bottom:1px solid var(--line);border-radius:0;background:transparent;box-shadow:none}body.view-reading .card h2{font-size:24px}body.view-reading .card header small{font-size:11px}
                @media(max-width:600px){main,body.view-cards main,body.view-reading main{width:min(100% - 20px,900px);margin-top:10px}.hero,body.view-cards .hero{padding:21px;border-radius:16px}.card,body.view-cards .card{padding:15px;border-radius:12px}.card header{align-items:flex-start}.card header small{font-size:10px}.view-switch button{font-size:11px}}
                </style></head><body class="view-compact"><main><header class="hero"><small>AI SCREENSHOT RECOGNITION</small>
                <h1>技术英语识图记录</h1><p>来源：__SOURCE__ · __CREATED_AT__</p><div class="stats"><span>__IMAGE_COUNT__ 张截图</span><span>__ITEM_COUNT__ 条语料</span><span>状态 __STATUS__</span></div><nav class="view-switch" aria-label="展示方式"><button class="active" type="button" data-view="compact">紧凑</button><button type="button" data-view="cards">卡片</button><button type="button" data-view="reading">阅读</button></nav></header><section class="grid">__CARDS__</section></main><script>document.querySelectorAll("[data-view]").forEach(function(button){button.addEventListener("click",function(){document.body.className="view-"+button.dataset.view;document.querySelectorAll("[data-view]").forEach(function(item){item.classList.toggle("active",item===button)})})});</script></body></html>
                """
                .replace("__SOURCE__", htmlText(sourceName))
                .replace("__CREATED_AT__", createdAt.format(TIME_FORMATTER))
                .replace("__IMAGE_COUNT__", String.valueOf(imageCount))
                .replace("__ITEM_COUNT__", String.valueOf(itemCount))
                .replace("__STATUS__", htmlText(status))
                .replace("__CARDS__", cards.toString());
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
