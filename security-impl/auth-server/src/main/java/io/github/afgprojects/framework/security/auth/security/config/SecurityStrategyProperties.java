package io.github.afgprojects.framework.security.auth.security.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 安全策略配置属性。
 *
 * <p>配置登录失败追踪、密码策略、IP 限制和设备限制等安全策略。
 *
 * <h3>配置示例</h3>
 * <pre>
 * afg:
 *   auth:
 *     security:
 *       enabled: true
 *       max-login-failures: 5
 *       lock-duration: 30m
 *       max-devices: 5
 *       password-policy:
 *         min-length: 8
 *         require-uppercase: true
 *         require-lowercase: true
 *         require-digit: true
 *         require-special-char: true
 *       ip-whitelist:
 *         - 192.168.1.*
 *         - 10.0.0.0/8
 *       ip-blacklist:
 *         - 192.168.100.*
 * </pre>
 *
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "afg.auth.security")
public class SecurityStrategyProperties {

    /**
     * 是否启用安全策略。
     */
    private boolean enabled = true;

    /**
     * 最大登录失败次数。
     *
     * <p>达到此次数后账户将被锁定。
     */
    private int maxLoginFailures = 5;

    /**
     * 账户锁定时长。
     */
    private Duration lockDuration = Duration.ofMinutes(30);

    /**
     * 最大设备数量。
     *
     * <p>同一账号同时登录的最大设备数。
     */
    private int maxDevices = 5;

    /**
     * 密码策略配置。
     */
    private PasswordPolicyConfig passwordPolicy = new PasswordPolicyConfig();

    /**
     * IP 白名单。
     *
     * <p>白名单中的 IP 可以绕过某些安全检查。
     */
    private List<String> ipWhitelist = new ArrayList<>();

    /**
     * IP 黑名单。
     *
     * <p>黑名单中的 IP 将被禁止访问。
     */
    private List<String> ipBlacklist = new ArrayList<>();

    /**
     * 密码策略配置。
     */
    @Data
    public static class PasswordPolicyConfig {

        /**
         * 密码最小长度。
         */
        private int minLength = 8;

        /**
         * 是否需要大写字母。
         */
        private boolean requireUppercase = true;

        /**
         * 是否需要小写字母。
         */
        private boolean requireLowercase = true;

        /**
         * 是否需要数字。
         */
        private boolean requireDigit = true;

        /**
         * 是否需要特殊字符。
         */
        private boolean requireSpecialChar = true;

        /**
         * 转换为 PasswordPolicy 对象。
         *
         * @return PasswordPolicy 实例
         */
        public io.github.afgprojects.framework.security.auth.security.PasswordPolicy toPasswordPolicy() {
            return new io.github.afgprojects.framework.security.auth.security.PasswordPolicy(
                    minLength,
                    requireUppercase,
                    requireLowercase,
                    requireDigit,
                    requireSpecialChar);
        }
    }
}
