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
            return true; // 跳过继续下一个文档
        }
        context.incrementRetryCount(document);
        try {
            Thread.sleep(retryDelay.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // 返回 false 表示需要重试当前操作
        // Pipeline 会检查 shouldRetry() 来判断
        return false;
    }

    /**
     * 检查是否应该重试当前操作。
     *
     * @param document 文档
     * @param context  上下文
     * @return true 表示应该重试
     */
    public boolean shouldRetry(@NonNull Document document, @NonNull EtlContext context) {
        if (strategy != ErrorHandlingStrategy.RETRY) {
            return false;
        }
        return context.getRetryCount(document) <= maxRetries;
    }

    /**
     * 获取最大重试次数。
     */
    public int getMaxRetries() {
        return maxRetries;
    }

    /**
     * 获取重试延迟。
     */
    @NonNull
    public Duration getRetryDelay() {
        return retryDelay;
    }
}