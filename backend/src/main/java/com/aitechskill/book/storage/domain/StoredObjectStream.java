package com.aitechskill.book.storage.domain;

import java.io.InputStream;

/** 对象存储下载流。调用方必须在传输完成后关闭输入流。 */
public record StoredObjectStream(InputStream inputStream, String contentType, long contentLength) {
}
