package io.github.afgprojects.framework.security.auth.oauth2.controller;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import io.github.afgprojects.framework.security.core.oauth2.OAuth2AuthorizationService;
import io.github.afgprojects.framework.security.core.oauth2.model.AuthorizationRequest;
import io.github.afgprojects.framework.security.core.oauth2.model.AuthorizationResponse;

/**
 * OAuth2 授权端点控制器。
 *
 * <p>实现 OAuth2 授权码流程的授权端点：
 * <ul>
 *   <li>GET /oauth2/authorize - 授权请求</li>
 *   <li>POST /oauth2/authorize - 授权确认</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/oauth2")
public class OAuth2AuthorizeController {

    private final OAuth2AuthorizationService authorizationService;

    /**
     * 构造函数。
     *
     * @param authorizationService 授权服务
     */
    public OAuth2AuthorizeController(@NonNull OAuth2AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    /**
     * 授权端点（GET）。
     *
     * <p>客户端发起授权请求，用户确认后重定向到客户端回调地址。
     *
     * @param userId 已认证用户 ID（从安全上下文获取）
     * @return 重定向到客户端回调地址
     */
    @GetMapping("/authorize")
    public RedirectView authorize(
            @RequestParam String responseType,
            @RequestParam String clientId,
            @RequestParam String redirectUri,
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String codeChallenge,
            @RequestParam(required = false) String codeChallengeMethod,
            @RequestParam String userId) {

        log.info("Authorization request: clientId={}, responseType={}", clientId, responseType);

        AuthorizationRequest request = new AuthorizationRequest(
                responseType, clientId, redirectUri,
                parseScope(scope), state, codeChallenge, codeChallengeMethod);

        AuthorizationResponse response = authorizationService.authorize(request, userId);

        // 构建重定向 URL
        String redirectUrl = buildRedirectUrl(response);

        return new RedirectView(redirectUrl);
    }

    /**
     * 授权端点（POST）。
     *
     * <p>用户确认授权后提交。
     *
     * @param request 授权请求参数
     * @param userId 已认证用户 ID
     * @return 重定向到客户端回调地址
     */
    @PostMapping("/authorize")
    public RedirectView authorizeConfirm(
            @RequestBody AuthorizeRequest request,
            @RequestParam String userId) {

        log.info("Authorization confirm: clientId={}, userId={}", request.clientId(), userId);

        AuthorizationRequest authRequest = new AuthorizationRequest(
                request.responseType(), request.clientId(), request.redirectUri(),
                request.parseScope(), request.state(),
                request.codeChallenge(), request.codeChallengeMethod());

        AuthorizationResponse response = authorizationService.authorize(authRequest, userId);

        String redirectUrl = buildRedirectUrl(response);

        return new RedirectView(redirectUrl);
    }

    /**
     * 构建重定向 URL。
     */
    private String buildRedirectUrl(AuthorizationResponse response) {
        StringBuilder url = new StringBuilder(response.redirectUri());

        if (response.isSuccess()) {
            url.append("?code=").append(response.code());
            if (response.state() != null) {
                url.append("&state=").append(response.state());
            }
        } else {
            url.append("?error=").append(response.error());
            if (response.errorDescription() != null) {
                url.append("&error_description=").append(encodeUrl(response.errorDescription()));
            }
            if (response.state() != null) {
                url.append("&state=").append(response.state());
            }
        }

        return url.toString();
    }

    /**
     * URL 编码。
     */
    private String encodeUrl(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * 解析 scope 字符串。
     */
    private java.util.Set<String> parseScope(String scope) {
        if (scope == null || scope.isBlank()) {
            return java.util.Set.of();
        }
        return java.util.Set.of(scope.split("\\s+"));
    }
}