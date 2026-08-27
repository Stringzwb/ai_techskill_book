package com.aitechskill.book.english.controller;

import com.aitechskill.book.english.domain.response.TechEnglishCorpusDetailResponse;
import com.aitechskill.book.english.domain.response.TechEnglishCorpusPageResponse;
import com.aitechskill.book.english.service.TechEnglishCorpusService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
