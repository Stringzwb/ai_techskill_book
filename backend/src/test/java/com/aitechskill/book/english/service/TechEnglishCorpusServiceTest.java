package com.aitechskill.book.english.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.aitechskill.book.common.exception.BusinessException;
import com.aitechskill.book.document.domain.DocumentTagRecord;
import com.aitechskill.book.english.domain.entity.TechEnglishCorpusEntity;
import com.aitechskill.book.english.domain.request.TechEnglishCorpusCreateRequest;
import com.aitechskill.book.english.domain.response.TechEnglishCorpusDetailResponse;
import com.aitechskill.book.english.mapper.TechEnglishCorpusMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 主平台技术英语语料服务测试。
 */
@ExtendWith(MockitoExtension.class)
class TechEnglishCorpusServiceTest {

    @Mock
    private TechEnglishCorpusMapper corpusMapper;
    private TechEnglishCorpusService service;

    @BeforeEach
    void setUp() {
        service = new TechEnglishCorpusService(corpusMapper);
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
                null,
                "backend",
                "INTERMEDIATE",
                "幂等",
                List.of(3L, 3L)), 7L);

        ArgumentCaptor<TechEnglishCorpusEntity> captor = ArgumentCaptor.forClass(TechEnglishCorpusEntity.class);
        verify(corpusMapper).insert(captor.capture());
        verify(corpusMapper).insertTagLinks(18L, List.of(3L));
        assertThat(captor.getValue().getStatus()).isEqualTo("PUBLISHED");
        assertThat(captor.getValue().getCreateby()).isEqualTo(7L);
        assertThat(response.id()).isEqualTo(18L);
        assertThat(response.knowledgeTags()).singleElement().satisfies(tag -> assertThat(tag.name()).isEqualTo("集合框架"));
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
                List.of(3L)), 7L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("请填写文章正文或文章链接");
    }
}
