package io.github.afgprojects.framework.ai.core.api.exception;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * AI 模块基础异常
 *
 * <p>所有 AI 模块异常的基类，提供统一的异常层次结构。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class AiException extends RuntimeException {

    private final @NonNull String errorCode;
    private final @Nullable String provider;

    /**
     * 创建异常
     *
     * @param message   异常消息
     * @param errorCode 错误代码
     */
    public AiException(@NonNull String message, @NonNull String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.provider = null;
    }

    /**
     * 创建异常（带原因）
     *
     * @param message   异常消息
     * @param errorCode 错误代码
     * @param cause     原因
     */
    public AiException(@NonNull String message, @NonNull String errorCode, @Nullable Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.provider = null;
    }

    /**
     * 创建异常（带提供商信息）
     *
     * @param message   异常消息
     * @param errorCode 错误代码
     * @param provider  提供商名称
     */
    public AiException(@NonNull String message, @NonNull String errorCode, @Nullable String provider) {
        super(message);
        this.errorCode = errorCode;
        this.provider = provider;
    }

    /**
     * 创建异常（带提供商信息和原因）
     *
     * @param message   异常消息
     * @param errorCode 错误代码
     * @param provider  提供商名称
     * @param cause     原因
     */
    public AiException(@NonNull String message, @NonNull String errorCode, @Nullable String provider, @Nullable Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.provider = provider;
    }

    /**
     * 获取错误代码
     *
     * @return 错误代码
     */
    public @NonNull String getErrorCode() {
        return errorCode;
    }

    /**
     * 获取提供商名称
     *
     * @return 提供商名称，可能为 null
     */
    public @Nullable String getProvider() {
        return provider;
    }

    /**
     * 错误代码常量
     */
    public static final class ErrorCodes {
        public static final String LLM_CONNECTION_FAILED = "LLM_001";
        public static final String LLM_RATE_LIMITED = "LLM_002";
        public static final String LLM_INVALID_RESPONSE = "LLM_003";
        public static final String LLM_TIMEOUT = "LLM_004";
        public static final String LLM_API_KEY_INVALID = "LLM_005";

        public static final String TOOL_NOT_FOUND = "TOOL_001";
        public static final String TOOL_EXECUTION_FAILED = "TOOL_002";
        public static final String TOOL_INVALID_INPUT = "TOOL_003";

        public static final String RAG_INDEX_FAILED = "RAG_001";
        public static final String RAG_SEARCH_FAILED = "RAG_002";
        public static final String RAG_DOCUMENT_LOAD_FAILED = "RAG_003";

        public static final String AGENT_ITERATION_EXCEEDED = "AGENT_001";
        public static final String AGENT_EXECUTION_FAILED = "AGENT_002";
        public static final String AGENT_TIMEOUT = "AGENT_003";

        public static final String MULTIAGENT_COORDINATION_FAILED = "MULTI_001";
        public static final String MULTIAGENT_AGENT_NOT_FOUND = "MULTI_002";
        public static final String MULTIAGENT_CONFLICT_UNRESOLVED = "MULTI_003";

        public static final String CONFIG_INVALID = "CONFIG_001";
        public static final String CONFIG_MISSING = "CONFIG_002";
    }
}