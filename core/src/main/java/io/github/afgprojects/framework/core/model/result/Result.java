package io.github.afgprojects.framework.core.model.result;

import org.jspecify.annotations.Nullable;

/**
 * @deprecated 使用 {@link io.github.afgprojects.framework.commons.model.Result} 代替
 */
@Deprecated(since = "1.1.0", forRemoval = true)
public record Result<T>(
        int code, String message, @Nullable T data, @Nullable String traceId, @Nullable String requestId) {
    
    public boolean isSuccess() {
        return code == 0;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(0, "success", data, null, null);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(0, message, data, null, null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null, null, null);
    }
}
