package com.aitechskill.book.user.domain.response;

import com.aitechskill.book.user.domain.entity.UserEntity;
import java.time.LocalDateTime;

/**
 * 用户个人资料响应。
 *
 * @param id 用户主键
 * @param username 用户名
 * @param phone 手机号
 * @param email 邮箱
 * @param memberLevel 会员等级编码
 * @param memberLevelLabel 会员等级名称
 * @param memberExpireTime 会员到期时间
 * @param avatarUrl 头像地址
 * @param authProvider 认证来源
 * @param lastLoginTime 最近登录时间
 * @param createtime 注册时间
 */
public record UserProfileResponse(
        Long id,
        String username,
        String phone,
        String email,
        String memberLevel,
        String memberLevelLabel,
        LocalDateTime memberExpireTime,
        String avatarUrl,
        String authProvider,
        LocalDateTime lastLoginTime,
        LocalDateTime createtime) {

    /**
     * 将用户实体转换为个人资料。
     *
     * @param user 用户实体
     * @return 个人资料
     */
    public static UserProfileResponse from(UserEntity user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getPhone(),
                user.getEmail(),
                user.getMemberLevel().getCode(),
                user.getMemberLevel().getLabel(),
                user.getMemberExpireTime(),
                user.getAvatarUrl(),
                user.getAuthProvider().getCode(),
                user.getLastLoginTime(),
                user.getCreatetime());
    }
}
