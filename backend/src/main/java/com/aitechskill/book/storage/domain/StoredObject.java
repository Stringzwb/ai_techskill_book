package com.aitechskill.book.storage.domain;

/**
 * 已写入对象的稳定元数据。
 *
 * @param objectKey 对象键
 * @param contentType 文件媒体类型
 * @param contentLength 文件大小
 */
public record StoredObject(String objectKey, String contentType, long contentLength) {
}
