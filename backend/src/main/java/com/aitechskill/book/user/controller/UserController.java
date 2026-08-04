package com.aitechskill.book.user.controller;

import com.aitechskill.book.auth.utils.UserContextHolder;
import com.aitechskill.book.user.domain.request.UpdateProfileRequest;
import com.aitechskill.book.user.domain.response.UserProfileResponse;
import com.aitechskill.book.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
