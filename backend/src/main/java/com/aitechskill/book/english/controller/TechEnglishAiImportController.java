package com.aitechskill.book.english.controller;

import com.aitechskill.book.auth.utils.UserContextHolder;
import com.aitechskill.book.english.domain.response.TechEnglishAiImportResponse;
import com.aitechskill.book.english.domain.response.TechEnglishAiRecognitionResponse;
import com.aitechskill.book.english.service.TechEnglishAiImportService;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
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

    public TechEnglishAiImportController(TechEnglishAiImportService importService) {
        this.importService = importService;
    }

    /** 最多上传十张截图并返回识别草稿，此阶段不要求知识标签。 */
    @PostMapping(value = "/screenshots", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TechEnglishAiRecognitionResponse recognizeScreenshots(
            @RequestParam String importType,
            @RequestParam(required = false) String scenario,
            @RequestParam(defaultValue = "2") int exampleCount,
            @RequestPart("images") List<MultipartFile> images) {
        return importService.recognizeScreenshots(
                importType,
                scenario,
                exampleCount,
                images,
                UserContextHolder.requireUserId());
    }

    /** 用户确认识别结果并选择标签后，保存截图并正式创建语料。 */
    @PostMapping(value = "/screenshots/{batchUuid}/confirm", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TechEnglishAiImportResponse confirmImport(
            @PathVariable String batchUuid,
            @RequestParam List<Long> tagIds,
            @RequestPart("images") List<MultipartFile> images) {
        return importService.confirmImport(
                batchUuid,
                tagIds,
                images,
                UserContextHolder.requireUserId());
    }
}
