package io.github.afgprojects.framework.security.auth.autoconfigure;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import io.github.afgprojects.framework.security.auth.oauth2.DefaultOAuth2AuthorizationService;
import io.github.afgprojects.framework.security.auth.oauth2.InMemoryAuthorizationCodeStorage;
import io.github.afgprojects.framework.security.auth.oauth2.InMemoryOAuth2ClientService;
import io.github.afgprojects.framework.security.auth.properties.AuthSecurityProperties;
import io.github.afgprojects.framework.security.auth.properties.oauth2.OAuth2Config;
import io.github.afgprojects.framework.security.auth.properties.token.TokenConfig;
import io.github.afgprojects.framework.security.core.oauth2.AuthorizationCodeStorage;
import io.github.afgprojects.framework.security.core.oauth2.OAuth2AuthorizationService;
import io.github.afgprojects.framework.security.core.oauth2.OAuth2ClientService;
import io.github.afgprojects.framework.security.core.oauth2.model.ClientDetails;
import io.github.afgprojects.framework.security.core.login.TokenService;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * OAuth2 自动配置。
 *
 * <p>自动配置 OAuth2 授权服务器所需的核心组件：
 * <ul>
 *   <li>OAuth2AuthorizationService - 授权服务</li>
 *   <li>OAuth2ClientService - 客户端服务</li>
 *   <li>AuthorizationCodeStorage - 授权码存储</li>
 * </ul>
 *
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "afg.security.auth-server.oauth2", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OAuth2AutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OAuth2AuthorizationService oAuth2AuthorizationService(
            @NonNull OAuth2ClientService clientService,
            @NonNull TokenService tokenService,
            @NonNull AuthorizationCodeStorage authorizationCodeStorage) {
        return new DefaultOAuth2AuthorizationService(clientService, tokenService, authorizationCodeStorage);
    }

    @Bean
    @ConditionalOnMissingBean
    public OAuth2ClientService oAuth2ClientService(@NonNull AuthSecurityProperties properties) {
        OAuth2Config oauth2Config = properties.getOauth2();
        TokenConfig tokenConfig = properties.getToken();

        Set<ClientDetails> clients = oauth2Config.getClients().stream()
                .map(config -> new ClientDetails(
                        config.clientId(),
                        config.clientSecret(),
                        config.clientName(),
                        config.redirectUris(),
                        config.scopes(),
                        config.grantTypes(),
                        config.requirePkce(),
                        tokenConfig.getAccessTokenTtl(),
                        tokenConfig.getRefreshTokenTtl()
                ))
                .collect(Collectors.toSet());

        return new InMemoryOAuth2ClientService(clients);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthorizationCodeStorage authorizationCodeStorage() {
        return new InMemoryAuthorizationCodeStorage();
    }
}