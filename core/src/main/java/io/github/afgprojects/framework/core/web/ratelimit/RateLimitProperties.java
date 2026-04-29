package io.github.afgprojects.framework.core.web.ratelimit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * 限流配置属性
 * <p>
 * 配置项前缀: afg.rate-limit
 * </p>
 *
 * <pre>
 * afg.rate-limit.enabled=true
 * afg.rate-limit.default-rate=10
 * afg.rate-limit.default-burst=20
 * afg.rate-limit.key-prefix=rateLimit
 * afg.rate-limit.fallback.enabled=true
 * afg.rate-limit.whitelist.ips=192.168.1.100,10.0.0.*
 * afg.rate-limit.whitelist.users=admin,system
 * afg.rate-limit.response-headers.enabled=true
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "afg.rate-limit")
public class RateLimitProperties {

    /**
     * 是否启用限流
     */
    private boolean enabled = true;

    /**
     * 默认每秒请求数
     */
    private long defaultRate = 10;

    /**
     * 默认突发容量（0 表示使用 rate * 2）
     */
    private long defaultBurst = 0;

    /**
     * 默认限流算法
     */
    private RateLimitAlgorithm defaultAlgorithm = RateLimitAlgorithm.TOKEN_BUCKET;

    /**
     * Redis key 前缀
     */
    private String keyPrefix = "rateLimit";

    /**
     * 回退配置
     */
    private final Fallback fallback = new Fallback();

    /**
     * 各维度的默认配置
     */
    private final Map<String, DimensionConfig> dimensions = new HashMap<>();

    /**
     * 白名单配置
     */
    private final Whitelist whitelist = new Whitelist();

    /**
     * 响应头配置
     */
    private final ResponseHeaders responseHeaders = new ResponseHeaders();

    /**
     * 本地限流配置（单机模式）
     */
    private final Local local = new Local();

    /**
     * 指标配置
     */
    private final Metrics metrics = new Metrics();

    @Data
    public static class Fallback {

        /**
         * 是否启用回退
         */
        private boolean enabled = true;

        /**
         * 默认回退消息
         */
        private String defaultMessage = "请求过于频繁，请稍后再试";

        /**
         * 故障模式：限流器异常时的行为
         * <ul>
         *   <li>allow - 放行请求（默认，避免影响业务）</li>
         *   <li>reject - 拒绝请求（更安全，但可能影响可用性）</li>
         * </ul>
         */
        private FailureMode failureMode = FailureMode.ALLOW;
    }

    /**
     * 故障模式枚举
     */
    public enum FailureMode {
        /** 放行请求 - 限流器异常时允许请求通过 */
        ALLOW,
        /** 拒绝请求 - 限流器异常时拒绝所有请求 */
        REJECT
    }

    @Data
    public static class DimensionConfig {

        /**
         * 每秒请求数
         */
        private long rate;

        /**
         * 突发容量
         */
        private long burst;

        /**
         * 限流算法
         */
        private RateLimitAlgorithm algorithm;

        /**
         * 时间窗口大小（秒）
         */
        private long windowSize;
    }

    @Data
    public static class Whitelist {

        /**
         * 是否启用白名单
         */
        private boolean enabled = true;

        /**
         * IP 白名单列表
         * <p>
         * 支持精确匹配和通配符匹配，如 192.168.1.100, 10.0.0.*
         * </p>
         */
        private List<String> ips = new ArrayList<>();

        /**
         * 用户ID白名单列表
         */
        private List<Long> userIds = new ArrayList<>();

        /**
         * 用户名白名单列表
         */
        private List<String> usernames = new ArrayList<>();

        /**
         * 租户ID白名单列表
         */
        private List<Long> tenantIds = new ArrayList<>();

        /**
         * 自定义白名单检查器 Bean 名称
         */
        private String customCheckerBean;
    }

    @Data
    public static class ResponseHeaders {

        /**
         * 是否在响应头中返回限流信息
         */
        private boolean enabled = true;

        /**
         * X-RateLimit-Limit 响应头名称
         */
        private String limitHeader = "X-RateLimit-Limit";

        /**
         * X-RateLimit-Remaining 响应头名称
         */
        private String remainingHeader = "X-RateLimit-Remaining";

        /**
         * X-RateLimit-Reset 响应头名称
         */
        private String resetHeader = "X-RateLimit-Reset";

        /**
         * Retry-After 响应头名称
         */
        private String retryAfterHeader = "Retry-After";
    }

    @Data
    public static class Local {

        /**
         * 是否启用本地限流（单机模式）
         * <p>
         * 当 Redis 不可用时，可以使用本地 Caffeine 缓存进行限流
         * </p>
         */
        private boolean enabled = false;

        /**
         * 本地限流缓存大小
         */
        private int cacheSize = 10000;

        /**
         * 本地限流缓存过期时间（秒）
         */
        private long expireAfterSeconds = 3600;
    }

    @Data
    public static class Metrics {

        /**
         * 是否启用限流指标
         */
        private boolean enabled = true;

        /**
         * 指标名称前缀
         */
        private String prefix = "afg.rate.limit";
    }
}
