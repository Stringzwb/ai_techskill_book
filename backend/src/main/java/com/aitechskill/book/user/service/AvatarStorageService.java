package com.aitechskill.book.user.service;

import com.aitechskill.book.storage.domain.StoredObjectContent;

/**
 * 用户头像存储服务。
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

    /**
     * 读取用户头像。
     *
     * @param objectKey 头像对象键
     * @return 头像内容
     */
    StoredObjectContent get(String objectKey);

    /**
     * 删除用户头像。
     *
     * @param objectKey 头像对象键
     */
    void delete(String objectKey);
}
