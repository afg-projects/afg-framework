package io.github.afgprojects.framework.ai.core.etl;

import org.jspecify.annotations.NonNull;

import java.time.Duration;

/**
 * 默认错误处理器。
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public class DefaultErrorHandler implements ErrorHandler {

    private final ErrorHandlingStrategy strategy;
    private final int maxRetries;
    private final Duration retryDelay;

    public DefaultErrorHandler(@NonNull ErrorHandlingStrategy strategy) {
        this(strategy, 3, Duration.ofSeconds(1));
    }

    public DefaultErrorHandler(@NonNull ErrorHandlingStrategy strategy, int maxRetries, @NonNull Duration retryDelay) {
        this.strategy = strategy;
        this.maxRetries = maxRetries;
        this.retryDelay = retryDelay;
    }

    @Override
    public boolean handle(@NonNull Document document, @NonNull Exception error, @NonNull EtlContext context) {
        return switch (strategy) {
            case FAIL_FAST -> {
                context.recordFailure(document, "fail_fast", error);
                yield false;
            }
            case CONTINUE -> true;
            case SKIP_AND_LOG -> {
                context.recordFailure(document, "skipped", error);
                yield true;
            }
            case RETRY -> handleRetry(document, error, context);
        };
    }

    private boolean handleRetry(@NonNull Document document, @NonNull Exception error, @NonNull EtlContext context) {
        int retries = context.getRetryCount(document);
        if (retries >= maxRetries) {
            context.recordFailure(document, "max_retries_exceeded", error);
            return true; // 跳过继续
        }
        context.incrementRetryCount(document);
        try {
            Thread.sleep(retryDelay.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return true;
    }
}