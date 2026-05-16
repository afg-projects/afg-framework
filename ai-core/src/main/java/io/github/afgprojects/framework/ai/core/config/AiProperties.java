package io.github.afgprojects.framework.ai.core.config;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * AI 模块配置属性
 *
 * <p>统一配置 AI 模块的核心参数，包括 LLM、RAG、Agent 等配置。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public record AiProperties(
        @NonNull LlmProperties llm,
        @Nullable RagProperties rag,
        @Nullable AgentProperties agent,
        @Nullable MultiAgentProperties multiAgent,
        @Nullable Map<String, Object> extensions
) {

    /**
     * 创建默认配置
     *
     * @param llm LLM 配置
     * @return AI 配置
     */
    public static @NonNull AiProperties defaults(@NonNull LlmProperties llm) {
        return new AiProperties(llm, null, null, null, null);
    }

    /**
     * LLM 配置属性
     *
     * @param provider    提供商类型（openai, anthropic, ollama）
     * @param model       模型名称
     * @param apiKey      API Key
     * @param baseUrl     API 基础 URL
     * @param temperature 温度参数
     * @param maxTokens   最大 Token 数
     * @param timeout     超时时间（毫秒）
     */
    public record LlmProperties(
            @NonNull String provider,
            @NonNull String model,
            @Nullable String apiKey,
            @Nullable String baseUrl,
            @Nullable Double temperature,
            @Nullable Integer maxTokens,
            @Nullable Long timeout
    ) {
        /**
         * 创建 OpenAI 配置
         */
        public static @NonNull LlmProperties openai(
                @NonNull String model,
                @NonNull String apiKey
        ) {
            return new LlmProperties("openai", model, apiKey, null, null, null, null);
        }

        /**
         * 创建 Anthropic 配置
         */
        public static @NonNull LlmProperties anthropic(
                @NonNull String model,
                @NonNull String apiKey
        ) {
            return new LlmProperties("anthropic", model, apiKey, null, null, null, null);
        }

        /**
         * 创建 Ollama 配置
         */
        public static @NonNull LlmProperties ollama(
                @NonNull String model,
                @NonNull String baseUrl
        ) {
            return new LlmProperties("ollama", model, null, baseUrl, null, null, null);
        }

        /**
         * 设置温度参数
         */
        public @NonNull LlmProperties withTemperature(double temperature) {
            return new LlmProperties(provider, model, apiKey, baseUrl, temperature, maxTokens, timeout);
        }

        /**
         * 设置最大 Token 数
         */
        public @NonNull LlmProperties withMaxTokens(int maxTokens) {
            return new LlmProperties(provider, model, apiKey, baseUrl, temperature, maxTokens, timeout);
        }

        /**
         * 设置超时时间
         */
        public @NonNull LlmProperties withTimeout(long timeout) {
            return new LlmProperties(provider, model, apiKey, baseUrl, temperature, maxTokens, timeout);
        }
    }

    /**
     * RAG 配置属性
     *
     * @param vectorStoreType 向量库类型（simple, milvus, redis）
     * @param embeddingModel  嵌入模型名称
     * @param chunkSize       文本分块大小
     * @param chunkOverlap    分块重叠大小
     * @param topK            检索 Top-K 数量
     * @param scoreThreshold  分数阈值
     */
    public record RagProperties(
            @NonNull String vectorStoreType,
            @Nullable String embeddingModel,
            @Nullable Integer chunkSize,
            @Nullable Integer chunkOverlap,
            @Nullable Integer topK,
            @Nullable Double scoreThreshold
    ) {
        /**
         * 创建简单向量库配置
         */
        public static @NonNull RagProperties simple() {
            return new RagProperties("simple", null, null, null, null, null);
        }

        /**
         * 创建 Milvus 配置
         */
        public static @NonNull RagProperties milvus() {
            return new RagProperties("milvus", null, null, null, null, null);
        }

        /**
         * 创建 Redis 配置
         */
        public static @NonNull RagProperties redis() {
            return new RagProperties("redis", null, null, null, null, null);
        }

        /**
         * 设置分块参数
         */
        public @NonNull RagProperties withChunking(int chunkSize, int chunkOverlap) {
            return new RagProperties(vectorStoreType, embeddingModel, chunkSize, chunkOverlap, topK, scoreThreshold);
        }

        /**
         * 设置检索参数
         */
        public @NonNull RagProperties withRetrieval(int topK, double scoreThreshold) {
            return new RagProperties(vectorStoreType, embeddingModel, chunkSize, chunkOverlap, topK, scoreThreshold);
        }
    }

    /**
     * Agent 配置属性
     *
     * @param maxIterations   最大迭代次数
     * @param timeout         单次执行超时（毫秒）
     * @param enableMemory    是否启用记忆
     * @param memoryMaxSize   记忆最大容量
     * @param enableStreaming 是否启用流式输出
     */
    public record AgentProperties(
            @Nullable Integer maxIterations,
            @Nullable Long timeout,
            @Nullable Boolean enableMemory,
            @Nullable Integer memoryMaxSize,
            @Nullable Boolean enableStreaming
    ) {
        /**
         * 创建默认配置
         */
        public static @NonNull AgentProperties defaults() {
            return new AgentProperties(10, 60000L, true, 100, false);
        }

        /**
         * 启用流式输出
         */
        public @NonNull AgentProperties withStreaming() {
            return new AgentProperties(maxIterations, timeout, enableMemory, memoryMaxSize, true);
        }

        /**
         * 设置超时时间
         */
        public @NonNull AgentProperties withTimeout(long timeout) {
            return new AgentProperties(maxIterations, timeout, enableMemory, memoryMaxSize, enableStreaming);
        }
    }

    /**
     * Multi-Agent 配置属性
     *
     * @param maxAgents       最大 Agent 数量
     * @param coordinationTimeout 协调超时（毫秒）
     * @param enableParallel  是否启用并行执行
     * @param messageTimeout  消息超时（毫秒）
     */
    public record MultiAgentProperties(
            @Nullable Integer maxAgents,
            @Nullable Long coordinationTimeout,
            @Nullable Boolean enableParallel,
            @Nullable Long messageTimeout
    ) {
        /**
         * 创建默认配置
         */
        public static @NonNull MultiAgentProperties defaults() {
            return new MultiAgentProperties(10, 300000L, true, 30000L);
        }

        /**
         * 禁用并行执行
         */
        public @NonNull MultiAgentProperties sequential() {
            return new MultiAgentProperties(maxAgents, coordinationTimeout, false, messageTimeout);
        }
    }
}