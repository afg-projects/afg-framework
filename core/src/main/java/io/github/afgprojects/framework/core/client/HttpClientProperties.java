package io.github.afgprojects.framework.core.client;

import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * HTTP 客户端配置属性
 */
@Data
@ConfigurationProperties(prefix = "afg.http-client")
public class HttpClientProperties {

    /**
     * 连接超时时间（毫秒）
     */
    private int connectTimeout = 5000;

    /**
     * 读取超时时间（毫秒）
     */
    private int readTimeout = 30000;

    /**
     * 重试配置
     */
    private RetryConfig retry = new RetryConfig();

    /**
     * 熔断器配置
     */
    private CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig();

    /**
     * 重试配置
     */
    @Data
    public static class RetryConfig {
        /**
         * 是否启用重试
         */
        private boolean enabled = true;

        /**
         * 最大重试次数
         */
        private int maxAttempts = 3;

        /**
         * 初始重试间隔（毫秒）
         */
        private long initialInterval = 1000;

        /**
         * 重试间隔乘数
         */
        private double multiplier = 2.0;

        /**
         * 最大重试间隔（毫秒）
         */
        private long maxInterval = 10000;

        /**
         * 触发重试的 HTTP 状态码
         */
        private Set<Integer> retryOnStatus = Set.of(502, 503, 504);
    }

    /**
     * 熔断器配置
     */
    @Data
    public static class CircuitBreakerConfig {
        /**
         * 是否启用熔断器
         */
        private boolean enabled = true;

        /**
         * 失败次数阈值，达到后开启熔断器
         */
        private int failureThreshold = 5;

        /**
         * 熔断器开启持续时间（毫秒）
         */
        private long openDuration = 30000;

        /**
         * 半开状态最大调用次数
         */
        private int halfOpenMaxCalls = 3;

        /**
         * 成功次数阈值，达到后关闭熔断器
         */
        private int successThreshold = 3;
    }

    /**
     * 客户端命名配置
     */
    @Data
    public static class NamedClientConfig {
        private String baseUrl;
        private int connectTimeout;
        private int readTimeout;
        private RetryConfig retry = new RetryConfig();
        private CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig();

        public HttpClientProperties merge(HttpClientProperties defaults) {
            HttpClientProperties merged = new HttpClientProperties();
            merged.setConnectTimeout(connectTimeout > 0 ? connectTimeout : defaults.getConnectTimeout());
            merged.setReadTimeout(readTimeout > 0 ? readTimeout : defaults.getReadTimeout());

            // 合并 retry 配置
            RetryConfig defaultRetry = defaults.getRetry();
            RetryConfig mergedRetry = new RetryConfig();
            if (retry != null) {
                mergedRetry.setEnabled(retry.isEnabled());
                mergedRetry.setMaxAttempts(retry.getMaxAttempts() != 3 ? retry.getMaxAttempts() : defaultRetry.getMaxAttempts());
                mergedRetry.setInitialInterval(retry.getInitialInterval() != 1000 ? retry.getInitialInterval() : defaultRetry.getInitialInterval());
                mergedRetry.setMultiplier(retry.getMultiplier() != 2.0 ? retry.getMultiplier() : defaultRetry.getMultiplier());
                mergedRetry.setMaxInterval(retry.getMaxInterval() != 10000 ? retry.getMaxInterval() : defaultRetry.getMaxInterval());
                mergedRetry.setRetryOnStatus(retry.getRetryOnStatus());
            }
            merged.setRetry(mergedRetry);

            // 合并 circuitBreaker 配置
            CircuitBreakerConfig defaultCb = defaults.getCircuitBreaker();
            CircuitBreakerConfig mergedCb = new CircuitBreakerConfig();
            if (circuitBreaker != null) {
                mergedCb.setEnabled(circuitBreaker.isEnabled());
                mergedCb.setFailureThreshold(circuitBreaker.getFailureThreshold() != 5 ? circuitBreaker.getFailureThreshold() : defaultCb.getFailureThreshold());
                mergedCb.setOpenDuration(circuitBreaker.getOpenDuration() != 30000 ? circuitBreaker.getOpenDuration() : defaultCb.getOpenDuration());
                mergedCb.setHalfOpenMaxCalls(circuitBreaker.getHalfOpenMaxCalls() != 3 ? circuitBreaker.getHalfOpenMaxCalls() : defaultCb.getHalfOpenMaxCalls());
                mergedCb.setSuccessThreshold(circuitBreaker.getSuccessThreshold() != 3 ? circuitBreaker.getSuccessThreshold() : defaultCb.getSuccessThreshold());
            }
            merged.setCircuitBreaker(mergedCb);

            return merged;
        }
    }
}
