package com.aitechskill.book.auth.config;

import com.aitechskill.book.auth.domain.SessionRecord;
import com.aitechskill.book.auth.service.SessionService;
import com.aitechskill.book.auth.service.SessionTokenService;
import com.aitechskill.book.auth.utils.UserContextHolder;
import com.aitechskill.book.common.exception.BusinessException;
import com.aitechskill.book.user.domain.entity.UserEntity;
import com.aitechskill.book.user.domain.enums.MemberLevel;
import com.aitechskill.book.user.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 校验受保护接口的 Redis 会话。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final SessionTokenService tokenService;
    private final SessionService sessionService;
    private final UserMapper userMapper;

    public AuthInterceptor(
            SessionTokenService tokenService,
            SessionService sessionService,
            UserMapper userMapper) {
        this.tokenService = tokenService;
        this.sessionService = sessionService;
        this.userMapper = userMapper;
    }

    /**
     * 请求进入控制器前校验登录状态。
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        if (isPublicCommunityRead(request)) {
            return true;
        }
        if (isPublicTechEnglishRead(request)) {
            return true;
        }
        String token = tokenService.resolveToken(request).orElseThrow(() ->
                new BusinessException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "请先登录"));
        SessionRecord session = sessionService.requireSession(token);
        UserEntity user = userMapper.selectById(session.userId());
        if (user == null) {
            sessionService.revokeSession(token);
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "用户不存在或已注销");
        }
        if (user.getMemberLevel() == MemberLevel.BANNED) {
            sessionService.revokeSession(token);
            throw new BusinessException(HttpStatus.FORBIDDEN, "USER_BANNED", "该账号已被封禁");
        }
        UserContextHolder.setUserId(user.getId());
        return true;
    }

    /** 分享库的列表、评论读取和附件读取公开，所有写操作仍需会话。 */
    private boolean isPublicCommunityRead(HttpServletRequest request) {
        if (!HttpMethod.GET.matches(request.getMethod())) {
            return false;
        }
        String path = request.getRequestURI();
        return "/api/community/posts".equals(path)
                || path.matches("/api/community/posts/[0-9]+")
                || path.matches("/api/community/posts/[0-9]+/comments")
                || path.matches("/api/community/attachments/[0-9]+/(content|preview)");
    }

    /** 技术英语语料的列表和详情公开，主站收录仍需会话。 */
    private boolean isPublicTechEnglishRead(HttpServletRequest request) {
        if (!HttpMethod.GET.matches(request.getMethod())) {
            return false;
        }
        String path = request.getRequestURI();
        return "/api/tech-english/corpus".equals(path)
                || path.matches("/api/tech-english/corpus/[0-9]+");
    }

    /**
     * 请求完成后清理线程变量。
     */
    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception exception) {
        UserContextHolder.clear();
    }
}
