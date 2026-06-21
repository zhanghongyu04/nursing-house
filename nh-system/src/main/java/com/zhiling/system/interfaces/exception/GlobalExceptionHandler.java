package com.zhiling.system.interfaces.exception;

import com.zhiling.common.exception.BaseException;
import com.zhiling.common.exception.ProjectException;
import com.zhiling.common.result.Result;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Objects;

/**
 * 全局异常处理器
 * 统一异常响应并设置正确的 HTTP 状态码
 *
 * @author zhanghongyu
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理文件上传大小超限异常 (413)
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<String>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("文件上传大小超限: {}", e.getMessage());
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "文件大小超出限制，单个文件最大10MB，总请求最大50MB");
    }

    /**
     * 处理 multipart 解析异常 (400)
     */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Result<String>> handleMultipartException(MultipartException e) {
        log.error("文件上传解析失败: {}", e.getMessage(), e);
        String message = "文件上传解析失败";
        if (e.getMessage() != null) {
            if (e.getMessage().contains("boundary")) {
                message = "请求格式错误，multipart/form-data缺少boundary参数";
            } else if (e.getMessage().contains("empty")) {
                message = "上传文件为空或格式无效";
            }
        }
        return build(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * 参数校验失败（@Valid）(400)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<String>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .filter(Objects::nonNull)
                .orElse("请求参数校验失败");
        return build(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * 参数绑定异常 (400)
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<String>> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .filter(Objects::nonNull)
                .orElse("请求参数绑定失败");
        return build(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * 约束校验异常（@Validated）(400)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<String>> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .findFirst()
                .map(v -> v.getMessage())
                .orElse("请求参数校验失败");
        return build(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * 缺少请求参数 (400)
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<String>> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        return build(HttpStatus.BAD_REQUEST, "缺少请求参数: " + e.getParameterName());
    }

    /**
     * 参数类型不匹配 (400)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<String>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        return build(HttpStatus.BAD_REQUEST, "参数类型错误: " + e.getName());
    }

    /**
     * 请求体不可读 (400)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<String>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: message={}, rootCause={}",
                e.getMessage(),
                e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : "null",
                e);
        return build(HttpStatus.BAD_REQUEST, "请求体格式错误或JSON解析失败");
    }

    /**
     * 非法参数异常 (400)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<String>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("非法参数: {}", e.getMessage());
        return build(HttpStatus.BAD_REQUEST, "参数错误: " + e.getMessage());
    }

    /**
     * 不支持的请求方法 (405)
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<String>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "不支持的请求方法: " + e.getMethod());
    }

    /**
     * 不支持的媒体类型 (415)
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Result<String>> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "不支持的媒体类型");
    }

    /**
     * 资源不存在 (404)
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Result<String>> handleNoHandlerFoundException(NoHandlerFoundException e) {
        return build(HttpStatus.NOT_FOUND, "请求资源不存在: " + e.getRequestURL());
    }

    /**
     * 无权限访问 (403)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<String>> handleAccessDeniedException(AccessDeniedException e) {
        return build(HttpStatus.FORBIDDEN, "无权限访问该资源");
    }

    /**
     * 自定义业务异常 ProjectException
     */
    @ExceptionHandler(ProjectException.class)
    public ResponseEntity<Result<String>> handleProjectException(ProjectException e) {
        HttpStatus status = toHttpStatus(e.getCode());
        String message = e.getMessage() != null ? e.getMessage() : "业务异常";
        return build(status, message);
    }

    /**
     * 基础异常 BaseException
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<Result<String>> handleBaseException(BaseException e) {
        HttpStatus status = toHttpStatus(parseIntOrDefault(e.getCode(), 500));
        String message = e.getDefaultMessage() != null ? e.getDefaultMessage() : "业务异常";
        return build(status, message);
    }

    /**
     * 运行时异常 (500)
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Result<String>> handleRuntimeException(RuntimeException e) {
        log.error("运行时异常: {}", e.getMessage(), e);
        String message = e.getMessage() != null ? e.getMessage() : "服务器内部错误";
        return build(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    /**
     * 兜底异常 (500)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<String>> handleException(Exception e) {
        log.error("未处理异常: {}", e.getMessage(), e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误");
    }

    /**
     * 方法：build
     *
     * @author zhanghongyu
     */
    private ResponseEntity<Result<String>> build(HttpStatus status, String message) {
        Result<String> result = Result.fail(status.value(), message);
        return ResponseEntity.status(status).body(result);
    }

    /**
     * 方法：parseIntOrDefault
     *
     * @author zhanghongyu
     */
    private int parseIntOrDefault(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    /**
     * 方法：toHttpStatus
     *
     * @author zhanghongyu
     */
    private HttpStatus toHttpStatus(int statusCode) {
        try {
            return HttpStatus.valueOf(statusCode);
        } catch (Exception ignored) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
}