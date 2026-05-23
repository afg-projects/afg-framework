package io.github.afgprojects.framework.security.auth.autoconfigure;

import lombok.extern.slf4j.Slf4j;
import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.jdbc.autoconfigure.DataManagerAutoConfiguration;
import io.github.afgprojects.framework.security.auth.captcha.DefaultCaptchaService;
import io.github.afgprojects.framework.security.auth.key.JwksController;
import io.github.afgprojects.framework.security.auth.key.RsaKeyPairManager;
import io.github.afgprojects.framework.security.auth.login.DefaultLoginService;
import io.github.afgprojects.framework.security.auth.login.strategy.EmailCaptchaLoginStrategy;
import io.github.afgprojects.framework.security.auth.login.strategy.MobileCaptchaLoginStrategy;
import io.github.afgprojects.framework.security.auth.login.strategy.UsernamePasswordLoginStrategy;
import io.github.afgprojects.framework.security.auth.storage.JdbcCaptchaStorage;
import io.github.afgprojects.framework.security.auth.storage.JdbcRefreshTokenStorage;
import io.github.afgprojects.framework.security.auth.storage.JdbcTokenBlacklist;
import io.github.afgprojects.framework.security.auth.token.DefaultTokenService;
import io.github.afgprojects.framework.security.core.authentication.AfgUserDetailsService;
import io.github.afgprojects.framework.security.core.login.CaptchaService;
import io.github.afgprojects.framework.security.core.login.LoginService;
import io.github.afgprojects.framework.security.core.login.TokenService;
import io.github.afgprojects.framework.security.core.login.strategy.LoginStrategy;
import io.github.afgprojects.framework.security.core.login.strategy.LoginStrategyFactory;
import io.github.afgprojects.framework.security.core.storage.AfgCaptchaStorage;
import io.github.afgprojects.framework.security.core.storage.AfgRefreshTokenStorage;
import io.github.afgprojects.framework.security.core.storage.AfgTokenBlacklist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

/**
 * 登录服务自动配置类
 *
 * <p>配置登录服务的核心组件：
 * <ul>
 *   <li>{@link LoginStrategyFactory} - 登录策略工厂</li>
 *   <li>{@link TokenService} - 令牌服务</li>
 *   <li>{@link CaptchaService} - 验证码服务</li>
 *   <li>{@link LoginService} - 登录服务</li>
 *   <li>内置登录策略 - 用户名密码、手机号、邮箱</li>
 * </ul>
 *
 * <p>支持通过实现 {@link LoginStrategy} 接口扩展自定义登录方式。
 *
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration(after = DataManagerAutoConfiguration.class)
@EnableConfigurationProperties(AuthSecurityProperties.class)
@ConditionalOnProperty(prefix = "afg.security.auth-server", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LoginAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(PasswordEncoder.class)
    public PasswordEncoder passwordEncoder() {
        log.info("Initializing BCryptPasswordEncoder");
        return new BCryptPasswordEncoder();
    }

    @Bean
    @ConditionalOnMissingBean(AfgCaptchaStorage.class)
    public AfgCaptchaStorage captchaStorage(DataManager dataManager) {
        log.info("Initializing JdbcCaptchaStorage");
        return new JdbcCaptchaStorage(dataManager);
    }

    @Bean
    @ConditionalOnMissingBean(AfgRefreshTokenStorage.class)
    public AfgRefreshTokenStorage refreshTokenStorage(DataManager dataManager) {
        log.info("Initializing JdbcRefreshTokenStorage");
        return new JdbcRefreshTokenStorage(dataManager);
    }

    @Bean
    @ConditionalOnMissingBean(AfgTokenBlacklist.class)
    public AfgTokenBlacklist tokenBlacklist(DataManager dataManager) {
        log.info("Initializing JdbcTokenBlacklist");
        return new JdbcTokenBlacklist(dataManager);
    }

    @Bean
    @ConditionalOnMissingBean(RsaKeyPairManager.class)
    public RsaKeyPairManager rsaKeyPairManager(AuthSecurityProperties properties) {
        String keyStorePath = properties.getToken().getKeyStorePath();
        log.info("Initializing RsaKeyPairManager with keyStorePath: {}", keyStorePath);
        return new RsaKeyPairManager(keyStorePath);
    }

    @Bean
    @ConditionalOnMissingBean(TokenService.class)
    public TokenService tokenService(
            RsaKeyPairManager keyPairManager,
            AuthSecurityProperties properties,
            AfgRefreshTokenStorage refreshTokenStorage,
            AfgTokenBlacklist tokenBlacklist) {
        log.info("Initializing DefaultTokenService with RS256");
        AuthSecurityProperties.TokenConfig tokenConfig = properties.getToken();
        return new DefaultTokenService(
                keyPairManager,
                tokenConfig.getIssuer(),
                tokenConfig.getAccessTokenTtl(),
                tokenConfig.getRefreshTokenTtl(),
                refreshTokenStorage,
                tokenBlacklist);
    }

    @Bean
    @ConditionalOnMissingBean(JwksController.class)
    public JwksController jwksController(RsaKeyPairManager keyPairManager) {
        log.info("Initializing JwksController");
        return new JwksController(keyPairManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public LoginStrategyFactory loginStrategyFactory(@Autowired(required = false) List<LoginStrategy> strategies) {
        LoginStrategyFactory factory = new LoginStrategyFactory();
        if (strategies != null) {
            factory.registerAll(strategies);
            log.info("Registered {} login strategies: {}", strategies.size(), factory.getRegisteredTypes());
        }
        return factory;
    }

    @Bean
    @ConditionalOnMissingBean(name = "usernamePasswordLoginStrategy")
    public UsernamePasswordLoginStrategy usernamePasswordLoginStrategy(
            AfgUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder,
            @Autowired(required = false) CaptchaService captchaService) {
        log.info("Initializing UsernamePasswordLoginStrategy");
        return new UsernamePasswordLoginStrategy(userDetailsService, passwordEncoder, captchaService);
    }

    @Bean
    @ConditionalOnMissingBean(name = "mobileCaptchaLoginStrategy")
    public MobileCaptchaLoginStrategy mobileCaptchaLoginStrategy(
            AfgUserDetailsService userDetailsService,
            CaptchaService captchaService) {
        log.info("Initializing MobileCaptchaLoginStrategy");
        return new MobileCaptchaLoginStrategy(userDetailsService, captchaService);
    }

    @Bean
    @ConditionalOnMissingBean(name = "emailCaptchaLoginStrategy")
    public EmailCaptchaLoginStrategy emailCaptchaLoginStrategy(
            AfgUserDetailsService userDetailsService,
            CaptchaService captchaService) {
        log.info("Initializing EmailCaptchaLoginStrategy");
        return new EmailCaptchaLoginStrategy(userDetailsService, captchaService);
    }

    @Bean
    @ConditionalOnMissingBean
    public CaptchaService captchaService(AfgCaptchaStorage captchaStorage) {
        log.info("Initializing DefaultCaptchaService");
        return new DefaultCaptchaService(captchaStorage);
    }

    @Bean
    @ConditionalOnMissingBean
    public LoginService loginService(
            LoginStrategyFactory strategyFactory,
            AfgUserDetailsService userDetailsService,
            TokenService tokenService,
            CaptchaService captchaService) {
        log.info("Initializing DefaultLoginService with {} strategies", strategyFactory.getRegisteredTypes().size());
        return new DefaultLoginService(strategyFactory, userDetailsService, tokenService, captchaService);
    }
}