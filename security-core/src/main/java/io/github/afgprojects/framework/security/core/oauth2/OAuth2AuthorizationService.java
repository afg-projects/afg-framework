package io.github.afgprojects.framework.security.core.oauth2;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import io.github.afgprojects.framework.security.core.oauth2.model.AuthorizationRequest;
import io.github.afgprojects.framework.security.core.oauth2.model.AuthorizationResponse;
import io.github.afgprojects.framework.security.core.oauth2.model.TokenRequest;
import io.github.afgprojects.framework.security.core.oauth2.model.TokenResponse;

/**
 * OAuth2 授权服务接口。
 *
 * <p>提供 OAuth2 授权码流程和令牌颁发功能。
 *
 * @since 1.0.0
 */
public interface OAuth2AuthorizationService {

    /**
     * 处理授权请求。
     *
     * <p>验证客户端和请求参数，生成授权码或返回错误。
     *
     * @param request 授权请求，永不为 null
     * @param userId 已认证用户 ID，永不为 null
     * @return 授权响应（包含授权码或重定向 URI），永不为 null
     */
    @NonNull
    AuthorizationResponse authorize(@NonNull AuthorizationRequest request, @NonNull String userId);

    /**
     * 颁发令牌。
     *
     * <p>使用授权码或刷新令牌换取访问令牌。
     *
     * @param request 令牌请求，永不为 null
     * @return 令牌响应，永不为 null
     */
    @NonNull
    TokenResponse issueToken(@NonNull TokenRequest request);

    /**
     * 验证访问令牌。
     *
     * @param accessToken 访问令牌，永不为 null
     * @return 令牌信息，如果无效则返回 null
     */
    @Nullable
    AccessTokenInfo validateToken(@NonNull String accessToken);

    /**
     * 撤销令牌。
     *
     * @param token 令牌，永不为 null
     * @param tokenTypeHint 令牌类型提示（access_token / refresh_token）
     */
    void revokeToken(@NonNull String token, @Nullable String tokenTypeHint);
}