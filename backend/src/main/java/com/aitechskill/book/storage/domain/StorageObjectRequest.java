package com.aitechskill.book.storage.domain;

/**
 * 对象存储写入请求。
 *
 * @param business 业务目录
 * @param ownerId 归属标识
 * @param extension 安全扩展名
 * @param contentType 文件媒体类型
 * @param content 文件内容
 */
public record StorageObjectRequest(
        String business,
        String ownerId,
        String extension,
        String contentType,
        byte[] content) {
}
