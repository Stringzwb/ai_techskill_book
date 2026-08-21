package com.aitechskill.book.community.domain.response;

/** Office 或文本附件的受保护文本预览。 */
public record CommunityAttachmentPreviewResponse(String title, String content, boolean truncated) {
}
