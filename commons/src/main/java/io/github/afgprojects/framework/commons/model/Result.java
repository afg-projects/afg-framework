package io.github.afgprojects.framework.commons.model;

import io.github.afgprojects.framework.commons.exception.ErrorCode;

import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 统一响应结果。
 *
 * <p>成功响应 {@code code=0}，失败响应携带非零错误码和消息。
 *
 * <p>使用示例：
 * <pre>{@code
 * Result.success(user)                          // → {"code":0,"message":"success","data":{...}}
 * Result.fail(CommonErrorCode.NOT_FOUND)         // → {"code":10100,"message":"资源不存在"}
 * Result.fail(10001, "用户名已存在")               // → {"code":10001,"message":"用户名已存在"}
 * }</pre>
 *
 * @param <T> 数据类型
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Result<T>(
        int code, String message, @Nullable T data, @Nullable String traceId, @Nullable String requestId) {

    private static final int SUCCESS_CODE = 0;

    /**
     * 判断是否成功
     *
     * @return 如果成功返回 true
     */
    public boolean isSuccess() {
        return code == SUCCESS_CODE;
    }

    /**
     * 创建成功结果
     *
     * @param data 数据
     * @return 成功结果
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(SUCCESS_CODE, "success", data, null, null);
    }

    /**
     * 创建成功结果（带消息）
     *
     * @param message 消息
     * @param data    数据
     * @return 成功结果
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(SUCCESS_CODE, message, data, null, null);
    }

    /**
     * 创建成功结果（无数据）
     *
     * @return 成功结果
     */
    public static <T> Result<T> success() {
        return new Result<>(SUCCESS_CODE, "success", null, null, null);
    }

    /**
     * 创建失败结果
     *
     * @param code    错误码
     * @param message 错误消息
     * @return 失败结果
     */
    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null, null, null);
    }

    /**
     * 从 ErrorCode 创建失败结果
     *
     * @param errorCode 错误码
     * @return 失败结果
     */
    public static <T> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null, null, null);
    }

    /**
     * 从 ErrorCode 和自定义消息创建失败结果
     *
     * @param errorCode 错误码
     * @param message   自定义错误消息
     * @return 失败结果
     */
    public static <T> Result<T> fail(ErrorCode errorCode, String message) {
        return new Result<>(errorCode.getCode(), message, null, null, null);
    }
}
