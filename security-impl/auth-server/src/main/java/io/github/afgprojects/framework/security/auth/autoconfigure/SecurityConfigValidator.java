package io.github.afgprojects.framework.security.auth.autoconfigure;

import lombok.extern.slf4j.Slf4j;
import io.github.afgprojects.framework.security.auth.properties.AuthSecurityProperties;
import io.github.afgprojects.framework.security.auth.properties.token.TokenConfig;
import io.github.afgprojects.framework.security.core.authentication.AfgUserDetailsService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * 认证服务器配置验证器。
 *
 * <p>在应用启动时验证配置项，提供清晰的错误提示。
 *
 * @since 1.1.0
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "afg.security.auth-server", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SecurityConfigValidator implements ApplicationRunner {

    private final AuthSecurityProperties properties;
    private final ApplicationContext applicationContext;

    public SecurityConfigValidator(AuthSecurityProperties properties, ApplicationContext applicationContext) {
        this.properties = properties;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Validating auth-server security configuration...");

        // 验证 Token 配置
        TokenConfig tokenConfig = properties.getToken();
        if (tokenConfig.getIssuer() == null || tokenConfig.getIssuer().isBlank()) {
            throw new IllegalStateException(
                "认证服务器需要配置 afg.security.auth-server.token.issuer\n" +
                "示例配置:\n" +
                "afg:\n" +
                "  security:\n" +
                "    auth-server:\n" +
                "      token:\n" +
                "        issuer: https://auth.example.com"
            );
        }

        // 检查 AfgUserDetailsService 实现
        try {
            String[] beanNames = applicationContext.getBeanNamesForType(AfgUserDetailsService.class);
            if (beanNames.length == 0) {
                log.warn(
                    "未找到 AfgUserDetailsService 实现，请创建 UserDetailsServiceImpl。\n" +
                    "示例:\n" +
                    "@Service\n" +
                    "public class UserDetailsServiceImpl implements AfgUserDetailsService {\n" +
                    "    @Override\n" +
                    "    public AfgUserDetails loadUserByUsername(String username) {\n" +
                    "        // 实现用户加载逻辑\n" +
                    "    }\n" +
                    "}"
                );
            }
        } catch (Exception e) {
            log.debug("Could not check for AfgUserDetailsService bean", e);
        }

        log.info("Auth-server configuration validated successfully. JWT signing uses RS256 with auto-generated RSA key pair.");
    }
}