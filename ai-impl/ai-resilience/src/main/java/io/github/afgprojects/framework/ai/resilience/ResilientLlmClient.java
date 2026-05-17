package io.github.afgprojects.framework.ai.resilience;

import io.github.afgprojects.framework.ai.core.model.*;
import io.github.afgprojects.framework.ai.core.resilience.*;
import io.github.afgprojects.framework.ai.core.tool.ToolDefinition;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * 带韧性保护的 LLM 客户端
 *
 * <p>为 LLM 客户端添加重试、熔断、降级保护：
 * <ul>
 *   <li>自动重试失败的请求</li>
 *   <li>熔断器防止级联故障</li>
 *   <li>降级策略提供备用响应</li>
 * </ul>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class ResilientLlmClient implements LlmClient {

    private final LlmClient delegate;
    private final ResilienceExecutor resilienceExecutor;
    private final FallbackStrategy<LlmResponse> fallbackStrategy;

    /**
     * 创建韧性 LLM 客户端
     *
     * @param delegate            原始 LLM 客户端
     * @param resilienceExecutor  韧性执行器
     * @param fallbackStrategy    降级策略（可选）
     */
    public ResilientLlmClient(
            @NonNull LlmClient delegate,
            @NonNull ResilienceExecutor resilienceExecutor,
            @Nullable FallbackStrategy<LlmResponse> fallbackStrategy
    ) {
        this.delegate = delegate;
        this.resilienceExecutor = resilienceExecutor;
        this.fallbackStrategy = fallbackStrategy != null
                ? fallbackStrategy
                : new DefaultLlmFallbackStrategy();
    }

    /**
     * 创建韧性 LLM 客户端（使用默认降级策略）
     */
    public ResilientLlmClient(@NonNull LlmClient delegate, @NonNull ResilienceExecutor resilienceExecutor) {
        this(delegate, resilienceExecutor, null);
    }

    @Override
    @NonNull
    public LlmResponse chat(@NonNull LlmRequest request) {
        return resilienceExecutor.executeWithFallback(
                () -> delegate.chat(request),
                new RequestAwareFallbackStrategy(request)
        );
    }

    @Override
    public reactor.core.publisher.@NonNull Flux<LlmResponse> chatStream(@NonNull LlmRequest request) {
        // 流式响应不适合重试，直接返回
        return delegate.chatStream(request)
                .onErrorResume(e -> {
                    LlmResponse fallbackResponse = fallbackStrategy.fallback(
                            (Exception) e,
                            new DefaultFallbackContext(request)
                    );
                    return fallbackResponse != null
                            ? reactor.core.publisher.Flux.just(fallbackResponse)
                            : reactor.core.publisher.Flux.error(e);
                });
    }

    @Override
    @NonNull
    public LlmResponse chatWithTools(@NonNull LlmRequest request, @NonNull List<ToolDefinition> tools) {
        return resilienceExecutor.executeWithFallback(
                () -> delegate.chatWithTools(request, tools),
                new RequestAwareFallbackStrategy(request)
        );
    }

    @Override
    @NonNull
    public LlmConfig getConfig() {
        return delegate.getConfig();
    }

    /**
     * 请求感知的降级策略
     */
    private class RequestAwareFallbackStrategy implements FallbackStrategy<LlmResponse> {
        private final LlmRequest request;

        RequestAwareFallbackStrategy(LlmRequest request) {
            this.request = request;
        }

        @Override
        @Nullable
        public LlmResponse fallback(@NonNull Exception exception, @NonNull FallbackContext context) {
            return fallbackStrategy.fallback(exception, new DefaultFallbackContext(request));
        }

        @Override
        public boolean shouldFallback(@NonNull Exception exception) {
            return fallbackStrategy.shouldFallback(exception);
        }
    }

    /**
     * 默认 LLM 降级策略
     */
    private static class DefaultLlmFallbackStrategy implements FallbackStrategy<LlmResponse> {
        @Override
        @Nullable
        public LlmResponse fallback(@NonNull Exception exception, @NonNull FallbackContext context) {
            // 返回错误响应而不是抛出异常
            return new LlmResponse(
                    "抱歉，服务暂时不可用，请稍后重试。",
                    List.of(),
                    null,
                    LlmResponse.FinishReason.UNKNOWN
            );
        }

        @Override
        public boolean shouldFallback(@NonNull Exception exception) {
            // 对所有异常都执行降级
            return true;
        }
    }

    /**
     * 默认降级上下文
     */
    private static class DefaultFallbackContext implements FallbackStrategy.FallbackContext {
        private final Object originalRequest;

        DefaultFallbackContext(Object originalRequest) {
            this.originalRequest = originalRequest;
        }

        @Override
        @Nullable
        public Object getOriginalRequest() {
            return originalRequest;
        }

        @Override
        @NonNull
        public String getRequestId() {
            return java.util.UUID.randomUUID().toString().substring(0, 8);
        }

        @Override
        public long getRequestTimestamp() {
            return System.currentTimeMillis();
        }
    }

    /**
     * 创建 Builder
     */
    public static Builder builder(@NonNull LlmClient delegate) {
        return new Builder(delegate);
    }

    /**
     * Builder
     */
    public static class Builder {
        private final LlmClient delegate;
        private RetryPolicy retryPolicy = new DefaultRetryPolicy();
        private CircuitBreaker circuitBreaker = new DefaultCircuitBreaker();
        private FallbackStrategy<LlmResponse> fallbackStrategy;

        Builder(LlmClient delegate) {
            this.delegate = delegate;
        }

        public Builder retryPolicy(RetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
            return this;
        }

        public Builder circuitBreaker(CircuitBreaker circuitBreaker) {
            this.circuitBreaker = circuitBreaker;
            return this;
        }

        public Builder fallbackStrategy(FallbackStrategy<LlmResponse> fallbackStrategy) {
            this.fallbackStrategy = fallbackStrategy;
            return this;
        }

        public ResilientLlmClient build() {
            ResilienceExecutor executor = new DefaultResilienceExecutor(retryPolicy, circuitBreaker);
            return new ResilientLlmClient(delegate, executor, fallbackStrategy);
        }
    }
}