package io.github.afgprojects.framework.security.auth.autoconfigure;

import io.github.afgprojects.framework.security.auth.totp.DefaultTotpService;
import io.github.afgprojects.framework.security.auth.totp.TotpController;
import io.github.afgprojects.framework.security.auth.totp.TwoFactorAuthenticationService;
import io.github.afgprojects.framework.security.auth.properties.AuthSecurityProperties;
import io.github.afgprojects.framework.security.core.totp.NoOpTotpService;
import io.github.afgprojects.framework.security.core.totp.TotpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * TOTP 双因素认证自动配置。
 *
 * <p>当 afg.security.auth-server.totp.enabled=true 时注册以下组件：
 * <ul>
 *   <li>{@link TotpService} - TOTP 服务（验证码生成和验证）</li>
 *   <li>{@link TwoFactorAuthenticationService} - 2FA 服务（启用/禁用/恢复码）</li>
 *   <li>{@link TotpController} - TOTP REST 控制器</li>
 * </ul>
 *
 * <p>当 afg.security.auth-server.totp.enabled=false 时注册 {@link NoOpTotpService} 作为降级实现。
 *
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration(after = LoginAutoConfiguration.class)
@EnableConfigurationProperties(AuthSecurityProperties.class)
@ConditionalOnProperty(prefix = "afg.security.auth-server", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TotpAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "afg.security.auth-server.totp", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(TotpService.class)
    public DefaultTotpService totpService() {
        log.info("Initializing DefaultTotpService");
        return new DefaultTotpService();
    }

    @Bean
    @ConditionalOnProperty(prefix = "afg.security.auth-server.totp", name = "enabled", havingValue = "false", matchIfMissing = true)
    @ConditionalOnMissingBean(TotpService.class)
    public NoOpTotpService noOpTotpService() {
        log.info("Initializing NoOpTotpService (TOTP disabled)");
        return new NoOpTotpService();
    }

    @Bean
    @ConditionalOnProperty(prefix = "afg.security.auth-server.totp", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean
    public TwoFactorAuthenticationService twoFactorAuthenticationService(TotpService totpService) {
        log.info("Initializing TwoFactorAuthenticationService");
        return new TwoFactorAuthenticationService(totpService);
    }

    @Bean
    @ConditionalOnProperty(prefix = "afg.security.auth-server.totp", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean
    public TotpController totpController(TwoFactorAuthenticationService twoFactorAuthService) {
        log.info("Initializing TotpController");
        return new TotpController(twoFactorAuthService);
    }
}
