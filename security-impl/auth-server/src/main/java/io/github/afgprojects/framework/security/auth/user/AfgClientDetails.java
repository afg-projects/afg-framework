package io.github.afgprojects.framework.security.auth.user;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import lombok.Builder;
import lombok.Data;

/**
 * OAuth2 客户端详情信息
 *
 * <p>用于存储和传递 OAuth2 客户端的配置信息
 *
 * @since 1.0.0
 */
@Data
@Builder
public class AfgClientDetails {

    /**
     * 客户端 ID
     */
    private String clientId;

    /**
     * 客户端密钥
     */
    @Nullable
    private String clientSecret;

    /**
     * 客户端名称
     */
    private String clientName;

    /**
     * 支持的授权类型
     */
    private Set<AuthorizationGrantType> authorizationGrantTypes;

    /**
     * 客户端认证方式
     */
    private Set<ClientAuthenticationMethod> clientAuthenticationMethods;

    /**
     * 重定向 URI
     */
    @Builder.Default
    private Set<String> redirectUris = Collections.emptySet();

    /**
     * 授权范围
     */
    private Set<String> scopes;

    /**
     * 转换为 Spring Security RegisteredClient
     *
     * @return RegisteredClient 实例
     */
    public RegisteredClient toRegisteredClient() {
        RegisteredClient.Builder builder = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(clientId)
                .clientSecret(clientSecret != null ? clientSecret : "")
                .clientName(clientName);

        authorizationGrantTypes.forEach(builder::authorizationGrantType);
        clientAuthenticationMethods.forEach(builder::clientAuthenticationMethod);
        redirectUris.forEach(builder::redirectUri);
        scopes.forEach(builder::scope);

        return builder.build();
    }
}