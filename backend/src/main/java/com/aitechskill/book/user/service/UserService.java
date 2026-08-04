package com.aitechskill.book.user.service;

import com.aitechskill.book.common.exception.BusinessException;
import com.aitechskill.book.user.domain.entity.UserEntity;
import com.aitechskill.book.user.domain.request.UpdateProfileRequest;
import com.aitechskill.book.user.domain.response.UserProfileResponse;
import com.aitechskill.book.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Locale;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户个人资料服务。
 */
@Service
public class UserService {

    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 查询个人资料。
     *
     * @param userId 用户主键
     * @return 个人资料
     */
    public UserProfileResponse getProfile(Long userId) {
        return UserProfileResponse.from(requireUser(userId));
    }

    /**
     * 修改允许用户维护的个人资料。
     *
     * @param userId 用户主键
     * @param request 修改内容
     * @return 更新后的个人资料
     */
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        requireUser(userId);
        String username = request.username().trim();
        String phone = request.phone().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        ensureAvailable(UserEntity::getUsername, username, userId, "用户名已被使用");
        ensureAvailable(UserEntity::getPhone, phone, userId, "手机号已被使用");
        ensureAvailable(UserEntity::getEmail, email, userId, "邮箱已被使用");

        UserEntity update = new UserEntity();
        update.setId(userId);
        update.setUsername(username);
        update.setPhone(phone);
        update.setEmail(email);
        try {
            userMapper.updateById(update);
        } catch (DuplicateKeyException exception) {
            throw conflict("用户名、手机号或邮箱已被使用");
        }
        return UserProfileResponse.from(requireUser(userId));
    }

    /**
     * 查询可用用户实体。
     *
     * @param userId 用户主键
     * @return 用户实体
     */
    public UserEntity requireUser(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "用户不存在或已注销");
        }
        return user;
    }

    /** 检查资料字段是否已被其他用户使用。 */
    private void ensureAvailable(
            com.baomidou.mybatisplus.core.toolkit.support.SFunction<UserEntity, String> field,
            String value,
            Long userId,
            String message) {
        long count = userMapper.selectCount(new LambdaQueryWrapper<UserEntity>()
                .eq(field, value)
                .ne(UserEntity::getId, userId));
        if (count > 0) {
            throw conflict(message);
        }
    }

    /** 创建用户资料冲突异常。 */
    private BusinessException conflict(String message) {
        return new BusinessException(HttpStatus.CONFLICT, "USER_ALREADY_EXISTS", message);
    }
}
