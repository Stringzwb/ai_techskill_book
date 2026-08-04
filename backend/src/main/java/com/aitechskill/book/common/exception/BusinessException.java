package com.aitechskill.book.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 可返回给前端的业务异常。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    /**
     * 创建业务异常。
     *
     * @param status HTTP 状态
     * @param code 业务错误码
     * @param message 错误说明
     */
    public BusinessException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    /**
     * 创建带内部原因的业务异常。
     *
     * @param status HTTP 状态
     * @param code 业务错误码
     * @param message 错误说明
     * @param cause 内部异常
     */
    public BusinessException(HttpStatus status, String code, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code;
    }
}
