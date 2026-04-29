package io.github.afgprojects.framework.core.model.result;

import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

import io.github.afgprojects.framework.core.model.exception.ErrorCode;
import io.github.afgprojects.framework.core.web.context.AfgRequestContextHolder;
import io.github.afgprojects.framework.core.web.context.RequestContext;

/**
 * Result 工厂方法
 * 自动填充 traceId/requestId，推荐使用此类代替 Result 的静态方法
 */
public final class Results {

    private static final int SUCCESS_CODE = 0;
    private static final String DEFAULT_SUCCESS_MESSAGE = "success";
    private static final int DEFAULT_FAIL_CODE = -1;

    private Results() {}

    public static <T> Result<T> success() {
        return withTrace(new Result<>(SUCCESS_CODE, DEFAULT_SUCCESS_MESSAGE, null, null, null));
    }

    public static <T> Result<T> success(T data) {
        return withTrace(new Result<>(SUCCESS_CODE, DEFAULT_SUCCESS_MESSAGE, data, null, null));
    }

    public static <T> Result<T> success(String message, T data) {
        return withTrace(new Result<>(SUCCESS_CODE, message, data, null, null));
    }

    public static <T> Result<T> fail() {
        return withTrace(new Result<>(DEFAULT_FAIL_CODE, "fail", null, null, null));
    }

    public static <T> Result<T> fail(String message) {
        return withTrace(new Result<>(DEFAULT_FAIL_CODE, message, null, null, null));
    }

    public static <T> Result<T> fail(int code, String message) {
        return withTrace(new Result<>(code, message, null, null, null));
    }

    public static <T> Result<T> fail(ErrorCode errorCode) {
        return withTrace(new Result<>(errorCode.getCode(), errorCode.getMessage(), null, null, null));
    }

    public static <T> Result<T> fail(ErrorCode errorCode, String message) {
        return withTrace(new Result<>(errorCode.getCode(), message, null, null, null));
    }

    /**
     * 返回国际化错误响应
     *
     * @param errorCode 错误码
     * @param locale 语言
     * @return 国际化错误响应
     */
    public static <T> Result<T> fail(ErrorCode errorCode, @Nullable Locale locale) {
        return withTrace(new Result<>(errorCode.getCode(), errorCode.getMessage(locale), null, null, null));
    }

    /**
     * 返回带参数的国际化错误响应
     *
     * @param errorCode 错误码
     * @param args 消息参数
     * @param locale 语言
     * @return 国际化错误响应
     */
    public static <T> Result<T> fail(ErrorCode errorCode, @Nullable Object[] args, @Nullable Locale locale) {
        return withTrace(new Result<>(errorCode.getCode(), errorCode.getMessage(args, locale), null, null, null));
    }

    public static <T> Result<PageData<T>> page(List<T> records, long total, long page, long size) {
        return withTrace(new Result<>(
                SUCCESS_CODE, DEFAULT_SUCCESS_MESSAGE, PageData.of(records, total, page, size), null, null));
    }

    private static <T> Result<T> withTrace(Result<T> result) {
        RequestContext context = AfgRequestContextHolder.getContext();
        if (context != null) {
            return new Result<>(
                    result.code(), result.message(), result.data(), context.getTraceId(), context.getRequestId());
        }
        return result;
    }
}
