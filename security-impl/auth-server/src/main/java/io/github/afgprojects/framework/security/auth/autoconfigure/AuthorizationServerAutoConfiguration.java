package io.github.afgprojects.framework.security.auth.autoconfigure;

import io.github.afgprojects.framework.security.auth.oauth2.controller.OAuth2Controller;
import io.github.afgprojects.framework.security.auth.oauth2.controller.OAuth2ExceptionControllerAdvice;
import io.github.afgprojects.framework.security.auth.token.AuthServerBearerTokenFilter;
import io.github.afgprojects.framework.security.auth.token.AuthServerBearerTokenResolver;
import io.github.afgprojects.framework.security.core.oauth2.OAuth2AuthorizationService;
import io.github.afgprojects.framework.security.core.oauth2.OAuth2ClientService;
import io.github.afgprojects.framework.security.core.login.TokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

import org.springframework.security.config.Customizer;

/**
 * 认证服务器安全自动配置。
 *
 * <p>配置多个 SecurityFilterChain，按优先级顺序匹配不同端点：
 * <ul>
 *   <li>OAuth2 端点 (/auth-api/oauth2/**) - 令牌端点公开，授权端点需认证</li>
 *   <li>内部服务端点 (/auth-api/internal/**) - 用于资源服务器远程调用</li>
 *   <li>认证 API 端点 (/auth-api/auth/**) - 登录相关接口</li>
 * </ul>
 *
 * <p>需要认证的端点通过 {@link AuthServerBearerTokenFilter} 自动解析 Bearer Token
 * 并填充 SecurityContext，无需依赖 resource-server 模块。
 *
 * <p>注意：认证服务器模块 context-path 为 /auth-api，所有路径都带此前缀。
 * 所有 SecurityFilterChain 均禁用 CSRF，因为这是纯 API 服务（无服务端渲染页面）。
 *
 * <p>参考 Spring Authorization Server 的多链配置模式。
 *
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration(after = LoginAutoConfiguration.class, afterName = "io.github.afgprojects.framework.core.autoconfigure.AfgAutoConfiguration")
@ConditionalOnProperty(prefix = "afg.security.auth-server", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AuthorizationServerAutoConfiguration {

    /**
     * 注册 AuthServerBearerTokenResolver Bean。
     */
    @Bean
    @ConditionalOnMissingBean
    public AuthServerBearerTokenResolver authServerBearerTokenResolver(TokenService tokenService) {
        log.info("Configuring AuthServerBearerTokenResolver");
        return new AuthServerBearerTokenResolver(tokenService);
    }

    /**
     * 注册 AuthServerBearerTokenFilter Bean。
     */
    @Bean
    @ConditionalOnMissingBean
    public AuthServerBearerTokenFilter authServerBearerTokenFilter(AuthServerBearerTokenResolver tokenResolver) {
        log.info("Configuring AuthServerBearerTokenFilter");
        return new AuthServerBearerTokenFilter(tokenResolver);
    }

    /**
     * 注册 OAuth2Controller Bean。
     */
    @Bean
    @ConditionalOnMissingBean
    public OAuth2Controller oAuth2Controller(
            OAuth2AuthorizationService authorizationService,
            OAuth2ClientService clientService) {
        log.info("Configuring OAuth2 controller");
        return new OAuth2Controller(authorizationService, clientService);
    }

    /**
     * 注册 OAuth2 异常处理器 Bean。
     */
    @Bean
    @ConditionalOnMissingBean
    public OAuth2ExceptionControllerAdvice oAuth2ExceptionControllerAdvice() {
        log.info("Configuring OAuth2 exception handler");
        return new OAuth2ExceptionControllerAdvice();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain oauth2SecurityFilterChain(
            HttpSecurity http,
            AuthServerBearerTokenFilter bearerTokenFilter) throws Exception {
        log.info("Configuring OAuth2 security filter chain");

        http
            .securityMatcher("/auth-api/oauth2/**")
            .addFilterBefore(bearerTokenFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(authorize ->
                authorize
                    .requestMatchers("/auth-api/oauth2/token", "/auth-api/oauth2/introspect", "/auth-api/oauth2/revoke").permitAll()
                    .requestMatchers("/auth-api/oauth2/authorize").authenticated()
                    .anyRequest().authenticated())
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .exceptionHandling(exceptions ->
                exceptions.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain internalSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring internal service security filter chain (signature-based auth)");

        http
            .securityMatcher("/auth-api/internal/**")
            .authorizeHttpRequests(authorize ->
                authorize.anyRequest().permitAll())
            .csrf(AbstractHttpConfigurer::disable)
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));

        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain authApiSecurityFilterChain(
            HttpSecurity http,
            AuthServerBearerTokenFilter bearerTokenFilter) throws Exception {
        log.info("Configuring auth API security filter chain");

        http
            .securityMatcher("/auth-api/auth/**")
            .addFilterBefore(bearerTokenFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(authorize ->
                authorize
                    .requestMatchers("/auth-api/auth/login", "/auth-api/auth/refresh", "/auth-api/auth/session").permitAll()
                    .requestMatchers("/auth-api/auth/captcha/**").permitAll()
                    .requestMatchers("/auth-api/auth/logout").authenticated()
                    .requestMatchers("/auth-api/auth/user-info").authenticated()
                    .anyRequest().authenticated())
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .exceptionHandling(exceptions ->
                exceptions.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));

        return http.build();
    }

    /**
     * 权限管理端点安全配置。
     *
     * <p>覆盖 /auth-api/ 下除 auth 和 oauth2 之外的端点（如角色管理、权限管理等），
     * 这些端点需要 Bearer Token 认证。
     *
     * @since 1.0.0
     */
    @Bean
    @Order(4)
    public SecurityFilterChain authMgmtSecurityFilterChain(
            HttpSecurity http,
            AuthServerBearerTokenFilter bearerTokenFilter) throws Exception {
        log.info("Configuring auth management security filter chain");

        http
            .securityMatcher("/auth-api/**")
            .addFilterBefore(bearerTokenFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(authorize ->
                authorize.anyRequest().authenticated())
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .exceptionHandling(exceptions ->
                exceptions.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));

        return http.build();
    }
}
