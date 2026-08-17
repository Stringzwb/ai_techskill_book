package com.aitechskill.book.tag.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aitechskill.book.auth.config.AuthInterceptor;
import com.aitechskill.book.tag.domain.response.KnowledgeTagTreeResponse;
import com.aitechskill.book.tag.service.KnowledgeTagService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 知识标签树公开接口测试。
 */
@WebMvcTest(KnowledgeTagController.class)
class KnowledgeTagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KnowledgeTagService knowledgeTagService;

    @MockBean
    private AuthInterceptor authInterceptor;

    /** 验证树接口返回三级节点结构。 */
    @Test
    void returnsKnowledgeTagTree() throws Exception {
        KnowledgeTagTreeResponse leaf = new KnowledgeTagTreeResponse(
                3L, "自动配置", 3, 10, "自动装配与条件注解", List.of());
        KnowledgeTagTreeResponse child = new KnowledgeTagTreeResponse(
                2L, "Spring Boot", 2, 10, "应用开发与生产配置", List.of(leaf));
        given(knowledgeTagService.getTree()).willReturn(List.of(new KnowledgeTagTreeResponse(
                1L, "Java 开发", 1, 10, "Java 服务端工程知识模块", List.of(child))));

        mockMvc.perform(get("/api/knowledge-tags/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Java 开发"))
                .andExpect(jsonPath("$[0].children[0].children[0].level").value(3));
    }
}
