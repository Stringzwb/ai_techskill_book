package com.aitechskill.book.user.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 用户会员等级。
 */
@Getter
public enum MemberLevel {
    SUPER("SUPER", "超级会员"),
    NORMAL("NORMAL", "普通会员"),
    GUEST("GUEST", "游客"),
    BANNED("BANNED", "已封禁");

    @EnumValue
    private final String code;
    private final String label;

    MemberLevel(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
