package io.github.afgprojects.framework.security.auth.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * 安全配置类
 *
 * <p>配置登录相关接口的安全策略：
 * <ul>
 *   <li>放行登录、登出、刷新令牌接口</li>
 *   <li>放行验证码相关接口</li>
 *   <li>其他接口需要认证</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "afg.auth.server", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AfgAuthorizationServerConfig {

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
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring default security filter chain");

        http
            .securityMatcher("/**")
            .authorizeHttpRequests(authorize ->
                authorize
                    // 登录相关接口放行 (auth-server 模块 context-path: /auth-api)
                    .requestMatchers("/auth-api/auth/login", "/auth-api/auth/logout", "/auth-api/auth/refresh").permitAll()
                    .requestMatchers("/auth-api/auth/captcha", "/auth-api/auth/captcha/sms", "/auth-api/auth/captcha/email").permitAll()
                    // H2 控制台
                    .requestMatchers("/h2-console/**").permitAll()
                    // Actuator
                    .requestMatchers("/actuator/**").permitAll()
                    .anyRequest().authenticated())
            // 配置未认证时返回 401 而不是重定向到登录页
            .exceptionHandling(exceptions ->
                exceptions.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            // 禁用表单登录相关过滤器
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .logout(logout -> logout.disable())
            .csrf(csrf -> csrf.ignoringRequestMatchers("/auth-api/auth/**", "/h2-console/**"))
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }
}