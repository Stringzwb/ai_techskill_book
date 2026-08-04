package com.aitechskill.book.auth.service;

import com.aitechskill.book.auth.domain.AuthResult;
import com.aitechskill.book.auth.domain.enums.AuthProvider;
import com.aitechskill.book.auth.domain.request.LoginRequest;
import com.aitechskill.book.auth.domain.request.RegisterRequest;
import com.aitechskill.book.common.exception.BusinessException;
import com.aitechskill.book.user.domain.entity.UserEntity;
import com.aitechskill.book.user.domain.enums.MemberLevel;
import com.aitechskill.book.user.domain.response.UserProfileResponse;
import com.aitechskill.book.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户注册与密码登录服务。
 */
@Service
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessionService;
    private final String defaultAvatarUrl;
    private final String dummyPasswordHash;

    public AuthService(
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            SessionService sessionService,
            @Value("${app.user.default-avatar-url}") String defaultAvatarUrl) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.sessionService = sessionService;
        this.defaultAvatarUrl = defaultAvatarUrl;
        this.dummyPasswordHash = passwordEncoder.encode("not-a-real-user-password");
    }

    /**
     * 注册密码用户并创建会话。
     *
     * @param request 注册信息
     * @return 认证结果
     */
    public AuthResult register(RegisterRequest request) {
        UserEntity user = new UserEntity();
        user.setUsername(request.username().trim());
        user.setPhone(request.phone().trim());
        user.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        ensureUnique(user, null);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setMemberLevel(MemberLevel.GUEST);
        user.setAvatarUrl(defaultAvatarUrl);
        user.setAuthProvider(AuthProvider.PASSWORD);
        user.setDeleted(0);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException exception) {
            throw conflict("用户名、手机号或邮箱已被使用");
        }
        String token = sessionService.createSession(user.getId());
        return new AuthResult(token, UserProfileResponse.from(user));
    }

    /**
     * 校验密码并创建会话。
     *
     * @param request 登录信息
     * @return 认证结果
     */
    public AuthResult login(LoginRequest request) {
        String account = request.account().trim();
        UserEntity user = userMapper.selectByLoginAccount(account);
        String storedHash = user == null ? dummyPasswordHash : user.getPasswordHash();
        if (!passwordEncoder.matches(request.password(), storedHash) || user == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "账号或密码错误");
        }
        if (user.getMemberLevel() == MemberLevel.BANNED) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "USER_BANNED", "该账号已被封禁");
        }
        user.setLastLoginTime(LocalDateTime.now());
        user.setUpdateby(user.getId());
        userMapper.updateById(user);
        String token = sessionService.createSession(user.getId());
        return new AuthResult(token, UserProfileResponse.from(user));
    }

    /** 校验用户名、手机号和邮箱是否可用。 */
    private void ensureUnique(UserEntity user, Long excludedUserId) {
        if (exists(UserEntity::getUsername, user.getUsername(), excludedUserId)) {
            throw conflict("用户名已被使用");
        }
        if (exists(UserEntity::getPhone, user.getPhone(), excludedUserId)) {
            throw conflict("手机号已被使用");
        }
        if (exists(UserEntity::getEmail, user.getEmail(), excludedUserId)) {
            throw conflict("邮箱已被使用");
        }
    }

    /** 按指定字段检查重复用户。 */
    private boolean exists(
            com.baomidou.mybatisplus.core.toolkit.support.SFunction<UserEntity, String> field,
            String value,
            Long excludedUserId) {
        LambdaQueryWrapper<UserEntity> query = new LambdaQueryWrapper<UserEntity>().eq(field, value);
        if (excludedUserId != null) {
            query.ne(UserEntity::getId, excludedUserId);
        }
        return userMapper.selectCount(query) > 0;
    }

    /** 创建用户资料冲突异常。 */
    private BusinessException conflict(String message) {
        return new BusinessException(HttpStatus.CONFLICT, "USER_ALREADY_EXISTS", message);
    }
}
