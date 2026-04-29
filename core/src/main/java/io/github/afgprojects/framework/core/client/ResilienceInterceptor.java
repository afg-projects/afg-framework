package io.github.afgprojects.framework.core.client;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import io.github.afgprojects.framework.core.model.exception.CommonErrorCode;

/**
 * 弹性拦截器
 * 实现 HTTP 客户端的重试和熔断功能
 */
public class ResilienceInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ResilienceInterceptor.class);

    private final HttpClientProperties properties;
    private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();

    public ResilienceInterceptor(@NonNull HttpClientProperties properties) {
        this.properties = properties;
    }

    @Override
    @NonNull public ClientHttpResponse intercept(
            @NonNull HttpRequest request, @NonNull byte[] body, @NonNull ClientHttpRequestExecution execution)
            throws IOException {
        String circuitBreakerKey = extractKey(request);
        CircuitBreaker circuitBreaker = getCircuitBreaker(circuitBreakerKey);

        checkCircuitBreakerOpen(circuitBreakerKey, circuitBreaker);

        RetryPolicy retryPolicy = createRetryPolicy();
        int maxAttempts = retryPolicy.getMaxAttempts();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                ClientHttpResponse response = execution.execute(request, body);
                int statusCode = response.getStatusCode().value();

                if (shouldRetryOnStatus(attempt, maxAttempts, retryPolicy, statusCode)) {
                    log.info(
                            "Retrying request to {} (attempt {}/{}) due to status {}",
                            request.getURI(),
                            attempt,
                            maxAttempts,
                            statusCode);
                    response.close();
                    waitBeforeRetry(retryPolicy, attempt);
                    continue;
                }

                recordResult(circuitBreaker, statusCode);
                return response;

            } catch (IOException e) {
                if (handleIOException(request, circuitBreaker, retryPolicy, attempt, maxAttempts, e)) {
                    continue;
                }
                throw e;
            }
        }

        // 所有重试都失败
        circuitBreaker.recordFailure();
        throw new RetryExhaustedException(
                CommonErrorCode.CLIENT_RETRY_EXHAUSTED.getCode(), "Retry exhausted for " + request.getURI());
    }

    private void checkCircuitBreakerOpen(String key, CircuitBreaker circuitBreaker) {
        if (properties.getCircuitBreaker().isEnabled() && !circuitBreaker.allowRequest()) {
            log.warn("Circuit breaker is OPEN for {}", key);
            throw new CircuitBreakerOpenException(
                    CommonErrorCode.CLIENT_CIRCUIT_OPEN.getCode(), "Circuit breaker is open for " + key);
        }
    }

    private boolean shouldRetryOnStatus(int attempt, int maxAttempts, RetryPolicy retryPolicy, int statusCode) {
        return attempt < maxAttempts && retryPolicy.shouldRetry(statusCode, null);
    }

    private void recordResult(CircuitBreaker circuitBreaker, int statusCode) {
        if (statusCode >= 500) {
            circuitBreaker.recordFailure();
        } else {
            circuitBreaker.recordSuccess();
        }
    }

    private boolean handleIOException(
            HttpRequest request,
            CircuitBreaker circuitBreaker,
            RetryPolicy retryPolicy,
            int attempt,
            int maxAttempts,
            IOException e)
            throws IOException {
        if (attempt < maxAttempts && retryPolicy.shouldRetry(0, e)) {
            log.warn(
                    "Retrying request to {} (attempt {}/{}) due to exception: {}",
                    request.getURI(),
                    attempt,
                    maxAttempts,
                    e.getMessage());
            waitBeforeRetry(retryPolicy, attempt);
            return true;
        }
        circuitBreaker.recordFailure();
        return false;
    }

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

    private String extractKey(HttpRequest request) {
        String host = request.getURI().getHost();
        return host != null ? host : "default";
    }

    private void waitBeforeRetry(RetryPolicy policy, int attempt) {
        try {
            Duration waitDuration = policy.getWaitDuration(attempt);
            Thread.sleep(waitDuration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 熔断器开启异常
     */
    public static class CircuitBreakerOpenException extends RuntimeException {
        private final int code;

        public CircuitBreakerOpenException(int code, String message) {
            super(message);
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }

    /**
     * 重试耗尽异常
     */
    public static class RetryExhaustedException extends RuntimeException {
        private final int code;

        public RetryExhaustedException(int code, String message) {
            super(message);
            this.code = code;
        }

        public RetryExhaustedException(int code, String message, Throwable cause) {
            super(message, cause);
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }
}
