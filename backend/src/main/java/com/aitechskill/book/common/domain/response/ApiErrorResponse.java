package com.aitechskill.book.common.domain.response;

import java.util.Map;

/**
 * 统一接口错误响应。
 *
 * @param code 业务错误码
 * @param message 错误说明
 * @param fieldErrors 字段校验错误
 */
public record ApiErrorResponse(String code, String message, Map<String, String> fieldErrors) {
}
