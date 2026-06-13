package io.github.afgprojects.framework.core.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * AFG Core 统一配置属性。
 *
 * <p>整合了缓存、事件、锁、数据源、安全、健康检查、限流、调度器、追踪等所有核心配置。
 *
 * <h3>配置示例</h3>
 * <pre>
 * afg:
 *   core:
 *     enabled: true
 *     cache:
 *       enabled: true
 *       type: multi-level
 *       default-ttl: 3600000
 *     event:
 *       enabled: true
 *       type: LOCAL
 *     lock:
 *       enabled: true
 *       key-prefix: "afg:lock"
 *     datasource:
 *       enabled: false
 *       primary: master
 *     security:
 *       xss:
 *         enabled: true
 *       signature:
 *         enabled: true
 *     health:
 *       liveness-enabled: true
 *       readiness-enabled: true
 *     rate-limit:
 *       enabled: true
 *       default-rate: 10
 *     scheduler:
 *       enabled: true
 *     tracing:
 *       enabled: true
 *     logging:
 *       mask-sensitive: true
 *     metrics:
 *       enabled: true
 *     virtual-thread:
 *       enabled: true
 * </pre>
 *
 * @since 1.1.0
 */
@Data
@ConfigurationProperties(prefix = "afg.core")
public class AfgCoreProperties {

    /**
     * 是否启用 AFG Core 功能。
     * 默认启用。
     */
    private boolean enabled = true;

    // ========== 缓存配置 ==========

    /**
     * 缓存配置。
     */
    private CacheConfig cache = new CacheConfig();

    // ========== 事件配置 ==========

    /**
     * 事件配置。
     */
    private EventConfig event = new EventConfig();

    // ========== 分布式锁配置 ==========

    /**
     * 分布式锁配置。
     */
    private LockConfig lock = new LockConfig();

    // ========== 数据源配置 ==========

    /**
     * 多数据源配置。
     */
    private DataSourceConfig datasource = new DataSourceConfig();

    // ========== 安全配置 ==========

    /**
     * 安全配置。
     */
    private SecurityConfig security = new SecurityConfig();

    // ========== 健康检查配置 ==========

    /**
     * 健康检查配置。
     */
    private HealthConfig health = new HealthConfig();

    // ========== 限流配置 ==========

    /**
     * 限流配置。
     */
    private RateLimitConfig rateLimit = new RateLimitConfig();

    // ========== 调度器配置 ==========

    /**
     * 调度器配置。
     */
    private SchedulerConfig scheduler = new SchedulerConfig();

    // ========== 追踪配置 ==========

    /**
     * 追踪配置。
     */
    private TracingConfig tracing = new TracingConfig();

    // ========== 日志配置 ==========

    /**
     * 日志配置。
     */
    private LoggingConfig logging = new LoggingConfig();

    // ========== 指标配置 ==========

    /**
     * 指标配置。
     */
    private MetricsConfig metrics = new MetricsConfig();

    // ========== 虚拟线程配置 ==========

    /**
     * 虚拟线程配置。
     */
    private VirtualThreadConfig virtualThread = new VirtualThreadConfig();

    // ========== 审计配置 ==========

    /**
     * 审计配置。
     */
    private AuditConfig audit = new AuditConfig();

    // ========== 批量操作配置 ==========

    /**
     * 批量操作配置。
     */
    private BatchConfig batch = new BatchConfig();

    // ========== HTTP 客户端配置 ==========

    /**
     * HTTP 客户端配置。
     */
    private HttpClientConfig httpClient = new HttpClientConfig();

    // ========== 云原生配置 ==========

    /**
     * 云原生配置。
     */
    private CloudNativeConfig cloudNative = new CloudNativeConfig();

    // ========== 功能开关配置 ==========

    /**
     * 功能开关配置。
     */
    private FeatureFlagConfig feature = new FeatureFlagConfig();

    // ========== 加密配置 ==========

    /**
     * 加密配置。
     */
    private EncryptionConfig encryption = new EncryptionConfig();

    // ========== 数据权限配置 ==========

    /**
     * 数据权限配置。
     */
    private DataScopeConfig dataScope = new DataScopeConfig();

    // ========== 访问日志配置 ==========

    /**
     * 访问日志配置。
     */
    private AccessLogConfig accessLog = new AccessLogConfig();

    // ========== 参数校验配置 ==========

    /**
     * 参数校验配置。
     */
    private ValidationConfig validation = new ValidationConfig();

    // ========== 防重复提交配置 ==========

    /**
     * 防重复提交配置。
     */
    private DuplicateSubmitConfig duplicateSubmit = new DuplicateSubmitConfig();

    // ========== 优雅关闭配置 ==========

    /**
     * 优雅关闭配置。
     */
    private ShutdownConfig shutdown = new ShutdownConfig();

    // ========== SSE 配置 ==========

    /**
     * SSE 配置。
     */
    private SseConfig sse = new SseConfig();

    // ========== ID 生成器配置 ==========

    /**
     * ID 生成器配置。
     */
    private IdGeneratorConfig idGenerator = new IdGeneratorConfig();

    // ========== 通知配置 ==========

    /**
     * 通知配置。
     */
    private NotificationConfig notification = new NotificationConfig();

    // ========== Webhook 配置 ==========

    /**
     * Webhook 配置。
     */
    private WebhookConfig webhook = new WebhookConfig();

    // ========== 状态机配置 ==========

    /**
     * 状态机配置。
     */
    private StateMachineConfig stateMachine = new StateMachineConfig();

    // ========== 导入导出配置 ==========

    /**
     * 导入导出配置。
     */
    private ImportExportConfig importExport = new ImportExportConfig();

    // ========== 枚举管理配置 ==========

    /**
     * 枚举管理配置。
     */
    private EnumManagementConfig enumManagement = new EnumManagementConfig();

    // ========== 缓存配置类 ==========

    /**
     * 缓存配置类。
     */
    @Data
    public static class CacheConfig {

        /**
         * 是否启用缓存。
         */
        private boolean enabled = true;

        /**
         * 缓存类型：local、distributed、multi-level。
         */
        private CacheType type = CacheType.LOCAL;

        /**
         * 默认过期时间（毫秒）。
         */
        private long defaultTtl = 0;

        /**
         * 是否缓存 null 值（防穿透）。
         */
        private boolean cacheNull = true;

        /**
         * 空值缓存过期时间（毫秒）。
         */
        private long nullValueTtl = 60000;

        /**
         * 本地缓存配置。
         */
        private LocalCacheConfig local = new LocalCacheConfig();

        /**
         * 分布式缓存配置。
         */
        private DistributedCacheConfig distributed = new DistributedCacheConfig();

        /**
         * 缓存类型枚举。
         */
        public enum CacheType {
            LOCAL,
            DISTRIBUTED,
            MULTI_LEVEL
        }

        /**
         * 本地缓存配置。
         */
        @Data
        public static class LocalCacheConfig {

            private boolean enabled = true;
            private int initialCapacity = 128;
            private int maximumSize = 10000;
            private @Nullable Duration expireAfterWrite;
            private @Nullable Duration expireAfterAccess;
            private boolean recordStats = true;
        }

        /**
         * 分布式缓存配置。
         */
        @Data
        public static class DistributedCacheConfig {

            private boolean enabled = true;
            private String keyPrefix = "afg:cache:";
            private long defaultTtl = 0;
        }
    }

    // ========== 事件配置类 ==========

    /**
     * 事件配置类。
     */
    @Data
    public static class EventConfig {

        /**
         * 是否启用事件驱动。
         */
        private boolean enabled = true;

        /**
         * 事件发布类型。
         */
        private EventType type = EventType.LOCAL;

        /**
         * 默认主题名称。
         */
        private String defaultTopic = "afg.events";

        /**
         * 本地事件配置。
         */
        private LocalEventConfig local = new LocalEventConfig();

        /**
         * Kafka 配置。
         */
        private KafkaEventConfig kafka = new KafkaEventConfig();

        /**
         * RabbitMQ 配置。
         */
        private RabbitMqEventConfig rabbitmq = new RabbitMqEventConfig();

        /**
         * 重试配置。
         */
        private EventRetryConfig retry = new EventRetryConfig();

        /**
         * 死信队列配置。
         */
        private DeadLetterConfig deadLetter = new DeadLetterConfig();

        /**
         * 事件类型枚举。
         */
        public enum EventType {
            LOCAL,
            KAFKA,
            RABBITMQ
        }

        /**
         * 消息确认模式。
         */
        public enum AckMode {
            AUTO,
            MANUAL,
            BATCH
        }

        @Data
        public static class LocalEventConfig {
            private boolean async;
            private int threadPoolSize = 4;
        }

        @Data
        public static class KafkaEventConfig {
            private @Nullable String bootstrapServers;
            private Map<String, Object> producer = new HashMap<>();
            private Map<String, Object> consumer = new HashMap<>();
            private boolean autoCreateTopics = true;
            private int partitions = 3;
            private short replicationFactor = 1;
        }

        @Data
        public static class RabbitMqEventConfig {
            private String host = "localhost";
            private int port = 5672;
            private String username = "guest";
            private String password = "guest";
            private String virtualHost = "/";
            private String exchange = "afg.events";
            private String queuePrefix = "afg.queue.";
            private AckMode ackMode = AckMode.AUTO;
            private int prefetchCount = 10;
            private boolean autoDeclare = true;
        }

        @Data
        public static class EventRetryConfig {
            private boolean enabled = true;
            private int maxAttempts = 3;
            private long initialInterval = 1000;
            private double multiplier = 2.0;
            private long maxInterval = 30000;
        }

        @Data
        public static class DeadLetterConfig {
            private boolean enabled = true;
            private String topicPrefix = "dlq.";
            private long retentionMs;
        }
    }

    // ========== 分布式锁配置类 ==========

    /**
     * 分布式锁配置类。
     */
    @Data
    public static class LockConfig {

        /**
         * 是否启用分布式锁。
         */
        private boolean enabled = true;

        /**
         * 锁键前缀。
         */
        private String keyPrefix = "afg:lock";

        /**
         * 默认等待时间（毫秒）。
         */
        private long defaultWaitTime = 5000;

        /**
         * 默认持有时间（毫秒）。
         * -1 表示使用 watchdog 自动续期。
         */
        private long defaultLeaseTime = -1;

        /**
         * 注解相关配置。
         */
        private LockAnnotationConfig annotations = new LockAnnotationConfig();

        @Data
        public static class LockAnnotationConfig {
            private boolean enabled = true;
        }
    }

    // ========== 数据源配置类 ==========

    /**
     * 数据源配置类。
     */
    @Data
    public static class DataSourceConfig {

        /**
         * 是否启用多数据源。
         */
        private boolean enabled = false;

        /**
         * 主数据源名称。
         */
        private String primary = "master";

        /**
         * 严格模式。
         */
        private boolean strict = false;

        /**
         * 数据源配置映射。
         */
        private Map<String, DataSourceInstanceConfig> datasources = new HashMap<>();

        /**
         * 读写分离配置。
         */
        private ReadWriteSeparationConfig readWriteSeparation = new ReadWriteSeparationConfig();

        @Data
        public static class DataSourceInstanceConfig {
            private @Nullable String url;
            private @Nullable String username;
            private @Nullable String password;
            private @Nullable String driverClassName;
            private boolean lazyInit = false;
            private Map<String, Object> poolConfig = new HashMap<>();
        }

        @Data
        public static class ReadWriteSeparationConfig {
            private boolean enabled = false;
            private List<String> readDatasources = new ArrayList<>();
            private @Nullable String writeDatasource;
            private LoadBalanceConfig loadBalance = new LoadBalanceConfig();
        }

        @Data
        public static class LoadBalanceConfig {
            private LoadBalanceStrategyType strategy = LoadBalanceStrategyType.ROUND_ROBIN;
            private long healthCheckInterval = 30000L;
            private boolean healthCheckEnabled = true;
            private Map<String, Integer> weights = new HashMap<>();
        }

        /**
         * 负载均衡策略类型。
         */
        public enum LoadBalanceStrategyType {
            ROUND_ROBIN,
            WEIGHTED,
            LEAST_CONNECTIONS
        }
    }

    // ========== 安全配置类 ==========

    /**
     * 安全配置类。
     */
    @Data
    public static class SecurityConfig {

        /**
         * XSS 防护配置。
         */
        private XssConfig xss = new XssConfig();

        /**
         * SQL 注入防护配置。
         */
        private SqlInjectionConfig sqlInjection = new SqlInjectionConfig();

        /**
         * 敏感数据脱敏配置。
         */
        private SensitiveDataConfig sensitiveData = new SensitiveDataConfig();

        /**
         * 安全头配置。
         */
        private SecurityHeaderConfig securityHeader = new SecurityHeaderConfig();

        /**
         * 签名验证配置。
         */
        private SignatureConfig signature = new SignatureConfig();

        @Data
        public static class XssConfig {
            private boolean enabled = true;
            private boolean richTextMode;
            private Set<String> allowedTags = Set.of(
                    "p", "br", "b", "i", "u", "strong", "em", "h1", "h2", "h3", "h4", "h5", "h6",
                    "ul", "ol", "li", "a", "img", "span", "div");
            private Set<String> allowedAttributes = Set.of("href", "src", "alt", "title", "class", "id", "target");
        }

        @Data
        public static class SqlInjectionConfig {
            private boolean enabled = true;
            private boolean rejectOnDetection = true;
            private boolean logDetection = true;
        }

        @Data
        public static class SensitiveDataConfig {
            private boolean enabled = true;
            private Set<String> sensitiveFields = Set.of(
                    "password", "pwd", "secret", "token", "apiKey", "api_key",
                    "creditCard", "credit_card", "idCard", "id_card", "phone", "mobile", "email", "address");
            private char maskChar = '*';
            private int keepPrefix = 3;
            private int keepSuffix = 4;
        }

        @Data
        public static class SecurityHeaderConfig {
            private String contentSecurityPolicy = "default-src 'self'";
            private String strictTransportSecurity = "max-age=31536000; includeSubDomains";
            private String frameOptions = "DENY";
            private String contentTypeOptions = "nosniff";
            private String xssProtection = "1; mode=block";
        }

        @Data
        public static class SignatureConfig {
            private boolean enabled = true;
            private String defaultKeyId = "default";
            private Map<String, SignatureKeyConfig> keys = new HashMap<>();
            private int timestampTolerance = 300;
            private int nonceCacheSize = 10000;
            private boolean nonceRequired = true;
            private SignatureAlgorithm defaultAlgorithm = SignatureAlgorithm.HMAC_SHA256;

            @Data
            public static class SignatureKeyConfig {
                private String secret;
                private boolean enabled = true;
                private @Nullable String description;
                private Set<String> allowedPaths = new java.util.HashSet<>();
                private Set<String> allowedScopes = new java.util.HashSet<>();

                /**
                 * 检查路径是否被允许
                 *
                 * @param path       请求路径
                 * @param pathMatcher 路径匹配器
                 * @return 是否允许
                 */
                public boolean isPathAllowed(String path, org.springframework.util.PathMatcher pathMatcher) {
                    if (allowedPaths.isEmpty()) {
                        return true; // 未配置时默认允许所有路径
                    }
                    for (String allowedPath : allowedPaths) {
                        if (pathMatcher.match(allowedPath, path)) {
                            return true;
                        }
                    }
                    return false;
                }
            }
        }

        /**
         * 签名算法。
         */
        public enum SignatureAlgorithm {
            HMAC_SHA256,
            HMAC_SHA512
        }
    }

    // ========== 健康检查配置类 ==========

    /**
     * 健康检查配置类。
     */
    @Data
    public static class HealthConfig {

        /**
         * 是否启用存活探针。
         */
        private boolean livenessEnabled = true;

        /**
         * 是否启用就绪探针。
         */
        private boolean readinessEnabled = true;

        /**
         * 是否启用深度检查。
         */
        private boolean deepEnabled = true;

        /**
         * 存活探针配置。
         */
        private LivenessConfig liveness = new LivenessConfig();

        /**
         * 就绪探针配置。
         */
        private ReadinessConfig readiness = new ReadinessConfig();

        /**
         * 深度检查配置。
         */
        private DeepConfig deep = new DeepConfig();

        /**
         * 数据源健康检查配置。
         */
        private DataSourceHealthConfig datasource = new DataSourceHealthConfig();

        @Data
        public static class LivenessConfig {
            private boolean deadlockDetectionEnabled = true;
            private Duration deadlockDetectionTimeout = Duration.ofSeconds(5);
            private boolean memoryCheckEnabled = true;
            private int memoryWarningThreshold = 80;
            private int memoryCriticalThreshold = 95;
        }

        @Data
        public static class ReadinessConfig {
            private boolean databaseCheckEnabled = true;
            private Duration databaseCheckTimeout = Duration.ofSeconds(3);
            private boolean redisCheckEnabled = true;
            private Duration redisCheckTimeout = Duration.ofSeconds(3);
            private boolean moduleCheckEnabled = true;
            private Set<String> requiredModules = Set.of();
        }

        @Data
        public static class DeepConfig {
            private Duration timeout = Duration.ofSeconds(10);
            private boolean externalServiceCheckEnabled = true;
            private boolean configCenterCheckEnabled = true;
            private Duration externalServiceTimeout = Duration.ofSeconds(5);
        }

        @Data
        public static class DataSourceHealthConfig {
            private boolean enabled = true;
            private String validationQuery = "SELECT 1";
            private int poolUsageWarningThreshold = 70;
            private int poolUsageCriticalThreshold = 90;
            private int threadsAwaitingWarningThreshold = 5;
            private int threadsAwaitingCriticalThreshold = 10;
            private int connectionTimeout = 3000;
        }
    }

    // ========== 限流配置类 ==========

    /**
     * 限流配置类。
     */
    @Data
    public static class RateLimitConfig {

        /**
         * 是否启用限流。
         */
        private boolean enabled = true;

        /**
         * 默认每秒请求数。
         */
        private long defaultRate = 10;

        /**
         * 默认突发容量。
         */
        private long defaultBurst = 0;

        /**
         * 默认限流算法。
         */
        private RateLimitAlgorithm defaultAlgorithm = RateLimitAlgorithm.TOKEN_BUCKET;

        /**
         * Redis key 前缀。
         */
        private String keyPrefix = "rateLimit";

        /**
         * 存储类型。
         */
        private String storageType = "local";

        /**
         * 回退配置。
         */
        private RateLimitFallbackConfig fallback = new RateLimitFallbackConfig();

        /**
         * 白名单配置。
         */
        private RateLimitWhitelistConfig whitelist = new RateLimitWhitelistConfig();

        /**
         * 响应头配置。
         */
        private RateLimitResponseHeadersConfig responseHeaders = new RateLimitResponseHeadersConfig();

        /**
         * 本地限流配置。
         */
        private RateLimitLocalConfig local = new RateLimitLocalConfig();

        /**
         * 限流算法枚举。
         */
        public enum RateLimitAlgorithm {
            TOKEN_BUCKET,
            SLIDING_WINDOW,
            FIXED_WINDOW,
            LEAKY_BUCKET
        }

        @Data
        public static class RateLimitFallbackConfig {
            private boolean enabled = true;
            private String defaultMessage = "请求过于频繁，请稍后再试";
            private FailureMode failureMode = FailureMode.ALLOW;
        }

        @Data
        public static class RateLimitWhitelistConfig {
            private boolean enabled = true;
            private List<String> ips = new ArrayList<>();
            private List<Long> userIds = new ArrayList<>();
            private List<String> usernames = new ArrayList<>();
            private List<Long> tenantIds = new ArrayList<>();
            private @Nullable String customCheckerBean;
        }

        @Data
        public static class RateLimitResponseHeadersConfig {
            private boolean enabled = true;
            private String limitHeader = "X-RateLimit-Limit";
            private String remainingHeader = "X-RateLimit-Remaining";
            private String resetHeader = "X-RateLimit-Reset";
            private String retryAfterHeader = "Retry-After";
        }

        @Data
        public static class RateLimitLocalConfig {
            private boolean enabled = false;
            private int cacheSize = 10000;
            private long expireAfterSeconds = 3600;
        }

        /**
         * 维度配置映射。
         * key 为维度名称（ip, user, tenant, api）。
         */
        private Map<String, DimensionConfig> dimensions = new HashMap<>();

        /**
         * 维度配置类。
         */
        @Data
        public static class DimensionConfig {
            /**
             * 每秒请求数。
             */
            private long rate;

            /**
             * 突发容量。
             */
            private long burst;

            /**
             * 时间窗口大小（秒）。
             */
            private long windowSize;

            /**
             * 限流算法。
             */
            private @Nullable RateLimitAlgorithm algorithm;
        }

        /**
         * 故障模式枚举。
         */
        public enum FailureMode {
            ALLOW,
            REJECT
        }
    }

    // ========== 调度器配置类 ==========

    /**
     * 调度器配置类。
     */
    @Data
    public static class SchedulerConfig {

        /**
         * 是否启用调度器。
         */
        private boolean enabled = true;

        /**
         * 默认任务超时时间。
         */
        private Duration defaultTimeout = Duration.ofMinutes(30);

        /**
         * 默认重试次数。
         */
        private int defaultRetryAttempts = 3;

        /**
         * 默认重试延迟。
         */
        private Duration defaultRetryDelay = Duration.ofSeconds(1);

        /**
         * 重试延迟倍数。
         */
        private double retryMultiplier = 2.0;

        /**
         * 任务线程池大小。
         */
        private int threadPoolSize = Runtime.getRuntime().availableProcessors();

        /**
         * 任务执行日志存储配置。
         */
        private LogStorageConfig logStorage = new LogStorageConfig();

        /**
         * 监控指标配置。
         */
        private SchedulerMetricsConfig metrics = new SchedulerMetricsConfig();

        /**
         * 动态任务配置。
         */
        private DynamicTaskConfig dynamicTask = new DynamicTaskConfig();

        /**
         * 注解处理配置。
         */
        private SchedulerAnnotationConfig annotations = new SchedulerAnnotationConfig();

        /**
         * 管理 API 配置。
         */
        private SchedulerApiConfig api = new SchedulerApiConfig();

        @Data
        public static class LogStorageConfig {
            private String type = "memory";
            private int maxSize = 10000;
            private Duration retention = Duration.ofDays(7);
            private boolean logSuccess = true;
            private boolean logErrorStack = true;
        }

        @Data
        public static class SchedulerMetricsConfig {
            private boolean enabled = true;
            private String prefix = "afg.scheduler";
            private Map<String, String> tags = new HashMap<>();
            private boolean recordDurationHistogram = true;
        }

        @Data
        public static class DynamicTaskConfig {
            private boolean enabled = false;
            private String sourceType = "config-center";
            private Duration refreshInterval = Duration.ofMinutes(1);
            private String configPrefix = "afg.tasks";
        }

        @Data
        public static class SchedulerAnnotationConfig {
            private boolean enabled = true;
        }

        @Data
        public static class SchedulerApiConfig {
            private boolean enabled = false;
            private String basePath = "/afg/scheduler";
        }
    }

    // ========== 追踪配置类 ==========

    /**
     * 追踪配置类。
     */
    @Data
    public static class TracingConfig {

        /**
         * 是否启用追踪功能。
         */
        private boolean enabled = true;

        /**
         * 注解相关配置。
         */
        private TracingAnnotationConfig annotations = new TracingAnnotationConfig();

        /**
         * 采样配置。
         */
        private SamplingConfig sampling = new SamplingConfig();

        /**
         * Baggage 配置。
         */
        private BaggageConfig baggage = new BaggageConfig();

        /**
         * 传播配置。
         */
        private PropagationConfig propagation = new PropagationConfig();

        /**
         * Zipkin 配置。
         */
        private ZipkinConfig zipkin = new ZipkinConfig();

        /**
         * Jaeger 配置。
         */
        private JaegerConfig jaeger = new JaegerConfig();

        /**
         * 采样策略枚举。
         */
        public enum SamplingStrategy {
            PROBABILITY,
            RATE_LIMITING,
            ALWAYS,
            NEVER
        }

        @Data
        public static class TracingAnnotationConfig {
            private boolean enabled = true;
        }

        @Data
        public static class SamplingConfig {
            private SamplingStrategy strategy = SamplingStrategy.PROBABILITY;
            private double probability = 1.0;
            private int rate = 100;
        }

        @Data
        public static class BaggageConfig {
            private boolean enabled = true;
            private List<String> remoteFields = List.of("tenantId", "userId", "traceId");
            private List<String> localFields = List.of();
            private Map<String, String> fieldMappings = new HashMap<>();
        }

        @Data
        public static class PropagationConfig {
            private boolean enabled = true;
            private boolean threadPoolEnabled = true;
        }

        @Data
        public static class ZipkinConfig {
            private boolean enabled = false;
            private String endpoint = "http://localhost:9411/api/v2/spans";
            private int connectTimeout = 5000;
            private int readTimeout = 10000;
            private boolean compressionEnabled = true;
            private int sendInterval = 5000;
        }

        @Data
        public static class JaegerConfig {
            private boolean enabled = false;
            private @Nullable String endpoint = "http://localhost:14268/api/traces";
            private @Nullable String otlpEndpoint;
            private boolean useOtlp = true;
            private int connectTimeout = 5000;
            private int readTimeout = 10000;
        }
    }

    // ========== 日志配置类 ==========

    /**
     * 日志配置类。
     */
    @Data
    public static class LoggingConfig {

        /**
         * 是否启用敏感信息脱敏。
         */
        private boolean maskSensitive = true;

        /**
         * MDC 配置。
         */
        private MdcConfig mdc = new MdcConfig();

        /**
         * 结构化日志配置。
         */
        private StructuredConfig structured = new StructuredConfig();

        /**
         * 日志文件配置。
         */
        private LogFileConfig file = new LogFileConfig();

        /**
         * 异步日志配置。
         */
        private AsyncLogConfig async = new AsyncLogConfig();

        @Data
        public static class MdcConfig {
            private boolean enabled = true;
            private String[] fields = {"traceId", "tenantId", "userId", "requestPath"};
        }

        @Data
        public static class StructuredConfig {
            private boolean enabled;
            private boolean prettyPrint;
        }

        @Data
        public static class LogFileConfig {
            private String path = "./logs";
            private String maxSize = "100MB";
            private int maxHistory = 30;
            private String totalSizeCap = "10GB";
        }

        @Data
        public static class AsyncLogConfig {
            private int queueSize = 512;
            private int discardingThreshold = 0;
            private boolean includeCallerData = true;
            private boolean neverBlock = true;
        }
    }

    // ========== 指标配置类 ==========

    /**
     * 指标配置类。
     */
    @Data
    public static class MetricsConfig {

        /**
         * 是否启用指标功能。
         */
        private boolean enabled = true;

        /**
         * 是否启用注解驱动的指标。
         */
        private boolean annotationsEnabled = true;

        /**
         * 通用标签。
         */
        private @Nullable Map<String, String> tags;

        /**
         * Histogram 配置。
         */
        private HistogramConfig histogram = new HistogramConfig();

        @Data
        public static class HistogramConfig {
            private boolean enabled = true;
            private boolean percentileHistogram = true;
            private double[] percentiles = {0.5, 0.95, 0.99};
            private Duration minimumExpectedValue = Duration.ofMillis(1);
            private Duration maximumExpectedValue = Duration.ofSeconds(10);
        }
    }

    // ========== 虚拟线程配置类 ==========

    /**
     * 虚拟线程配置类。
     */
    @Data
    public static class VirtualThreadConfig {

        /**
         * 是否启用虚拟线程。
         */
        private boolean enabled = true;

        /**
         * 虚拟线程名前缀。
         */
        private String namePrefix = "afg-vt-";
    }

    // ========== 审计配置类 ==========

    /**
     * 审计配置类。
     */
    @Data
    public static class AuditConfig {

        /**
         * 是否启用审计日志。
         */
        private boolean enabled = true;

        /**
         * 存储类型。
         */
        private StorageType storageType = StorageType.LOG;

        /**
         * 最大保留条数。
         */
        private int maxSize = 10000;

        /**
         * 日志保留时间。
         */
        private Duration ttl = Duration.ofDays(7);

        /**
         * 是否多租户模式。
         */
        private boolean multiTenant = true;

        /**
         * 默认敏感字段列表。
         */
        private String[] sensitiveFields = {"password", "token", "secret", "apikey", "credential", "accesstoken"};

        /**
         * 存储类型枚举。
         */
        public enum StorageType {
            REDIS,
            DATABASE,
            LOG,
            NONE
        }
    }

    // ========== 批量操作配置类 ==========

    /**
     * 批量操作配置类。
     */
    @Data
    public static class BatchConfig {

        /**
         * 默认批次大小。
         */
        private int defaultBatchSize = 100;

        /**
         * 默认并行度。
         */
        private int defaultParallelism = 0;

        /**
         * 错误容忍率。
         */
        private double errorTolerance = 1.0;

        /**
         * 是否在遇到错误时立即停止。
         */
        private boolean stopOnError = false;

        /**
         * 重试配置。
         */
        private BatchRetryConfig retry = new BatchRetryConfig();

        /**
         * 限流配置。
         */
        private BatchRateLimitConfig rateLimit = new BatchRateLimitConfig();

        @Data
        public static class BatchRetryConfig {
            private boolean enabled = false;
            private int maxAttempts = 3;
            private long initialInterval = 1000;
            private double multiplier = 2.0;
            private long maxInterval = 10000;
        }

        @Data
        public static class BatchRateLimitConfig {
            private boolean enabled = false;
            private int permitsPerSecond = 100;
            private long maxWaitMillis = 5000;
        }

        /**
         * 获取实际并行度。
         */
        public int getActualParallelism() {
            return defaultParallelism > 0 ? defaultParallelism : Runtime.getRuntime().availableProcessors();
        }
    }

    // ========== HTTP 客户端配置类 ==========

    /**
     * HTTP 客户端配置类。
     */
    @Data
    public static class HttpClientConfig {

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
        private HttpRetryConfig retry = new HttpRetryConfig();

        /**
         * 熔断器配置。
         */
        private HttpCircuitBreakerConfig circuitBreaker = new HttpCircuitBreakerConfig();

        @Data
        public static class HttpRetryConfig {
            private boolean enabled = true;
            private int maxAttempts = 3;
            private long initialInterval = 1000;
            private double multiplier = 2.0;
            private long maxInterval = 10000;
            private Set<Integer> retryOnStatus = Set.of(502, 503, 504);
        }

        @Data
        public static class HttpCircuitBreakerConfig {
            private boolean enabled = true;
            private int failureThreshold = 5;
            private long openDuration = 30000;
            private int halfOpenMaxCalls = 3;
            private int successThreshold = 3;
        }
    }

    // ========== 云原生配置类 ==========

    /**
     * 云原生配置类。
     */
    @Data
    public static class CloudNativeConfig {

        /**
         * Kubernetes 配置。
         */
        private KubernetesConfig kubernetes = new KubernetesConfig();

        /**
         * 优雅停机配置。
         */
        private GracefulShutdownConfig gracefulShutdown = new GracefulShutdownConfig();

        /**
         * 配置外部化。
         */
        private ConfigExternalizationConfig configExternalization = new ConfigExternalizationConfig();

        /**
         * 探针配置。
         */
        private ProbeConfig probe = new ProbeConfig();

        @Data
        public static class KubernetesConfig {
            private boolean enabled = true;
            private @Nullable String namespace;
            private @Nullable String serviceAccount;
            private @Nullable String podName;
            private @Nullable String podIp;
            private @Nullable String nodeName;
        }

        @Data
        public static class GracefulShutdownConfig {
            private boolean enabled = true;
            private Duration timeout = Duration.ofSeconds(30);
            private boolean waitForRequests = true;
            private Duration requestWaitTimeout = Duration.ofSeconds(10);
        }

        @Data
        public static class ConfigExternalizationConfig {
            private boolean enabled = true;
            private @Nullable String configMap;
            private @Nullable String secret;
            private boolean autoRefresh = true;
            private Duration refreshInterval = Duration.ofMinutes(1);
        }

        @Data
        public static class ProbeConfig {
            private boolean enabled = true;
            private ProbeDetailConfig liveness = new ProbeDetailConfig();
            private ProbeDetailConfig readiness = new ProbeDetailConfig();
            private ProbeDetailConfig startup = new ProbeDetailConfig();
        }

        @Data
        public static class ProbeDetailConfig {
            private String path = "/health/default";
            private Duration initialDelay = Duration.ofSeconds(10);
            private Duration period = Duration.ofSeconds(10);
            private Duration timeout = Duration.ofSeconds(5);
            private int successThreshold = 1;
            private int failureThreshold = 3;
        }
    }

    // ========== 功能开关配置类 ==========

    /**
     * 功能开关配置类。
     */
    @Data
    public static class FeatureFlagConfig {

        /**
         * 是否启用功能开关。
         */
        private boolean enabled = true;

        /**
         * 存储类型。
         */
        private StorageType storageType = StorageType.MEMORY;

        /**
         * Redis 配置。
         */
        private FeatureRedisConfig redis = new FeatureRedisConfig();

        /**
         * 默认灰度策略。
         */
        private GrayscaleStrategy defaultStrategy = GrayscaleStrategy.ALL;

        /**
         * 功能开关缓存过期时间（秒）。
         */
        private long cacheExpireSeconds = 60;

        /**
         * 是否启用本地缓存。
         */
        private boolean localCacheEnabled = true;

        /**
         * 本地缓存最大大小。
         */
        private int localCacheMaxSize = 1000;

        /**
         * 存储类型枚举。
         */
        public enum StorageType {
            MEMORY,
            REDIS,
            REDISSON
        }

        /**
         * 灰度策略枚举。
         */
        public enum GrayscaleStrategy {
            ALL,
            PERCENTAGE,
            USER_ID,
            TENANT_ID
        }

        @Data
        public static class FeatureRedisConfig {
            private String keyPrefix = "afg:feature:";
            private String flagsMapKey = "flags";
            private String rulesMapKey = "rules";
        }
    }

    // ========== 加密配置类 ==========

    /**
     * 加密配置类。
     */
    @Data
    public static class EncryptionConfig {

        /**
         * 是否启用配置加密。
         */
        private boolean enabled;

        /**
         * 加密算法。
         */
        private String algorithm = "AES-256-GCM";

        /**
         * 加密密钥。
         */
        private @Nullable String secretKey;

        /**
         * 加密值前缀。
         */
        private String prefix = "ENC(";

        /**
         * 加密值后缀。
         */
        private String suffix = ")";
    }

    // ========== 数据权限配置类 ==========

    /**
     * 数据权限配置类。
     */
    @Data
    public static class DataScopeConfig {

        /**
         * 是否启用数据权限。
         */
        private boolean enabled = true;

        /**
         * 部门表名。
         */
        private String deptTable = "sys_dept";

        /**
         * 部门表 ID 字段名。
         */
        private String deptIdColumn = "id";

        /**
         * 部门表父级 ID 字段名。
         */
        private String deptParentColumn = "parent_id";

        /**
         * 默认数据范围类型。
         */
        private DataScopeType defaultScopeType = DataScopeType.DEPT;

        /**
         * 用户 ID 字段名。
         */
        private String userIdColumn = "create_by";

        /**
         * 是否缓存部门层级关系。
         */
        private boolean cacheDeptHierarchy = true;

        /**
         * 部门层级缓存过期时间（秒）。
         */
        private long cacheExpireSeconds = 300;

        /**
         * 是否在无上下文时忽略权限过滤。
         */
        private boolean ignoreWhenNoContext = false;

        /**
         * 需要忽略数据权限的表名列表。
         */
        private String[] ignoreTables = {};

        /**
         * 需要忽略数据权限的 Mapper 方法列表。
         */
        private String[] ignoreMethods = {};

        /**
         * 数据范围类型枚举。
         */
        public enum DataScopeType {
            ALL,
            SELF,
            DEPT,
            DEPT_AND_CHILD,
            CUSTOM
        }
    }

    // ========== 访问日志配置类 ==========

    /**
     * 访问日志配置类。
     */
    @Data
    public static class AccessLogConfig {

        /**
         * 是否启用访问日志。
         */
        private boolean enabled = true;

        /**
         * 排除路径列表（支持 Ant 风格模式）。
         */
        private java.util.List<String> excludePaths = new java.util.ArrayList<>(java.util.List.of("/health", "/actuator/**"));

        /**
         * 是否包含查询字符串。
         */
        private boolean includeQueryString = true;

        /**
         * 是否包含客户端 IP。
         */
        private boolean includeClientIp = true;

        /**
         * 慢请求阈值（毫秒），超过此值将在日志中标记 SLOW。
         */
        private long slowRequestThreshold = 3000;
    }

    // ========== 参数校验配置类 ==========

    /**
     * 参数校验配置类。
     */
    @Data
    public static class ValidationConfig {

        /**
         * 是否启用 Bean Validation（含统一异常处理）。
         */
        private boolean enabled = true;

        /**
         * 是否在错误响应中包含字段错误详情。
         */
        private boolean includeFieldErrors = true;

        /**
         * 参数校验失败时的默认错误消息。
         */
        private String defaultErrorMessage = "参数校验失败";
    }

    // ========== 防重复提交配置类 ==========

    /**
     * 防重复提交配置类。
     */
    @Data
    public static class DuplicateSubmitConfig {

        /**
         * 是否启用防重复提交。
         */
        private boolean enabled = true;

        /**
         * 去重键前缀。
         */
        private String keyPrefix = "afg:duplicate-submit";

        /**
         * 默认去重间隔（毫秒）。
         */
        private long defaultInterval = 3000;

        /**
         * 注解相关配置。
         */
        private DuplicateSubmitAnnotationConfig annotations = new DuplicateSubmitAnnotationConfig();

        @Data
        public static class DuplicateSubmitAnnotationConfig {
            private boolean enabled = true;
        }
    }

    // ========== 优雅关闭配置类 ==========

    /**
     * 优雅关闭配置类。
     */
    @Data
    public static class ShutdownConfig {

        /**
         * 是否启用优雅关闭。
         */
        private boolean enabled = true;

        /**
         * 关闭超时时间。
         */
        private Duration timeout = Duration.ofSeconds(30);

        /**
         * 是否启用同一阶段内相同 order 的回调并行执行。
         */
        private boolean parallelExecutionEnabled;

        /**
         * 关闭阶段配置。
         */
        private List<ShutdownPhase> phases = new ArrayList<>();

        /**
         * 关闭阶段配置。
         */
        @Data
        public static class ShutdownPhase {
            private String name;
            private Duration timeout;

            public ShutdownPhase() {}

            public ShutdownPhase(String name, Duration timeout) {
                this.name = name;
                this.timeout = timeout;
            }
        }
    }

    // ========== SSE 配置类 ==========

    /**
     * SSE 配置类。
     */
    @Data
    public static class SseConfig {

        /**
         * 是否启用 SSE 功能。
         */
        private boolean enabled = true;

        /**
         * SSE 连接超时时间（毫秒）。
         * 默认 5 分钟。
         */
        private long timeout = 300000;

        /**
         * 最大并发连接数。
         */
        private int maxConnections = 1000;

        /**
         * 心跳间隔（毫秒）。
         * 0 表示禁用心跳。
         */
        private long heartbeatInterval = 30000;
    }

    // ========== ID 生成器配置类 ==========

    /**
     * ID 生成器配置类。
     */
    @Data
    public static class IdGeneratorConfig {

        /**
         * 是否启用 ID 生成器。
         */
        private boolean enabled = true;

        /**
         * ID 生成器类型。
         */
        private IdGeneratorType type = IdGeneratorType.SNOWFLAKE;

        /**
         * Snowflake 配置。
         */
        private SnowflakeConfig snowflake = new SnowflakeConfig();

        /**
         * ID 生成器类型枚举。
         */
        public enum IdGeneratorType {
            SNOWFLAKE,
            SEGMENT,
            UUID
        }

        /**
         * Snowflake 配置。
         */
        @Data
        public static class SnowflakeConfig {

            /**
             * 机器 ID（0-31）。
             */
            private long workerId = 1;

            /**
             * 数据中心 ID（0-31）。
             */
            private long datacenterId = 1;

            /**
             * 起始纪元（毫秒）。
             * 默认使用 Twitter 纪元：2010-11-04 09:42:54 UTC。
             */
            private long twepoch = 1288834974657L;

            /**
             * 最大容忍时钟回拨（毫秒）。
             * 回拨超过此值将抛出异常。
             */
            private long maxTolerateClockSkewMs = 5;
        }
    }

    // ========== 通知配置类 ==========

    /**
     * 通知配置类。
     */
    @Data
    public static class NotificationConfig {

        /**
         * 是否启用通知服务。
         */
        private boolean enabled = true;

        /**
         * 默认通知渠道。
         */
        private NotificationChannel defaultChannel = NotificationChannel.EMAIL;

        /**
         * 是否记录通知日志（使用 LogNotificationService 时自动生效）。
         */
        private boolean logNotifications = true;

        /**
         * 重试次数。
         */
        private int retryCount = 3;

        /**
         * 重试间隔（毫秒）。
         */
        private long retryIntervalMs = 1000;

        /**
         * 通知渠道枚举。
         */
        public enum NotificationChannel {
            EMAIL, SMS, IN_APP, WEBHOOK, DINGTALK, FEISHU, WECOM
        }
    }

    // ========== Webhook 配置类 ==========

    /**
     * Webhook 配置类。
     */
    @Data
    public static class WebhookConfig {

        /**
         * 是否启用 Webhook 功能。
         */
        private boolean enabled = true;

        /**
         * 连接超时时间（毫秒）。
         */
        private int connectTimeout = 5000;

        /**
         * 读取超时时间（毫秒）。
         */
        private int readTimeout = 10000;

        /**
         * 最大重试次数。
         */
        private int maxRetries = 3;

        /**
         * 重试间隔（毫秒）。
         */
        private long retryIntervalMs = 1000;

        /**
         * 签名算法。
         */
        private String signatureAlgorithm = "HmacSHA256";

        /**
         * 签名头名称。
         */
        private String signatureHeader = "X-Webhook-Signature";
    }

    // ========== 状态机配置类 ==========

    /**
     * 状态机配置类。
     */
    @Data
    public static class StateMachineConfig {

        /**
         * 是否启用状态机功能。
         */
        private boolean enabled = true;

        /**
         * 严格模式。
         * <p>
         * 启用时，非法状态转换抛出
         * {@link io.github.afgprojects.framework.core.statemachine.exception.InvalidTransitionException}；
         * 禁用时，非法转换静默忽略。
         * </p>
         */
        private boolean strictMode = true;
    }

    // ========== 导入导出配置类 ==========

    /**
     * 导入导出配置类。
     */
    @Data
    public static class ImportExportConfig {

        /**
         * 是否启用导入导出功能。
         */
        private boolean enabled = true;

        /**
         * 默认导出格式（csv / excel）。
         */
        private String defaultFormat = "csv";

        /**
         * 默认字符编码。
         */
        private String defaultCharset = "UTF-8";

        /**
         * 最大导入行数（防止内存溢出）。
         */
        private int maxImportRows = 10000;
    }

    // ========== 枚举管理配置类 ==========

    /**
     * 枚举管理配置类。
     */
    @Data
    public static class EnumManagementConfig {

        /**
         * 是否启用枚举管理功能。
         */
        private boolean enabled = true;

        /**
         * 是否暴露 REST 端点。
         */
        private boolean exposeEndpoint = true;

        /**
         * REST 端点路径。
         */
        private String endpointPath = "/afg/enums";
    }
}
