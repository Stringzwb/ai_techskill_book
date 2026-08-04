package com.aitechskill.book.user.domain.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 修改个人资料请求，头像和会员信息不允许由用户修改。
 *
 * @param username 用户名
 * @param phone 手机号
 * @param email 邮箱
 */
public record UpdateProfileRequest(
        @NotBlank(message = "请输入用户名")
        @Pattern(regexp = "^[\\p{L}\\p{N}_-]{3,32}$", message = "用户名需为3-32位文字、字母、数字、下划线或短横线")
        String username,
        @NotBlank(message = "请输入手机号")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的11位手机号")
        String phone,
        @NotBlank(message = "请输入邮箱")
        @Email(message = "请输入正确的邮箱")
        @Size(max = 128, message = "邮箱不能超过128个字符")
        String email) {
}
