package com.aitechskill.book.english.service;

import com.aitechskill.book.common.exception.BusinessException;
import com.aitechskill.book.english.domain.TechEnglishRecognitionSessionSummaryRow;
import com.aitechskill.book.english.domain.ai.TechEnglishAiImportDraft;
import com.aitechskill.book.english.domain.entity.TechEnglishAiRecognitionRecordEntity;
import com.aitechskill.book.english.domain.response.TechEnglishAiRecognitionItemResponse;
import com.aitechskill.book.english.domain.response.TechEnglishRecognitionHistoryDetailResponse;
import com.aitechskill.book.english.domain.response.TechEnglishRecognitionHistoryPageResponse;
import com.aitechskill.book.english.domain.response.TechEnglishRecognitionHistorySummaryResponse;
import com.aitechskill.book.english.domain.response.TechEnglishRecognitionHistoryTaskResponse;
import com.aitechskill.book.storage.domain.StoredObject;
import com.aitechskill.book.english.mapper.TechEnglishAiRecognitionRecordMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * 永久记录每个 AI 识图子任务并提供当前用户的只读历史。
 */
@Service
public class TechEnglishAiRecognitionRecordService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TechEnglishAiRecognitionRecordService.class);
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    private final TechEnglishAiRecognitionRecordMapper recordMapper;
    private final ObjectMapper objectMapper;

    public TechEnglishAiRecognitionRecordService(
            TechEnglishAiRecognitionRecordMapper recordMapper,
            ObjectMapper objectMapper) {
        this.recordMapper = recordMapper;
        this.objectMapper = objectMapper;
    }

    /** 在模型调用前创建可跟踪的识图记录。 */
    public void start(
            String sessionUuid,
            String batchUuid,
            int chunkIndex,
            int chunkCount,
            int imageCount,
            int exampleCount,
            String sourceName,
            String scenario,
            long userId) {
        TechEnglishAiRecognitionRecordEntity record = new TechEnglishAiRecognitionRecordEntity();
        record.setSessionUuid(sessionUuid);
        record.setBatchUuid(batchUuid);
        record.setStatus("PROCESSING");
        record.setSourceName(sourceName);
        record.setScenario(scenario);
        record.setChunkIndex(chunkIndex);
        record.setChunkCount(chunkCount);
        record.setImageCount(imageCount);
        record.setItemCount(0);
        record.setExampleCount(exampleCount);
        record.setCreateby(userId);
        record.setUpdateby(userId);
        recordMapper.insert(record);
    }

    /** 保存 AI 原始响应、清洗结果和原图对象元数据。 */
    public void recognized(
            String batchUuid,
            String rawResult,
            List<TechEnglishAiRecognitionItemResponse> items,
            List<StoredObject> sourceImages) {
        int updated = recordMapper.update(null, Wrappers.<TechEnglishAiRecognitionRecordEntity>lambdaUpdate()
                    .eq(TechEnglishAiRecognitionRecordEntity::getBatchUuid, batchUuid)
                    .eq(TechEnglishAiRecognitionRecordEntity::getDeleted, 0)
                    .set(TechEnglishAiRecognitionRecordEntity::getStatus, "RECOGNIZED")
                    .set(TechEnglishAiRecognitionRecordEntity::getItemCount, items.size())
                    .set(TechEnglishAiRecognitionRecordEntity::getResultJson, toJson(items))
                    .set(TechEnglishAiRecognitionRecordEntity::getRawResultJson, rawResult)
                    .set(TechEnglishAiRecognitionRecordEntity::getSourceImagesJson, toJson(sourceImages))
                    .set(TechEnglishAiRecognitionRecordEntity::getErrorCode, null)
                    .set(TechEnglishAiRecognitionRecordEntity::getErrorMessage, null)
                    .set(TechEnglishAiRecognitionRecordEntity::getCompletedAt, LocalDateTime.now()));
        if (updated != 1) {
            throw new IllegalStateException("技术英语 AI 识图记录不存在，批次=" + batchUuid);
        }
    }

    /** 在调用模型前记录已保存的来源截图，使失败批次也可从页面重试。 */
    public void sourceImagesSaved(String batchUuid, List<StoredObject> sourceImages) {
        int updated = recordMapper.update(null, Wrappers.<TechEnglishAiRecognitionRecordEntity>lambdaUpdate()
                .eq(TechEnglishAiRecognitionRecordEntity::getBatchUuid, batchUuid)
                .eq(TechEnglishAiRecognitionRecordEntity::getDeleted, 0)
                .set(TechEnglishAiRecognitionRecordEntity::getSourceImagesJson, toJson(sourceImages)));
        if (updated != 1) {
            throw new IllegalStateException("技术英语 AI 识图记录不存在，批次=" + batchUuid);
        }
    }

    /** 保存不包含上游原始响应的安全失败摘要。 */
    public void failed(String batchUuid, RuntimeException exception) {
        String errorCode = exception instanceof BusinessException business
                ? business.getCode() : "TECH_ENGLISH_AI_FAILED";
        String errorMessage = exception instanceof BusinessException business
                ? business.getMessage() : "AI 识别服务暂时不可用";
        try {
            recordMapper.update(null, Wrappers.<TechEnglishAiRecognitionRecordEntity>lambdaUpdate()
                    .eq(TechEnglishAiRecognitionRecordEntity::getBatchUuid, batchUuid)
                    .eq(TechEnglishAiRecognitionRecordEntity::getDeleted, 0)
                    .set(TechEnglishAiRecognitionRecordEntity::getStatus, "FAILED")
                    .set(TechEnglishAiRecognitionRecordEntity::getErrorCode, errorCode)
                    .set(TechEnglishAiRecognitionRecordEntity::getErrorMessage, abbreviate(errorMessage, 500))
                    .set(TechEnglishAiRecognitionRecordEntity::getCompletedAt, LocalDateTime.now()));
        } catch (DataAccessException databaseException) {
            LOGGER.error("技术英语 AI 识图失败摘要未能写入记录，批次={}", batchUuid);
        }
    }

    /** 确认入库成功后幂等更新记录状态。 */
    public void imported(String batchUuid) {
        try {
            int updated = recordMapper.update(null, Wrappers.<TechEnglishAiRecognitionRecordEntity>lambdaUpdate()
                    .eq(TechEnglishAiRecognitionRecordEntity::getBatchUuid, batchUuid)
                    .eq(TechEnglishAiRecognitionRecordEntity::getDeleted, 0)
                    .eq(TechEnglishAiRecognitionRecordEntity::getStatus, "RECOGNIZED")
                    .set(TechEnglishAiRecognitionRecordEntity::getStatus, "IMPORTED")
                    .set(TechEnglishAiRecognitionRecordEntity::getImportedAt, LocalDateTime.now()));
            if (updated != 1) {
                throw new IllegalStateException("技术英语 AI 识图入库状态更新失败，批次=" + batchUuid);
            }
        } catch (DataAccessException exception) {
            throw new IllegalStateException("技术英语 AI 识图入库状态更新失败，批次=" + batchUuid, exception);
        }
    }

    /** 为发布前已存在的 Redis 草稿在确认时补建记录。 */
    public void ensureLegacyRecognized(
            TechEnglishAiImportDraft draft,
            int imageCount,
            List<TechEnglishAiRecognitionItemResponse> items,
            long userId) {
        Long count = recordMapper.selectCount(Wrappers.<TechEnglishAiRecognitionRecordEntity>lambdaQuery()
                .eq(TechEnglishAiRecognitionRecordEntity::getBatchUuid, draft.batchUuid())
                .eq(TechEnglishAiRecognitionRecordEntity::getDeleted, 0));
        if (count != null && count > 0) {
            return;
        }
        String sessionUuid = draft.sessionUuid() == null ? draft.batchUuid() : draft.sessionUuid();
        int chunkIndex = draft.chunkIndex() <= 0 ? 1 : draft.chunkIndex();
        int chunkCount = draft.chunkCount() <= 0 ? 1 : draft.chunkCount();
        start(sessionUuid, draft.batchUuid(), chunkIndex, chunkCount, imageCount,
                draft.exampleCount(), draft.sourceName(), draft.scenario(), userId);
        recognized(draft.batchUuid(), draft.payloadJson(), items, List.of());
    }

    /** 分页查询当前用户的识图历史。 */
    public TechEnglishRecognitionHistoryPageResponse page(long userId, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        long total = recordMapper.countSessions(userId);
        List<TechEnglishRecognitionHistorySummaryResponse> items = total == 0
                ? List.of()
                : recordMapper.selectSessionPage(userId, (long) (safePage - 1) * safeSize, safeSize)
                        .stream().map(this::toSummary).toList();
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / safeSize);
        return new TechEnglishRecognitionHistoryPageResponse(
                total, safePage, safeSize, totalPages, items);
    }

    /** 读取当前用户一次上传会话的完整识图结果。 */
    public TechEnglishRecognitionHistoryDetailResponse detail(long userId, String sessionUuid) {
        List<TechEnglishAiRecognitionRecordEntity> tasks = recordMapper.selectSessionTasks(userId, sessionUuid);
        if (tasks.isEmpty()) {
            throw new BusinessException(HttpStatus.NOT_FOUND,
                    "TECH_ENGLISH_RECOGNITION_HISTORY_NOT_FOUND", "识图记录不存在");
        }
        List<TechEnglishRecognitionHistoryTaskResponse> responses = tasks.stream()
                .map(this::toTask).toList();
        TechEnglishAiRecognitionRecordEntity first = tasks.get(0);
        String status = aggregateTaskStatus(tasks);
        int imageCount = tasks.stream().mapToInt(item -> safeInt(item.getImageCount())).sum();
        int itemCount = tasks.stream().mapToInt(item -> safeInt(item.getItemCount())).sum();
        LocalDateTime updatedAt = tasks.stream()
                .map(TechEnglishAiRecognitionRecordEntity::getUpdatetime)
                .max(LocalDateTime::compareTo)
                .orElse(first.getUpdatetime());
        return new TechEnglishRecognitionHistoryDetailResponse(
                sessionUuid,
                status,
                first.getSourceName(),
                first.getScenario(),
                imageCount,
                itemCount,
                first.getCreatetime(),
                updatedAt,
                responses);
    }

    /** 软删除当前用户的一次识图会话，并返回删除前的任务供清理来源图片。 */
    public List<TechEnglishAiRecognitionRecordEntity> deleteSession(long userId, String sessionUuid) {
        List<TechEnglishAiRecognitionRecordEntity> tasks = recordMapper.selectSessionTasks(userId, sessionUuid);
        if (tasks.isEmpty()) {
            throw new BusinessException(HttpStatus.NOT_FOUND,
                    "TECH_ENGLISH_RECOGNITION_HISTORY_NOT_FOUND", "识图记录不存在");
        }
        if (tasks.stream().anyMatch(task -> "PROCESSING".equals(task.getStatus()))) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "TECH_ENGLISH_RECOGNITION_DELETE_PROCESSING", "识图任务仍在处理中，请完成或等待后再删除");
        }
        int updated = recordMapper.update(null, Wrappers.<TechEnglishAiRecognitionRecordEntity>lambdaUpdate()
                .eq(TechEnglishAiRecognitionRecordEntity::getCreateby, userId)
                .eq(TechEnglishAiRecognitionRecordEntity::getSessionUuid, sessionUuid)
                .eq(TechEnglishAiRecognitionRecordEntity::getDeleted, 0)
                .set(TechEnglishAiRecognitionRecordEntity::getDeleted, 1)
                .set(TechEnglishAiRecognitionRecordEntity::getUpdateby, userId)
                .set(TechEnglishAiRecognitionRecordEntity::getUpdatetime, LocalDateTime.now()));
        if (updated != tasks.size()) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "TECH_ENGLISH_RECOGNITION_DELETE_CONFLICT", "识图任务状态已变化，请刷新后重试");
        }
        return List.copyOf(tasks);
    }

    /** 读取当前用户某个识图批次的详情。 */
    public TechEnglishRecognitionHistoryTaskResponse batch(long userId, String batchUuid) {
        TechEnglishAiRecognitionRecordEntity task = recordMapper.selectOne(Wrappers.<TechEnglishAiRecognitionRecordEntity>lambdaQuery()
                .eq(TechEnglishAiRecognitionRecordEntity::getCreateby, userId)
                .eq(TechEnglishAiRecognitionRecordEntity::getBatchUuid, batchUuid)
                .eq(TechEnglishAiRecognitionRecordEntity::getDeleted, 0));
        if (task == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND,
                    "TECH_ENGLISH_RECOGNITION_HISTORY_NOT_FOUND", "识图记录不存在");
        }
        return toTask(task);
    }

    /** 读取当前用户某个识图批次是否已入库。 */
    public boolean isImported(long userId, String batchUuid) {
        return recordMapper.selectCount(Wrappers.<TechEnglishAiRecognitionRecordEntity>lambdaQuery()
                .eq(TechEnglishAiRecognitionRecordEntity::getCreateby, userId)
                .eq(TechEnglishAiRecognitionRecordEntity::getBatchUuid, batchUuid)
                .eq(TechEnglishAiRecognitionRecordEntity::getStatus, "IMPORTED")
                .eq(TechEnglishAiRecognitionRecordEntity::getDeleted, 0)) > 0;
    }

    /** 读取可从历史批次继续入库的记录。 */
    public TechEnglishAiRecognitionRecordEntity requireImportRecord(long userId, String batchUuid) {
        TechEnglishAiRecognitionRecordEntity record = findImportRecord(userId, batchUuid);
        if (record == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND,
                    "TECH_ENGLISH_RECOGNITION_HISTORY_NOT_FOUND", "识图记录不存在");
        }
        if ("IMPORTED".equals(record.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "TECH_ENGLISH_AI_ALREADY_IMPORTED", "该识别结果已经入库，请勿重复提交");
        }
        if (!"RECOGNIZED".equals(record.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "TECH_ENGLISH_AI_NOT_READY", "该批次当前没有可入库的识别结果");
        }
        if (record.getRawResultJson() == null || record.getRawResultJson().isBlank()) {
            throw new BusinessException(HttpStatus.GONE,
                    "TECH_ENGLISH_AI_SOURCE_UNAVAILABLE", "该历史批次缺少完整识别结果，请重新识别");
        }
        return record;
    }

    /** 查询当前用户的识图记录，不改变不存在时的业务分支。 */
    public TechEnglishAiRecognitionRecordEntity findImportRecord(long userId, String batchUuid) {
        return recordMapper.selectOne(
                Wrappers.<TechEnglishAiRecognitionRecordEntity>lambdaQuery()
                        .eq(TechEnglishAiRecognitionRecordEntity::getCreateby, userId)
                        .eq(TechEnglishAiRecognitionRecordEntity::getBatchUuid, batchUuid)
                        .eq(TechEnglishAiRecognitionRecordEntity::getDeleted, 0));
    }

    /** 锁定失败批次的一次重试，避免同一批次并发重复调用模型。 */
    public TechEnglishAiRecognitionRecordEntity beginRetry(long userId, String batchUuid) {
        TechEnglishAiRecognitionRecordEntity record = findImportRecord(userId, batchUuid);
        if (record == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND,
                    "TECH_ENGLISH_RECOGNITION_HISTORY_NOT_FOUND", "识图记录不存在");
        }
        if (!"FAILED".equals(record.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "TECH_ENGLISH_AI_RETRY_NOT_ALLOWED", "只有失败的识图批次可以重试");
        }
        if (sourceImages(record).isEmpty()) {
            throw new BusinessException(HttpStatus.GONE,
                    "TECH_ENGLISH_AI_SOURCE_UNAVAILABLE", "该失败批次没有可重试的来源截图，请重新上传本组图片");
        }
        int updated = recordMapper.update(null, Wrappers.<TechEnglishAiRecognitionRecordEntity>lambdaUpdate()
                .eq(TechEnglishAiRecognitionRecordEntity::getId, record.getId())
                .eq(TechEnglishAiRecognitionRecordEntity::getStatus, "FAILED")
                .eq(TechEnglishAiRecognitionRecordEntity::getDeleted, 0)
                .set(TechEnglishAiRecognitionRecordEntity::getStatus, "PROCESSING")
                .set(TechEnglishAiRecognitionRecordEntity::getErrorCode, null)
                .set(TechEnglishAiRecognitionRecordEntity::getErrorMessage, null)
                .set(TechEnglishAiRecognitionRecordEntity::getCompletedAt, null));
        if (updated != 1) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "TECH_ENGLISH_AI_RETRYING", "该识图批次正在重试，请稍后查看结果");
        }
        record.setStatus("PROCESSING");
        record.setErrorCode(null);
        record.setErrorMessage(null);
        record.setCompletedAt(null);
        return record;
    }

    /** 读取识别阶段已经保存的原图对象。 */
    public List<StoredObject> sourceImages(TechEnglishAiRecognitionRecordEntity record) {
        if (record.getSourceImagesJson() == null || record.getSourceImagesJson().isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(record.getSourceImagesJson(),
                    new TypeReference<List<StoredObject>>() { });
        } catch (JsonProcessingException exception) {
            throw new BusinessException(HttpStatus.GONE,
                    "TECH_ENGLISH_AI_SOURCE_UNAVAILABLE", "该批次原图记录无法读取", exception);
        }
    }

    /** 将聚合查询转换为页面摘要。 */
    private TechEnglishRecognitionHistorySummaryResponse toSummary(
            TechEnglishRecognitionSessionSummaryRow row) {
        int taskCount = safeLong(row.getTaskCount());
        int processingCount = safeLong(row.getProcessingCount());
        int failedCount = safeLong(row.getFailedCount());
        int importedCount = safeLong(row.getImportedCount());
        String status = aggregateStatus(taskCount, processingCount, failedCount, importedCount);
        return new TechEnglishRecognitionHistorySummaryResponse(
                row.getSessionUuid(),
                status,
                row.getSourceName(),
                row.getScenario(),
                safeInt(row.getChunkCount()),
                taskCount - processingCount,
                safeLong(row.getImageCount()),
                safeLong(row.getItemCount()),
                importedCount,
                row.getCreatetime(),
                row.getUpdatetime());
    }

    /** 将数据库子任务转换为历史详情。 */
    private TechEnglishRecognitionHistoryTaskResponse toTask(
            TechEnglishAiRecognitionRecordEntity task) {
        return new TechEnglishRecognitionHistoryTaskResponse(
                task.getBatchUuid(),
                task.getStatus(),
                safeInt(task.getChunkIndex()),
                safeInt(task.getChunkCount()),
                safeInt(task.getImageCount()),
                safeInt(task.getItemCount()),
                task.getErrorCode(),
                task.getErrorMessage(),
                task.getCreatetime(),
                task.getCompletedAt(),
                task.getImportedAt(),
                parseItems(task.getResultJson()));
    }

    /** 根据子任务状态计算上传会话状态。 */
    private String aggregateTaskStatus(List<TechEnglishAiRecognitionRecordEntity> tasks) {
        int processing = (int) tasks.stream().filter(item -> "PROCESSING".equals(item.getStatus())).count();
        int failed = (int) tasks.stream().filter(item -> "FAILED".equals(item.getStatus())).count();
        int imported = (int) tasks.stream().filter(item -> "IMPORTED".equals(item.getStatus())).count();
        return aggregateStatus(tasks.size(), processing, failed, imported);
    }

    /** 根据子任务计数计算上传会话状态。 */
    private String aggregateStatus(int taskCount, int processing, int failed, int imported) {
        if (processing > 0) {
            return "PROCESSING";
        }
        if (failed == taskCount) {
            return "FAILED";
        }
        if (failed > 0 || (imported > 0 && imported < taskCount)) {
            return "PARTIAL";
        }
        if (imported == taskCount && taskCount > 0) {
            return "IMPORTED";
        }
        return "RECOGNIZED";
    }

    /** 解析已清洗的识图预览项。 */
    private List<TechEnglishAiRecognitionItemResponse> parseItems(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() { });
        } catch (JsonProcessingException exception) {
            LOGGER.warn("技术英语 AI 识图历史结果无法解析");
            return List.of();
        }
    }

    /** 序列化已清洗的识图结果。 */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法序列化技术英语识图记录", exception);
        }
    }

    /** 将可空整数转换为安全数值。 */
    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    /** 将可空长整数转换为页面计数。 */
    private int safeLong(Long value) {
        if (value == null) {
            return 0;
        }
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : value.intValue();
    }

    /** 截断可展示的失败说明。 */
    private String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }
}
