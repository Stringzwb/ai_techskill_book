package com.aitechskill.book.user.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.aitechskill.book.common.exception.BusinessException;
import com.aitechskill.book.user.domain.entity.UserEntity;
import com.aitechskill.book.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 用户资料和头像更新服务测试。
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private AvatarStorageService avatarStorageService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userMapper, avatarStorageService);
    }

    @Test
    void deletesNewAvatarWhenDatabaseUpdateFails() {
        UserEntity user = new UserEntity();
        user.setId(9L);
        byte[] content = new byte[] {1};
        given(userMapper.selectById(9L)).willReturn(user);
        given(avatarStorageService.save(9L, content, "image/png"))
                .willReturn("prod/avatar/2026/08/9/new.png");
        given(userMapper.updateById(org.mockito.ArgumentMatchers.any(UserEntity.class))).willReturn(0);

        assertThatThrownBy(() -> userService.updateAvatar(9L, content, "image/png"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("头像更新失败，请重试");
        verify(avatarStorageService).delete("prod/avatar/2026/08/9/new.png");
    }
}
