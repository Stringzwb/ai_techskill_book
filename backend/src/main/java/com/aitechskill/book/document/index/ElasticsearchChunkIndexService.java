package com.aitechskill.book.document.index;

import com.aitechskill.book.document.domain.DocumentTagRecord;
import com.aitechskill.book.document.domain.entity.KnowledgeDocumentEntity;
import com.aitechskill.book.document.mapper.KnowledgeDocumentMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** 通过 Elasticsearch REST API 替换一篇文档的全部 Markdown 分块。 */
@Service
@ConditionalOnProperty(name = "app.index.enabled", havingValue = "true")
public class ElasticsearchChunkIndexService {

    private final KnowledgeDocumentMapper documentMapper;
    private final MarkdownChunker chunker;
    private final DocumentEmbeddingService embeddingService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String endpoint;
    private final String username;
    private final String password;
    private final String indexName;
    private final int chunkSize;
    private final int chunkOverlap;
    private final int embeddingDimensions;
    private volatile boolean indexReady;

    public ElasticsearchChunkIndexService(
            KnowledgeDocumentMapper documentMapper,
            MarkdownChunker chunker,
            ObjectProvider<DocumentEmbeddingService> embeddingService,
            ObjectMapper objectMapper,
            @Value("${app.index.elasticsearch.endpoint}") String endpoint,
            @Value("${app.index.elasticsearch.username:}") String username,
            @Value("${app.index.elasticsearch.password:}") String password,
            @Value("${app.index.elasticsearch.index}") String indexName,
            @Value("${app.index.elasticsearch.chunk-size}") int chunkSize,
            @Value("${app.index.elasticsearch.chunk-overlap}") int chunkOverlap,
            @Value("${app.index.embedding.dimensions:1536}") int embeddingDimensions) {
        this.documentMapper = documentMapper;
        this.chunker = chunker;
        this.embeddingService = embeddingService.getIfAvailable();
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        this.endpoint = endpoint.replaceAll("/+$", "");
        this.username = username;
        this.password = password;
        this.indexName = indexName;
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
        this.embeddingDimensions = embeddingDimensions;
    }

    /** 删除文档的所有旧分块。 */
    public void deleteDocument(long documentId) {
        ensureIndex();
        Map<String, Object> body = Map.of(
                "query", Map.of("term", Map.of("document_id", documentId)));
        request("POST", "/" + indexName + "/_delete_by_query?conflicts=proceed&refresh=wait_for", body);
    }

    /** 删除旧版本后批量写入当前正文分块。 */
    public void replaceDocument(KnowledgeDocumentEntity document) {
        if (document.getMarkdownContent() == null) {
            throw new IllegalStateException("Markdown 正文尚未迁移到 MySQL");
        }
        deleteDocument(document.getId());
        List<DocumentTagRecord> tags = documentMapper.selectTagsByDocumentIds(List.of(document.getId()));
        List<MarkdownChunker.Chunk> chunks = chunker.chunk(document.getMarkdownContent(), chunkSize, chunkOverlap);
        if (chunks.isEmpty()) {
            return;
        }
        List<float[]> embeddings = embeddingService == null
                ? List.of()
                : embeddingService.embedAll(chunks.stream().map(MarkdownChunker.Chunk::content).toList());
        List<String> lines = new ArrayList<>(chunks.size() * 2);
        for (MarkdownChunker.Chunk chunk : chunks) {
            Map<String, Object> action = Map.of(
                    "index", Map.of("_id", document.getId() + ":" + document.getContentVersion() + ":" + chunk.index()));
            lines.add(write(action));
            Map<String, Object> source = new LinkedHashMap<>();
            source.put("document_id", document.getId());
            source.put("content_version", document.getContentVersion());
            source.put("chunk_index", chunk.index());
            source.put("title", document.getTitle());
            source.put("summary", document.getSummary());
            source.put("heading", chunk.heading());
            source.put("content", chunk.content());
            source.put("tag_names", tags.stream().map(DocumentTagRecord::name).toList());
            if (!embeddings.isEmpty()) {
                source.put("embedding", embeddings.get(chunk.index()));
            }
            lines.add(write(source));
        }
        requestBulk("/" + indexName + "/_bulk?refresh=wait_for", String.join("\n", lines) + "\n");
    }

    /** 初始化文档索引 mapping；索引已存在时保留现有 mapping。 */
    private void ensureIndex() {
        if (indexReady) {
            return;
        }
        synchronized (this) {
            if (indexReady) {
                return;
            }
            Map<String, Object> properties = new LinkedHashMap<>();
            properties.put("document_id", Map.of("type", "long"));
            properties.put("content_version", Map.of("type", "long"));
            properties.put("chunk_index", Map.of("type", "integer"));
            properties.put("title", Map.of("type", "text"));
            properties.put("summary", Map.of("type", "text"));
            properties.put("heading", Map.of("type", "keyword"));
            properties.put("content", Map.of("type", "text"));
            properties.put("tag_names", Map.of("type", "keyword"));
            properties.put("embedding", Map.of(
                    "type", "dense_vector",
                    "dims", embeddingDimensions,
                    "index", true,
                    "similarity", "cosine"));
            Map<String, Object> body = Map.of("mappings", Map.of("properties", properties));
            int status = requestStatus("PUT", "/" + indexName, body);
            if (status != 200 && status != 201 && status != 400) {
                throw new IllegalStateException("Elasticsearch 索引初始化失败，HTTP " + status);
            }
            indexReady = true;
        }
    }

    private void requestBulk(String path, String body) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(endpoint + path))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/x-ndjson")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        applyAuthentication(builder);
        send(builder.build());
    }

    private void request(String method, String path, Map<String, Object> body) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(endpoint + path))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(body), StandardCharsets.UTF_8));
            applyAuthentication(builder);
            int status = sendStatus(builder.build());
            if (status < 200 || status >= 300) {
                throw new IllegalStateException("Elasticsearch 请求失败，HTTP " + status);
            }
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Elasticsearch 请求序列化失败", exception);
        }
    }

    private String write(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Elasticsearch 文档序列化失败", exception);
        }
    }

    private void applyAuthentication(HttpRequest.Builder builder) {
        if (!username.isBlank()) {
            String credentials = username + ":" + password;
            String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            builder.header("Authorization", "Basic " + encoded);
        }
    }

    private void send(HttpRequest request) {
        int status = sendStatus(request);
        if (status < 200 || status >= 300) {
            throw new IllegalStateException("Elasticsearch 请求失败，HTTP " + status);
        }
    }

    private int requestStatus(String method, String path, Map<String, Object> body) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(endpoint + path))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(body), StandardCharsets.UTF_8));
            applyAuthentication(builder);
            return sendStatus(builder.build());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Elasticsearch 请求序列化失败", exception);
        }
    }

    private int sendStatus(HttpRequest request) {
        try {
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Elasticsearch 请求被中断", exception);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Elasticsearch 请求失败", exception);
        }
    }
}
