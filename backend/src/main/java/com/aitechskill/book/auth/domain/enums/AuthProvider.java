package com.aitechskill.book.auth.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 用户认证来源。
 */
@Getter
public enum AuthProvider {
    PASSWORD("PASSWORD", "密码"),
    WECHAT("WECHAT", "微信");

    @EnumValue
    private final String code;
    private final String label;

    AuthProvider(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
