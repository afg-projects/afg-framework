package io.github.afgprojects.framework.security.auth.config;

import io.github.afgprojects.framework.security.auth.login.DefaultLoginService;
import io.github.afgprojects.framework.security.auth.login.strategy.EmailCaptchaLoginStrategy;
import io.github.afgprojects.framework.security.auth.login.strategy.MobileCaptchaLoginStrategy;
import io.github.afgprojects.framework.security.auth.login.strategy.UsernamePasswordLoginStrategy;
import io.github.afgprojects.framework.security.core.authentication.AfgUserDetailsService;
import io.github.afgprojects.framework.security.core.login.CaptchaService;
import io.github.afgprojects.framework.security.core.login.LoginService;
import io.github.afgprojects.framework.security.core.login.TokenService;
import io.github.afgprojects.framework.security.core.login.strategy.LoginStrategy;
import io.github.afgprojects.framework.security.core.login.strategy.LoginStrategyFactory;
import io.github.afgprojects.framework.security.core.storage.AfgCaptchaStorage;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

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
@AutoConfiguration
@EnableConfigurationProperties({AuthServerProperties.class, LoginProperties.class})
@ConditionalOnProperty(prefix = "afg.auth.login", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LoginAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(LoginAutoConfiguration.class);

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
    @ConditionalOnBean({AfgUserDetailsService.class})
    public UsernamePasswordLoginStrategy usernamePasswordLoginStrategy(
            AfgUserDetailsService userDetailsService,
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder,
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
    @ConditionalOnBean({AfgUserDetailsService.class, CaptchaService.class})
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
    @ConditionalOnBean({AfgUserDetailsService.class, CaptchaService.class})
    public EmailCaptchaLoginStrategy emailCaptchaLoginStrategy(
            AfgUserDetailsService userDetailsService,
            CaptchaService captchaService) {
        log.info("Initializing EmailCaptchaLoginStrategy");
        return new EmailCaptchaLoginStrategy(userDetailsService, captchaService);
    }

    /**
     * 创建验证码服务
     *
     * @param loginProperties 登录配置属性
     * @param captchaStorage 验证码存储
     * @return CaptchaService 实例
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(AfgCaptchaStorage.class)
    public CaptchaService captchaService(LoginProperties loginProperties, AfgCaptchaStorage captchaStorage) {
        log.info("Initializing CaptchaService with captchaTtl={}", loginProperties.getCaptchaTtl());
        return new DefaultCaptchaService(loginProperties, captchaStorage);
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
    @ConditionalOnBean({LoginStrategyFactory.class, AfgUserDetailsService.class, TokenService.class, CaptchaService.class})
    public LoginService loginService(
            LoginStrategyFactory strategyFactory,
            AfgUserDetailsService userDetailsService,
            TokenService tokenService,
            CaptchaService captchaService) {
        log.info("Initializing DefaultLoginService with {} strategies", strategyFactory.getRegisteredTypes().size());
        return new DefaultLoginService(strategyFactory, userDetailsService, tokenService, captchaService);
    }

    /**
     * 默认验证码服务实现
     */
    private static class DefaultCaptchaService implements CaptchaService {

        private final LoginProperties loginProperties;
        private final AfgCaptchaStorage captchaStorage;

        DefaultCaptchaService(LoginProperties loginProperties, AfgCaptchaStorage captchaStorage) {
            this.loginProperties = loginProperties;
            this.captchaStorage = captchaStorage;
        }

        @Override
        public io.github.afgprojects.framework.security.core.login.model.CaptchaResponse generate(
                io.github.afgprojects.framework.security.core.login.model.CaptchaRequest request) {
            // TODO: Implement captcha generation
            throw new UnsupportedOperationException("CaptchaService implementation pending");
        }

        @Override
        public boolean validate(String captchaKey, String captchaValue) {
            // TODO: Implement captcha validation
            throw new UnsupportedOperationException("CaptchaService implementation pending");
        }

        @Override
        public void delete(String captchaKey) {
            captchaStorage.delete(captchaKey);
        }
    }
}