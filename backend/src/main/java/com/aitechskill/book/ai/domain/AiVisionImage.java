package com.aitechskill.book.ai.domain;

/**
 * 已从受控存储读取、待发送给视觉模型的图片内容。
 *
 * @param contentType 图片媒体类型
 * @param bytes 图片二进制内容
 */
public record AiVisionImage(String contentType, byte[] bytes) {
}
