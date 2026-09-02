package com.aitechskill.book.english.service;

import com.aitechskill.book.common.exception.BusinessException;
import com.aitechskill.book.english.config.TechEnglishImportProperties;
import com.aitechskill.book.english.domain.ai.TechEnglishAiImportDraft;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * 使用 Redis 保存短期 AI 识别草稿和确认锁。
 */
@Service
public class TechEnglishAiImportDraftStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(TechEnglishAiImportDraftStore.class);
    private static final String DRAFT_KEY_PREFIX = "tech-english:ai-import:draft:";
    private static final String LOCK_KEY_PREFIX = "tech-english:ai-import:lock:";
    private static final Duration CONFIRM_LOCK_TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration draftTtl;

    public TechEnglishAiImportDraftStore(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            TechEnglishImportProperties properties) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.draftTtl = properties.getDraftTtl();
        if (draftTtl == null || draftTtl.isZero() || draftTtl.isNegative()) {
            throw new IllegalStateException("TECH_ENGLISH_IMPORT_DRAFT_TTL 必须大于 0");
        }
    }

    /** 保存等待确认的识别草稿。 */
    public void save(TechEnglishAiImportDraft draft) {
        try {
            redisTemplate.opsForValue().set(
                    draftKey(draft.batchUuid()),
                    objectMapper.writeValueAsString(draft),
                    draftTtl);
        } catch (DataAccessException exception) {
            throw unavailable(exception);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法序列化技术英语识别草稿", exception);
        }
    }

    /** 读取当前用户尚未过期的识别草稿。 */
    public TechEnglishAiImportDraft require(String batchUuid, long userId) {
        try {
            String value = redisTemplate.opsForValue().get(draftKey(batchUuid));
            if (value == null) {
                throw expired();
            }
            TechEnglishAiImportDraft draft = objectMapper.readValue(value, TechEnglishAiImportDraft.class);
            if (draft.userId() != userId) {
                throw expired();
            }
            return draft;
        } catch (BusinessException exception) {
            throw exception;
        } catch (DataAccessException exception) {
            throw unavailable(exception);
        } catch (JsonProcessingException exception) {
            throw expired();
        }
    }

    /** 获取短期确认锁，避免同一草稿并发重复入库。 */
    public boolean acquireConfirmation(String batchUuid, long userId) {
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(
                    lockKey(batchUuid), Long.toString(userId), CONFIRM_LOCK_TTL));
        } catch (DataAccessException exception) {
            throw unavailable(exception);
        }
    }

    /** 确认成功后尽力清理草稿和确认锁，不影响已提交的数据库结果。 */
    public void complete(String batchUuid) {
        try {
            redisTemplate.delete(java.util.List.of(draftKey(batchUuid), lockKey(batchUuid)));
        } catch (DataAccessException exception) {
            LOGGER.warn("技术英语 AI 草稿确认成功后未能从 Redis 清理，批次={}", batchUuid);
        }
    }

    /** 删除用户主动移除的识图草稿及确认锁。 */
    public void discard(String batchUuid) {
        try {
            redisTemplate.delete(java.util.List.of(draftKey(batchUuid), lockKey(batchUuid)));
        } catch (DataAccessException exception) {
            LOGGER.warn("技术英语 AI 识图任务删除后未能清理 Redis 草稿，批次={}", batchUuid);
        }
    }

    /** 确认失败后尽力释放锁，允许用户重试。 */
    public void releaseConfirmation(String batchUuid) {
        try {
            redisTemplate.delete(lockKey(batchUuid));
        } catch (DataAccessException exception) {
            LOGGER.warn("技术英语 AI 草稿确认失败后未能释放 Redis 锁，批次={}", batchUuid);
        }
    }

    /** 返回草稿有效期。 */
    public Duration draftTtl() {
        return draftTtl;
    }

    /** 创建草稿键。 */
    private String draftKey(String batchUuid) {
        return DRAFT_KEY_PREFIX + batchUuid;
    }

    /** 创建确认锁键。 */
    private String lockKey(String batchUuid) {
        return LOCK_KEY_PREFIX + batchUuid;
    }

    /** 创建草稿过期异常。 */
    private BusinessException expired() {
        return new BusinessException(
                HttpStatus.GONE,
                "TECH_ENGLISH_AI_DRAFT_EXPIRED",
                "识别结果已过期，请重新上传截图识别");
    }

    /** 创建 Redis 暂不可用异常。 */
    private BusinessException unavailable(Exception cause) {
        return new BusinessException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "TECH_ENGLISH_AI_DRAFT_UNAVAILABLE",
                "识别结果暂时无法保存，请稍后重试",
                cause);
    }
}
