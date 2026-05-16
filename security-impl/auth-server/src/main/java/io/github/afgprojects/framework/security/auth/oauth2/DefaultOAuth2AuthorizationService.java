package io.github.afgprojects.framework.security.auth.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import io.github.afgprojects.framework.security.core.login.TokenService;
import io.github.afgprojects.framework.security.core.oauth2.AccessTokenInfo;
import io.github.afgprojects.framework.security.core.oauth2.AuthorizationCodeStorage;
import io.github.afgprojects.framework.security.core.oauth2.OAuth2AuthorizationService;
import io.github.afgprojects.framework.security.core.oauth2.OAuth2ClientService;
import io.github.afgprojects.framework.security.core.oauth2.OAuth2Exception;
import io.github.afgprojects.framework.security.core.oauth2.model.AuthorizationCode;
import io.github.afgprojects.framework.security.core.oauth2.model.AuthorizationRequest;
import io.github.afgprojects.framework.security.core.oauth2.model.AuthorizationResponse;
import io.github.afgprojects.framework.security.core.oauth2.model.ClientDetails;
import io.github.afgprojects.framework.security.core.oauth2.model.TokenRequest;
import io.github.afgprojects.framework.security.core.oauth2.model.TokenResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认 OAuth2 授权服务实现。
 *
 * <p>支持以下授权类型：
 * <ul>
 *   <li>Authorization Code - 授权码模式</li>
 *   <li>Authorization Code + PKCE - 移动端/SPA 安全增强</li>
 *   <li>Refresh Token - 刷新令牌</li>
 *   <li>Client Credentials - 客户端凭证（服务间调用）</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Slf4j
public class DefaultOAuth2AuthorizationService implements OAuth2AuthorizationService {

    private static final Duration DEFAULT_AUTH_CODE_TTL = Duration.ofMinutes(5);

    private final ConcurrentHashMap<String, AuthorizationCode> authorizationCodes = new ConcurrentHashMap<>();

    private final OAuth2ClientService clientService;
    private final TokenService tokenService;
    private final AuthorizationCodeStorage authorizationCodeStorage;

    /**
     * 构造函数。
     *
     * @param clientService 客户端服务
     * @param tokenService 令牌服务
     * @param authorizationCodeStorage 授权码存储（可选，默认内存）
     */
    public DefaultOAuth2AuthorizationService(
            @NonNull OAuth2ClientService clientService,
            @NonNull TokenService tokenService,
            @Nullable AuthorizationCodeStorage authorizationCodeStorage) {
        this.clientService = clientService;
        this.tokenService = tokenService;
        this.authorizationCodeStorage = authorizationCodeStorage;
    }

    @Override
    @NonNull
    public AuthorizationResponse authorize(@NonNull AuthorizationRequest request, @NonNull String userId) {
        log.debug("Processing authorization request: clientId={}, responseType={}",
                request.clientId(), request.responseType());

        // 1. 加载客户端
        ClientDetails client = clientService.loadClientByClientId(request.clientId());
        if (client == null) {
            return AuthorizationResponse.error(
                    "invalid_client", "客户端不存在", request.state(), request.redirectUri());
        }

        // 2. 验证响应类型
        if (!"code".equals(request.responseType())) {
            return AuthorizationResponse.error(
                    "unsupported_response_type", "不支持的响应类型", request.state(), request.redirectUri());
        }

        // 3. 验证重定向 URI
        if (!client.isRedirectUriAllowed(request.redirectUri())) {
            return AuthorizationResponse.error(
                    "invalid_request", "无效的重定向 URI", request.state(), request.redirectUri());
        }

        // 4. PKCE 检查
        if (client.requirePkce() && !request.isPkce()) {
            return AuthorizationResponse.error(
                    "invalid_request", "客户端需要 PKCE", request.state(), request.redirectUri());
        }

        // 5. 生成授权码
        String code = generateAuthorizationCode();
        Instant now = Instant.now();
        AuthorizationCode authCode = new AuthorizationCode(
                code,
                request.clientId(),
                userId,
                request.redirectUri(),
                request.scope(),
                request.codeChallenge(),
                request.codeChallengeMethod(),
                now.plus(DEFAULT_AUTH_CODE_TTL),
                now);

        // 6. 存储授权码
        saveAuthorizationCode(authCode);

        log.info("Authorization code generated: clientId={}, userId={}", request.clientId(), userId);

        return AuthorizationResponse.success(code, request.state(), request.redirectUri());
    }

    @Override
    @NonNull
    public TokenResponse issueToken(@NonNull TokenRequest request) {
        log.debug("Processing token request: grantType={}, clientId={}",
                request.grantType(), request.clientId());

        // 1. 验证客户端
        ClientDetails client = clientService.loadClientByClientId(request.clientId());
        if (client == null) {
            throw OAuth2Exception.invalidClient("客户端不存在");
        }

        // 2. 验证客户端凭证（机密客户端）
        if (client.getClientType() == ClientDetails.ClientType.CONFIDENTIAL) {
            if (!clientService.validateClientCredentials(request.clientId(), request.clientSecret())) {
                throw OAuth2Exception.invalidClient("客户端凭证无效");
            }
        }

        // 3. 验证授权类型
        if (!client.isGrantTypeAllowed(request.grantType())) {
            throw OAuth2Exception.unsupportedGrantType("不支持的授权类型");
        }

        // 4. 根据授权类型处理
        return switch (request.grantType()) {
            case TokenRequest.GRANT_AUTHORIZATION_CODE -> handleAuthorizationCodeGrant(request, client);
            case TokenRequest.GRANT_REFRESH_TOKEN -> handleRefreshTokenGrant(request, client);
            case TokenRequest.GRANT_CLIENT_CREDENTIALS -> handleClientCredentialsGrant(request, client);
            default -> throw OAuth2Exception.unsupportedGrantType("不支持的授权类型: " + request.grantType());
        };
    }

    @Override
    @Nullable
    public AccessTokenInfo validateToken(@NonNull String accessToken) {
        if (!tokenService.validateAccessToken(accessToken)) {
            return null;
        }

        String userId = tokenService.extractUserId(accessToken);
        if (userId == null) {
            return null;
        }

        String username = tokenService.extractUsername(accessToken);
        Set<String> roles = tokenService.extractRoles(accessToken);
        Set<String> permissions = tokenService.extractPermissions(accessToken);
        String tenantId = tokenService.extractTenantId(accessToken);

        // 合并 roles 和 permissions 作为 scopes
        Set<String> scopes = new java.util.HashSet<>();
        scopes.addAll(roles);
        scopes.addAll(permissions);

        // 从 token 中提取过期时间（简化实现，使用当前时间 + TTL）
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(tokenService.getAccessTokenTtl());

        return new AccessTokenInfo(userId, username, "", scopes, tenantId, expiresAt, now);
    }

    @Override
    public void revokeToken(@NonNull String token, @Nullable String tokenTypeHint) {
        tokenService.invalidateToken(token);
        log.info("Token revoked");
    }

    // ==================== 私有方法 ====================

    /**
     * 处理授权码授权。
     */
    private TokenResponse handleAuthorizationCodeGrant(TokenRequest request, ClientDetails client) {
        // 1. 获取授权码
        AuthorizationCode authCode = getAuthorizationCode(request.code());
        if (authCode == null) {
            throw OAuth2Exception.invalidGrant("授权码无效或已过期");
        }

        // 2. 验证授权码
        if (!authCode.clientId().equals(request.clientId())) {
            throw OAuth2Exception.invalidGrant("授权码不属于该客户端");
        }

        if (request.redirectUri() != null && !authCode.redirectUri().equals(request.redirectUri())) {
            throw OAuth2Exception.invalidGrant("重定向 URI 不匹配");
        }

        if (authCode.isExpired()) {
            throw OAuth2Exception.invalidGrant("授权码已过期");
        }

        // 3. PKCE 验证
        if (authCode.isPkce()) {
            if (request.codeVerifier() == null) {
                throw OAuth2Exception.invalidGrant("缺少 code_verifier");
            }
            if (!verifyPkce(authCode.codeChallenge(), authCode.codeChallengeMethod(), request.codeVerifier())) {
                throw OAuth2Exception.invalidGrant("PKCE 验证失败");
            }
        }

        // 4. 删除已使用的授权码（防止重放攻击）
        removeAuthorizationCode(request.code());

        // 5. 生成令牌
        Set<String> scopes = authCode.scopes() != null ? authCode.scopes() : Set.of();
        String accessToken = tokenService.generateAccessToken(
                authCode.userId(),
                null, // username
                scopes, // roles
                Set.of(), // permissions
                null); // tenantId

        String refreshToken = tokenService.generateRefreshToken(authCode.userId(), null);

        log.info("Token issued: clientId={}, userId={}", request.clientId(), authCode.userId());

        return TokenResponse.of(
                accessToken,
                client.accessTokenTtl().getSeconds(),
                refreshToken,
                authCode.scopes() != null ? String.join(" ", authCode.scopes()) : null);
    }

    /**
     * 处理刷新令牌授权。
     */
    private TokenResponse handleRefreshTokenGrant(TokenRequest request, ClientDetails client) {
        // 1. 验证刷新令牌
        if (!tokenService.validateRefreshToken(request.refreshToken())) {
            throw OAuth2Exception.invalidGrant("刷新令牌无效或已过期");
        }

        // 2. 提取用户 ID
        String userId = tokenService.extractUserId(request.refreshToken());
        if (userId == null) {
            throw OAuth2Exception.invalidGrant("无法提取用户信息");
        }

        // 3. 撤销旧刷新令牌
        tokenService.invalidateToken(request.refreshToken());

        // 4. 生成新令牌
        Set<String> scopes = request.scope() != null ? request.scope() : Set.of();
        String accessToken = tokenService.generateAccessToken(userId, null, scopes, Set.of(), null);
        String refreshToken = tokenService.generateRefreshToken(userId, null);

        log.info("Token refreshed: clientId={}, userId={}", request.clientId(), userId);

        return TokenResponse.of(
                accessToken,
                client.accessTokenTtl().getSeconds(),
                refreshToken,
                request.scope() != null ? String.join(" ", request.scope()) : null);
    }

    /**
     * 处理客户端凭证授权。
     */
    private TokenResponse handleClientCredentialsGrant(TokenRequest request, ClientDetails client) {
        // 生成客户端访问令牌
        Set<String> scopes = request.scope() != null ? request.scope() : client.scopes();
        String accessToken = tokenService.generateAccessToken(
                request.clientId(),
                request.clientId(), // username = clientId for client credentials
                scopes,
                Set.of(),
                null);

        log.info("Client credentials token issued: clientId={}", request.clientId());

        return TokenResponse.of(
                accessToken,
                client.accessTokenTtl().getSeconds(),
                null, // 客户端凭证授权不返回刷新令牌
                request.scope() != null ? String.join(" ", request.scope()) : null);
    }

    /**
     * 生成授权码。
     */
    private String generateAuthorizationCode() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 保存授权码。
     */
    private void saveAuthorizationCode(AuthorizationCode authCode) {
        if (authorizationCodeStorage != null) {
            authorizationCodeStorage.save(authCode);
        } else {
            authorizationCodes.put(authCode.code(), authCode);
        }
    }

    /**
     * 获取授权码。
     */
    private AuthorizationCode getAuthorizationCode(String code) {
        if (authorizationCodeStorage != null) {
            return authorizationCodeStorage.findByCode(code);
        }
        AuthorizationCode authCode = authorizationCodes.get(code);
        if (authCode != null && authCode.isExpired()) {
            authorizationCodes.remove(code);
            return null;
        }
        return authCode;
    }

    /**
     * 删除授权码。
     */
    private void removeAuthorizationCode(String code) {
        if (authorizationCodeStorage != null) {
            authorizationCodeStorage.delete(code);
        } else {
            authorizationCodes.remove(code);
        }
    }

    /**
     * 验证 PKCE。
     */
    private boolean verifyPkce(String challenge, String method, String verifier) {
        if ("plain".equals(method)) {
            return challenge.equals(verifier);
        } else if ("S256".equals(method)) {
            String computed = sha256Base64Url(verifier);
            return challenge.equals(computed);
        }
        return false;
    }

    /**
     * SHA-256 Base64Url 编码。
     */
    private String sha256Base64Url(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}