package io.github.afgprojects.framework.ai.autoconfigure;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 模块 Spring Boot 配置属性
 *
 * <p>支持 application.yml 配置绑定：
 * <pre>{@code
 * afg:
 *   ai:
 *     enabled: true
 *     llm:
 *       provider: openai
 *       model: gpt-4
 *       api-key: ${OPENAI_API_KEY}
 *     resilience:
 *       enabled: true
 *       retry:
 *         max-retries: 3
 *       circuit-breaker:
 *         failure-rate-threshold: 0.5
 *     observability:
 *       enabled: true
 *       metrics:
 *         enabled: true
 *     security:
 *       enabled: true
 *       pii-detection:
 *         enabled: true
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "afg.ai")
public class AiConfigurationProperties {

    /**
     * 是否启用 AI 模块
     */
    private boolean enabled = true;

    /**
     * LLM 配置
     */
    private LlmConfig llm = new LlmConfig();

    /**
     * Agent 配置
     */
    private AgentConfig agent = new AgentConfig();

    /**
     * 韧性配置
     */
    private ResilienceConfig resilience = new ResilienceConfig();

    /**
     * 可观测性配置
     */
    private ObservabilityConfig observability = new ObservabilityConfig();

    /**
     * 安全配置
     */
    private SecurityConfig security = new SecurityConfig();

    /**
     * 持久化配置
     */
    private PersistenceConfig persistence = new PersistenceConfig();

    /**
     * 性能配置
     */
    private PerformanceConfig performance = new PerformanceConfig();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LlmConfig getLlm() {
        return llm;
    }

    public void setLlm(LlmConfig llm) {
        this.llm = llm;
    }

    public AgentConfig getAgent() {
        return agent;
    }

    public void setAgent(AgentConfig agent) {
        this.agent = agent;
    }

    public ResilienceConfig getResilience() {
        return resilience;
    }

    public void setResilience(ResilienceConfig resilience) {
        this.resilience = resilience;
    }

    public ObservabilityConfig getObservability() {
        return observability;
    }

    public void setObservability(ObservabilityConfig observability) {
        this.observability = observability;
    }

    public SecurityConfig getSecurity() {
        return security;
    }

    public void setSecurity(SecurityConfig security) {
        this.security = security;
    }

    public PersistenceConfig getPersistence() {
        return persistence;
    }

    public void setPersistence(PersistenceConfig persistence) {
        this.persistence = persistence;
    }

    public PerformanceConfig getPerformance() {
        return performance;
    }

    public void setPerformance(PerformanceConfig performance) {
        this.performance = performance;
    }

    /**
     * LLM 配置
     */
    public static class LlmConfig {
        private String provider = "openai";
        private String model = "gpt-4";
        private @Nullable String apiKey;
        private @Nullable String baseUrl;
        private @Nullable Double temperature;
        private @Nullable Integer maxTokens;
        private @Nullable Long timeout;

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public @Nullable String getApiKey() { return apiKey; }
        public void setApiKey(@Nullable String apiKey) { this.apiKey = apiKey; }
        public @Nullable String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(@Nullable String baseUrl) { this.baseUrl = baseUrl; }
        public @Nullable Double getTemperature() { return temperature; }
        public void setTemperature(@Nullable Double temperature) { this.temperature = temperature; }
        public @Nullable Integer getMaxTokens() { return maxTokens; }
        public void setMaxTokens(@Nullable Integer maxTokens) { this.maxTokens = maxTokens; }
        public @Nullable Long getTimeout() { return timeout; }
        public void setTimeout(@Nullable Long timeout) { this.timeout = timeout; }
    }

    /**
     * Agent 配置
     */
    public static class AgentConfig {
        private Integer maxIterations = 10;
        private Long timeout = 60000L;
        private Boolean enableMemory = true;
        private Integer memoryMaxSize = 100;

        public Integer getMaxIterations() { return maxIterations; }
        public void setMaxIterations(Integer maxIterations) { this.maxIterations = maxIterations; }
        public Long getTimeout() { return timeout; }
        public void setTimeout(Long timeout) { this.timeout = timeout; }
        public boolean isEnableMemory() { return enableMemory; }
        public void setEnableMemory(Boolean enableMemory) { this.enableMemory = enableMemory; }
        public Integer getMemoryMaxSize() { return memoryMaxSize; }
        public void setMemoryMaxSize(Integer memoryMaxSize) { this.memoryMaxSize = memoryMaxSize; }
    }

    /**
     * 韧性配置
     */
    public static class ResilienceConfig {
        private boolean enabled = true;

        /**
         * 重试配置
         */
        private RetryConfig retry = new RetryConfig();

        /**
         * 熔断器配置
         */
        private CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig();

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public RetryConfig getRetry() { return retry; }
        public void setRetry(RetryConfig retry) { this.retry = retry; }
        public CircuitBreakerConfig getCircuitBreaker() { return circuitBreaker; }
        public void setCircuitBreaker(CircuitBreakerConfig circuitBreaker) { this.circuitBreaker = circuitBreaker; }
    }

    /**
     * 重试配置
     */
    public static class RetryConfig {
        private int maxRetries = 3;
        private long initialIntervalMs = 1000;
        private double multiplier = 2.0;
        private long maxIntervalMs = 30000;
        private double jitterFactor = 0.5;

        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
        public long getInitialIntervalMs() { return initialIntervalMs; }
        public void setInitialIntervalMs(long initialIntervalMs) { this.initialIntervalMs = initialIntervalMs; }
        public double getMultiplier() { return multiplier; }
        public void setMultiplier(double multiplier) { this.multiplier = multiplier; }
        public long getMaxIntervalMs() { return maxIntervalMs; }
        public void setMaxIntervalMs(long maxIntervalMs) { this.maxIntervalMs = maxIntervalMs; }
        public double getJitterFactor() { return jitterFactor; }
        public void setJitterFactor(double jitterFactor) { this.jitterFactor = jitterFactor; }
    }

    /**
     * 熔断器配置
     */
    public static class CircuitBreakerConfig {
        private int windowSize = 100;
        private double failureRateThreshold = 0.5;
        private int halfOpenMaxCalls = 10;
        private long openStateTimeoutMs = 30000;

        public int getWindowSize() { return windowSize; }
        public void setWindowSize(int windowSize) { this.windowSize = windowSize; }
        public double getFailureRateThreshold() { return failureRateThreshold; }
        public void setFailureRateThreshold(double failureRateThreshold) { this.failureRateThreshold = failureRateThreshold; }
        public int getHalfOpenMaxCalls() { return halfOpenMaxCalls; }
        public void setHalfOpenMaxCalls(int halfOpenMaxCalls) { this.halfOpenMaxCalls = halfOpenMaxCalls; }
        public long getOpenStateTimeoutMs() { return openStateTimeoutMs; }
        public void setOpenStateTimeoutMs(long openStateTimeoutMs) { this.openStateTimeoutMs = openStateTimeoutMs; }
    }

    /**
     * 可观测性配置
     */
    public static class ObservabilityConfig {
        private boolean enabled = true;

        /**
         * 指标配置
         */
        private MetricsConfig metrics = new MetricsConfig();

        /**
         * 追踪配置
         */
        private TracingConfig tracing = new TracingConfig();

        /**
         * 审计日志配置
         */
        private AuditConfig audit = new AuditConfig();

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public MetricsConfig getMetrics() { return metrics; }
        public void setMetrics(MetricsConfig metrics) { this.metrics = metrics; }
        public TracingConfig getTracing() { return tracing; }
        public void setTracing(TracingConfig tracing) { this.tracing = tracing; }
        public AuditConfig getAudit() { return audit; }
        public void setAudit(AuditConfig audit) { this.audit = audit; }
    }

    /**
     * 指标配置
     */
    public static class MetricsConfig {
        private boolean enabled = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    /**
     * 追踪配置
     */
    public static class TracingConfig {
        private boolean enabled = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    /**
     * 审计日志配置
     */
    public static class AuditConfig {
        private boolean enabled = true;
        private int maxEntries = 10000;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getMaxEntries() { return maxEntries; }
        public void setMaxEntries(int maxEntries) { this.maxEntries = maxEntries; }
    }

    /**
     * 安全配置
     */
    public static class SecurityConfig {
        private boolean enabled = true;

        /**
         * API Key 管理配置
         */
        private ApiKeyConfig apiKey = new ApiKeyConfig();

        /**
         * 内容安全配置
         */
        private ContentSafetyConfig contentSafety = new ContentSafetyConfig();

        /**
         * PII 检测配置
         */
        private PiiConfig pii = new PiiConfig();

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public ApiKeyConfig getApiKey() { return apiKey; }
        public void setApiKey(ApiKeyConfig apiKey) { this.apiKey = apiKey; }
        public ContentSafetyConfig getContentSafety() { return contentSafety; }
        public void setContentSafety(ContentSafetyConfig contentSafety) { this.contentSafety = contentSafety; }
        public PiiConfig getPii() { return pii; }
        public void setPii(PiiConfig pii) { this.pii = pii; }
    }

    /**
     * API Key 配置
     */
    public static class ApiKeyConfig {
        private boolean enabled = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    /**
     * 内容安全配置
     */
    public static class ContentSafetyConfig {
        private boolean enabled = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    /**
     * PII 检测配置
     */
    public static class PiiConfig {
        private boolean enabled = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    /**
     * 持久化配置
     */
    public static class PersistenceConfig {
        private boolean enabled = true;

        /**
         * 会话配置
         */
        private SessionConfig session = new SessionConfig();

        /**
         * 消息历史配置
         */
        private MessageHistoryConfig messageHistory = new MessageHistoryConfig();

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public SessionConfig getSession() { return session; }
        public void setSession(SessionConfig session) { this.session = session; }
        public MessageHistoryConfig getMessageHistory() { return messageHistory; }
        public void setMessageHistory(MessageHistoryConfig messageHistory) { this.messageHistory = messageHistory; }
    }

    /**
     * 会话配置
     */
    public static class SessionConfig {
        private boolean enabled = true;
        private int maxSessionsPerUser = 100;
        private long defaultExpiresInSeconds = 3600;
        private String tableName = "ai_session";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getMaxSessionsPerUser() { return maxSessionsPerUser; }
        public void setMaxSessionsPerUser(int maxSessionsPerUser) { this.maxSessionsPerUser = maxSessionsPerUser; }
        public long getDefaultExpiresInSeconds() { return defaultExpiresInSeconds; }
        public void setDefaultExpiresInSeconds(long defaultExpiresInSeconds) { this.defaultExpiresInSeconds = defaultExpiresInSeconds; }
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
    }

    /**
     * 消息历史配置
     */
    public static class MessageHistoryConfig {
        private boolean enabled = true;
        private int maxMessagesPerSession = 1000;
        private String tableName = "ai_message_history";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getMaxMessagesPerSession() { return maxMessagesPerSession; }
        public void setMaxMessagesPerSession(int maxMessagesPerSession) { this.maxMessagesPerSession = maxMessagesPerSession; }
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
    }

    /**
     * 性能配置
     */
    public static class PerformanceConfig {
        private boolean enabled = true;

        /**
         * 缓存配置
         */
        private CacheConfig cache = new CacheConfig();

        /**
         * 速率限制配置
         */
        private RateLimitConfig rateLimit = new RateLimitConfig();

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public CacheConfig getCache() { return cache; }
        public void setCache(CacheConfig cache) { this.cache = cache; }
        public RateLimitConfig getRateLimit() { return rateLimit; }
        public void setRateLimit(RateLimitConfig rateLimit) { this.rateLimit = rateLimit; }
    }

    /**
     * 缓存配置
     */
    public static class CacheConfig {
        private boolean enabled = true;
        private long maxSize = 1000;
        private long ttlSeconds = 600;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public long getMaxSize() { return maxSize; }
        public void setMaxSize(long maxSize) { this.maxSize = maxSize; }
        public long getTtlSeconds() { return ttlSeconds; }
        public void setTtlSeconds(long ttlSeconds) { this.ttlSeconds = ttlSeconds; }
    }

    /**
     * 速率限制配置
     */
    public static class RateLimitConfig {
        private boolean enabled = true;
        private long defaultPermits = 100;
        private long windowSeconds = 1;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public long getDefaultPermits() { return defaultPermits; }
        public void setDefaultPermits(long defaultPermits) { this.defaultPermits = defaultPermits; }
        public long getWindowSeconds() { return windowSeconds; }
        public void setWindowSeconds(long windowSeconds) { this.windowSeconds = windowSeconds; }
    }
}