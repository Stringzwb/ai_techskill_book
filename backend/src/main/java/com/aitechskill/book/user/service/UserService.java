package com.aitechskill.book.user.service;

import com.aitechskill.book.common.exception.BusinessException;
import com.aitechskill.book.storage.domain.StoredObjectContent;
import com.aitechskill.book.storage.exception.ObjectStorageException;
import com.aitechskill.book.user.domain.entity.UserEntity;
import com.aitechskill.book.user.domain.request.UpdateProfileRequest;
import com.aitechskill.book.user.domain.response.UserProfileResponse;
import com.aitechskill.book.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

/**
 * 用户个人资料服务。
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserMapper userMapper;
    private final AvatarStorageService avatarStorageService;

    public UserService(UserMapper userMapper, AvatarStorageService avatarStorageService) {
        this.userMapper = userMapper;
        this.avatarStorageService = avatarStorageService;
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
     * 上传新头像并在事务提交后清理旧对象。
     *
     * @param userId 用户主键
     * @param content 头像内容
     * @param contentType 头像媒体类型
     * @return 更新后的个人资料
     */
    @Transactional
    public UserProfileResponse updateAvatar(Long userId, byte[] content, String contentType) {
        UserEntity user = requireUser(userId);
        String oldObjectKey = user.getAvatarObjectKey();
        String newObjectKey = avatarStorageService.save(userId, content, contentType);
        boolean cleanupRegistered = false;
        try {
            UserEntity update = new UserEntity();
            update.setId(userId);
            update.setAvatarObjectKey(newObjectKey);
            update.setAvatarUrl(buildAvatarUrl(userId, newObjectKey));
            if (userMapper.updateById(update) != 1) {
                throw new BusinessException(HttpStatus.CONFLICT, "AVATAR_UPDATE_FAILED", "头像更新失败，请重试");
            }
            registerAvatarCleanup(newObjectKey, oldObjectKey);
            cleanupRegistered = true;
            return UserProfileResponse.from(requireUser(userId));
        } catch (RuntimeException exception) {
            if (!cleanupRegistered) {
                deleteQuietly(newObjectKey);
            }
            throw exception;
        }
    }

    /**
     * 读取指定用户的头像内容。
     *
     * @param userId 用户主键
     * @return 头像内容
     */
    public StoredObjectContent getAvatar(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null || !StringUtils.hasText(user.getAvatarObjectKey())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "AVATAR_NOT_FOUND", "用户头像不存在");
        }
        return avatarStorageService.get(user.getAvatarObjectKey());
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

    /** 使用对象键末段生成可安全缓存的受控头像地址。 */
    private String buildAvatarUrl(Long userId, String objectKey) {
        String version = objectKey.substring(objectKey.lastIndexOf('/') + 1, objectKey.lastIndexOf('.'));
        return "/api/users/" + userId + "/avatar?v=" + version;
    }

    /** 事务提交后删除旧头像，回滚后删除本次新上传对象。 */
    private void registerAvatarCleanup(String newObjectKey, String oldObjectKey) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (StringUtils.hasText(oldObjectKey)) {
                    deleteQuietly(oldObjectKey);
                }
            }

            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    deleteQuietly(newObjectKey);
                }
            }
        });
    }

    /** 清理对象失败只记录安全日志，避免破坏已完成的资料更新。 */
    private void deleteQuietly(String objectKey) {
        try {
            avatarStorageService.delete(objectKey);
        } catch (ObjectStorageException exception) {
            log.warn("头像对象清理失败，objectKey={}", objectKey);
        }
    }
}
