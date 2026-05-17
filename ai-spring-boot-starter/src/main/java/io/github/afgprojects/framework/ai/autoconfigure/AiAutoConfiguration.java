package io.github.afgprojects.framework.ai.autoconfigure;

import io.github.afgprojects.framework.ai.core.model.LlmClient;
import io.github.afgprojects.framework.ai.core.model.LlmConfig;
import io.github.afgprojects.framework.ai.core.tool.ToolRegistry;
import io.github.afgprojects.framework.ai.llm.openai.OpenAiLlmClient;
import io.github.afgprojects.framework.ai.agent.tool.DefaultToolRegistry;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.Duration;

/**
 * AI 模块自动配置
 *
 * <p>自动配置 AI 模块的核心组件：
 * <ul>
 *   <li>LlmClient - LLM 客户端（根据配置选择提供商）</li>
 *   <li>ToolRegistry - 工具注册表</li>
 *   <li>韧性模块 - 重试、熔断、降级</li>
 *   <li>可观测性模块 - 指标、追踪、审计</li>
 *   <li>安全模块 - API Key 管理、内容安全、PII 检测</li>
 *   <li>持久化模块 - 会话存储、消息历史</li>
 *   <li>性能优化模块 - 缓存、速率限制</li>
 * </ul>
 *
 * <p>配置示例：
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
 *     observability:
 *       enabled: true
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(AiConfigurationProperties.class)
@ConditionalOnProperty(prefix = "afg.ai", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({
        ResilienceAutoConfiguration.class,
        ObservabilityAutoConfiguration.class,
        SecurityAutoConfiguration.class,
        PersistenceAutoConfiguration.class,
        PerformanceAutoConfiguration.class
})
public class AiAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AiAutoConfiguration.class);

    /**
     * 配置 OpenAI LLM 客户端
     *
     * <p>当配置了 OpenAI API Key 时自动创建。
     */
    @Bean
    @ConditionalOnClass(OpenAiLlmClient.class)
    @ConditionalOnProperty(prefix = "afg.ai.llm", name = "provider", havingValue = "openai")
    @ConditionalOnMissingBean(LlmClient.class)
    public LlmClient openAiLlmClient(@NonNull AiConfigurationProperties properties) {
        var llmProps = properties.getLlm();

        log.info("Creating OpenAI LLM client with model: {}", llmProps.getModel());

        LlmConfig config = LlmConfig.of(llmProps.getApiKey(), llmProps.getModel());

        if (llmProps.getBaseUrl() != null) {
            config = config.withBaseUrl(llmProps.getBaseUrl());
        }
        if (llmProps.getTimeout() != null) {
            config = config.withTimeout(Duration.ofMillis(llmProps.getTimeout()));
        }

        return new OpenAiLlmClient(config);
    }

    /**
     * 配置默认工具注册表
     */
    @Bean
    @ConditionalOnMissingBean(ToolRegistry.class)
    public ToolRegistry toolRegistry() {
        log.info("Creating default tool registry");
        return new DefaultToolRegistry();
    }
}
