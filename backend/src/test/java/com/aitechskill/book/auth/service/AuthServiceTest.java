package com.aitechskill.book.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import com.aitechskill.book.auth.domain.AuthResult;
import com.aitechskill.book.auth.domain.request.LoginRequest;
import com.aitechskill.book.auth.domain.request.RegisterRequest;
import com.aitechskill.book.common.exception.BusinessException;
import com.aitechskill.book.user.domain.entity.UserEntity;
import com.aitechskill.book.user.domain.enums.MemberLevel;
import com.aitechskill.book.user.domain.enums.UserRole;
import com.aitechskill.book.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 用户注册和登录服务测试。
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SessionService sessionService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        given(passwordEncoder.encode(any())).willReturn("encoded-password");
        authService = new AuthService(userMapper, passwordEncoder, sessionService, "/default-avatar.svg");
    }

    @Test
    void registersGuestUserAndCreatesSession() {
        doAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(18L);
            return 1;
        }).when(userMapper).insert(any(UserEntity.class));
        given(sessionService.createSession(18L)).willReturn("session-token");

        AuthResult result = authService.register(new RegisterRequest(
                "codex_user", "13800138000", "User@Example.com", "password123"));

        assertThat(result.token()).isEqualTo("session-token");
        assertThat(result.user().memberLevel()).isEqualTo(MemberLevel.GUEST.getCode());
        assertThat(result.user().userRole()).isEqualTo(UserRole.USER.getCode());
        assertThat(result.user().email()).isEqualTo("user@example.com");
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void rejectsInvalidCredentialsWithoutRevealingAccountState() {
        given(userMapper.selectByLoginAccount("missing")).willReturn(null);
        given(passwordEncoder.matches("wrong-password", "encoded-password")).willReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("missing", "wrong-password")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("账号或密码错误");
    }

    @Test
    void rejectsBannedUserAfterPasswordCheck() {
        UserEntity user = new UserEntity();
        user.setPasswordHash("stored-hash");
        user.setMemberLevel(MemberLevel.BANNED);
        given(userMapper.selectByLoginAccount("banned-user")).willReturn(user);
        given(passwordEncoder.matches("password123", "stored-hash")).willReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("banned-user", "password123")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("该账号已被封禁");
    }
}
