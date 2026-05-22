package io.github.afgprojects.framework.security.auth.autoconfigure;

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
 * 认证服务器安全自动配置。
 *
 * <p>配置多个 SecurityFilterChain，按优先级顺序匹配不同端点：
 * <ul>
 *   <li>OAuth2 端点 (/auth-api/oauth2/**) - 令牌端点公开，授权端点需认证</li>
 *   <li>内部服务端点 (/auth-api/internal/**) - 用于资源服务器远程调用</li>
 *   <li>认证 API 端点 (/auth-api/auth/**) - 登录相关接口</li>
 * </ul>
 *
 * <p>注意：认证服务器模块 context-path 为 /auth-api，所有路径都带此前缀。
 *
 * <p>参考 Spring Authorization Server 的多链配置模式。
 *
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "afg.security.auth-server", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AuthorizationServerAutoConfiguration {

    @Bean
    @Order(1)
    public SecurityFilterChain oauth2SecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring OAuth2 security filter chain");

        http
            .securityMatcher("/auth-api/oauth2/**")
            .authorizeHttpRequests(authorize ->
                authorize
                    .requestMatchers("/auth-api/oauth2/token", "/auth-api/oauth2/introspect", "/auth-api/oauth2/revoke").permitAll()
                    .requestMatchers("/auth-api/oauth2/authorize").authenticated()
                    .anyRequest().authenticated())
            .exceptionHandling(exceptions ->
                exceptions.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .csrf(csrf -> csrf.ignoringRequestMatchers("/auth-api/oauth2/**"))
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
            .csrf(csrf -> csrf.ignoringRequestMatchers("/auth-api/internal/**"))
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));

        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain authApiSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring auth API security filter chain");

        http
            .securityMatcher("/auth-api/auth/**")
            .authorizeHttpRequests(authorize ->
                authorize
                    .requestMatchers("/auth-api/auth/login", "/auth-api/auth/refresh").permitAll()
                    .requestMatchers("/auth-api/auth/captcha/**").permitAll()
                    .requestMatchers("/auth-api/auth/logout").authenticated()
                    .anyRequest().authenticated())
            .exceptionHandling(exceptions ->
                exceptions.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .csrf(csrf -> csrf.ignoringRequestMatchers("/auth-api/auth/**"))
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));

        return http.build();
    }
}