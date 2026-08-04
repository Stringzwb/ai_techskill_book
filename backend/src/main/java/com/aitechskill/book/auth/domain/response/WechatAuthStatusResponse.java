package com.aitechskill.book.auth.domain.response;

/**
 * 微信登录能力状态。
 *
 * @param enabled 是否已启用
 * @param message 状态说明
 */
public record WechatAuthStatusResponse(boolean enabled, String message) {
}
