package com.aitechskill.book.auth.service;

import com.aitechskill.book.auth.domain.AuthResult;

/**
 * 微信登录扩展接口，当前版本不提供实现。
 */
public interface WechatAuthGateway {

    /**
     * 使用微信授权码登录。
     *
     * @param authorizationCode 微信授权码
     * @return 认证结果
     */
    AuthResult login(String authorizationCode);
}
