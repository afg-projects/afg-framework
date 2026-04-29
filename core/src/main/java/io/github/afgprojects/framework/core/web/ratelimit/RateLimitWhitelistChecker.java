package io.github.afgprojects.framework.core.web.ratelimit;

import org.jspecify.annotations.Nullable;

import io.github.afgprojects.framework.core.web.context.AfgRequestContextHolder;
import io.github.afgprojects.framework.core.web.context.RequestContext;

/**
 * 限流白名单检查器
 * <p>
 * 检查请求是否在白名单中，白名单内的请求不受限流控制
 * </p>
 */
public class RateLimitWhitelistChecker {

    private final RateLimitProperties properties;

    /**
     * 构造函数
     *
     * @param properties 限流配置属性
     */
    public RateLimitWhitelistChecker(RateLimitProperties properties) {
        this.properties = properties;
    }

    /**
     * 检查当前请求是否在白名单中
     *
     * @param dimension 限流维度
     * @return 是否在白名单中
     */
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
     *
     * @return 是否在白名单中
     */
    private boolean isIpInWhitelist() {
        var ipWhitelist = properties.getWhitelist().getIps();
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
     *
     * @param clientIp 客户端 IP
     * @param pattern  IP 模式（支持 * 通配符）
     * @return 是否匹配
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
     *
     * @return 是否在白名单中
     */
    private boolean isUserInWhitelist() {
        var whitelist = properties.getWhitelist();

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
     *
     * @return 是否在白名单中
     */
    private boolean isTenantInWhitelist() {
        var whitelist = properties.getWhitelist();

        RequestContext context = AfgRequestContextHolder.getContext();
        if (context == null) {
            return false;
        }

        Long tenantId = context.getTenantId();
        return tenantId != null && whitelist.getTenantIds().contains(tenantId);
    }

    /**
     * 自定义白名单检查接口
     * <p>
     * 业务可以实现此接口提供自定义的白名单检查逻辑
     * </p>
     */
    public interface CustomWhitelistChecker {

        /**
         * 检查是否在白名单中
         *
         * @param dimension 限流维度
         * @param context   请求上下文
         * @return 是否在白名单中
         */
        boolean isInWhitelist(RateLimitDimension dimension, @Nullable RequestContext context);
    }
}