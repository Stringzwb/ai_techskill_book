package com.aitechskill.book.user.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 用户角色，用于区分普通用户与平台管理人员。
 */
@Getter
public enum UserRole {
    USER("USER", "普通用户"),
    SUPER_ADMIN("SUPER_ADMIN", "超级管理员");

    @EnumValue
    private final String code;
    private final String label;

    UserRole(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
