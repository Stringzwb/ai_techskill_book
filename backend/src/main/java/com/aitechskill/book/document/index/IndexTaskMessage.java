package com.aitechskill.book.document.index;

/** Kafka 文档索引任务消息。 */
public record IndexTaskMessage(
        long taskId,
        long documentId,
        long contentVersion,
        String taskType) {
}
