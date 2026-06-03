package io.github.afgprojects.framework.security.auth.properties.security;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * 安全策略配置。
 */
@Data
public class SecurityConfig {

    /**
     * 是否启用安全策略。
     */
    private boolean enabled = true;

    /**
     * 最大登录失败次数。
     * 达到此次数后账户将被锁定。
     */
    private int maxLoginFailures = 5;

    /**
     * 账户锁定时长。
     */
    private Duration lockDuration = Duration.ofMinutes(30);

    /**
     * 最大设备数量。
     * 同一账号同时登录的最大设备数。
     */
    private int maxDevices = 5;

    /**
     * 密码策略配置。
     */
    private PasswordPolicyConfig passwordPolicy = new PasswordPolicyConfig();

    /**
     * IP 白名单。
     * 白名单中的 IP 可以绕过某些安全检查。
     */
    private List<String> ipWhitelist = new ArrayList<>();

    /**
     * IP 黑名单。
     * 黑名单中的 IP 将被禁止访问。
     */
    private List<String> ipBlacklist = new ArrayList<>();
}
