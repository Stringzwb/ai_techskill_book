package com.aitechskill.book.auth.domain;

import com.aitechskill.book.user.domain.response.UserProfileResponse;

/**
 * 认证服务内部结果。
 *
 * @param token 原始会话令牌
 * @param user 用户资料
 */
public record AuthResult(String token, UserProfileResponse user) {
}
