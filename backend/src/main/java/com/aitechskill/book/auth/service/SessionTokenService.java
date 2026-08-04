package com.aitechskill.book.auth.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

/**
 * 解析并写入浏览器会话令牌。
 */
@Service
public class SessionTokenService {

    private final String cookieName;
    private final boolean secureCookie;
    private final Duration sessionTtl;

    public SessionTokenService(
            @Value("${app.session.cookie-name}") String cookieName,
            @Value("${app.session.secure-cookie}") boolean secureCookie,
            @Value("${app.session.ttl}") Duration sessionTtl) {
        this.cookieName = cookieName;
        this.secureCookie = secureCookie;
        this.sessionTtl = sessionTtl;
    }

    /**
     * 从 Bearer 头或 Cookie 中读取令牌。
     *
     * @param request HTTP 请求
     * @return 可选令牌
     */
    public Optional<String> resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return Optional.of(authorization.substring(7).trim()).filter(value -> !value.isEmpty());
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> !value.isBlank())
                .findFirst();
    }

    /**
     * 写入安全会话 Cookie。
     *
     * @param response HTTP 响应
     * @param token 原始会话令牌
     */
    public void writeCookie(HttpServletResponse response, String token) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(token, sessionTtl).toString());
    }

    /**
     * 清除浏览器会话 Cookie。
     *
     * @param response HTTP 响应
     */
    public void clearCookie(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("", Duration.ZERO).toString());
    }

    /** 构建仅服务端脚本可读的会话 Cookie。 */
    private ResponseCookie buildCookie(String value, Duration maxAge) {
        return ResponseCookie.from(cookieName, value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
