package io.github.afgprojects.framework.ai.core.exception;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * LLM 相关异常
 *
 * <p>处理 LLM API 调用过程中的异常情况。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class LlmException extends AiException {

    /**
     * 创建 LLM 异常
     *
     * @param message   异常消息
     * @param errorCode 错误代码
     * @param provider  提供商名称
     */
    public LlmException(@NonNull String message, @NonNull String errorCode, @Nullable String provider) {
        super(message, errorCode, provider);
    }

    /**
     * 创建 LLM 异常（带原因）
     *
     * @param message   异常消息
     * @param errorCode 错误代码
     * @param provider  提供商名称
     * @param cause     原因
     */
    public LlmException(@NonNull String message, @NonNull String errorCode, @Nullable String provider, @Nullable Throwable cause) {
        super(message, errorCode, provider, cause);
    }

    /**
     * 创建连接失败异常
     */
    public static @NonNull LlmException connectionFailed(@NonNull String provider, @Nullable Throwable cause) {
        return new LlmException(
                "Failed to connect to LLM provider: " + provider,
                ErrorCodes.LLM_CONNECTION_FAILED,
                provider,
                cause
        );
    }

    /**
     * 创建速率限制异常
     */
    public static @NonNull LlmException rateLimited(@NonNull String provider) {
        return new LlmException(
                "Rate limit exceeded for provider: " + provider,
                ErrorCodes.LLM_RATE_LIMITED,
                provider
        );
    }

    /**
     * 创建无效响应异常
     */
    public static @NonNull LlmException invalidResponse(@NonNull String provider, @NonNull String details) {
        return new LlmException(
                "Invalid response from provider " + provider + ": " + details,
                ErrorCodes.LLM_INVALID_RESPONSE,
                provider
        );
    }

    /**
     * 创建超时异常
     */
    public static @NonNull LlmException timeout(@NonNull String provider, long timeoutMs) {
        return new LlmException(
                "Request to provider " + provider + " timed out after " + timeoutMs + "ms",
                ErrorCodes.LLM_TIMEOUT,
                provider
        );
    }

    /**
     * 创建 API Key 无效异常
     */
    public static @NonNull LlmException apiKeyInvalid(@NonNull String provider) {
        return new LlmException(
                "Invalid API key for provider: " + provider,
                ErrorCodes.LLM_API_KEY_INVALID,
                provider
        );
    }
}