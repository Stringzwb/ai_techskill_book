package com.aitechskill.book.storage.domain;

/**
 * 对象存储读取结果。
 *
 * @param content 文件内容
 * @param contentType 文件媒体类型
 * @param contentLength 文件大小
 */
public record StoredObjectContent(byte[] content, String contentType, long contentLength) {
}
