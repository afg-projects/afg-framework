package io.github.afgprojects.framework.security.auth.config;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;

import io.github.afgprojects.framework.security.auth.token.JwtTokenProvider;
import io.github.afgprojects.framework.security.auth.user.AfgClientDetails;
import io.github.afgprojects.framework.security.auth.user.AfgClientDetailsService;

/**
 * OAuth2 授权服务器自动配置类
 *
 * <p>配置 OAuth2 授权服务器的核心组件：
 * <ul>
 *   <li>{@link AuthServerProperties} - 配置属性</li>
 *   <li>{@link JwtTokenProvider} - JWT Token 提供者</li>
 *   <li>{@link AuthorizationServerSettings} - 授权服务器设置</li>
 *   <li>{@link RegisteredClientRepository} - 客户端仓库（可选）</li>
 * </ul>
 *
 * <p>配置示例：
 * <pre>
 * afg:
 *   auth:
 *     server:
 *       enabled: true
 *       issuer: https://auth.example.com
 *       signing-key: your-256-bit-signing-key
 *       access-token-ttl: 2h
 *       refresh-token-ttl: 7d
 *       require-pkce: true
 * </pre>
 *
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(AuthServerProperties.class)
@ConditionalOnProperty(prefix = "afg.auth.server", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AuthServerAutoConfiguration {

    /**
     * 创建 JWT Token 提供者
     *
     * @param properties 授权服务器配置属性
     * @return JwtTokenProvider 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public JwtTokenProvider jwtTokenProvider(AuthServerProperties properties) {
        String signingKey = properties.getSigningKey();
        if (signingKey == null || signingKey.length() < 32) {
            throw new IllegalArgumentException(
                    "Signing key must be at least 256 bits (32 characters). " +
                            "Please configure 'afg.auth.server.signing-key' property.");
        }

        String issuer = properties.getIssuer();
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException(
                    "Issuer must be configured. " +
                            "Please configure 'afg.auth.server.issuer' property.");
        }

        log.info("Initializing JwtTokenProvider with issuer: {}", issuer);
        return new JwtTokenProvider(
                signingKey,
                issuer,
                properties.getAccessTokenTtl());
    }

    /**
     * 创建授权服务器设置
     *
     * @param properties 授权服务器配置属性
     * @return AuthorizationServerSettings 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public AuthorizationServerSettings authorizationServerSettings(AuthServerProperties properties) {
        log.info("Initializing AuthorizationServerSettings with issuer: {}", properties.getIssuer());
        return AuthorizationServerSettings.builder()
                .issuer(properties.getIssuer())
                .build();
    }

    /**
     * 创建注册客户端仓库
     *
     * <p>当业务系统提供 {@link AfgClientDetailsService} 实现时，
     * 自动创建适配 Spring Authorization Server 的 {@link RegisteredClientRepository}
     *
     * @param clientDetailsService 客户端详情服务
     * @return RegisteredClientRepository 实例
     */
    @Bean
    @ConditionalOnBean(AfgClientDetailsService.class)
    @ConditionalOnMissingBean(RegisteredClientRepository.class)
    public RegisteredClientRepository registeredClientRepository(AfgClientDetailsService clientDetailsService) {
        log.info("Initializing RegisteredClientRepository with AfgClientDetailsService");
        return new AfgClientDetailsRepositoryAdapter(clientDetailsService);
    }
}