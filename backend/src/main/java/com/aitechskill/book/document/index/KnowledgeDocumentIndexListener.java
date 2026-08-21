package com.aitechskill.book.document.index;

import com.aitechskill.book.document.domain.entity.KnowledgeDocumentEntity;
import com.aitechskill.book.document.mapper.KnowledgeDocumentMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** 消费文档索引任务并更新 Elasticsearch。 */
@Component
@ConditionalOnProperty(name = "app.index.enabled", havingValue = "true")
public class KnowledgeDocumentIndexListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(KnowledgeDocumentIndexListener.class);

    private final KnowledgeDocumentMapper documentMapper;
    private final ElasticsearchChunkIndexService indexService;
    private final ObjectMapper objectMapper;

    public KnowledgeDocumentIndexListener(
            KnowledgeDocumentMapper documentMapper,
            ElasticsearchChunkIndexService indexService,
            ObjectMapper objectMapper) {
        this.documentMapper = documentMapper;
        this.indexService = indexService;
        this.objectMapper = objectMapper;
    }

    /** 处理索引消息；消息正文始终从 MySQL 读取。 */
    @KafkaListener(
            topics = "${app.index.kafka.topic}",
            groupId = "${app.index.kafka.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void onMessage(String payload) {
        try {
            process(objectMapper.readValue(payload, IndexTaskMessage.class));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("文档索引消息格式无效，已忽略");
        }
    }

    private void process(IndexTaskMessage message) {
        if (documentMapper.claimIndexTask(message.taskId()) != 1) {
            return;
        }
        try {
            KnowledgeDocumentEntity document = documentMapper.selectForIndex(message.documentId());
            if (document == null || !Long.valueOf(message.contentVersion()).equals(document.getContentVersion())) {
                documentMapper.markIndexTaskSucceeded(message.taskId());
                return;
            }
            if ("DELETE".equals(message.taskType())) {
                indexService.deleteDocument(message.documentId());
            } else if ("UPSERT".equals(message.taskType()) && "PUBLISHED".equals(document.getStatus())) {
                indexService.replaceDocument(document);
            }
            documentMapper.markIndexTaskSucceeded(message.taskId());
            documentMapper.markDocumentIndexSucceeded(message.documentId(), message.contentVersion());
        } catch (RuntimeException exception) {
            String error = "索引处理失败";
            documentMapper.markIndexTaskFailed(message.taskId(), error, LocalDateTime.now().plusSeconds(30));
            documentMapper.markDocumentIndexFailed(message.documentId(), message.contentVersion(), error);
            LOGGER.warn("文档索引处理失败，taskId={}", message.taskId());
        }
    }
}
