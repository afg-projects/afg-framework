package io.github.afgprojects.framework.security.core.oauth2.model;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * OAuth2 授权响应。
 *
 * @param code 授权码
 * @param state 状态参数（回显）
 * @param redirectUri 重定向 URI
 * @param error 错误码
 * @param errorDescription 错误描述
 * @since 1.0.0
 */
public record AuthorizationResponse(
        @Nullable String code,
        @Nullable String state,
        @NonNull String redirectUri,
        @Nullable String error,
        @Nullable String errorDescription) {

    /**
     * 创建成功响应。
     *
     * @param code 授权码
     * @param state 状态参数
     * @param redirectUri 重定向 URI
     * @return 成功响应
     */
    public static AuthorizationResponse success(
            @NonNull String code,
            @Nullable String state,
            @NonNull String redirectUri) {
        return new AuthorizationResponse(code, state, redirectUri, null, null);
    }

    /**
     * 创建错误响应。
     *
     * @param error 错误码
     * @param errorDescription 错误描述
     * @param state 状态参数
     * @param redirectUri 重定向 URI
     * @return 错误响应
     */
    public static AuthorizationResponse error(
            @NonNull String error,
            @Nullable String errorDescription,
            @Nullable String state,
            @NonNull String redirectUri) {
        return new AuthorizationResponse(null, state, redirectUri, error, errorDescription);
    }

    /**
     * 是否成功。
     *
     * @return 如果成功返回 true
     */
    public boolean isSuccess() {
        return code != null && error == null;
    }
}
