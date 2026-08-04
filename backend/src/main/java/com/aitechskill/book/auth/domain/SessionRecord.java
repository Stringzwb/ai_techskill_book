package com.aitechskill.book.auth.domain;

import java.time.Instant;

/**
 * Redis 用户会话记录。
 *
 * @param userId 用户主键
 * @param createdAt 会话创建时间
 */
public record SessionRecord(Long userId, Instant createdAt) {
}
