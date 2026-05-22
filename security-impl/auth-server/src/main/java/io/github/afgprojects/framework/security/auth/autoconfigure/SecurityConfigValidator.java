package io.github.afgprojects.framework.security.auth.autoconfigure;

import lombok.extern.slf4j.Slf4j;
import io.github.afgprojects.framework.security.core.authentication.AfgUserDetailsService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * 认证服务器配置验证器。
 *
 * <p>在应用启动时验证必填配置项，提供清晰的错误提示。
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
        AuthSecurityProperties.TokenConfig tokenConfig = properties.getToken();
        if (tokenConfig.getSigningKey() == null || tokenConfig.getSigningKey().isBlank()) {
            throw new IllegalStateException(
                "认证服务器需要配置 afg.security.auth-server.token.signing-key\n" +
                "示例配置:\n" +
                "afg:\n" +
                "  security:\n" +
                "    auth-server:\n" +
                "      token:\n" +
                "        signing-key: your-secret-key-at-least-256-bits-long"
            );
        }

        if (tokenConfig.getSigningKey().length() < 256 / 8) {
            log.warn("签名密钥长度不足 256 位（32 字节），建议使用更长的密钥以提高安全性");
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

        log.info("Auth-server configuration validated successfully");
    }
}