package com.aitechskill.book.auth.controller;

import com.aitechskill.book.auth.domain.AuthResult;
import com.aitechskill.book.auth.domain.request.LoginRequest;
import com.aitechskill.book.auth.domain.request.RegisterRequest;
import com.aitechskill.book.auth.domain.response.AuthResponse;
import com.aitechskill.book.auth.domain.response.WechatAuthStatusResponse;
import com.aitechskill.book.auth.service.AuthService;
import com.aitechskill.book.auth.service.SessionService;
import com.aitechskill.book.auth.service.SessionTokenService;
import com.aitechskill.book.auth.utils.UserContextHolder;
import com.aitechskill.book.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户认证接口。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final SessionService sessionService;
    private final SessionTokenService tokenService;

    public AuthController(
            AuthService authService,
            UserService userService,
            SessionService sessionService,
            SessionTokenService tokenService) {
        this.authService = authService;
        this.userService = userService;
        this.sessionService = sessionService;
        this.tokenService = tokenService;
    }

    /**
     * 注册密码用户。
     *
     * @param request 注册信息
     * @param response HTTP 响应
     * @return 认证结果
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        AuthResult result = authService.register(request);
        tokenService.writeCookie(response, result.token());
        return new AuthResponse(result.user());
    }

    /**
     * 使用密码登录。
     *
     * @param request 登录信息
     * @param response HTTP 响应
     * @return 认证结果
     */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthResult result = authService.login(request);
        tokenService.writeCookie(response, result.token());
        return new AuthResponse(result.user());
    }

    /**
     * 注销当前会话。
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            tokenService.resolveToken(request).ifPresent(sessionService::revokeSession);
        } finally {
            tokenService.clearCookie(response);
        }
    }

    /**
     * 查询当前登录用户。
     *
     * @return 当前用户
     */
    @GetMapping("/me")
    public AuthResponse me() {
        return new AuthResponse(userService.getProfile(UserContextHolder.requireUserId()));
    }

    /**
     * 查询微信登录启用状态。
     *
     * @return 微信登录状态
     */
    @GetMapping("/wechat/status")
    public WechatAuthStatusResponse wechatStatus() {
        return new WechatAuthStatusResponse(false, "微信登录接口已预留，当前版本暂未启用");
    }
}
