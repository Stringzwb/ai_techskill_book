package com.aitechskill.book.english.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.aitechskill.book.common.exception.BusinessException;
import com.aitechskill.book.document.domain.DocumentTagRecord;
import com.aitechskill.book.english.domain.entity.TechEnglishCorpusEntity;
import com.aitechskill.book.english.domain.entity.TechEnglishVocabularyExampleEntity;
import com.aitechskill.book.english.domain.request.TechEnglishCorpusCreateRequest;
import com.aitechskill.book.english.domain.request.TechEnglishVocabularyExampleRequest;
import com.aitechskill.book.english.domain.response.TechEnglishCorpusDetailResponse;
import com.aitechskill.book.english.mapper.TechEnglishCorpusMapper;
import com.aitechskill.book.english.mapper.TechEnglishVocabularyExampleMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

/**
 * 主平台技术英语语料服务测试。
 */
@ExtendWith(MockitoExtension.class)
class TechEnglishCorpusServiceTest {

    @Mock
    private TechEnglishCorpusMapper corpusMapper;
    @Mock
    private TechEnglishVocabularyExampleMapper vocabularyExampleMapper;
    @Mock
    private TechEnglishImageStorageService imageStorageService;
    private TechEnglishCorpusService service;

    @BeforeEach
    void setUp() {
        service = new TechEnglishCorpusService(
                corpusMapper,
                vocabularyExampleMapper,
                imageStorageService,
                new ObjectMapper());
    }

    /** 验证主站轻收录会发布语料并绑定知识标签。 */
    @Test
    void createsPublishedCorpusWithKnowledgeTags() {
        given(corpusMapper.countActiveTags(List.of(3L))).willReturn(1L);
        given(corpusMapper.insert(any(TechEnglishCorpusEntity.class))).willAnswer(invocation -> {
            TechEnglishCorpusEntity corpus = invocation.getArgument(0);
            corpus.setId(18L);
            return 1;
        });
        given(corpusMapper.insertTagLinks(18L, List.of(3L))).willReturn(1);
        given(corpusMapper.selectTagsByCorpusIds(List.of(18L)))
                .willReturn(List.of(new DocumentTagRecord(18L, 3L, "集合框架", 3)));

        TechEnglishCorpusDetailResponse response = service.create(new TechEnglishCorpusCreateRequest(
                "VOCABULARY",
                "Idempotent",
                "idempotent",
                "eye-DEMP-uh-tuhnt",
                "Safe to retry without changing the final result.",
                null,
                null,
                null,
                null,
                "backend",
                null,
                "INTERMEDIATE",
                "幂等",
                List.of(3L, 3L),
                List.of(),
                false), null, 7L);

        ArgumentCaptor<TechEnglishCorpusEntity> captor = ArgumentCaptor.forClass(TechEnglishCorpusEntity.class);
        verify(corpusMapper).insert(captor.capture());
        verify(corpusMapper).insertTagLinks(18L, List.of(3L));
        assertThat(captor.getValue().getStatus()).isEqualTo("PUBLISHED");
        assertThat(captor.getValue().getCreateby()).isEqualTo(7L);
        assertThat(response.id()).isEqualTo(18L);
        assertThat(response.knowledgeTags()).singleElement().satisfies(tag -> assertThat(tag.name()).isEqualTo("集合框架"));
    }

    /** 验证语料库筛选会去重标签并把多个标签传给查询层。 */
    @Test
    void searchesCorpusWithMultipleTags() {
        given(corpusMapper.countPublished(null, null, List.of(3L, 4L))).willReturn(0L);

        var response = service.search(null, null, List.of(3L, 3L, -1L, 4L), 1, 12);

        assertThat(response.total()).isZero();
        verify(corpusMapper).countPublished(null, null, List.of(3L, 4L));
    }

    /** 验证不再允许用图片类型新建语料。 */
    @Test
    void rejectsImageCorpusFromUploadedFile() {
        MockMultipartFile image = new MockMultipartFile("imageFile", "flow.png", "image/png", new byte[] {1, 2, 3});

        assertThatThrownBy(() -> service.create(new TechEnglishCorpusCreateRequest(
                "IMAGE",
                "Queue retry diagram",
                null,
                null,
                "Retry queue architecture",
                null,
                "Queue retry diagram",
                null,
                null,
                "architecture",
                null,
                "ADVANCED",
                null,
                List.of(6L),
                List.of(),
                false), image, 7L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("语料类型不合法");
        verify(imageStorageService, never()).save(org.mockito.ArgumentMatchers.anyLong(), any());
    }

    /** 验证文章语料必须有正文或链接。 */
    @Test
    void rejectsArticleWithoutMarkdownOrLink() {
        given(corpusMapper.countActiveTags(List.of(3L))).willReturn(1L);

        assertThatThrownBy(() -> service.create(new TechEnglishCorpusCreateRequest(
                "ARTICLE",
                "Clean Architecture",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "BEGINNER",
                null,
                List.of(3L),
                List.of(),
                false), null, 7L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("请填写文章正文或文章链接");
    }

    /** 验证词汇可以省略标题，并将例句同步为句子语料。 */
    @Test
    void createsVocabularyExamplesAndOptionalSentenceCorpus() {
        given(corpusMapper.countActiveTags(List.of(3L))).willReturn(1L);
        given(corpusMapper.insert(any(TechEnglishCorpusEntity.class))).willAnswer(invocation -> {
            TechEnglishCorpusEntity corpus = invocation.getArgument(0);
            corpus.setId("VOCABULARY".equals(corpus.getCorpusType()) ? 31L : 32L);
            return 1;
        });
        given(corpusMapper.insertTagLinks(31L, List.of(3L))).willReturn(1);
        given(corpusMapper.insertTagLinks(32L, List.of(3L))).willReturn(1);
        given(vocabularyExampleMapper.insert(any(TechEnglishVocabularyExampleEntity.class))).willAnswer(invocation -> {
            TechEnglishVocabularyExampleEntity example = invocation.getArgument(0);
            example.setId(41L);
            return 1;
        });
        given(corpusMapper.selectTagsByCorpusIds(List.of(31L)))
                .willReturn(List.of(new DocumentTagRecord(31L, 3L, "集合框架", 3)));

        TechEnglishCorpusDetailResponse response = service.create(new TechEnglishCorpusCreateRequest(
                "VOCABULARY",
                "",
                "idempotent",
                null,
                "Safe to retry without changing the final result.",
                null,
                null,
                null,
                null,
                "backend",
                null,
                "INTERMEDIATE",
                "幂等",
                List.of(3L),
                List.of(new TechEnglishVocabularyExampleRequest(
                        "A PUT request should be idempotent.",
                        "PUT 请求应当是幂等的。")),
                true), null, 7L);

        ArgumentCaptor<TechEnglishCorpusEntity> corpusCaptor = ArgumentCaptor.forClass(TechEnglishCorpusEntity.class);
        ArgumentCaptor<TechEnglishVocabularyExampleEntity> exampleCaptor = ArgumentCaptor.forClass(TechEnglishVocabularyExampleEntity.class);
        verify(corpusMapper, org.mockito.Mockito.times(2)).insert(corpusCaptor.capture());
        verify(vocabularyExampleMapper).insert(exampleCaptor.capture());
        assertThat(corpusCaptor.getAllValues().get(0).getTitle()).isEqualTo("idempotent");
        assertThat(corpusCaptor.getAllValues().get(1).getCorpusType()).isEqualTo("SENTENCE");
        assertThat(exampleCaptor.getValue().getSentenceCorpusId()).isEqualTo(32L);
        assertThat(response.vocabularyExamples()).singleElement().satisfies(example -> {
            assertThat(example.englishText()).isEqualTo("A PUT request should be idempotent.");
            assertThat(example.sentenceCorpusId()).isEqualTo(32L);
        });
    }

    /** 用户点击保存后，AI 例句应单独生成句子语料并复制词汇标签。 */
    @Test
    void savesVocabularyExampleAsSentenceOnDemand() {
        TechEnglishCorpusEntity vocabulary = corpus(31L, "VOCABULARY", "idempotent");
        vocabulary.setScenario("backend");
        vocabulary.setDifficulty("INTERMEDIATE");
        TechEnglishVocabularyExampleEntity example = new TechEnglishVocabularyExampleEntity();
        example.setId(41L);
        example.setVocabularyCorpusId(31L);
        example.setEnglishText("A PUT request should be idempotent.");
        example.setTranslationText("PUT 请求应当是幂等的。");
        TechEnglishCorpusEntity sentence = corpus(32L, "SENTENCE", "A PUT request should be idempotent.");
        sentence.setDifficulty("INTERMEDIATE");
        given(corpusMapper.selectPublishedById(31L)).willReturn(vocabulary);
        given(vocabularyExampleMapper.selectActiveByVocabularyAndIdForUpdate(31L, 41L)).willReturn(example);
        given(corpusMapper.selectTagsByCorpusIds(List.of(31L)))
                .willReturn(List.of(new DocumentTagRecord(31L, 3L, "集合框架", 3)));
        given(corpusMapper.insert(any(TechEnglishCorpusEntity.class))).willAnswer(invocation -> {
            TechEnglishCorpusEntity created = invocation.getArgument(0);
            created.setId(32L);
            return 1;
        });
        given(corpusMapper.selectPublishedById(32L)).willReturn(sentence);
        given(corpusMapper.selectTagsByCorpusIds(List.of(32L))).willReturn(List.of());

        TechEnglishCorpusDetailResponse response = service.saveVocabularyExampleAsSentence(31L, 41L, 7L);

        ArgumentCaptor<TechEnglishCorpusEntity> sentenceCaptor = ArgumentCaptor.forClass(TechEnglishCorpusEntity.class);
        verify(corpusMapper).insert(sentenceCaptor.capture());
        verify(corpusMapper).insertTagLinks(32L, List.of(3L));
        verify(vocabularyExampleMapper).updateById(example);
        assertThat(sentenceCaptor.getValue().getCorpusType()).isEqualTo("SENTENCE");
        assertThat(sentenceCaptor.getValue().getEnglishText()).isEqualTo("A PUT request should be idempotent.");
        assertThat(example.getSentenceCorpusId()).isEqualTo(32L);
        assertThat(response.id()).isEqualTo(32L);
    }

    /** 已保存的例句再次点击时复用已有句子语料。 */
    @Test
    void reusesSentenceCorpusForSavedVocabularyExample() {
        TechEnglishCorpusEntity vocabulary = corpus(31L, "VOCABULARY", "idempotent");
        TechEnglishVocabularyExampleEntity example = new TechEnglishVocabularyExampleEntity();
        example.setId(41L);
        example.setVocabularyCorpusId(31L);
        example.setSentenceCorpusId(32L);
        TechEnglishCorpusEntity sentence = corpus(32L, "SENTENCE", "A PUT request should be idempotent.");
        given(corpusMapper.selectPublishedById(31L)).willReturn(vocabulary);
        given(vocabularyExampleMapper.selectActiveByVocabularyAndIdForUpdate(31L, 41L)).willReturn(example);
        given(corpusMapper.selectPublishedById(32L)).willReturn(sentence);
        given(corpusMapper.selectTagsByCorpusIds(List.of(32L))).willReturn(List.of());

        TechEnglishCorpusDetailResponse response = service.saveVocabularyExampleAsSentence(31L, 41L, 7L);

        verify(corpusMapper, never()).insert(any(TechEnglishCorpusEntity.class));
        verify(vocabularyExampleMapper, never()).updateById(any(TechEnglishVocabularyExampleEntity.class));
        assertThat(response.id()).isEqualTo(32L);
    }

    /** 构造技术英语语料实体。 */
    private TechEnglishCorpusEntity corpus(long id, String corpusType, String englishText) {
        TechEnglishCorpusEntity corpus = new TechEnglishCorpusEntity();
        corpus.setId(id);
        corpus.setCorpusType(corpusType);
        corpus.setTitle(englishText);
        corpus.setEnglishText(englishText);
        corpus.setStatus("PUBLISHED");
        return corpus;
    }
}
