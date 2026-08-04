package com.aitechskill.book.user.service;

/**
 * 用户头像存储扩展接口，当前版本不提供实现和上传入口。
 */
public interface AvatarStorageService {

    /**
     * 保存用户头像。
     *
     * @param userId 用户主键
     * @param content 头像内容
     * @param contentType 文件类型
     * @return 头像访问地址
     */
    String save(Long userId, byte[] content, String contentType);
}
