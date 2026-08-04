package com.aitechskill.book.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.aitechskill.book.common.exception.BusinessException;
import com.aitechskill.book.storage.domain.StorageObjectRequest;
import com.aitechskill.book.storage.domain.StoredObject;
import com.aitechskill.book.storage.service.ObjectStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.unit.DataSize;

/**
 * 用户头像格式和存储规则测试。
 */
@ExtendWith(MockitoExtension.class)
class DefaultAvatarStorageServiceTest {

    @Mock
    private ObjectStorageService objectStorageService;

    private DefaultAvatarStorageService avatarStorageService;

    @BeforeEach
    void setUp() {
        avatarStorageService = new DefaultAvatarStorageService(
                objectStorageService, DataSize.ofMegabytes(5));
    }

    @Test
    void storesValidatedPngUnderAvatarBusiness() {
        byte[] png = new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x01
        };
        given(objectStorageService.put(any())).willReturn(new StoredObject(
                "prod/avatar/2026/08/7/avatar.png", "image/png", png.length));

        String objectKey = avatarStorageService.save(7L, png, "image/png");

        ArgumentCaptor<StorageObjectRequest> request = ArgumentCaptor.forClass(StorageObjectRequest.class);
        verify(objectStorageService).put(request.capture());
        assertThat(objectKey).isEqualTo("prod/avatar/2026/08/7/avatar.png");
        assertThat(request.getValue().business()).isEqualTo("avatar");
        assertThat(request.getValue().ownerId()).isEqualTo("7");
        assertThat(request.getValue().extension()).isEqualTo("png");
    }

    @Test
    void rejectsContentTypeThatDoesNotMatchFileHeader() {
        byte[] png = new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        };

        assertThatThrownBy(() -> avatarStorageService.save(7L, png, "image/jpeg"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("仅支持 JPG、PNG 或 WebP 格式的头像");
        verifyNoInteractions(objectStorageService);
    }
}
