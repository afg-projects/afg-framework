package io.github.afgprojects.framework.core.api.ratelimit;

import java.util.List;

import io.github.afgprojects.framework.core.web.context.AfgRequestContextHolder;
import io.github.afgprojects.framework.core.web.context.RequestContext;
import io.github.afgprojects.framework.core.web.ratelimit.RateLimitProperties;

/**
 * 默认白名单策略实现
 * <p>
 * 基于配置文件的白名单检查，支持 IP、用户、租户维度的白名单。
 * </p>
 */
public class DefaultWhitelistStrategy implements WhitelistStrategy {

    private final RateLimitProperties properties;

    /**
     * 构造函数
     *
     * @param properties 限流配置属性
     */
    public DefaultWhitelistStrategy(RateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean isInWhitelist(RateLimitDimension dimension) {
        if (!properties.getWhitelist().isEnabled()) {
            return false;
        }

        return switch (dimension) {
            case IP -> isIpInWhitelist();
            case USER -> isUserInWhitelist();
            case TENANT -> isTenantInWhitelist();
            case API -> false; // API 维度不检查白名单
        };
    }

    /**
     * 检查 IP 是否在白名单中
     */
    private boolean isIpInWhitelist() {
        List<String> ipWhitelist = properties.getWhitelist().getIps();
        if (ipWhitelist.isEmpty()) {
            return false;
        }

        RequestContext context = AfgRequestContextHolder.getContext();
        if (context == null) {
            return false;
        }

        String clientIp = context.getClientIp();
        if (clientIp == null || clientIp.isEmpty()) {
            return false;
        }

        for (String pattern : ipWhitelist) {
            if (matchIpPattern(clientIp, pattern)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 匹配 IP 模式
     */
    private boolean matchIpPattern(String clientIp, String pattern) {
        if (pattern.equals(clientIp)) {
            return true;
        }

        // 支持 * 通配符
        if (pattern.contains("*")) {
            String regex = pattern.replace(".", "\\.").replace("*", ".*");
            return clientIp.matches(regex);
        }

        return false;
    }

    /**
     * 检查用户是否在白名单中
     */
    private boolean isUserInWhitelist() {
        RateLimitProperties.Whitelist whitelist = properties.getWhitelist();

        RequestContext context = AfgRequestContextHolder.getContext();
        if (context == null) {
            return false;
        }

        // 检查用户ID白名单
        Long userId = context.getUserId();
        if (userId != null && whitelist.getUserIds().contains(userId)) {
            return true;
        }

        // 检查用户名白名单
        String username = context.getUsername();
        if (username != null && !username.isEmpty() && whitelist.getUsernames().contains(username)) {
            return true;
        }

        return false;
    }

    /**
     * 检查租户是否在白名单中
     */
    private boolean isTenantInWhitelist() {
        RateLimitProperties.Whitelist whitelist = properties.getWhitelist();

        RequestContext context = AfgRequestContextHolder.getContext();
        if (context == null) {
            return false;
        }

        Long tenantId = context.getTenantId();
        return tenantId != null && whitelist.getTenantIds().contains(tenantId);
    }
}
