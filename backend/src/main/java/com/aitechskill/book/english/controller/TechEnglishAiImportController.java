package com.aitechskill.book.english.controller;

import com.aitechskill.book.auth.utils.UserContextHolder;
import com.aitechskill.book.english.domain.TechEnglishRecognitionExport;
import com.aitechskill.book.english.domain.response.TechEnglishAiImportResponse;
import com.aitechskill.book.english.domain.response.TechEnglishAiRecognitionResponse;
import com.aitechskill.book.english.domain.response.TechEnglishRecognitionHistoryDetailResponse;
import com.aitechskill.book.english.domain.response.TechEnglishRecognitionHistoryPageResponse;
import com.aitechskill.book.english.service.TechEnglishAiRecognitionRecordService;
import com.aitechskill.book.english.service.TechEnglishAiImportService;
import com.aitechskill.book.english.service.TechEnglishRecognitionExportService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 技术英语截图批量识别与入库接口。
 */
@RestController
@RequestMapping("/api/tech-english/imports")
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
public class TechEnglishAiImportController {

    private final TechEnglishAiImportService importService;
    private final TechEnglishAiRecognitionRecordService recordService;
    private final TechEnglishRecognitionExportService exportService;

    public TechEnglishAiImportController(
            TechEnglishAiImportService importService,
            TechEnglishAiRecognitionRecordService recordService,
            TechEnglishRecognitionExportService exportService) {
        this.importService = importService;
        this.recordService = recordService;
        this.exportService = exportService;
    }

    /** 最多上传二十张截图并返回识别草稿，此阶段不要求知识标签。 */
    @PostMapping(value = "/screenshots", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TechEnglishAiRecognitionResponse recognizeScreenshots(
            @RequestParam String sessionUuid,
            @RequestParam int chunkIndex,
            @RequestParam int chunkCount,
            @RequestParam(required = false) String scenario,
            @RequestParam(defaultValue = "2") int exampleCount,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        return importService.recognizeScreenshots(
                sessionUuid,
                chunkIndex,
                chunkCount,
                scenario,
                exampleCount,
                images,
                UserContextHolder.requireUserId());
    }

    /** 用户确认识别结果并选择标签后，保存截图并正式创建语料。 */
    @PostMapping(value = "/screenshots/{batchUuid}/confirm", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TechEnglishAiImportResponse confirmImport(
            @PathVariable String batchUuid,
            @RequestParam String itemTagAssignments,
            @RequestPart("images") List<MultipartFile> images) {
        return importService.confirmImport(
                batchUuid,
                itemTagAssignments,
                images,
                UserContextHolder.requireUserId());
    }

    /** 分页查询当前用户的识图记录。 */
    @GetMapping("/history")
    public TechEnglishRecognitionHistoryPageResponse history(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return recordService.page(UserContextHolder.requireUserId(), page, size);
    }

    /** 读取当前用户一次上传会话的完整识图结果。 */
    @GetMapping("/history/{sessionUuid}")
    public TechEnglishRecognitionHistoryDetailResponse historyDetail(
            @PathVariable String sessionUuid) {
        return recordService.detail(UserContextHolder.requireUserId(), sessionUuid);
    }

    /** 将一次识图结果导出为 Markdown 或 HTML 文件。 */
    @GetMapping("/history/{sessionUuid}/export")
    public ResponseEntity<byte[]> exportHistory(
            @PathVariable String sessionUuid,
            @RequestParam(defaultValue = "markdown") String format) {
        TechEnglishRecognitionExport export = exportService.export(
                UserContextHolder.requireUserId(), sessionUuid, format);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(export.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(export.filename(), StandardCharsets.UTF_8)
                        .build().toString())
                .body(export.content());
    }

    /** 将一次识图会话中的单个批次导出为 Markdown 或 HTML 文件。 */
    @GetMapping("/history/{sessionUuid}/batches/{batchUuid}/export")
    public ResponseEntity<byte[]> exportHistoryBatch(
            @PathVariable String sessionUuid,
            @PathVariable String batchUuid,
            @RequestParam(defaultValue = "markdown") String format) {
        TechEnglishRecognitionExport export = exportService.exportBatch(
                UserContextHolder.requireUserId(), sessionUuid, batchUuid, format);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(export.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(export.filename(), StandardCharsets.UTF_8)
                        .build().toString())
                .body(export.content());
    }
}
