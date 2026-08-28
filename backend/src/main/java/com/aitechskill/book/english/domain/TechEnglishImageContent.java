package com.aitechskill.book.english.domain;

import java.io.InputStream;

/**
 * 技术英语图片语料读取结果。
 */
public record TechEnglishImageContent(InputStream inputStream, String contentType, long contentLength) {
}

