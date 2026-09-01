package com.aitechskill.book.english.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

/**
 * 为已清洗的 AI 识图语料生成稳定项标识。
 */
final class TechEnglishAiRecognitionItemKey {

    private TechEnglishAiRecognitionItemKey() {
    }

    /** 根据类型、图片序号和英文原文生成项标识。 */
    static String create(String corpusType, int sourceImageIndex, String englishText) {
        String normalized = corpusType + "\n" + sourceImageIndex + "\n"
                + englishText.trim().toLowerCase(Locale.ROOT);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(normalized.getBytes(StandardCharsets.UTF_8));
            return corpusType.toLowerCase(Locale.ROOT) + "-"
                    + HexFormat.of().formatHex(digest, 0, 16);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前环境不支持 SHA-256", exception);
        }
    }
}
