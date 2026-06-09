package io.github.afgprojects.framework.security.auth.oauth2.controller;

import io.github.afgprojects.framework.security.core.oauth2.OAuth2Exception;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * OAuth2 异常处理器。
 *
 * <p>将 {@link OAuth2Exception} 转换为标准的 OAuth2 错误响应（RFC 6749 §5.2）。
 *
 * @since 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class OAuth2ExceptionControllerAdvice {

    /**
     * 处理 OAuth2Exception。
     *
     * @param ex OAuth2 异常
     * @return 标准错误响应
     */
    @ExceptionHandler(OAuth2Exception.class)
    public ResponseEntity<OAuth2ErrorResponse> handleOAuth2Exception(@NonNull OAuth2Exception ex) {
        log.warn("OAuth2 error: {} - {}", ex.getErrorCode(), ex.getErrorDescription());

        HttpStatus status = mapErrorCodeToHttpStatus(ex.getErrorCode());
        OAuth2ErrorResponse body = new OAuth2ErrorResponse(ex.getErrorCode(), ex.getErrorDescription());
        return ResponseEntity.status(status).body(body);
    }

    /**
     * 根据错误码映射 HTTP 状态码。
     */
    private HttpStatus mapErrorCodeToHttpStatus(@NonNull String errorCode) {
        return switch (errorCode) {
            case "invalid_client" -> HttpStatus.UNAUTHORIZED;
            case "access_denied" -> HttpStatus.FORBIDDEN;
            case "server_error" -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    /**
     * OAuth2 错误响应（RFC 6749 §5.2）。
     *
     * @param error 错误码
     * @param errorDescription 错误描述
     */
    public record OAuth2ErrorResponse(
            @NonNull String error,
            @Nullable String error_description) {}
}