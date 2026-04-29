package io.github.afgprojects.framework.core.client;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;

import io.github.afgprojects.framework.core.client.ResilienceInterceptor.CircuitBreakerOpenException;
import io.github.afgprojects.framework.core.client.ResilienceInterceptor.RetryExhaustedException;
import io.github.afgprojects.framework.core.model.exception.CommonErrorCode;

/**
 * 异步弹性拦截器
 * <p>
 * 使用 {@link CompletableFuture} 和 {@link ScheduledExecutorService} 实现非阻塞异步重试机制，
 * 避免 {@code Thread.sleep()} 阻塞线程，提高系统吞吐量。
 *
 * <h2>特性</h2>
 * <ul>
 *   <li>非阻塞异步重试：使用调度器实现延迟重试，不阻塞调用线程</li>
 *   <li>熔断器集成：复用 {@link CircuitBreaker} 实现熔断保护</li>
 *   <li>指数退避：支持可配置的指数退避重试策略</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * AsyncResilienceInterceptor interceptor = new AsyncResilienceInterceptor(properties);
 *
 * // 异步执行请求
 * CompletableFuture<ClientHttpResponse> future = interceptor.executeAsync(
 *     request,
 *     body,
 *     () -> execution.execute(request, body)
 * );
 *
 * // 添加回调
 * future.thenAccept(response -> {
 *     // 处理响应
 * }).exceptionally(ex -> {
 *     // 处理异常
 *     return null;
 * });
 * }</pre>
 */
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public class AsyncResilienceInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AsyncResilienceInterceptor.class);

    /**
     * 默认调度线程池大小
     */
    private static final int DEFAULT_SCHEDULER_POOL_SIZE = 4;

    /**
     * HTTP 客户端配置属性
     */
    private final HttpClientProperties properties;

    /**
     * 调度执行器，用于延迟执行重试任务
     */
    private final ScheduledExecutorService scheduler;

    /**
     * 熔断器注册表，按主机分组
     */
    private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();

    /**
     * 创建异步弹性拦截器（使用默认调度器）
     *
     * @param properties HTTP 客户端配置属性
     */
    public AsyncResilienceInterceptor(@NonNull HttpClientProperties properties) {
        this(properties, Executors.newScheduledThreadPool(DEFAULT_SCHEDULER_POOL_SIZE, r -> {
            Thread thread = new Thread(r, "async-resilience-scheduler");
            thread.setDaemon(true);
            return thread;
        }));
    }

    /**
     * 创建异步弹性拦截器（使用指定调度器）
     *
     * @param properties HTTP 客户端配置属性
     * @param scheduler  调度执行器
     */
    public AsyncResilienceInterceptor(@NonNull HttpClientProperties properties, @NonNull ScheduledExecutorService scheduler) {
        this.properties = properties;
        this.scheduler = scheduler;
    }

    /**
     * 异步执行 HTTP 请求，支持重试和熔断
     *
     * @param request   HTTP 请求
     * @param body      请求体
     * @param execution 请求执行器
     * @return 异步响应结果
     */
    @NonNull
    public CompletableFuture<ClientHttpResponse> executeAsync(
            @NonNull HttpRequest request,
            @NonNull byte[] body,
            @NonNull Supplier<ClientHttpResponse> execution) {
        return executeAsync(request, body, execution, 1);
    }

    /**
     * 异步执行 HTTP 请求（内部方法，支持递归重试）
     *
     * @param request   HTTP 请求
     * @param body      请求体
     * @param execution 请求执行器
     * @param attempt   当前尝试次数
     * @return 异步响应结果
     */
    private CompletableFuture<ClientHttpResponse> executeAsync(
            @NonNull HttpRequest request,
            @NonNull byte[] body,
            @NonNull Supplier<ClientHttpResponse> execution,
            int attempt) {

        return CompletableFuture.supplyAsync(() -> {
            // 检查熔断器状态
            String circuitBreakerKey = extractKey(request);
            CircuitBreaker circuitBreaker = getCircuitBreaker(circuitBreakerKey);

            if (properties.getCircuitBreaker().isEnabled() && !circuitBreaker.allowRequest()) {
                log.warn("Circuit breaker is OPEN for {}", circuitBreakerKey);
                throw new CircuitBreakerOpenException(
                        CommonErrorCode.CLIENT_CIRCUIT_OPEN.getCode(),
                        "Circuit breaker is open for " + circuitBreakerKey);
            }

            // 执行请求
            try {
                return new ExecutionResult(execution.get(), circuitBreaker, circuitBreakerKey);
            } catch (Exception e) {
                return new ExecutionResult(e, circuitBreaker, circuitBreakerKey);
            }
        }).thenCompose(result -> handleExecutionResult(request, body, execution, attempt, result));
    }

    /**
     * 处理执行结果，决定是否重试
     */
    private CompletableFuture<ClientHttpResponse> handleExecutionResult(
            @NonNull HttpRequest request,
            @NonNull byte[] body,
            @NonNull Supplier<ClientHttpResponse> execution,
            int attempt,
            ExecutionResult result) {

        RetryPolicy retryPolicy = createRetryPolicy();
        int maxAttempts = retryPolicy.getMaxAttempts();

        // 处理异常情况
        if (result.exception != null) {
            return handleException(request, body, execution, attempt, maxAttempts, result, retryPolicy);
        }

        // 处理正常响应
        ClientHttpResponse response = result.response;
        int statusCode;
        try {
            statusCode = response.getStatusCode().value();
        } catch (IOException e) {
            // 无法获取状态码，记录失败并返回异常
            result.circuitBreaker.recordFailure();
            return CompletableFuture.failedFuture(e);
        }

        // 检查是否需要重试（基于状态码）
        if (attempt < maxAttempts && retryPolicy.shouldRetry(statusCode, null)) {
            log.info(
                    "Async retry scheduled for {} (attempt {}/{}) due to status {}",
                    request.getURI(),
                    attempt,
                    maxAttempts,
                    statusCode);

            try {
                response.close();
            } catch (Exception e) {
                log.debug("Failed to close response", e);
            }

            return scheduleRetry(request, body, execution, attempt, retryPolicy);
        }

        // 记录结果
        recordResult(result.circuitBreaker, statusCode);
        return CompletableFuture.completedFuture(response);
    }

    /**
     * 处理异常情况，决定是否重试
     */
    private CompletableFuture<ClientHttpResponse> handleException(
            @NonNull HttpRequest request,
            @NonNull byte[] body,
            @NonNull Supplier<ClientHttpResponse> execution,
            int attempt,
            int maxAttempts,
            ExecutionResult result,
            RetryPolicy retryPolicy) {

        Exception exception = result.exception;
        CircuitBreaker circuitBreaker = result.circuitBreaker;

        // 检查是否应该重试
        if (attempt < maxAttempts && retryPolicy.shouldRetry(0, exception)) {
            log.warn(
                    "Async retry scheduled for {} (attempt {}/{}) due to exception: {}",
                    request.getURI(),
                    attempt,
                    maxAttempts,
                    exception.getMessage());

            return scheduleRetry(request, body, execution, attempt, retryPolicy);
        }

        // 重试耗尽或不可重试异常
        circuitBreaker.recordFailure();

        if (exception instanceof RuntimeException) {
            return CompletableFuture.failedFuture(exception);
        }
        return CompletableFuture.failedFuture(new RetryExhaustedException(
                CommonErrorCode.CLIENT_RETRY_EXHAUSTED.getCode(),
                "Retry exhausted for " + request.getURI(),
                exception));
    }

    /**
     * 调度延迟重试
     */
    private CompletableFuture<ClientHttpResponse> scheduleRetry(
            @NonNull HttpRequest request,
            @NonNull byte[] body,
            @NonNull Supplier<ClientHttpResponse> execution,
            int attempt,
            RetryPolicy retryPolicy) {

        Duration waitDuration = retryPolicy.getWaitDuration(attempt);
        CompletableFuture<ClientHttpResponse> future = new CompletableFuture<>();

        scheduler.schedule(() -> {
            executeAsync(request, body, execution, attempt + 1)
                    .whenComplete((response, ex) -> {
                        if (ex != null) {
                            future.completeExceptionally(ex);
                        } else {
                            future.complete(response);
                        }
                    });
        }, waitDuration.toMillis(), TimeUnit.MILLISECONDS);

        return future;
    }

    /**
     * 记录请求结果到熔断器
     */
    private void recordResult(CircuitBreaker circuitBreaker, int statusCode) {
        if (statusCode >= 500) {
            circuitBreaker.recordFailure();
        } else {
            circuitBreaker.recordSuccess();
        }
    }

    /**
     * 获取或创建熔断器
     */
    private CircuitBreaker getCircuitBreaker(String key) {
        return circuitBreakers.computeIfAbsent(key, k -> {
            HttpClientProperties.CircuitBreakerConfig config = properties.getCircuitBreaker();
            return new CircuitBreaker(
                    k,
                    config.getFailureThreshold(),
                    Duration.ofMillis(config.getOpenDuration()),
                    config.getHalfOpenMaxCalls(),
                    config.getSuccessThreshold());
        });
    }

    /**
     * 创建重试策略
     */
    private RetryPolicy createRetryPolicy() {
        HttpClientProperties.RetryConfig config = properties.getRetry();
        return RetryPolicy.builder()
                .maxAttempts(config.getMaxAttempts())
                .initialInterval(config.getInitialInterval())
                .multiplier(config.getMultiplier())
                .maxInterval(config.getMaxInterval())
                .retryOnStatus(config.getRetryOnStatus())
                .build();
    }

    /**
     * 从请求中提取熔断器 key
     */
    private String extractKey(HttpRequest request) {
        String host = request.getURI().getHost();
        return host != null ? host : "default";
    }

    /**
     * 关闭调度器
     * <p>
     * 应在应用关闭时调用，释放线程池资源
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 获取调度执行器（用于测试）
     */
    @NonNull
    ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    /**
     * 执行结果包装类
     */
    private static class ExecutionResult {
        @Nullable
        final ClientHttpResponse response;
        @Nullable
        final Exception exception;
        final CircuitBreaker circuitBreaker;
        final String circuitBreakerKey;

        ExecutionResult(@NonNull ClientHttpResponse response, @NonNull CircuitBreaker circuitBreaker, String key) {
            this.response = response;
            this.exception = null;
            this.circuitBreaker = circuitBreaker;
            this.circuitBreakerKey = key;
        }

        ExecutionResult(@NonNull Exception exception, @NonNull CircuitBreaker circuitBreaker, String key) {
            this.response = null;
            this.exception = exception;
            this.circuitBreaker = circuitBreaker;
            this.circuitBreakerKey = key;
        }
    }
}
