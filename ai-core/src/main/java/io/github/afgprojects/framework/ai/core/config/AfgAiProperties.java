package io.github.afgprojects.framework.ai.core.config;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * AFG AI 统一配置属性。
 *
 * <p>整合了对话客户端、智能体、模型管理、工作流、管道、RAG、持久化、韧性、性能、安全、
 * 可观测性、ETL、工具、技能、应用管理等所有 AI 模块配置。
 *
 * <h3>配置示例</h3>
 * <pre>
 * afg:
 *   ai:
 *     enabled: true
 *     chat:
 *       enabled: true
 *       default-name: default
 *       default-system-prompt: "You are a helpful assistant."
 *       memory:
 *         max-messages: 20
 *     agent:
 *       enabled: true
 *       max-iterations: 10
 *       timeout-ms: 30000
 *     model:
 *       enabled: true
 *       default-type: CHAT
 *     workflow:
 *       enabled: true
 *       max-parallel-nodes: 10
 *       checkpoint-policy: EVERY_STAGE
 *     pipeline:
 *       enabled: true
 *     rag:
 *       enabled: true
 *       embedding-dimensions: 1536
 *       search-mode: BLEND
 *       similarity-threshold: 0.7
 *       top-n: 5
 *     persistence:
 *       enabled: true
 *       session:
 *         max-sessions-per-user: 100
 *         table-name: ai_agent_session
 *       message-history:
 *         max-messages-per-session: 1000
 *         table-name: ai_message_history
 *     resilience:
 *       enabled: true
 *       retry:
 *         max-retries: 3
 *         initial-interval-ms: 1000
 *         multiplier: 2.0
 *         jitter-factor: 0.5
 *       circuit-breaker:
 *         window-size: 100
 *         failure-rate-threshold: 0.5
 *         open-state-timeout-ms: 30000
 *     performance:
 *       enabled: true
 *       cache:
 *         max-size: 1000
 *         ttl-seconds: 300
 *       rate-limit:
 *         default-permits: 10
 *         window-seconds: 60
 *     security:
 *       enabled: true
 *       api-key:
 *         key-prefix: "afg:ai:apikey"
 *       content-safety:
 *         enabled: true
 *       pii:
 *         enabled: true
 *     observability:
 *       enabled: true
 *       audit:
 *         enabled: true
 *         max-entries: 10000
 *       metrics:
 *         enabled: true
 *       tracing:
 *         enabled: true
 *     etl:
 *       enabled: true
 *       splitter:
 *         chunk-size: 500
 *         chunk-overlap: 50
 *     tool:
 *       enabled: true
 *       persistent-enabled: false
 *       discovery:
 *         enabled: false
 *       security:
 *         enabled: false
 *     skill:
 *       enabled: true
 *       persistent-enabled: false
 *     application:
 *       enabled: true
 * </pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "afg.ai")
@SuppressWarnings("PMD.TooManyFields")
public class AfgAiProperties {

    /**
     * 是否启用 AFG AI 功能。
     * 默认启用。
     */
    private boolean enabled = true;

    // ========== 对话客户端配置 ==========

    /**
     * 对话客户端配置。
     */
    private ChatConfig chat = new ChatConfig();

    // ========== 智能体配置 ==========

    /**
     * 智能体配置。
     */
    private AgentConfig agent = new AgentConfig();

    // ========== 模型管理配置 ==========

    /**
     * 模型管理配置。
     */
    private ModelConfig model = new ModelConfig();

    // ========== 工作流配置 ==========

    /**
     * 工作流配置。
     */
    private WorkflowConfig workflow = new WorkflowConfig();

    // ========== 对话管道配置 ==========

    /**
     * 对话管道配置。
     */
    private PipelineConfig pipeline = new PipelineConfig();

    // ========== RAG 配置 ==========

    /**
     * RAG 配置。
     */
    private RagConfig rag = new RagConfig();

    // ========== 持久化配置 ==========

    /**
     * 持久化配置。
     */
    private PersistenceConfig persistence = new PersistenceConfig();

    // ========== 韧性配置 ==========

    /**
     * 韧性配置。
     */
    private ResilienceConfig resilience = new ResilienceConfig();

    // ========== 性能配置 ==========

    /**
     * 性能配置。
     */
    private PerformanceConfig performance = new PerformanceConfig();

    // ========== 安全配置 ==========

    /**
     * 安全配置。
     */
    private SecurityConfig security = new SecurityConfig();

    // ========== 可观测性配置 ==========

    /**
     * 可观测性配置。
     */
    private ObservabilityConfig observability = new ObservabilityConfig();

    // ========== ETL 配置 ==========

    /**
     * ETL 配置。
     */
    private EtlConfig etl = new EtlConfig();

    // ========== 工具系统配置 ==========

    /**
     * 工具系统配置。
     */
    private ToolConfig tool = new ToolConfig();

    // ========== 技能系统配置 ==========

    /**
     * 技能系统配置。
     */
    private SkillConfig skill = new SkillConfig();

    // ========== 应用管理配置 ==========

    /**
     * 应用管理配置。
     */
    private ApplicationConfig application = new ApplicationConfig();

    // ========== 对话客户端配置类 ==========

    /**
     * 对话客户端配置类。
     */
    @Data
    public static class ChatConfig {

        /**
         * 是否启用对话客户端。
         */
        private boolean enabled = true;

        /**
         * 默认对话客户端名称。
         */
        private String defaultName = "default";

        /**
         * 默认系统提示词。
         */
        private @Nullable String defaultSystemPrompt;

        /**
         * 对话记忆配置。
         */
        private ChatMemoryConfig memory = new ChatMemoryConfig();

        /**
         * 对话记忆配置类。
         */
        @Data
        public static class ChatMemoryConfig {

            /**
             * 最大保留消息数。
             */
            private int maxMessages = 20;
        }
    }

    // ========== 智能体配置类 ==========

    /**
     * 智能体配置类。
     */
    @Data
    public static class AgentConfig {

        /**
         * 是否启用智能体。
         */
        private boolean enabled = true;

        /**
         * 最大迭代次数。
         */
        private int maxIterations = 10;

        /**
         * 超时时间（毫秒）。
         */
        private long timeoutMs = 30000L;

        /**
         * ReAct 执行器配置。
         */
        private ReActConfig reAct = new ReActConfig();

        /**
         * ReAct 执行器配置类。
         */
        @Data
        public static class ReActConfig {

            /**
             * 是否启用 ReAct 执行器。
             */
            private boolean enabled = true;

            /**
             * 最大推理步数。
             */
            private int maxSteps = 10;
        }
    }

    // ========== 模型管理配置类 ==========

    /**
     * 模型管理配置类。
     */
    @Data
    public static class ModelConfig {

        /**
         * 是否启用模型管理。
         */
        private boolean enabled = true;

        /**
         * 默认模型类型。
         */
        private ModelType defaultType = ModelType.CHAT;

        /**
         * 模型类型枚举。
         */
        public enum ModelType {
            CHAT,
            EMBEDDING,
            RERANK,
            IMAGE,
            AUDIO
        }
    }

    // ========== 工作流配置类 ==========

    /**
     * 工作流配置类。
     */
    @Data
    public static class WorkflowConfig {

        /**
         * 是否启用工作流。
         */
        private boolean enabled = true;

        /**
         * 最大并行节点数。
         */
        private int maxParallelNodes = 10;

        /**
         * 检查点策略。
         */
        private CheckpointPolicy checkpointPolicy = CheckpointPolicy.EVERY_STAGE;

        /**
         * 检查点策略枚举。
         */
        public enum CheckpointPolicy {
            EVERY_NODE,
            EVERY_STAGE,
            MANUAL,
            ON_INTERRUPT,
            NONE
        }
    }

    // ========== 对话管道配置类 ==========

    /**
     * 对话管道配置类。
     */
    @Data
    public static class PipelineConfig {

        /**
         * 是否启用对话管道。
         */
        private boolean enabled = true;
    }

    // ========== RAG 配置类 ==========

    /**
     * RAG 配置类。
     */
    @Data
    public static class RagConfig {

        /**
         * 是否启用 RAG。
         */
        private boolean enabled = true;

        /**
         * 向量维度。
         */
        private int embeddingDimensions = 1536;

        /**
         * 搜索模式：VECTOR、KEYWORD、BLEND。
         */
        private SearchMode searchMode = SearchMode.BLEND;

        /**
         * 相似度阈值（0.0 ~ 1.0）。
         */
        private double similarityThreshold = 0.7;

        /**
         * 返回结果数量。
         */
        private int topN = 5;

        /**
         * 搜索模式枚举。
         */
        public enum SearchMode {
            VECTOR,
            KEYWORD,
            BLEND
        }
    }

    // ========== 持久化配置类 ==========

    /**
     * 持久化配置类。
     */
    @Data
    public static class PersistenceConfig {

        /**
         * 是否启用持久化。
         */
        private boolean enabled = true;

        /**
         * 会话存储配置。
         */
        private SessionConfig session = new SessionConfig();

        /**
         * 消息历史配置。
         */
        private MessageHistoryConfig messageHistory = new MessageHistoryConfig();

        /**
         * 会话存储配置类。
         */
        @Data
        public static class SessionConfig {

            /**
             * 每用户最大会话数。
             */
            private int maxSessionsPerUser = 100;

            /**
             * 会话表名。
             */
            private String tableName = "ai_agent_session";
        }

        /**
         * 消息历史配置类。
         */
        @Data
        public static class MessageHistoryConfig {

            /**
             * 每会话最大消息数。
             */
            private int maxMessagesPerSession = 1000;

            /**
             * 消息历史表名。
             */
            private String tableName = "ai_message_history";
        }
    }

    // ========== 韧性配置类 ==========

    /**
     * 韧性配置类。
     */
    @Data
    public static class ResilienceConfig {

        /**
         * 是否启用韧性机制。
         */
        private boolean enabled = true;

        /**
         * 重试配置。
         */
        private RetryConfig retry = new RetryConfig();

        /**
         * 熔断器配置。
         */
        private CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig();

        /**
         * 重试配置类。
         */
        @Data
        public static class RetryConfig {

            /**
             * 最大重试次数。
             */
            private int maxRetries = 3;

            /**
             * 初始重试间隔（毫秒）。
             */
            private long initialIntervalMs = 1000;

            /**
             * 退避乘数。
             */
            private double multiplier = 2.0;

            /**
             * 最大重试间隔（毫秒）。
             */
            private long maxIntervalMs = 30000;

            /**
             * 抖动因子（0.0 ~ 1.0）。
             */
            private double jitterFactor = 0.5;
        }

        /**
         * 熔断器配置类。
         */
        @Data
        public static class CircuitBreakerConfig {

            /**
             * 熔断器名称。
             */
            private String name = "default";

            /**
             * 滑动窗口大小。
             */
            private int windowSize = 100;

            /**
             * 失败率阈值（0.0 ~ 1.0）。
             */
            private double failureRateThreshold = 0.5;

            /**
             * 半开状态最大探测次数。
             */
            private int halfOpenMaxCalls = 10;

            /**
             * 熔断开启状态超时时间（毫秒）。
             */
            private long openStateTimeoutMs = 30000;
        }
    }

    // ========== 性能配置类 ==========

    /**
     * 性能配置类。
     */
    @Data
    public static class PerformanceConfig {

        /**
         * 是否启用性能优化。
         */
        private boolean enabled = true;

        /**
         * 缓存配置。
         */
        private CacheConfig cache = new CacheConfig();

        /**
         * 限流配置。
         */
        private RateLimitConfig rateLimit = new RateLimitConfig();

        /**
         * 缓存配置类。
         */
        @Data
        public static class CacheConfig {

            /**
             * 缓存最大条目数。
             */
            private int maxSize = 1000;

            /**
             * 缓存过期时间（秒）。
             */
            private long ttlSeconds = 300;
        }

        /**
         * 限流配置类。
         */
        @Data
        public static class RateLimitConfig {

            /**
             * 默认许可数。
             */
            private int defaultPermits = 10;

            /**
             * 限流窗口时间（秒）。
             */
            private long windowSeconds = 60;
        }
    }

    // ========== 安全配置类 ==========

    /**
     * 安全配置类。
     */
    @Data
    public static class SecurityConfig {

        /**
         * 是否启用安全功能。
         */
        private boolean enabled = true;

        /**
         * API Key 配置。
         */
        private ApiKeyConfig apiKey = new ApiKeyConfig();

        /**
         * 内容安全配置。
         */
        private ContentSafetyConfig contentSafety = new ContentSafetyConfig();

        /**
         * PII 检测配置。
         */
        private PiiConfig pii = new PiiConfig();

        /**
         * API Key 配置类。
         */
        @Data
        public static class ApiKeyConfig {

            /**
             * API Key 存储前缀。
             */
            private String keyPrefix = "afg:ai:apikey";
        }

        /**
         * 内容安全配置类。
         */
        @Data
        public static class ContentSafetyConfig {

            /**
             * 是否启用内容安全检查。
             */
            private boolean enabled = true;
        }

        /**
         * PII 检测配置类。
         */
        @Data
        public static class PiiConfig {

            /**
             * 是否启用 PII 检测。
             */
            private boolean enabled = true;
        }
    }

    // ========== 可观测性配置类 ==========

    /**
     * 可观测性配置类。
     */
    @Data
    public static class ObservabilityConfig {

        /**
         * 是否启用可观测性。
         */
        private boolean enabled = true;

        /**
         * 审计日志配置。
         */
        private AuditConfig audit = new AuditConfig();

        /**
         * 指标配置。
         */
        private MetricsConfig metrics = new MetricsConfig();

        /**
         * 链路追踪配置。
         */
        private TracingConfig tracing = new TracingConfig();

        /**
         * 审计日志配置类。
         */
        @Data
        public static class AuditConfig {

            /**
             * 是否启用审计日志。
             */
            private boolean enabled = true;

            /**
             * 最大审计条目数。
             */
            private int maxEntries = 10000;
        }

        /**
         * 指标配置类。
         */
        @Data
        public static class MetricsConfig {

            /**
             * 是否启用指标采集。
             */
            private boolean enabled = true;
        }

        /**
         * 链路追踪配置类。
         */
        @Data
        public static class TracingConfig {

            /**
             * 是否启用链路追踪。
             */
            private boolean enabled = true;
        }
    }

    // ========== ETL 配置类 ==========

    /**
     * ETL 配置类。
     */
    @Data
    public static class EtlConfig {

        /**
         * 是否启用 ETL。
         */
        private boolean enabled = true;

        /**
         * 文档分割配置。
         */
        private SplitterConfig splitter = new SplitterConfig();

        /**
         * 文档分割配置类。
         */
        @Data
        public static class SplitterConfig {

            /**
             * 分块大小（字符数）。
             */
            private int chunkSize = 500;

            /**
             * 分块重叠（字符数）。
             */
            private int chunkOverlap = 50;
        }
    }

    // ========== 工具系统配置类 ==========

    /**
     * 工具系统配置类。
     */
    @Data
    public static class ToolConfig {

        /**
         * 是否启用工具系统。
         */
        private boolean enabled = true;

        /**
         * 是否启用持久化工具注册。
         */
        private boolean persistentEnabled = false;

        /**
         * 工具发现配置。
         */
        private DiscoveryConfig discovery = new DiscoveryConfig();

        /**
         * 工具安全配置。
         */
        private ToolSecurityConfig security = new ToolSecurityConfig();

        /**
         * 工具发现配置类。
         */
        @Data
        public static class DiscoveryConfig {

            /**
             * 是否启用远程工具发现。
             */
            private boolean enabled = false;
        }

        /**
         * 工具安全配置类。
         */
        @Data
        public static class ToolSecurityConfig {

            /**
             * 是否启用工具安全校验。
             */
            private boolean enabled = false;
        }
    }

    // ========== 技能系统配置类 ==========

    /**
     * 技能系统配置类。
     */
    @Data
    public static class SkillConfig {

        /**
         * 是否启用技能系统。
         */
        private boolean enabled = true;

        /**
         * 是否启用持久化技能注册。
         */
        private boolean persistentEnabled = false;
    }

    // ========== 应用管理配置类 ==========

    /**
     * 应用管理配置类。
     */
    @Data
    public static class ApplicationConfig {

        /**
         * 是否启用应用管理。
         */
        private boolean enabled = true;
    }
}
