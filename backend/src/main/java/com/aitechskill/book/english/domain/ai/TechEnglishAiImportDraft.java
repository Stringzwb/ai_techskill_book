package com.aitechskill.book.english.domain.ai;

import java.time.Instant;
import java.util.List;

/**
 * 截图识别完成后等待用户确认的临时草稿。
 *
 * @param sessionUuid 页面一次上传会话标识
 * @param batchUuid 草稿批次标识
 * @param chunkIndex 当前并发子任务序号
 * @param chunkCount 并发子任务总数
 * @param userId 草稿所属用户
 * @param importType 识别模式，新草稿固定为 AUTO
 * @param sourceName 语料来源
 * @param scenario 例句场景
 * @param exampleCount 例句数量
 * @param imageFingerprints 原始截图指纹
 * @param payloadJson 已校验模板类型的 AI JSON
 * @param createdAt 草稿创建时间
 * @param batchName 用户填写的识图批次名称，仅用于识图记录
 */
public record TechEnglishAiImportDraft(
        String sessionUuid,
        String batchUuid,
        int chunkIndex,
        int chunkCount,
        long userId,
        String importType,
        String sourceName,
        String scenario,
        int exampleCount,
        List<ImageFingerprint> imageFingerprints,
        String payloadJson,
        Instant createdAt,
        String batchName) {

    /** 兼容未保存批次名称的旧 Redis 草稿。 */
    public TechEnglishAiImportDraft(
            String sessionUuid,
            String batchUuid,
            int chunkIndex,
            int chunkCount,
            long userId,
            String importType,
            String sourceName,
            String scenario,
            int exampleCount,
            List<ImageFingerprint> imageFingerprints,
            String payloadJson,
            Instant createdAt) {
        this(sessionUuid, batchUuid, chunkIndex, chunkCount, userId, importType, sourceName,
                scenario, exampleCount, imageFingerprints, payloadJson, createdAt, null);
    }

    /**
     * 原始截图的顺序和内容指纹。
     *
     * @param contentLength 文件体积
     * @param contentType 媒体类型
     * @param sha256 SHA-256 摘要
     */
    public record ImageFingerprint(long contentLength, String contentType, String sha256) {
    }
}
