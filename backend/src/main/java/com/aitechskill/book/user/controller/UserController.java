package com.aitechskill.book.user.controller;

import com.aitechskill.book.auth.utils.UserContextHolder;
import com.aitechskill.book.common.exception.BusinessException;
import com.aitechskill.book.storage.domain.StoredObjectContent;
import com.aitechskill.book.user.domain.request.UpdateProfileRequest;
import com.aitechskill.book.user.domain.response.UserProfileResponse;
import com.aitechskill.book.user.service.UserService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 当前用户个人资料接口。
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 查询当前用户资料。
     *
     * @return 当前用户资料
     */
    @GetMapping("/me")
    public UserProfileResponse getProfile() {
        return userService.getProfile(UserContextHolder.requireUserId());
    }

    /**
     * 修改当前用户资料。
     *
     * @param request 修改内容
     * @return 更新后的资料
     */
    @PutMapping("/me")
    public UserProfileResponse updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(UserContextHolder.requireUserId(), request);
    }

    /**
     * 上传并替换当前用户头像。
     *
     * @param file 头像文件
     * @return 更新后的资料
     */
    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserProfileResponse uploadAvatar(@RequestPart("file") MultipartFile file) {
        try {
            return userService.updateAvatar(
                    UserContextHolder.requireUserId(), file.getBytes(), file.getContentType());
        } catch (IOException exception) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_AVATAR", "头像文件读取失败");
        }
    }

    /**
     * 读取受登录保护的用户头像。
     *
     * @param userId 用户主键
     * @return 头像文件
     */
    @GetMapping("/{userId}/avatar")
    public ResponseEntity<byte[]> getAvatar(@PathVariable Long userId) {
        StoredObjectContent avatar = userService.getAvatar(userId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(avatar.contentType()))
                .contentLength(avatar.contentLength())
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePrivate().immutable())
                .header("X-Content-Type-Options", "nosniff")
                .body(avatar.content());
    }
}
