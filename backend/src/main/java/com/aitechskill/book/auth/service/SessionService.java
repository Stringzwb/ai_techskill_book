package com.aitechskill.book.auth.service;

import com.aitechskill.book.auth.domain.SessionRecord;
import com.aitechskill.book.common.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * 使用 Redis 管理用户会话。
 */
@Service
public class SessionService {

    private static final String SESSION_KEY_PREFIX = "auth:session:";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration sessionTtl;

    public SessionService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${app.session.ttl}") Duration sessionTtl) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.sessionTtl = sessionTtl;
    }

    /**
     * 创建用户会话。
     *
     * @param userId 用户主键
     * @return 原始会话令牌
     */
    public String createSession(Long userId) {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        SessionRecord record = new SessionRecord(userId, Instant.now());
        try {
            redisTemplate.opsForValue().set(sessionKey(token), objectMapper.writeValueAsString(record), sessionTtl);
            return token;
        } catch (DataAccessException exception) {
            throw redisUnavailable(exception);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法序列化用户会话", exception);
        }
    }

    /**
     * 读取并续期用户会话。
     *
     * @param token 原始会话令牌
     * @return 会话记录
     */
    public SessionRecord requireSession(String token) {
        try {
            String key = sessionKey(token);
            String value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                throw unauthorized();
            }
            redisTemplate.expire(key, sessionTtl);
            return objectMapper.readValue(value, SessionRecord.class);
        } catch (BusinessException exception) {
            throw exception;
        } catch (DataAccessException exception) {
            throw redisUnavailable(exception);
        } catch (JsonProcessingException exception) {
            revokeSession(token);
            throw unauthorized();
        }
    }

    /**
     * 注销用户会话。
     *
     * @param token 原始会话令牌
     */
    public void revokeSession(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        try {
            redisTemplate.delete(sessionKey(token));
        } catch (DataAccessException exception) {
            throw redisUnavailable(exception);
        }
    }

    /** 生成只包含令牌摘要的 Redis 键。 */
    private String sessionKey(String token) {
        if (token == null || token.length() < 32 || token.length() > 128) {
            throw unauthorized();
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return SESSION_KEY_PREFIX + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前环境不支持 SHA-256", exception);
        }
    }

    /** 创建未登录异常。 */
    private BusinessException unauthorized() {
        return new BusinessException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "登录已失效，请重新登录");
    }

    /** 创建会话服务不可用异常。 */
    private BusinessException redisUnavailable(Exception cause) {
        return new BusinessException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "SESSION_UNAVAILABLE",
                "会话服务暂时不可用",
                cause);
    }
}
