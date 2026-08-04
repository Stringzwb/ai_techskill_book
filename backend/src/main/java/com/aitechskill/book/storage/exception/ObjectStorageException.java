package com.aitechskill.book.storage.exception;

/**
 * 对象存储访问异常，不包含凭据和供应商响应正文。
 */
public class ObjectStorageException extends RuntimeException {

    /**
     * 创建对象存储异常。
     *
     * @param message 安全错误信息
     * @param cause 原始异常
     */
    public ObjectStorageException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 创建无原始异常的对象存储异常。
     *
     * @param message 安全错误信息
     */
    public ObjectStorageException(String message) {
        super(message);
    }
}
