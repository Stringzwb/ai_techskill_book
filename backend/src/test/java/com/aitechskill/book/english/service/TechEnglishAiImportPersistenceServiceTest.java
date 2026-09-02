package com.aitechskill.book.english.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.aitechskill.book.english.domain.ai.TechEnglishAutoImportPayload;
import com.aitechskill.book.english.domain.ai.TechEnglishSentenceImportPayload;
import com.aitechskill.book.english.domain.ai.TechEnglishVocabularyImportPayload;
import com.aitechskill.book.english.domain.entity.TechEnglishCorpusEntity;
import com.aitechskill.book.english.domain.entity.TechEnglishSentencePatternEntity;
import com.aitechskill.book.english.domain.response.TechEnglishCorpusDetailResponse;
import com.aitechskill.book.english.mapper.TechEnglishCorpusMapper;
import com.aitechskill.book.english.mapper.TechEnglishSentencePatternMapper;
import com.aitechskill.book.english.mapper.TechEnglishVocabularyExampleMapper;
import com.aitechskill.book.storage.domain.StoredObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * AI 识别结果的事务入库测试。
 */
@ExtendWith(MockitoExtension.class)
class TechEnglishAiImportPersistenceServiceTest {

    @Mock
    private TechEnglishCorpusMapper corpusMapper;
    @Mock
    private TechEnglishVocabularyExampleMapper vocabularyExampleMapper;
    @Mock
    private TechEnglishSentencePatternMapper sentencePatternMapper;
    @Mock
    private TechEnglishCorpusService corpusService;

    private TechEnglishAiImportPersistenceService service;

    @BeforeEach
    void setUp() {
        service = new TechEnglishAiImportPersistenceService(
                corpusMapper, vocabularyExampleMapper, sentencePatternMapper, corpusService, new ObjectMapper());
    }

    /** 同一识别批次包含词汇和句子时，两类语料都应在事务中创建。 */
    @Test
    void savesMixedVocabularyAndSentences() {
        given(corpusMapper.selectList(any())).willReturn(List.of());
        given(corpusMapper.insert(any(TechEnglishCorpusEntity.class))).willAnswer(invocation -> {
            TechEnglishCorpusEntity corpus = invocation.getArgument(0);
            corpus.setId("VOCABULARY".equals(corpus.getCorpusType()) ? 101L : 102L);
            return 1;
        });
        given(corpusService.getPublishedCorpus(101L)).willReturn(detail(101L, "VOCABULARY"));
        given(corpusService.getPublishedCorpus(102L)).willReturn(detail(102L, "SENTENCE"));
        given(corpusMapper.selectOne(any())).willReturn(null);
        given(sentencePatternMapper.selectActiveByNormalizedPattern("... can still be ...")).willReturn(null);
        given(sentencePatternMapper.insert(any(TechEnglishSentencePatternEntity.class))).willAnswer(invocation -> {
            TechEnglishSentencePatternEntity pattern = invocation.getArgument(0);
            pattern.setId(301L);
            return 1;
        });
        TechEnglishAutoImportPayload payload = new TechEnglishAutoImportPayload(
                "MINT_AUTO_IMPORT_V1",
                new TechEnglishVocabularyImportPayload(
                        "MINT_VOCABULARY_IMPORT_V1",
                        List.of(new TechEnglishVocabularyImportPayload.Item(
                                1, "VOCABULARY", "resilient", "adjective", "有韧性的", null, null, List.of(), List.of()))),
                new TechEnglishSentenceImportPayload(
                        "MINT_SENTENCE_IMPORT_V1",
                        List.of(new TechEnglishSentenceImportPayload.Item(
                                1, "Small systems can still be resilient.", "小型系统仍然可以很有韧性。",
                                List.of(), "... can still be ...", "表示仍然具备某种特性", List.of(), List.of()))));

        List<TechEnglishCorpusDetailResponse> created = service.saveAuto(
                "mixed-batch",
                payload,
                List.of(new StoredObject("prod/tech_english/source.png", "image/png", 3)),
                Map.of(),
                "薄荷阅读",
                "系统可靠性",
                0,
                7L);

        ArgumentCaptor<TechEnglishCorpusEntity> captor = ArgumentCaptor.forClass(TechEnglishCorpusEntity.class);
        verify(corpusMapper, times(3)).insert(captor.capture());
        assertThat(captor.getAllValues()).extracting(TechEnglishCorpusEntity::getCorpusType)
                .containsExactly("VOCABULARY", "SENTENCE", "PATTERN");
        assertThat(created).extracting(TechEnglishCorpusDetailResponse::corpusType)
                .containsExactly("VOCABULARY", "SENTENCE");
        verify(sentencePatternMapper).insertCorpusLink(301L, 102L, 7L);
    }

    /** 构造最小化的语料详情返回值。 */
    private TechEnglishCorpusDetailResponse detail(long id, String corpusType) {
        return new TechEnglishCorpusDetailResponse(
                id, corpusType, corpusType, corpusType, null, null, null, null,
                null, null, null, null, "mixed-batch", "薄荷阅读", null,
                null, List.of(), "INTERMEDIATE", null, null, null, null,
                List.of(), List.of(), null, null, List.of(), List.of());
    }
}
