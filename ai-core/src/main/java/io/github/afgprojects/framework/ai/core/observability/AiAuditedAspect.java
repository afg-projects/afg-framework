package io.github.afgprojects.framework.ai.core.observability;

import io.github.afgprojects.framework.ai.core.api.observability.AuditLogger;
import io.github.afgprojects.framework.ai.core.observability.annotation.AiAudited;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * @AiAudited 注解的 AOP 切面，拦截标注了 @AiAudited 的方法，
 * 记录操作审计日志，包括执行时长和错误信息。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Aspect
@Slf4j
@RequiredArgsConstructor
public class AiAuditedAspect {

    private final AuditLogger auditLogger;

    @Around("@annotation(aiAudited)")
    public Object aroundAiAudited(ProceedingJoinPoint joinPoint, AiAudited aiAudited) throws Throwable {
        String operation = aiAudited.operation().isEmpty()
                ? joinPoint.getSignature().toShortString()
                : aiAudited.operation();
        long startTime = System.nanoTime();

        log.debug("AiAudited aspect: operation={}", operation);

        Object result = null;
        Throwable error = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            AuditLogger.AuditStatus status = error != null
                    ? AuditLogger.AuditStatus.FAILURE
                    : AuditLogger.AuditStatus.SUCCESS;

            auditLogger.log(
                    null,
                    operation,
                    null,
                    null,
                    null,
                    status
            );

            log.debug("AiAudited aspect completed: operation={}, durationMs={}ms, status={}",
                    operation, durationMs, status);
        }
    }
}
