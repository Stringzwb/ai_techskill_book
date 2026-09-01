package com.aitechskill.book.english.domain;

/**
 * 识图历史导出文件。
 *
 * @param filename 下载文件名
 * @param contentType 文件媒体类型
 * @param content 文件字节
 */
public record TechEnglishRecognitionExport(
        String filename,
        String contentType,
        byte[] content) {
}
