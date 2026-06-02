package io.github.afgprojects.framework.ai.core.performance;

import io.github.afgprojects.framework.ai.core.api.exception.AiException;
import io.github.afgprojects.framework.ai.core.api.performance.RateLimiter;
import io.github.afgprojects.framework.ai.core.config.AfgAiProperties;
import io.github.afgprojects.framework.ai.core.performance.annotation.AiRateLimited;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @AiRateLimited 注解的 AOP 切面，拦截标注了 @AiRateLimited 的方法，提供速率限制能力。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Aspect
@Slf4j
@RequiredArgsConstructor
public class AiRateLimitedAspect {

    private final Map<String, RateLimiter> limiters;
    private final AfgAiProperties properties;

    public AiRateLimitedAspect(AfgAiProperties properties) {
        this.limiters = new ConcurrentHashMap<>();
        this.properties = properties;
    }

    @Around("@annotation(aiRateLimited)")
    public Object aroundAiRateLimited(ProceedingJoinPoint joinPoint, AiRateLimited aiRateLimited) throws Throwable {
        String key = aiRateLimited.value().isEmpty() ? joinPoint.getSignature().toLongString() : aiRateLimited.value();
        log.debug("AiRateLimited aspect: key={}, permits={}", key, aiRateLimited.permitsPerSecond());

        RateLimiter limiter = limiters.computeIfAbsent(key, k ->
                new DefaultRateLimiter((long) aiRateLimited.permitsPerSecond(), Duration.ofSeconds(1)));

        boolean acquired;
        if (aiRateLimited.timeoutMs() > 0) {
            acquired = limiter.tryAcquire(key, Duration.ofMillis(aiRateLimited.timeoutMs()));
        } else {
            acquired = limiter.tryAcquire(key);
        }

        if (!acquired) {
            throw new AiException("Rate limit exceeded for: " + key,
                    AiException.ErrorCodes.LLM_RATE_LIMITED);
        }

        return joinPoint.proceed();
    }
}
