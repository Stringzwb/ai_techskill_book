package com.aitechskill.book.english.controller;

import com.aitechskill.book.auth.utils.UserContextHolder;
import com.aitechskill.book.english.domain.TechEnglishImageContent;
import com.aitechskill.book.english.domain.request.TechEnglishCorpusCreateRequest;
import com.aitechskill.book.english.domain.request.TechEnglishVocabularyExampleRequest;
import com.aitechskill.book.english.domain.response.TechEnglishCorpusDetailResponse;
import com.aitechskill.book.english.domain.response.TechEnglishCorpusPageResponse;
import com.aitechskill.book.english.service.TechEnglishCorpusService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
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
 * 主平台技术英语语料公开检索接口。
 */
@RestController
@RequestMapping("/api/tech-english/corpus")
public class TechEnglishCorpusController {

    private final TechEnglishCorpusService corpusService;

    public TechEnglishCorpusController(TechEnglishCorpusService corpusService) {
        this.corpusService = corpusService;
    }

    /** 查询已发布技术英语语料。 */
    @GetMapping
    public TechEnglishCorpusPageResponse search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String corpusType,
            @RequestParam(required = false) Long tagId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size) {
        return corpusService.search(keyword, corpusType, tagId, page, size);
    }

    /** 读取一条已发布技术英语语料。 */
    @GetMapping("/{id}")
    public TechEnglishCorpusDetailResponse getCorpus(@PathVariable long id) {
        return corpusService.getPublishedCorpus(id);
    }

    /** 登录用户从主站轻量收录技术英语语料。 */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TechEnglishCorpusDetailResponse create(
            @RequestParam String corpusType,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String englishText,
            @RequestParam(required = false) String phonetic,
            @RequestParam(required = false) String explanation,
            @RequestParam(required = false) String articleMarkdown,
            @RequestParam(required = false) String imageAlt,
            @RequestParam(required = false) String sourceName,
            @RequestParam(required = false) String sourceUrl,
            @RequestParam(required = false) String scenario,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String translationText,
            @RequestParam List<Long> tagIds,
            @RequestParam(required = false) List<String> exampleEnglishTexts,
            @RequestParam(required = false) List<String> exampleTranslationTexts,
            @RequestParam(defaultValue = "false") boolean syncExamplesToSentences,
            @RequestPart(required = false) MultipartFile imageFile) {
        TechEnglishCorpusCreateRequest request = new TechEnglishCorpusCreateRequest(
                corpusType,
                title,
                englishText,
                phonetic,
                explanation,
                articleMarkdown,
                imageAlt,
                sourceName,
                sourceUrl,
                scenario,
                difficulty,
                translationText,
                tagIds,
                toVocabularyExamples(exampleEnglishTexts, exampleTranslationTexts),
                syncExamplesToSentences);
        return corpusService.create(request, imageFile, UserContextHolder.requireUserId());
    }

    /** 将 multipart 中两组例句字段按索引配对。 */
    private List<TechEnglishVocabularyExampleRequest> toVocabularyExamples(
            List<String> exampleEnglishTexts,
            List<String> exampleTranslationTexts) {
        int englishCount = exampleEnglishTexts == null ? 0 : exampleEnglishTexts.size();
        int translationCount = exampleTranslationTexts == null ? 0 : exampleTranslationTexts.size();
        int size = Math.max(englishCount, translationCount);
        if (size == 0) {
            return List.of();
        }
        List<TechEnglishVocabularyExampleRequest> examples = new ArrayList<>(size);
        for (int index = 0; index < size; index += 1) {
            examples.add(new TechEnglishVocabularyExampleRequest(
                    index < englishCount ? exampleEnglishTexts.get(index) : null,
                    index < translationCount ? exampleTranslationTexts.get(index) : null));
        }
        return examples;
    }

    /** 读取已发布图片语料文件。 */
    @GetMapping("/{id}/image")
    public ResponseEntity<InputStreamResource> getImage(@PathVariable long id) {
        TechEnglishImageContent image = corpusService.getPublishedImage(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .contentLength(image.contentLength())
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePrivate().immutable())
                .header("X-Content-Type-Options", "nosniff")
                .body(new InputStreamResource(image.inputStream()));
    }
}
