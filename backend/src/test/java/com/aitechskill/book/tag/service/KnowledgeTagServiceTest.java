package com.aitechskill.book.tag.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.aitechskill.book.tag.domain.entity.KnowledgeTagEntity;
import com.aitechskill.book.tag.domain.response.KnowledgeTagTreeResponse;
import com.aitechskill.book.tag.mapper.KnowledgeTagMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 主平台知识标签树查询服务测试。
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeTagServiceTest {

    @Mock
    private KnowledgeTagMapper knowledgeTagMapper;

    private KnowledgeTagService knowledgeTagService;

    @BeforeEach
    void setUp() {
        knowledgeTagService = new KnowledgeTagService(knowledgeTagMapper);
    }

    /** 验证扁平标签记录按层级转换为树。 */
    @Test
    void convertsFlatTagsToThreeLevelTree() {
        given(knowledgeTagMapper.selectList(any())).willReturn(List.of(
                node(1L, "Java 开发", 0L, 1),
                node(2L, "Spring Boot", 1L, 2),
                node(3L, "自动配置", 2L, 3)));

        List<KnowledgeTagTreeResponse> tree = knowledgeTagService.getTree();

        assertThat(tree).singleElement().satisfies(module -> {
            assertThat(module.name()).isEqualTo("Java 开发");
            assertThat(module.children()).singleElement().satisfies(secondary ->
                    assertThat(secondary.children()).singleElement().satisfies(tertiary ->
                            assertThat(tertiary.name()).isEqualTo("自动配置")));
        });
    }

    /** 创建带审计基础字段的测试标签。 */
    private KnowledgeTagEntity node(long id, String name, long parentId, int level) {
        KnowledgeTagEntity entity = new KnowledgeTagEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setParentId(parentId);
        entity.setLevel(level);
        entity.setSortOrder(10);
        entity.setDescription(null);
        entity.setDeleted(0);
        return entity;
    }
}
