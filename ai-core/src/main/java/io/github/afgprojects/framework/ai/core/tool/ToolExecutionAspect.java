package io.github.afgprojects.framework.ai.core.tool;

import io.github.afgprojects.framework.ai.core.api.tool.ToolExecutionRecorder;
import io.github.afgprojects.framework.ai.core.tool.annotation.ToolExecution;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jspecify.annotations.Nullable;

import java.time.Duration;

/**
 * @ToolExecution 注解的 AOP 切面，拦截标注了 @ToolExecution 的方法，
 * 记录工具执行的生命周期事件。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Aspect
@Slf4j
@RequiredArgsConstructor
public class ToolExecutionAspect {

    private final @Nullable ToolExecutionRecorder recorder;

    @Around("@annotation(toolExecution)")
    public Object aroundToolExecution(ProceedingJoinPoint joinPoint, ToolExecution toolExecution) throws Throwable {
        String toolName = toolExecution.value().isEmpty()
                ? joinPoint.getSignature().getName()
                : toolExecution.value();
        long startTime = System.nanoTime();

        log.debug("ToolExecution aspect: tool={}", toolName);

        String executionId = null;
        if (toolExecution.audited() && recorder != null) {
            executionId = recorder.recordStart(toolName, java.util.Map.of(),
                    io.github.afgprojects.framework.ai.core.api.tool.DefaultToolContext.EMPTY);
        }

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
            Duration duration = Duration.ofMillis(durationMs);

            if (toolExecution.audited() && recorder != null && executionId != null) {
                if (error != null) {
                    recorder.recordFailure(executionId, error.getMessage(), duration);
                } else {
                    recorder.recordSuccess(executionId, result, duration);
                }
            }

            log.debug("ToolExecution aspect completed: tool={}, durationMs={}ms, error={}",
                    toolName, durationMs, error != null ? error.getMessage() : "none");
        }
    }
}
