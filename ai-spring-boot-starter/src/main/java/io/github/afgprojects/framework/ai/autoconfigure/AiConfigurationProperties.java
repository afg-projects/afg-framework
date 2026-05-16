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
 *       temperature: 0.7
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

    /**
     * LLM 配置
     */
    public static class LlmConfig {
        /**
         * 提供商类型（openai, anthropic, ollama）
         */
        private String provider = "openai";

        /**
         * 模型名称
         */
        private String model = "gpt-4";

        /**
         * API Key
         */
        private @Nullable String apiKey;

        /**
         * API 基础 URL
         */
        private @Nullable String baseUrl;

        /**
         * 温度参数
         */
        private @Nullable Double temperature;

        /**
         * 最大 Token 数
         */
        private @Nullable Integer maxTokens;

        /**
         * 超时时间（毫秒）
         */
        private @Nullable Long timeout;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public @Nullable String getApiKey() {
            return apiKey;
        }

        public void setApiKey(@Nullable String apiKey) {
            this.apiKey = apiKey;
        }

        public @Nullable String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(@Nullable String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public @Nullable Double getTemperature() {
            return temperature;
        }

        public void setTemperature(@Nullable Double temperature) {
            this.temperature = temperature;
        }

        public @Nullable Integer getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(@Nullable Integer maxTokens) {
            this.maxTokens = maxTokens;
        }

        public @Nullable Long getTimeout() {
            return timeout;
        }

        public void setTimeout(@Nullable Long timeout) {
            this.timeout = timeout;
        }
    }

    /**
     * Agent 配置
     */
    public static class AgentConfig {
        /**
         * 最大迭代次数
         */
        private Integer maxIterations = 10;

        /**
         * 执行超时（毫秒）
         */
        private Long timeout = 60000L;

        /**
         * 是否启用记忆
         */
        private Boolean enableMemory = true;

        /**
         * 记忆最大容量
         */
        private Integer memoryMaxSize = 100;

        public Integer getMaxIterations() {
            return maxIterations;
        }

        public void setMaxIterations(Integer maxIterations) {
            this.maxIterations = maxIterations;
        }

        public Long getTimeout() {
            return timeout;
        }

        public void setTimeout(Long timeout) {
            this.timeout = timeout;
        }

        public boolean isEnableMemory() {
            return enableMemory;
        }

        public void setEnableMemory(Boolean enableMemory) {
            this.enableMemory = enableMemory;
        }

        public Integer getMemoryMaxSize() {
            return memoryMaxSize;
        }

        public void setMemoryMaxSize(Integer memoryMaxSize) {
            this.memoryMaxSize = memoryMaxSize;
        }
    }
}
