package com.aitechskill.book.document.index;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.data.segment.TextSegment;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** 使用 OpenAI 兼容 Embeddings API 生成 RAG 向量。 */
@Service
@ConditionalOnProperty(name = "app.index.embedding.enabled", havingValue = "true")
public class OpenAiDocumentEmbeddingService implements DocumentEmbeddingService {

    private final EmbeddingModel embeddingModel;

    public OpenAiDocumentEmbeddingService(
            @Value("${app.index.embedding.api-key}") String apiKey,
            @Value("${app.index.embedding.base-url}") String baseUrl,
            @Value("${app.index.embedding.model}") String model) {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("INDEX_EMBEDDING_ENABLED=true 时必须配置 OPENAI_API_KEY");
        }
        this.embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(model)
                .build();
    }

    @Override
    public List<float[]> embedAll(List<String> texts) {
        return embeddingModel.embedAll(texts.stream().map(TextSegment::from).toList()).content().stream()
                .map(embedding -> embedding.vector())
                .toList();
    }
}
