package io.github.afgprojects.framework.core.model.result;

import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 统一响应结果
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
     * 创建失败结果
     *
     * @param code    错误码
     * @param message 错误消息
     * @return 失败结果
     */
    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null, null, null);
    }
}
