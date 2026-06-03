package io.github.afgprojects.framework.core.properties.httpclient;

import java.util.Set;

import lombok.Data;

/**
 * HTTP 客户端配置。
 */
@Data
public class AfgCoreHttpClientProperties {

    /**
     * 连接超时时间（毫秒）。
     */
    private int connectTimeout = 5000;

    /**
     * 读取超时时间（毫秒）。
     */
    private int readTimeout = 30000;

    /**
     * 重试配置。
     */
    private AfgCoreHttpRetryProperties retry = new AfgCoreHttpRetryProperties();

    /**
     * 熔断器配置。
     */
    private AfgCoreHttpCircuitBreakerProperties circuitBreaker = new AfgCoreHttpCircuitBreakerProperties();
}
