package io.github.afgprojects.framework.security.auth.oauth2.config;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import io.github.afgprojects.framework.security.auth.oauth2.DefaultOAuth2AuthorizationService;
import io.github.afgprojects.framework.security.auth.oauth2.InMemoryAuthorizationCodeStorage;
import io.github.afgprojects.framework.security.auth.oauth2.InMemoryOAuth2ClientService;
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
@EnableConfigurationProperties(OAuth2Properties.class)
@ConditionalOnProperty(prefix = "afg.auth.oauth2", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OAuth2AutoConfiguration {

    /**
     * 配置 OAuth2 授权服务。
     *
     * @param clientService 客户端服务
     * @param tokenService 令牌服务
     * @param authorizationCodeStorage 授权码存储
     * @return OAuth2 授权服务
     */
    @Bean
    @ConditionalOnMissingBean
    public OAuth2AuthorizationService oAuth2AuthorizationService(
            @NonNull OAuth2ClientService clientService,
            @NonNull TokenService tokenService,
            @NonNull AuthorizationCodeStorage authorizationCodeStorage) {
        return new DefaultOAuth2AuthorizationService(clientService, tokenService, authorizationCodeStorage);
    }

    /**
     * 配置 OAuth2 客户端服务。
     *
     * @param properties OAuth2 配置属性
     * @return OAuth2 客户端服务
     */
    @Bean
    @ConditionalOnMissingBean
    public OAuth2ClientService oAuth2ClientService(@NonNull OAuth2Properties properties) {
        // 将配置中的客户端转换为 ClientDetails
        Set<ClientDetails> clients = properties.getClients().stream()
                .map(config -> new ClientDetails(
                        config.clientId(),
                        config.clientSecret(),
                        config.clientName(),
                        config.redirectUris(),
                        config.scopes(),
                        config.grantTypes(),
                        config.requirePkce(),
                        properties.getAccessTokenTtl(),
                        properties.getRefreshTokenTtl()
                ))
                .collect(Collectors.toSet());

        return new InMemoryOAuth2ClientService(clients);
    }

    /**
     * 配置授权码存储。
     *
     * @return 授权码存储（内存实现）
     */
    @Bean
    @ConditionalOnMissingBean
    public AuthorizationCodeStorage authorizationCodeStorage() {
        return new InMemoryAuthorizationCodeStorage();
    }
}