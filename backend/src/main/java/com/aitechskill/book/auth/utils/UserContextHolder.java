package com.aitechskill.book.auth.utils;

import com.aitechskill.book.common.exception.BusinessException;
import java.util.Optional;
import org.springframework.http.HttpStatus;

/**
 * 保存当前请求的登录用户。
 */
public final class UserContextHolder {

    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();

    private UserContextHolder() {
    }

    /**
     * 设置当前用户。
     *
     * @param userId 用户主键
     */
    public static void setUserId(Long userId) {
        CURRENT_USER_ID.set(userId);
    }

    /**
     * 获取当前用户。
     *
     * @return 可选用户主键
     */
    public static Optional<Long> currentUserId() {
        return Optional.ofNullable(CURRENT_USER_ID.get());
    }

    /**
     * 获取必须存在的当前用户。
     *
     * @return 用户主键
     */
    public static Long requireUserId() {
        return currentUserId().orElseThrow(() -> new BusinessException(
                HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "请先登录"));
    }

    /**
     * 清理当前请求用户。
     */
    public static void clear() {
        CURRENT_USER_ID.remove();
    }
}
