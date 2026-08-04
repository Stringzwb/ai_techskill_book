package com.aitechskill.book.common.controller;

import com.aitechskill.book.common.domain.response.ApiErrorResponse;
import com.aitechskill.book.common.exception.BusinessException;
import com.aitechskill.book.storage.exception.ObjectStorageException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 统一处理接口异常。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 返回业务异常。
     *
     * @param exception 业务异常
     * @return 标准错误响应
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessException(BusinessException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(new ApiErrorResponse(exception.getCode(), exception.getMessage(), Map.of()));
    }

    /**
     * 返回字段校验异常。
     *
     * @param exception 校验异常
     * @return 标准错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse("VALIDATION_ERROR", "请检查填写内容", fieldErrors));
    }

    /**
     * 返回请求体解析异常。
     *
     * @return 标准错误响应
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableRequest() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse("INVALID_REQUEST", "请求内容格式不正确", Map.of()));
    }

    /**
     * 返回请求体过大异常。
     *
     * @return 标准错误响应
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleUploadTooLarge() {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ApiErrorResponse("UPLOAD_TOO_LARGE", "上传文件体积超过限制", Map.of()));
    }

    /**
     * 隐藏对象存储供应商错误详情并返回可重试状态。
     *
     * @return 标准错误响应
     */
    @ExceptionHandler(ObjectStorageException.class)
    public ResponseEntity<ApiErrorResponse> handleObjectStorageException() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiErrorResponse("STORAGE_UNAVAILABLE", "文件存储服务暂不可用，请稍后重试", Map.of()));
    }
}
