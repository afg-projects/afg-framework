package io.github.afgprojects.framework.security.auth.oauth2.controller;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.afgprojects.framework.security.core.oauth2.AccessTokenInfo;
import io.github.afgprojects.framework.security.core.oauth2.OAuth2AuthorizationService;
import io.github.afgprojects.framework.security.core.oauth2.OAuth2Exception;
import io.github.afgprojects.framework.security.core.oauth2.model.TokenRequest;
import io.github.afgprojects.framework.security.core.oauth2.model.TokenResponse;

/**
 * OAuth2 令牌端点控制器。
 *
 * <p>实现 OAuth2 令牌端点：
 * <ul>
 *   <li>POST /oauth2/token - 令牌颁发</li>
 *   <li>POST /oauth2/revoke - 令牌撤销</li>
 *   <li>POST /oauth2/introspect - 令牌自省</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/oauth2")
public class OAuth2TokenController {

    private final OAuth2AuthorizationService authorizationService;

    /**
     * 构造函数。
     *
     * @param authorizationService 授权服务
     */
    public OAuth2TokenController(@NonNull OAuth2AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    /**
     * 令牌端点。
     *
     * <p>使用授权码或刷新令牌换取访问令牌。
     *
     * @param request 令牌请求
     * @return 令牌响应
     */
    @PostMapping("/token")
    public ResponseEntity<TokenResponse> token(@RequestBody TokenEndpointRequest request) {
        log.info("Token request: grantType={}, clientId={}", request.grantType(), request.clientId());

        TokenRequest tokenRequest = new TokenRequest(
                request.grantType(),
                request.code(),
                request.refreshToken(),
                request.redirectUri(),
                request.clientId(),
                request.clientSecret(),
                request.codeVerifier(),
                request.parseScope());

        TokenResponse response = authorizationService.issueToken(tokenRequest);

        log.info("Token issued: clientId={}", request.clientId());

        return ResponseEntity.ok(response);
    }

    /**
     * 令牌撤销端点。
     *
     * @param request 撤销请求
     * @return 空响应
     */
    @PostMapping("/revoke")
    public ResponseEntity<Void> revoke(@RequestBody RevokeRequest request) {
        log.info("Token revoke request");

        authorizationService.revokeToken(request.token(), request.tokenTypeHint());

        return ResponseEntity.ok().build();
    }

    /**
     * 令牌自省端点。
     *
     * <p>验证令牌并返回令牌信息。
     *
     * @param token 要验证的令牌
     * @return 令牌信息
     */
    @PostMapping("/introspect")
    public ResponseEntity<IntrospectResponse> introspect(@RequestParam String token) {
        log.debug("Token introspect request");

        AccessTokenInfo info = authorizationService.validateToken(token);

        if (info == null) {
            return ResponseEntity.ok(IntrospectResponse.inactive());
        }

        IntrospectResponse response = IntrospectResponse.active(
                String.join(" ", info.scopes()),
                info.clientId(),
                info.username(),
                info.userId(),
                info.expiresAt().getEpochSecond(),
                info.issuedAt().getEpochSecond());

        return ResponseEntity.ok(response);
    }

    /**
     * OAuth2 异常处理。
     *
     * @param e OAuth2 异常
     * @return 错误响应
     */
    @ExceptionHandler(OAuth2Exception.class)
    public ResponseEntity<ErrorResponse> handleOAuth2Exception(OAuth2Exception e) {
        log.warn("OAuth2 error: {} - {}", e.getErrorCode(), e.getErrorDescription());

        ErrorResponse error = new ErrorResponse(e.getErrorCode(), e.getErrorDescription());

        // 根据错误类型返回不同的 HTTP 状态码
        HttpStatus status = switch (e.getErrorCode()) {
            case "invalid_client", "invalid_grant" -> HttpStatus.BAD_REQUEST;
            case "invalid_request" -> HttpStatus.BAD_REQUEST;
            case "unsupported_grant_type", "unsupported_response_type" -> HttpStatus.BAD_REQUEST;
            case "access_denied" -> HttpStatus.FORBIDDEN;
            case "server_error" -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.BAD_REQUEST;
        };

        return ResponseEntity.status(status).body(error);
    }

    /**
     * 错误响应。
     */
    public record ErrorResponse(String error, String error_description) {
    }
}