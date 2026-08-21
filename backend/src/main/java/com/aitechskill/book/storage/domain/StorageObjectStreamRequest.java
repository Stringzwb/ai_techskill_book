package com.aitechskill.book.storage.domain;

import java.io.InputStream;

/** 用于大附件的流式对象存储写入请求。 */
public record StorageObjectStreamRequest(String business, String ownerId, String extension,
        String contentType, long contentLength, InputStream inputStream) { }
