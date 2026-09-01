package com.aitechskill.book.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.aitechskill.book.ai.config.AiModelClients;
import com.aitechskill.book.ai.config.AiModelProperties;
import com.aitechskill.book.common.exception.BusinessException;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

/**
 * AI 主备调用和图片校验测试。
 */
@ExtendWith(MockitoExtension.class)
class AiChatServiceTest {

    @Mock
    private ChatLanguageModel primaryModel;

    @Mock
    private ChatLanguageModel secondaryModel;

    private AiChatService service;

    @BeforeEach
    void setUp() {
        AiModelProperties properties = new AiModelProperties();
        properties.setModel("gpt-5.5");
        properties.setImageMaxSize(DataSize.ofMegabytes(1));
        service = new AiChatService(new AiModelClients(List.of(primaryModel, secondaryModel)), properties);
    }

    @Test
    void returnsTextResponseFromPrimaryModel() {
        given(primaryModel.generate(anyList())).willReturn(success("primary reply"));

        var response = service.chat("  hello  ");

        assertThat(response.text()).isEqualTo("primary reply");
        assertThat(response.model()).isEqualTo("gpt-5.5");
        assertThat(response.inputTokens()).isEqualTo(12);
        assertThat(response.outputTokens()).isEqualTo(5);
        assertThat(response.totalTokens()).isEqualTo(17);
    }

    @Test
    void fallsBackToSecondaryModel() {
        given(primaryModel.generate(anyList())).willThrow(new IllegalStateException("upstream unavailable"));
        given(secondaryModel.generate(anyList())).willReturn(success("secondary reply"));

        var response = service.chat("hello");

        assertThat(response.text()).isEqualTo("secondary reply");
        verify(secondaryModel).generate(anyList());
    }

    @Test
    void sendsValidatedImageAndTextToModel() {
        given(primaryModel.generate(anyList())).willReturn(success("vision reply"));
        byte[] png = new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x01
        };
        MockMultipartFile image = new MockMultipartFile("image", "sample.png", "image/png", png);

        var response = service.vision("识别图片", image);

        assertThat(response.text()).isEqualTo("vision reply");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ChatMessage>> messagesCaptor = ArgumentCaptor.forClass(List.class);
        verify(primaryModel).generate(messagesCaptor.capture());
        UserMessage userMessage = (UserMessage) messagesCaptor.getValue().get(0);
        assertThat(userMessage.contents()).hasSize(2);
        assertThat(userMessage.contents().get(1)).isInstanceOf(ImageContent.class);
    }

    @Test
    void rejectsImageWhenSignatureDoesNotMatchMimeType() {
        MockMultipartFile image = new MockMultipartFile(
                "image", "fake.png", "image/png", new byte[] {0x01, 0x02, 0x03});

        assertThatThrownBy(() -> service.vision("识别图片", image))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("AI_IMAGE_INVALID");
    }

    /** 创建带 Token 用量的模型成功响应。 */
    private Response<AiMessage> success(String text) {
        return Response.from(AiMessage.from(text), new TokenUsage(12, 5));
    }
}
