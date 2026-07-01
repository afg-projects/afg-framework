package io.github.afgprojects.framework.ai.core.properties;

import io.github.afgprojects.framework.ai.core.properties.agent.AgentConfig;
import io.github.afgprojects.framework.ai.core.properties.application.ApplicationConfig;
import io.github.afgprojects.framework.ai.core.properties.chat.ChatConfig;
import io.github.afgprojects.framework.ai.core.properties.etl.EtlConfig;
import io.github.afgprojects.framework.ai.core.properties.model.ModelConfig;
import io.github.afgprojects.framework.ai.core.properties.observability.ObservabilityConfig;
import io.github.afgprojects.framework.ai.core.properties.performance.PerformanceConfig;
import io.github.afgprojects.framework.ai.core.properties.persistence.PersistenceConfig;
import io.github.afgprojects.framework.ai.core.properties.pipeline.PipelineConfig;
import io.github.afgprojects.framework.ai.core.properties.rag.RagConfig;
import io.github.afgprojects.framework.ai.core.properties.resilience.ResilienceConfig;
import io.github.afgprojects.framework.ai.core.properties.security.SecurityConfig;
import io.github.afgprojects.framework.ai.core.properties.skill.SkillConfig;
import io.github.afgprojects.framework.ai.core.properties.tool.ToolConfig;
import io.github.afgprojects.framework.ai.core.properties.workflow.WorkflowConfig;

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
}
