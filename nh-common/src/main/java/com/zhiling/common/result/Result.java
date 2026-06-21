package com.zhiling.common.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统一返回结果对象。
 *
 * 设计目标：
 * 1. 统一接口成功/失败返回结构，避免各模块自定义响应体。
 * 2. 与全局异常处理器协同，通过 code 表达 HTTP/业务状态。
 * 3. 提供兼容旧代码的 success/fail 工厂方法，降低迁移成本。
 *
 * 字段约定：
 * - code: 状态码（推荐与 HTTP 状态码一致）
 * - message: 提示文案
 * - data: 业务数据载荷
 *
 * @param <T> 响应数据类型
 *
 * @author zhanghongyu
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Result<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 通用成功状态码 */
    public static final int CODE_SUCCESS = 200;
    /** 请求参数错误 */
    public static final int CODE_BAD_REQUEST = 400;
    /** 未认证 */
    public static final int CODE_UNAUTHORIZED = 401;
    /** 已认证但无权限 */
    public static final int CODE_FORBIDDEN = 403;
    /** 资源不存在 */
    public static final int CODE_NOT_FOUND = 404;
    /** 业务状态冲突 */
    public static final int CODE_CONFLICT = 409;
    /** 不可处理实体 */
    public static final int CODE_UNPROCESSABLE_ENTITY = 422;
    /** 通用失败状态码（未指定时默认） */
    public static final int CODE_FAIL = 500;

    /** 状态码 */
    private Integer code;
    /** 消息 */
    private String message;
    /** 响应数据 */
    private T data;

    /**
     * 无响应数据的成功响应（code=200）。
     */
    public static <T> Result<T> success() {
        return Result.<T>builder()
                .code(CODE_SUCCESS)
                .build();
    }

    /**
     * 带响应数据的成功响应（code=200）。
     */
    public static <T> Result<T> success(T data) {
        return Result.<T>builder()
                .code(CODE_SUCCESS)
                .data(data)
                .build();
    }

    /**
     * 带消息的成功响应（code=200）。
     */
    public static <T> Result<T> successMessage(String message) {
        return Result.<T>builder()
                .code(CODE_SUCCESS)
                .message(message)
                .build();
    }

    /**
     * 带消息和数据的成功响应（code=200）。
     */
    public static <T> Result<T> success(String message, T data) {
        return Result.<T>builder()
                .code(CODE_SUCCESS)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * 失败响应（默认 code=500）。
     */
    public static <T> Result<T> fail() {
        return Result.<T>builder()
                .code(CODE_FAIL)
                .build();
    }

    /**
     * 失败响应（默认 code=500）。
     */
    public static <T> Result<T> fail(String message) {
        return Result.<T>builder()
                .code(CODE_FAIL)
                .message(message)
                .build();
    }

    /**
     * 失败响应（指定状态码和消息）。
     */
    public static <T> Result<T> fail(int code, String message) {
        return Result.<T>builder()
                .code(code)
                .message(message)
                .build();
    }

    /**
     * 失败响应（指定状态码、消息和数据）。
     */
    public static <T> Result<T> fail(int code, String message, T data) {
        return Result.<T>builder()
                .code(code)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * 400 参数错误。
     */
    public static <T> Result<T> badRequest(String message) {
        return fail(CODE_BAD_REQUEST, message);
    }

    /**
     * 401 未认证。
     */
    public static <T> Result<T> unauthorized(String message) {
        return fail(CODE_UNAUTHORIZED, message);
    }

    /**
     * 403 无权限。
     */
    public static <T> Result<T> forbidden(String message) {
        return fail(CODE_FORBIDDEN, message);
    }

    /**
     * 404 资源不存在。
     */
    public static <T> Result<T> notFound(String message) {
        return fail(CODE_NOT_FOUND, message);
    }

    /**
     * 409 状态冲突。
     */
    public static <T> Result<T> conflict(String message) {
        return fail(CODE_CONFLICT, message);
    }

    /**
     * 422 不可处理实体。
     */
    public static <T> Result<T> unprocessable(String message) {
        return fail(CODE_UNPROCESSABLE_ENTITY, message);
    }

    /**
     * 500 服务端异常。
     */
    public static <T> Result<T> serverError(String message) {
        return fail(CODE_FAIL, message);
    }

    /**
     * 统一构造器（全参）。
     */
    public static <T> Result<T> of(int code, String message, T data) {
        return Result.<T>builder()
                .code(code)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * 当前响应是否成功（code==200）。
     */
    public boolean isSuccess() {
        return code != null && code == CODE_SUCCESS;
    }
}
