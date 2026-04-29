package io.github.afgprojects.framework.core.trace;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 追踪配置属性
 * <p>
 * 配置项前缀: afg.tracing
 * </p>
 *
 * <h3>配置示例</h3>
 * <pre>
 * afg:
 *   tracing:
 *     enabled: true
 *     annotations:
 *       enabled: true
 *     sampling:
 *       strategy: probability
 *       probability: 1.0
 *       rate: 100
 *     baggage:
 *       enabled: true
 *       remote-fields: tenantId,userId
 *       local-fields: customField
 *     propagation:
 *       enabled: true
 *       thread-pool-enabled: true
 *     # Zipkin 配置
 *     zipkin:
 *       enabled: true
 *       endpoint: http://localhost:9411/api/v2/spans
 *       connect-timeout: 5000
 *       read-timeout: 10000
 *     # Jaeger 配置（通过 OTLP 协议）
 *     jaeger:
 *       enabled: false
 *       endpoint: http://localhost:14268/api/traces
 *       # 或使用 OTLP 协议
 *       otlp-endpoint: http://localhost:4317
 * </pre>
 */
@ConfigurationProperties(prefix = "afg.tracing")
public class TracingProperties {

    /**
     * 是否启用追踪功能
     */
    private boolean enabled = true;

    /**
     * 注解相关配置
     */
    private final Annotations annotations = new Annotations();

    /**
     * 采样配置
     */
    private final Sampling sampling = new Sampling();

    /**
     * Baggage 配置
     */
    private final Baggage baggage = new Baggage();

    /**
     * 传播配置
     */
    private final Propagation propagation = new Propagation();

    /**
     * Zipkin 配置
     */
    private final Zipkin zipkin = new Zipkin();

    /**
     * Jaeger 配置
     */
    private final Jaeger jaeger = new Jaeger();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Annotations getAnnotations() {
        return annotations;
    }

    public Sampling getSampling() {
        return sampling;
    }

    public Baggage getBaggage() {
        return baggage;
    }

    public Propagation getPropagation() {
        return propagation;
    }

    public Zipkin getZipkin() {
        return zipkin;
    }

    public Jaeger getJaeger() {
        return jaeger;
    }

    /**
     * 注解配置
     */
    public static class Annotations {

        /**
         * 是否启用 @Traced 注解支持
         */
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * 采样配置
     */
    public static class Sampling {

        /**
         * 采样策略
         */
        private SamplingStrategy strategy = SamplingStrategy.PROBABILITY;

        /**
         * 概率采样概率（0.0 - 1.0）
         * <p>
         * 仅当 strategy 为 PROBABILITY 时有效
         * </p>
         */
        private double probability = 1.0;

        /**
         * 限流采样速率（每秒请求数）
         * <p>
         * 仅当 strategy 为 RATE_LIMITING 时有效
         * </p>
         */
        private int rate = 100;

        public SamplingStrategy getStrategy() {
            return strategy;
        }

        public void setStrategy(SamplingStrategy strategy) {
            this.strategy = strategy;
        }

        public double getProbability() {
            return probability;
        }

        public void setProbability(double probability) {
            this.probability = probability;
        }

        public int getRate() {
            return rate;
        }

        public void setRate(int rate) {
            this.rate = rate;
        }
    }

    /**
     * Baggage 配置
     */
    public static class Baggage {

        /**
         * 是否启用 Baggage 传播
         */
        private boolean enabled = true;

        /**
         * 远程传播字段列表
         * <p>
         * 这些字段会跨服务传播
         * </p>
         */
        private List<String> remoteFields = List.of("tenantId", "userId", "traceId");

        /**
         * 本地传播字段列表
         * <p>
         * 这些字段仅在当前服务内传播
         * </p>
         */
        private List<String> localFields = List.of();

        /**
         * Baggage 字段映射
         * <p>
         * 用于自定义请求头名称
         * </p>
         */
        private Map<String, String> fieldMappings = new HashMap<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getRemoteFields() {
            return remoteFields;
        }

        public void setRemoteFields(List<String> remoteFields) {
            this.remoteFields = remoteFields;
        }

        public List<String> getLocalFields() {
            return localFields;
        }

        public void setLocalFields(List<String> localFields) {
            this.localFields = localFields;
        }

        public Map<String, String> getFieldMappings() {
            return fieldMappings;
        }

        public void setFieldMappings(Map<String, String> fieldMappings) {
            this.fieldMappings = fieldMappings;
        }
    }

    /**
     * 传播配置
     */
    public static class Propagation {

        /**
         * 是否启用跨线程传播
         */
        private boolean enabled = true;

        /**
         * 是否自动包装线程池
         */
        private boolean threadPoolEnabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isThreadPoolEnabled() {
            return threadPoolEnabled;
        }

        public void setThreadPoolEnabled(boolean threadPoolEnabled) {
            this.threadPoolEnabled = threadPoolEnabled;
        }
    }

    /**
     * Zipkin 配置
     */
    public static class Zipkin {

        /**
         * 是否启用 Zipkin 报告器
         */
        private boolean enabled = false;

        /**
         * Zipkin 服务端点
         * <p>
         * 默认: http://localhost:9411/api/v2/spans
         * </p>
         */
        private String endpoint = "http://localhost:9411/api/v2/spans";

        /**
         * 连接超时（毫秒）
         */
        private int connectTimeout = 5000;

        /**
         * 读取超时（毫秒）
         */
        private int readTimeout = 10000;

        /**
         * 是否启用压缩
         */
        private boolean compressionEnabled = true;

        /**
         * 发送间隔（毫秒）
         */
        private int sendInterval = 5000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public int getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public int getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
        }

        public boolean isCompressionEnabled() {
            return compressionEnabled;
        }

        public void setCompressionEnabled(boolean compressionEnabled) {
            this.compressionEnabled = compressionEnabled;
        }

        public int getSendInterval() {
            return sendInterval;
        }

        public void setSendInterval(int sendInterval) {
            this.sendInterval = sendInterval;
        }
    }

    /**
     * Jaeger 配置
     * <p>
     * Jaeger 支持两种协议：
     * <ul>
     *   <li>Jaeger Thrift HTTP 协议（legacy）</li>
     *   <li>OpenTelemetry OTLP 协议（推荐）</li>
     * </ul>
     * </p>
     */
    public static class Jaeger {

        /**
         * 是否启用 Jaeger 报告器
         */
        private boolean enabled = false;

        /**
         * Jaeger Thrift HTTP 端点
         * <p>
         * 默认: http://localhost:14268/api/traces
         * </p>
         */
        private @Nullable String endpoint = "http://localhost:14268/api/traces";

        /**
         * OpenTelemetry OTLP 端点
         * <p>
         * 使用 OTLP 协议发送到 Jaeger（Jaeger 1.35+ 支持）
         * 默认: http://localhost:4317（gRPC）或 http://localhost:4318（HTTP）
         * </p>
         */
        private @Nullable String otlpEndpoint;

        /**
         * 是否使用 OTLP 协议
         * <p>
         * 推荐使用 OTLP 协议，Jaeger 1.35+ 原生支持
         * </p>
         */
        private boolean useOtlp = true;

        /**
         * 连接超时（毫秒）
         */
        private int connectTimeout = 5000;

        /**
         * 读取超时（毫秒）
         */
        private int readTimeout = 10000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public @Nullable String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(@Nullable String endpoint) {
            this.endpoint = endpoint;
        }

        public @Nullable String getOtlpEndpoint() {
            return otlpEndpoint;
        }

        public void setOtlpEndpoint(@Nullable String otlpEndpoint) {
            this.otlpEndpoint = otlpEndpoint;
        }

        public boolean isUseOtlp() {
            return useOtlp;
        }

        public void setUseOtlp(boolean useOtlp) {
            this.useOtlp = useOtlp;
        }

        public int getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public int getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
        }
    }
}
