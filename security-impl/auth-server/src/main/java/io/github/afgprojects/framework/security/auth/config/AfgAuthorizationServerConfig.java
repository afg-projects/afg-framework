package io.github.afgprojects.framework.security.auth.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * 认证服务器安全配置。
 *
 * <p>配置多个 SecurityFilterChain，按优先级顺序匹配不同端点：
 * <ul>
 *   <li>OAuth2 端点 (/oauth2/**) - 令牌端点公开，授权端点需认证</li>
 *   <li>内部服务端点 (/internal/**) - 用于资源服务器远程调用</li>
 *   <li>认证 API 端点 (/auth-api/**) - 登录相关接口</li>
 * </ul>
 *
 * <p>参考 Spring Authorization Server 的多链配置模式。
 *
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "afg.auth.server", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AfgAuthorizationServerConfig {

    /**
     * OAuth2 端点安全过滤器链。
     *
     * <p>配置 OAuth2 协议端点的安全策略：
     * <ul>
     *   <li>/oauth2/token - 令牌端点，公开访问（客户端认证通过 Basic Auth）</li>
     *   <li>/oauth2/introspect - 令牌自省端点，公开访问（客户端认证）</li>
     *   <li>/oauth2/revoke - 令牌撤销端点，公开访问（客户端认证）</li>
     *   <li>/oauth2/authorize - 授权端点，需要用户认证</li>
     * </ul>
     *
     * @param http HttpSecurity 配置
     * @return OAuth2 安全过滤器链
     */
    @Bean
    @Order(1)
    public SecurityFilterChain oauth2SecurityFilterChain(HttpSecurity http) {
        log.info("Configuring OAuth2 security filter chain");

        http
            .securityMatcher("/oauth2/**")
            .authorizeHttpRequests(authorize ->
                authorize
                    // 令牌端点公开（客户端通过 Basic Auth 认证）
                    .requestMatchers("/oauth2/token", "/oauth2/introspect", "/oauth2/revoke").permitAll()
                    // 授权端点需要用户认证
                    .requestMatchers("/oauth2/authorize").authenticated()
                    .anyRequest().authenticated())
            .exceptionHandling(exceptions ->
                exceptions.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .csrf(csrf -> csrf.ignoringRequestMatchers("/oauth2/**"))
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));

        return http.build();
    }

    /**
     * 内部服务端点安全过滤器链。
     *
     * <p>配置供资源服务器远程调用的内部端点：
     * <ul>
     *   <li>/internal/permissions/** - 权限查询接口</li>
     * </ul>
     *
     * <p>这些端点通常通过服务间认证（如 API Key、内部网络隔离）保护。
     *
     * @param http HttpSecurity 配置
     * @return 内部服务安全过滤器链
     */
    @Bean
    @Order(2)
    public SecurityFilterChain internalSecurityFilterChain(HttpSecurity http) {
        log.info("Configuring internal service security filter chain");

        http
            .securityMatcher("/internal/**")
            .authorizeHttpRequests(authorize ->
                authorize
                    // 内部服务端点暂时公开（后续可添加服务间认证）
                    .anyRequest().permitAll())
            .csrf(csrf -> csrf.ignoringRequestMatchers("/internal/**"))
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));

        return http.build();
    }

    /**
     * 认证 API 端点安全过滤器链。
     *
     * <p>配置认证相关 API 的安全策略：
     * <ul>
     *   <li>/auth-api/auth/login - 登录接口，公开</li>
     *   <li>/auth-api/auth/logout - 登出接口，需认证</li>
     *   <li>/auth-api/auth/refresh - 刷新令牌，公开</li>
     *   <li>/auth-api/auth/captcha/** - 验证码接口，公开</li>
     * </ul>
     *
     * @param http HttpSecurity 配置
     * @return 认证 API 安全过滤器链
     */
    @Bean
    @Order(3)
    public SecurityFilterChain authApiSecurityFilterChain(HttpSecurity http) {
        log.info("Configuring auth API security filter chain");

        http
            .securityMatcher("/auth-api/**")
            .authorizeHttpRequests(authorize ->
                authorize
                    // 登录相关接口放行
                    .requestMatchers("/auth-api/auth/login", "/auth-api/auth/refresh").permitAll()
                    .requestMatchers("/auth-api/auth/captcha", "/auth-api/auth/captcha/**").permitAll()
                    // 登出需要认证
                    .requestMatchers("/auth-api/auth/logout").authenticated()
                    // 其他接口需要认证
                    .anyRequest().authenticated())
            .exceptionHandling(exceptions ->
                exceptions.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .csrf(csrf -> csrf.ignoringRequestMatchers("/auth-api/**"))
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));

        return http.build();
    }
}