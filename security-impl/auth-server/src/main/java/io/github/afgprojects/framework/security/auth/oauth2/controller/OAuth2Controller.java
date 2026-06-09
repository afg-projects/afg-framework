package io.github.afgprojects.framework.security.auth.oauth2.controller;

import io.github.afgprojects.framework.security.core.authentication.AfgAuthentication;
import io.github.afgprojects.framework.security.core.oauth2.AccessTokenInfo;
import io.github.afgprojects.framework.security.core.oauth2.OAuth2AuthorizationService;
import io.github.afgprojects.framework.security.core.oauth2.OAuth2ClientService;
import io.github.afgprojects.framework.security.core.oauth2.model.AuthorizationRequest;
import io.github.afgprojects.framework.security.core.oauth2.model.AuthorizationResponse;
import io.github.afgprojects.framework.security.core.oauth2.model.ClientDetails;
import io.github.afgprojects.framework.security.core.oauth2.model.TokenRequest;
import io.github.afgprojects.framework.security.core.oauth2.model.TokenResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;

/**
 * OAuth2 授权服务器 REST 控制器。
 *
 * <p>提供 OAuth2 授权码流程的 REST API 端点：
 * <ul>
 *   <li>GET /oauth2/authorize - 验证授权请求参数，返回审批信息（SPA 调用）</li>
 *   <li>POST /oauth2/authorize - 用户确认授权，生成授权码并返回重定向 URL</li>
 *   <li>POST /oauth2/token - 用授权码/刷新令牌换取访问令牌</li>
 *   <li>POST /oauth2/introspect - Token 内省（RFC 7662）</li>
 *   <li>POST /oauth2/revoke - Token 撤销（RFC 7009）</li>
 * </ul>
 *
 * <p>框架通过 ModuleWebAutoConfiguration 自动为 auth-server 模块下的 Controller
 * 添加 /auth-api 前缀，因此完整路径为 /auth-api/oauth2/authorize 等。
 *
 * <p>设计决策：授权审批流程采用 JSON API 方式（而非传统 302 重定向），
 * 以兼容 SPA 前端架构。前端自行处理重定向跳转。
 *
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/oauth2")
public class OAuth2Controller {

    private final OAuth2AuthorizationService authorizationService;
    private final OAuth2ClientService clientService;

    public OAuth2Controller(
            @NonNull OAuth2AuthorizationService authorizationService,
            @NonNull OAuth2ClientService clientService) {
        this.authorizationService = authorizationService;
        this.clientService = clientService;
    }

    // ==================== 授权端点 ====================

    /**
     * 获取授权审批信息。
     *
     * <p>SPA 前端调用此端点获取客户端名称和请求的权限范围，
     * 用于渲染授权审批页面。
     *
     * <p>需要用户已认证（Bearer Token）。
     *
     * @param responseType 响应类型（必须为 "code"）
     * @param clientId 客户端 ID
     * @param redirectUri 重定向 URI
     * @param scope 请求的权限范围（空格分隔）
     * @param state 状态参数
     * @param codeChallenge PKCE code_challenge
     * @param codeChallengeMethod PKCE 方法（S256 / plain）
     * @return 授权审批信息或错误
     */
    @GetMapping("/authorize")
    public ResponseEntity<AuthorizeInfoResponse> authorizeInfo(
            @RequestParam String responseType,
            @RequestParam String clientId,
            @RequestParam String redirectUri,
            @RequestParam(required = false) @Nullable String scope,
            @RequestParam(required = false) @Nullable String state,
            @RequestParam(required = false) @Nullable String codeChallenge,
            @RequestParam(required = false) @Nullable String codeChallengeMethod) {

        log.debug("Authorization info request: clientId={}, responseType={}, scope={}", clientId, responseType, scope);

        // 1. 验证客户端
        ClientDetails client = clientService.loadClientByClientId(clientId);
        if (client == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AuthorizeInfoResponse.error("invalid_client", "客户端不存在"));
        }

        // 2. 验证响应类型
        if (!"code".equals(responseType)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AuthorizeInfoResponse.error("unsupported_response_type", "不支持的响应类型"));
        }

        // 3. 验证重定向 URI
        if (!client.isRedirectUriAllowed(redirectUri)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AuthorizeInfoResponse.error("invalid_request", "无效的重定向 URI"));
        }

        // 4. PKCE 检查
        if (client.requirePkce() && (codeChallenge == null || codeChallenge.isEmpty())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AuthorizeInfoResponse.error("invalid_request", "客户端需要 PKCE"));
        }

        // 5. 验证请求的 scope
        Set<String> requestedScopes = parseScope(scope);
        if (!requestedScopes.isEmpty() && !client.isScopeAllowed(requestedScopes)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AuthorizeInfoResponse.error("invalid_scope", "请求的权限范围超出客户端允许的范围"));
        }

        // 返回审批信息
        return ResponseEntity.ok(AuthorizeInfoResponse.consent(
                client.clientId(),
                client.clientName(),
                requestedScopes.isEmpty() ? client.scopes() : requestedScopes,
                true));
    }

    /**
     * 用户确认/拒绝授权。
     *
     * <p>SPA 前端在审批页面上调用此端点，传入用户的决定。
     * 如果用户同意，服务生成授权码并返回重定向 URL；
     * 如果用户拒绝，返回包含错误信息的重定向 URL。
     *
     * <p>需要用户已认证（Bearer Token）。
     *
     * @param request 授权确认请求
     * @return 包含重定向 URL 的响应
     */
    @PostMapping("/authorize")
    public ResponseEntity<AuthorizeConsentResponse> authorizeConsent(
            @RequestBody AuthorizeConsentRequest request,
            HttpServletRequest httpRequest) {

        log.debug("Authorization consent: clientId={}, approved={}", request.clientId(), request.approved());

        // 提取已认证用户 ID（优先从 SecurityContext，降级从 Bearer Token 解析）
        String userId = extractUserId(httpRequest);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthorizeConsentResponse.error("unauthorized", "用户未认证", null));
        }

        if (request.approved()) {
            // 用户同意授权 → 调用授权服务生成授权码
            AuthorizationRequest authRequest = new AuthorizationRequest(
                    request.responseType(),
                    request.clientId(),
                    request.redirectUri(),
                    parseScope(request.scope()),
                    request.state(),
                    request.codeChallenge(),
                    request.codeChallengeMethod());

            AuthorizationResponse authResponse = authorizationService.authorize(authRequest, userId);

            if (authResponse.isSuccess()) {
                // 构建重定向 URL
                String redirectUrl = buildRedirectUrl(authResponse);
                return ResponseEntity.ok(AuthorizeConsentResponse.success(redirectUrl));
            } else {
                // 授权服务返回错误（如 PKCE 验证失败等）
                String errorUrl = buildErrorRedirectUrl(authResponse);
                return ResponseEntity.ok(AuthorizeConsentResponse.errorRedirect(errorUrl));
            }
        } else {
            // 用户拒绝授权
            String errorUrl = buildAccessDeniedUrl(
                    request.redirectUri(), request.state(),
                    "用户拒绝了授权请求");
            return ResponseEntity.ok(AuthorizeConsentResponse.errorRedirect(errorUrl));
        }
    }

    // ==================== 令牌端点 ====================

    /**
     * 令牌端点（RFC 6749 Section 3.2）。
     *
     * <p>支持以下授权类型：
     * <ul>
     *   <li>authorization_code - 用授权码换取令牌</li>
     *   <li>refresh_token - 用刷新令牌获取新令牌</li>
     *   <li>client_credentials - 客户端凭证模式（服务间调用）</li>
     * </ul>
     *
     * <p>机密客户端可通过 Authorization: Basic 头传递凭证。
     *
     * @param request 令牌请求
     * @param authorization 可选的 Basic Auth 头
     * @return 令牌响应
     */
    @PostMapping("/token")
    public ResponseEntity<TokenResponse> token(
            @RequestBody TokenEndpointRequest request,
            @RequestHeader(value = "Authorization", required = false) @Nullable String authorization) {

        log.debug("Token request: grantType={}, clientId={}", request.grantType(), request.clientId());

        // 解析客户端凭证（支持 Basic Auth 和请求体两种方式）
        String clientId = request.clientId();
        String clientSecret = request.clientSecret();

        if (authorization != null && authorization.startsWith("Basic ")) {
            String[] credentials = decodeBasicAuth(authorization);
            if (credentials != null && credentials.length == 2) {
                clientId = credentials[0];
                clientSecret = credentials[1];
            }
        }

        // 构建 SPI 层 TokenRequest
        TokenRequest tokenRequest = new TokenRequest(
                request.grantType(),
                request.code(),
                request.refreshToken(),
                request.redirectUri(),
                clientId,
                clientSecret,
                request.codeVerifier(),
                request.parseScope());

        TokenResponse tokenResponse = authorizationService.issueToken(tokenRequest);
        return ResponseEntity.ok(tokenResponse);
    }

    // ==================== 内省端点 ====================

    /**
     * 令牌内省端点（RFC 7662）。
     *
     * <p>验证令牌有效性并返回令牌元数据。
     *
     * @param request 内省请求
     * @return 内省响应
     */
    @PostMapping("/introspect")
    public IntrospectResponse introspect(@RequestBody IntrospectRequest request) {
        log.debug("Token introspect: tokenTypeHint={}", request.tokenTypeHint());

        AccessTokenInfo tokenInfo = authorizationService.validateToken(request.token());
        if (tokenInfo == null || !tokenInfo.isValid()) {
            return IntrospectResponse.inactive();
        }

        return IntrospectResponse.active(
                tokenInfo.scopes() != null ? String.join(" ", tokenInfo.scopes()) : "",
                tokenInfo.clientId(),
                tokenInfo.username(),
                tokenInfo.userId(),
                tokenInfo.expiresAt().getEpochSecond(),
                tokenInfo.issuedAt().getEpochSecond());
    }

    // ==================== 撤销端点 ====================

    /**
     * 令牌撤销端点（RFC 7009）。
     *
     * <p>撤销指定令牌。即使令牌不存在或已过期，也返回 200 OK。
     *
     * @param request 撤销请求
     * @return 200 OK（空响应体）
     */
    @PostMapping("/revoke")
    public ResponseEntity<Void> revoke(@RequestBody RevokeRequest request) {
        log.debug("Token revoke: tokenTypeHint={}", request.tokenTypeHint());

        authorizationService.revokeToken(request.token(), request.tokenTypeHint());
        return ResponseEntity.ok().build();
    }

    // ==================== 私有方法 ====================

    /**
     * 从请求中提取已认证用户 ID。
     *
     * <p>由 {@link io.github.afgprojects.framework.security.auth.token.AuthServerBearerTokenFilter}
     * 在请求前自动解析 Bearer Token 并填充 SecurityContext，此方法直接从 SecurityContext 获取。
     */
    @Nullable
    private String extractUserId(HttpServletRequest request) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof AfgAuthentication afgAuth) {
            return afgAuth.getUserId();
        }
        if (authentication != null && authentication.getPrincipal() != null) {
            return authentication.getName();
        }
        return null;
    }

    /**
     * 解析 scope 字符串为 Set。
     */
    private Set<String> parseScope(@Nullable String scope) {
        if (scope == null || scope.isBlank()) {
            return Set.of();
        }
        return Set.of(scope.split("\\s+"));
    }

    /**
     * 构建授权成功重定向 URL。
     */
    @NonNull
    private String buildRedirectUrl(@NonNull AuthorizationResponse response) {
        StringBuilder url = new StringBuilder(response.redirectUri());
        url.append("?code=").append(encode(response.code()));
        if (response.state() != null) {
            url.append("&state=").append(encode(response.state()));
        }
        return url.toString();
    }

    /**
     * 构建授权错误重定向 URL。
     */
    @NonNull
    private String buildErrorRedirectUrl(@NonNull AuthorizationResponse response) {
        StringBuilder url = new StringBuilder(response.redirectUri());
        url.append("?error=").append(encode(response.error()));
        if (response.errorDescription() != null) {
            url.append("&error_description=").append(encode(response.errorDescription()));
        }
        if (response.state() != null) {
            url.append("&state=").append(encode(response.state()));
        }
        return url.toString();
    }

    /**
     * 构建用户拒绝授权的重定向 URL。
     */
    @NonNull
    private String buildAccessDeniedUrl(
            @NonNull String redirectUri,
            @Nullable String state,
            @NonNull String description) {
        StringBuilder url = new StringBuilder(redirectUri);
        url.append("?error=access_denied");
        url.append("&error_description=").append(encode(description));
        if (state != null) {
            url.append("&state=").append(encode(state));
        }
        return url.toString();
    }

    /**
     * URL 编码。
     */
    private String encode(@Nullable String value) {
        if (value == null) return "";
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * 解码 Basic Auth 头。
     *
     * @param authorization "Basic base64(clientId:clientSecret)"
     * @return [clientId, clientSecret] 或 null
     */
    @Nullable
    private String[] decodeBasicAuth(@NonNull String authorization) {
        try {
            String base64 = authorization.substring(6);
            String decoded = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
            int colon = decoded.indexOf(':');
            if (colon > 0) {
                return new String[]{decoded.substring(0, colon), decoded.substring(colon + 1)};
            }
        } catch (Exception e) {
            log.warn("Failed to decode Basic Auth header", e);
        }
        return null;
    }

    // ==================== 响应 DTO ====================

    /**
     * 授权审批信息响应（GET /authorize 返回）。
     */
    public record AuthorizeInfoResponse(
            @Nullable String clientId,
            @Nullable String clientName,
            @Nullable Set<String> scopes,
            boolean needConsent,
            @Nullable String error,
            @Nullable String errorDescription) {

        /**
         * 创建审批信息响应。
         */
        public static AuthorizeInfoResponse consent(
                @NonNull String clientId,
                @NonNull String clientName,
                @NonNull Set<String> scopes,
                boolean needConsent) {
            return new AuthorizeInfoResponse(clientId, clientName, scopes, needConsent, null, null);
        }

        /**
         * 创建错误响应。
         */
        public static AuthorizeInfoResponse error(
                @NonNull String error,
                @Nullable String errorDescription) {
            return new AuthorizeInfoResponse(null, null, null, false, error, errorDescription);
        }
    }

    /**
     * 授权确认响应（POST /authorize 返回）。
     */
    public record AuthorizeConsentResponse(
            boolean success,
            @Nullable String redirectUrl,
            @Nullable String error,
            @Nullable String errorDescription) {

        /**
         * 创建成功响应（包含重定向 URL）。
         */
        public static AuthorizeConsentResponse success(@NonNull String redirectUrl) {
            return new AuthorizeConsentResponse(true, redirectUrl, null, null);
        }

        /**
         * 创建错误重定向响应。
         */
        public static AuthorizeConsentResponse errorRedirect(@NonNull String redirectUrl) {
            return new AuthorizeConsentResponse(false, redirectUrl, null, null);
        }

        /**
         * 创建非重定向错误响应。
         */
        public static AuthorizeConsentResponse error(
                @NonNull String error,
                @Nullable String errorDescription,
                @Nullable String redirectUrl) {
            return new AuthorizeConsentResponse(false, redirectUrl, error, errorDescription);
        }
    }

    /**
     * 授权确认请求（POST /authorize 接收）。
     */
    public record AuthorizeConsentRequest(
            @NonNull String responseType,
            @NonNull String clientId,
            @NonNull String redirectUri,
            @Nullable String scope,
            @Nullable String state,
            @Nullable String codeChallenge,
            @Nullable String codeChallengeMethod,
            boolean approved) {}
}