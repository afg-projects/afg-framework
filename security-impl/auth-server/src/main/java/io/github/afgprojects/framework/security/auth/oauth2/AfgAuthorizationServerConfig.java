package io.github.afgprojects.framework.security.auth.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import java.util.UUID;

import io.github.afgprojects.framework.security.auth.config.AfgClientDetailsRepositoryAdapter;
import io.github.afgprojects.framework.security.auth.config.AuthServerProperties;
import io.github.afgprojects.framework.security.auth.user.AfgClientDetailsService;

/**
 * Spring Authorization Server 配置类
 *
 * <p>配置 OAuth2 授权服务器的核心组件：
 * <ul>
 *   <li>授权服务器安全过滤器链</li>
 *   <li>JWT 解码器</li>
 *   <li>客户端仓库</li>
 *   <li>授权服务器设置</li>
 * </ul>
 *
 * <p>支持以下 OAuth2 授权类型：
 * <ul>
 *   <li>Authorization Code - 授权码模式</li>
 *   <li>Authorization Code + PKCE - 移动端/SPA 安全增强</li>
 *   <li>Client Credentials - 客户端凭证</li>
 *   <li>Refresh Token - 刷新令牌</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@EnableWebSecurity
@ConditionalOnProperty(prefix = "afg.auth.server", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AfgAuthorizationServerConfig {

    /**
     * 授权服务器安全过滤器链
     *
     * <p>配置 OAuth2 授权端点的安全策略：
     * <ul>
     *   <li>/oauth2/authorize - 授权端点</li>
     *   <li>/oauth2/token - 令牌端点</li>
     *   <li>/oauth2/introspect - 令牌自省端点</li>
     *   <li>/oauth2/revoke - 令牌撤销端点</li>
     *   <li>/.well-known/oauth-authorization-server - 发现端点</li>
     * </ul>
     *
     * @param http HttpSecurity 配置
     * @return 安全过滤器链
     * @throws Exception 配置异常
     */
    @Bean
    @Order(1)
    @ConditionalOnMissingBean(name = "authorizationServerSecurityFilterChain")
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring OAuth2 Authorization Server security filter chain");

        http
            .securityMatcher(
                "/oauth2/authorize",
                "/oauth2/token",
                "/oauth2/introspect",
                "/oauth2/revoke",
                "/oauth2/jwks",
                "/.well-known/**",
                "/userinfo"
            )
            .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .exceptionHandling(exceptions ->
                exceptions.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")));

        return http.build();
    }

    /**
     * 默认安全过滤器链
     *
     * <p>配置非 OAuth2 端点的安全策略
     *
     * @param http HttpSecurity 配置
     * @return 安全过滤器链
     * @throws Exception 配置异常
     */
    @Bean
    @Order(2)
    @ConditionalOnMissingBean(name = "defaultSecurityFilterChain")
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring default security filter chain");

        http
            .authorizeHttpRequests(authorize ->
                authorize
                    .requestMatchers("/actuator/**").permitAll()
                    .anyRequest().authenticated())
            .formLogin(Customizer.withDefaults());

        return http.build();
    }

    /**
     * 注册客户端仓库
     *
     * <p>如果业务系统提供了 {@link AfgClientDetailsService} 实现，则使用适配器；
     * 否则创建一个默认的测试客户端。
     *
     * @param clientDetailsService 客户端详情服务（可选）
     * @param properties 授权服务器配置属性
     * @return 注册客户端仓库
     */
    @Bean
    @ConditionalOnMissingBean
    public RegisteredClientRepository registeredClientRepository(
            @NonNull AuthServerProperties properties,
            @org.springframework.lang.Nullable AfgClientDetailsService clientDetailsService) {

        if (clientDetailsService != null) {
            log.info("Using AfgClientDetailsService adapter for RegisteredClientRepository");
            return new AfgClientDetailsRepositoryAdapter(clientDetailsService);
        }

        log.info("Creating default RegisteredClientRepository with test client");
        RegisteredClient defaultClient = createDefaultClient(properties);
        return new InMemoryRegisteredClientRepository(defaultClient);
    }

    /**
     * 授权服务器设置
     *
     * @param properties 授权服务器配置属性
     * @return 授权服务器设置
     */
    @Bean
    @ConditionalOnMissingBean
    public AuthorizationServerSettings authorizationServerSettings(@NonNull AuthServerProperties properties) {
        log.info("Configuring AuthorizationServerSettings with issuer: {}", properties.getIssuer());

        return AuthorizationServerSettings.builder()
                .issuer(properties.getIssuer())
                .authorizationEndpoint("/oauth2/authorize")
                .tokenEndpoint("/oauth2/token")
                .tokenIntrospectionEndpoint("/oauth2/introspect")
                .tokenRevocationEndpoint("/oauth2/revoke")
                .jwkSetEndpoint("/oauth2/jwks")
                .oidcUserInfoEndpoint("/userinfo")
                .build();
    }

    /**
     * 创建默认测试客户端
     *
     * @param properties 授权服务器配置属性
     * @return 注册客户端
     */
    private RegisteredClient createDefaultClient(AuthServerProperties properties) {
        TokenSettings tokenSettings = TokenSettings.builder()
                .accessTokenTimeToLive(properties.getAccessTokenTtl())
                .refreshTokenTimeToLive(properties.getRefreshTokenTtl())
                .build();

        ClientSettings clientSettings = ClientSettings.builder()
                .requireProofKey(properties.isRequirePkce())
                .requireAuthorizationConsent(true)
                .build();

        return RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("afg-client")
                .clientSecret("{noop}afg-secret") // 仅用于测试，生产环境应使用 BCrypt
                .clientName("AFG Default Client")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .redirectUri("http://localhost:8080/login/oauth2/code/afg-client")
                .redirectUri("http://localhost:8080/authorized")
                .scope(OidcScopes.OPENID)
                .scope("read")
                .scope("write")
                .tokenSettings(tokenSettings)
                .clientSettings(clientSettings)
                .build();
    }
}
