package io.github.afgprojects.framework.security.auth.autoconfigure;

import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import io.github.afgprojects.framework.security.auth.security.DefaultDeviceLimiter;
import io.github.afgprojects.framework.security.auth.security.DefaultIpRestrictionChecker;
import io.github.afgprojects.framework.security.auth.security.DefaultLoginFailureTracker;
import io.github.afgprojects.framework.security.auth.security.DefaultPasswordValidator;
import io.github.afgprojects.framework.security.core.security.DeviceLimiter;
import io.github.afgprojects.framework.security.core.security.IpRestrictionChecker;
import io.github.afgprojects.framework.security.core.security.LoginFailureTracker;
import io.github.afgprojects.framework.security.core.security.PasswordValidator;
import io.github.afgprojects.framework.security.core.security.model.IpRestrictionRule;
import io.github.afgprojects.framework.security.core.storage.AfgDeviceStorage;

/**
 * 安全策略自动配置类。
 *
 * <p>自动配置以下安全策略组件：
 * <ul>
 *   <li>{@link LoginFailureTracker} - 登录失败追踪器</li>
 *   <li>{@link PasswordValidator} - 密码验证器</li>
 *   <li>{@link IpRestrictionChecker} - IP 限制检查器</li>
 *   <li>{@link DeviceLimiter} - 设备限制器</li>
 * </ul>
 *
 * <p>配置示例：
 * <pre>
 * afg:
 *   security:
 *     auth-server:
 *       security:
 *         enabled: true
 *         max-login-failures: 5
 *         lock-duration: 30m
 *         max-devices: 5
 *         password-policy:
 *           min-length: 8
 *           require-uppercase: true
 *           require-lowercase: true
 *           require-digit: true
 *           require-special-char: true
 *         ip-whitelist:
 *           - 192.168.1.*
 *         ip-blacklist:
 *           - 192.168.100.*
 * </pre>
 *
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(AuthSecurityProperties.class)
@ConditionalOnProperty(prefix = "afg.security.auth-server.security", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SecurityStrategyAutoConfiguration {

    /**
     * 创建登录失败追踪器。
     *
     * @param properties 认证服务器配置属性
     * @return DefaultLoginFailureTracker 实例
     */
    @Bean
    @ConditionalOnMissingBean(LoginFailureTracker.class)
    public DefaultLoginFailureTracker loginFailureTracker(AuthSecurityProperties properties) {
        AuthSecurityProperties.SecurityConfig securityConfig = properties.getSecurity();
        int maxFailures = securityConfig.getMaxLoginFailures();
        var lockDuration = securityConfig.getLockDuration();

        log.info("Initializing DefaultLoginFailureTracker with maxFailures={}, lockDuration={}",
                maxFailures, lockDuration);

        return new DefaultLoginFailureTracker(maxFailures, lockDuration);
    }

    /**
     * 创建密码验证器。
     *
     * @param properties 认证服务器配置属性
     * @return DefaultPasswordValidator 实例
     */
    @Bean
    @ConditionalOnMissingBean(PasswordValidator.class)
    public DefaultPasswordValidator passwordValidator(AuthSecurityProperties properties) {
        AuthSecurityProperties.SecurityConfig securityConfig = properties.getSecurity();
        var policyConfig = securityConfig.getPasswordPolicy();
        var passwordPolicy = policyConfig.toPasswordPolicy();

        log.info("Initializing DefaultPasswordValidator with minLength={}, requireUppercase={}, requireLowercase={}, requireDigit={}, requireSpecialChar={}",
                policyConfig.getMinLength(),
                policyConfig.isRequireUppercase(),
                policyConfig.isRequireLowercase(),
                policyConfig.isRequireDigit(),
                policyConfig.isRequireSpecialChar());

        return new DefaultPasswordValidator(passwordPolicy);
    }

    /**
     * 创建 IP 限制检查器。
     *
     * @param properties 认证服务器配置属性
     * @return DefaultIpRestrictionChecker 实例
     */
    @Bean
    @ConditionalOnMissingBean(IpRestrictionChecker.class)
    public DefaultIpRestrictionChecker ipRestrictionChecker(AuthSecurityProperties properties) {
        AuthSecurityProperties.SecurityConfig securityConfig = properties.getSecurity();
        List<String> ipWhitelist = securityConfig.getIpWhitelist();
        List<String> ipBlacklist = securityConfig.getIpBlacklist();

        log.info("Initializing DefaultIpRestrictionChecker with whitelist={}, blacklist={}",
                ipWhitelist, ipBlacklist);

        DefaultIpRestrictionChecker checker = new DefaultIpRestrictionChecker();

        if (ipWhitelist != null && !ipWhitelist.isEmpty()) {
            List<IpRestrictionRule> whitelistRules = ipWhitelist.stream()
                    .map(ip -> new IpRestrictionRule(IpRestrictionRule.Type.WHITELIST, ip, null))
                    .collect(Collectors.toList());
            checker.setWhitelistRules(whitelistRules);
        }

        if (ipBlacklist != null && !ipBlacklist.isEmpty()) {
            List<IpRestrictionRule> blacklistRules = ipBlacklist.stream()
                    .map(ip -> new IpRestrictionRule(IpRestrictionRule.Type.BLACKLIST, ip, null))
                    .collect(Collectors.toList());
            checker.setBlacklistRules(blacklistRules);
        }

        return checker;
    }

    /**
     * 创建设备限制器。
     *
     * <p>需要业务系统提供 {@link AfgDeviceStorage} 实现。
     *
     * @param properties    认证服务器配置属性
     * @param deviceStorage 设备存储
     * @return DefaultDeviceLimiter 实例
     */
    @Bean
    @ConditionalOnBean(AfgDeviceStorage.class)
    @ConditionalOnMissingBean(DeviceLimiter.class)
    @Nullable
    public DefaultDeviceLimiter deviceLimiter(
            AuthSecurityProperties properties,
            AfgDeviceStorage deviceStorage) {

        AuthSecurityProperties.SecurityConfig securityConfig = properties.getSecurity();
        int maxDevices = securityConfig.getMaxDevices();

        log.info("Initializing DefaultDeviceLimiter with maxDevices={}", maxDevices);

        return new DefaultDeviceLimiter(deviceStorage, maxDevices);
    }
}