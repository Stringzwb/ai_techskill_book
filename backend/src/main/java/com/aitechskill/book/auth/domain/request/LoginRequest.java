package com.aitechskill.book.auth.domain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 密码登录请求。
 *
 * @param account 用户名、手机号或邮箱
 * @param password 密码
 */
public record LoginRequest(
        @NotBlank(message = "请输入账号")
        @Size(max = 128, message = "账号不能超过128个字符")
        String account,
        @NotBlank(message = "请输入密码")
        @Size(max = 64, message = "密码不能超过64位")
        String password) {
}
