package io.github.afgprojects.framework.security.auth.config;

import lombok.extern.slf4j.Slf4j;
import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.jdbc.autoconfigure.DataManagerAutoConfiguration;
import io.github.afgprojects.framework.security.auth.captcha.DefaultCaptchaService;
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
import org.jspecify.annotations.NonNull;
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
@EnableConfigurationProperties({AuthServerProperties.class, LoginProperties.class})
@ConditionalOnProperty(prefix = "afg.auth.login", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LoginAutoConfiguration {

    /**
     * 创建密码编码器。
     *
     * @return BCryptPasswordEncoder 实例
     */
    @Bean
    @ConditionalOnMissingBean(PasswordEncoder.class)
    public PasswordEncoder passwordEncoder() {
        log.info("Initializing BCryptPasswordEncoder");
        return new BCryptPasswordEncoder();
    }

    /**
     * 创建验证码存储。
     *
     * @param dataManager 数据管理器
     * @return JdbcCaptchaStorage 实例
     */
    @Bean
    @ConditionalOnMissingBean(AfgCaptchaStorage.class)
    public AfgCaptchaStorage captchaStorage(DataManager dataManager) {
        log.info("Initializing JdbcCaptchaStorage");
        return new JdbcCaptchaStorage(dataManager);
    }

    /**
     * 创建 Refresh Token 存储。
     *
     * @param dataManager 数据管理器
     * @return JdbcRefreshTokenStorage 实例
     */
    @Bean
    @ConditionalOnMissingBean(AfgRefreshTokenStorage.class)
    public AfgRefreshTokenStorage refreshTokenStorage(DataManager dataManager) {
        log.info("Initializing JdbcRefreshTokenStorage");
        return new JdbcRefreshTokenStorage(dataManager);
    }

    /**
     * 创建 Token 黑名单。
     *
     * @param dataManager 数据管理器
     * @return JdbcTokenBlacklist 实例
     */
    @Bean
    @ConditionalOnMissingBean(AfgTokenBlacklist.class)
    public AfgTokenBlacklist tokenBlacklist(DataManager dataManager) {
        log.info("Initializing JdbcTokenBlacklist");
        return new JdbcTokenBlacklist(dataManager);
    }

    /**
     * 创建令牌服务。
     *
     * @param loginProperties 登录配置属性
     * @param refreshTokenStorage Refresh Token 存储
     * @param tokenBlacklist Token 黑名单
     * @return DefaultTokenService 实例
     */
    @Bean
    @ConditionalOnMissingBean(TokenService.class)
    public TokenService tokenService(
            LoginProperties loginProperties,
            AfgRefreshTokenStorage refreshTokenStorage,
            AfgTokenBlacklist tokenBlacklist) {
        log.info("Initializing DefaultTokenService");
        return new DefaultTokenService(
                loginProperties.getSigningKey(),
                loginProperties.getIssuer(),
                loginProperties.getAccessTokenTtl(),
                loginProperties.getRefreshTokenTtl(),
                refreshTokenStorage,
                tokenBlacklist);
    }

    /**
     * 创建登录策略工厂。
     *
     * <p>自动注入所有 {@link LoginStrategy} 实现。
     *
     * @param strategies 登录策略列表（自动注入）
     * @return LoginStrategyFactory 实例
     */
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

    /**
     * 创建用户名密码登录策略。
     *
     * @param userDetailsService 用户详情服务
     * @param passwordEncoder 密码编码器
     * @param captchaService 验证码服务（可选）
     * @return UsernamePasswordLoginStrategy 实例
     */
    @Bean
    @ConditionalOnMissingBean(name = "usernamePasswordLoginStrategy")
    public UsernamePasswordLoginStrategy usernamePasswordLoginStrategy(
            AfgUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder,
            @Autowired(required = false) CaptchaService captchaService) {
        log.info("Initializing UsernamePasswordLoginStrategy");
        return new UsernamePasswordLoginStrategy(userDetailsService, passwordEncoder, captchaService);
    }

    /**
     * 创建手机号验证码登录策略。
     *
     * @param userDetailsService 用户详情服务
     * @param captchaService 验证码服务
     * @return MobileCaptchaLoginStrategy 实例
     */
    @Bean
    @ConditionalOnMissingBean(name = "mobileCaptchaLoginStrategy")
    public MobileCaptchaLoginStrategy mobileCaptchaLoginStrategy(
            AfgUserDetailsService userDetailsService,
            CaptchaService captchaService) {
        log.info("Initializing MobileCaptchaLoginStrategy");
        return new MobileCaptchaLoginStrategy(userDetailsService, captchaService);
    }

    /**
     * 创建邮箱验证码登录策略。
     *
     * @param userDetailsService 用户详情服务
     * @param captchaService 验证码服务
     * @return EmailCaptchaLoginStrategy 实例
     */
    @Bean
    @ConditionalOnMissingBean(name = "emailCaptchaLoginStrategy")
    public EmailCaptchaLoginStrategy emailCaptchaLoginStrategy(
            AfgUserDetailsService userDetailsService,
            CaptchaService captchaService) {
        log.info("Initializing EmailCaptchaLoginStrategy");
        return new EmailCaptchaLoginStrategy(userDetailsService, captchaService);
    }

    /**
     * 创建验证码服务
     *
     * @param captchaStorage 验证码存储
     * @return CaptchaService 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public CaptchaService captchaService(AfgCaptchaStorage captchaStorage) {
        log.info("Initializing DefaultCaptchaService");
        return new DefaultCaptchaService(captchaStorage);
    }

    /**
     * 创建登录服务。
     *
     * @param strategyFactory 登录策略工厂
     * @param userDetailsService 用户详情服务
     * @param tokenService 令牌服务
     * @param captchaService 验证码服务
     * @return LoginService 实例
     */
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