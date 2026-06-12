package io.github.afgprojects.framework.security.resource.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 默认资源服务器安全自动配置。
 *
 * <p>提供默认的 SecurityFilterChain，放行所有接口。
 * 如果应用需要自定义安全规则，可以定义自己的 SecurityFilterChain Bean 覆盖此配置。
 *
 * <p>注意：当同时引入 auth-server 模块时，此配置会被自动禁用，
 * 因为 auth-server 会定义自己的 SecurityFilterChain。
 *
 * @since 1.0.0
 */
@AutoConfiguration(after = ResourceServerAutoConfiguration.class)
@ConditionalOnClass(SecurityFilterChain.class)
@ConditionalOnProperty(prefix = "afg.security.resource-server", name = "default-security", havingValue = "true", matchIfMissing = true)
@EnableWebSecurity
public class DefaultSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .anyRequest().permitAll()
            )
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }
}