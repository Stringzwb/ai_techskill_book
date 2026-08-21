package com.aitechskill.book.community.domain.response;

import java.io.InputStream;

/** 受保护附件的传输信息。 */
public record CommunityAttachmentContent(
        InputStream inputStream,
        String contentType,
        long contentLength,
        String originalName) {
}
