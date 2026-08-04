package com.aitechskill.book.auth.domain.response;

import com.aitechskill.book.user.domain.response.UserProfileResponse;

/**
 * 登录或注册成功响应。
 *
 * @param user 当前用户资料
 */
public record AuthResponse(UserProfileResponse user) {
}
