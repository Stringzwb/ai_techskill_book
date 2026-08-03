package com.aitechskill.book.home;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(KnowledgeHomeController.class)
class KnowledgeHomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KnowledgeHomeService service;

    @Test
    void returnsHomePayload() throws Exception {
        given(service.getHome()).willReturn(new HomeResponse(
                "技术岗AI知识库",
                "把复杂技术，变成可执行的成长路径",
                6,
                4,
                6,
                List.of(),
                List.of()));

        mockMvc.perform(get("/api/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName").value("技术岗AI知识库"))
                .andExpect(jsonPath("$.articleCount").value(6));
    }
}
