package io.github.afgprojects.framework.ai.core.tool;

import io.github.afgprojects.framework.ai.core.api.tool.DefaultToolContext;
import io.github.afgprojects.framework.ai.core.api.tool.ToolContext;
import io.github.afgprojects.framework.ai.core.api.tool.ToolExecutionRecorder;
import io.github.afgprojects.framework.ai.core.tool.annotation.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * @Tool 注解的 AOP 切面，拦截标注了 @Tool 的方法，
 * 记录工具执行的生命周期事件。
 *
 * <p>与 {@link ToolExecutionAspect} 的区别：
 * <ul>
 *   <li>本切面处理 {@link Tool} 声明式注解 — 记录工具执行日志</li>
 *   <li>{@link ToolExecutionAspect} 处理 {@link io.github.afgprojects.framework.ai.core.tool.annotation.ToolExecution} 审计注解</li>
 * </ul>
 *
 * <p>拦截逻辑：
 * <ol>
 *   <li>从注解获取 Tool 名称</li>
 *   <li>从方法参数和 @ToolParam 提取参数信息</li>
 *   <li>记录执行开始事件</li>
 *   <li>执行原方法</li>
 *   <li>记录执行成功/失败事件</li>
 * </ol>
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Aspect
@Slf4j
@RequiredArgsConstructor
public class ToolAspect {

    private final @Nullable ToolExecutionRecorder recorder;

    @Around("@annotation(toolAnnotation)")
    public Object aroundTool(ProceedingJoinPoint joinPoint, Tool toolAnnotation) throws Throwable {
        String toolName = toolAnnotation.name();
        log.debug("ToolAspect: intercepting tool '{}' on method {}", toolName, joinPoint.getSignature().getName());

        // 提取工具参数
        Map<String, Object> arguments = extractArguments(joinPoint);

        long startTime = System.nanoTime();
        String executionId = null;

        if (recorder != null) {
            ToolContext context = DefaultToolContext.EMPTY;
            executionId = recorder.recordStart(toolName, arguments, context);
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

            if (recorder != null && executionId != null) {
                if (error != null) {
                    recorder.recordFailure(executionId, error.getMessage(), duration);
                } else {
                    recorder.recordSuccess(executionId, result, duration);
                }
            }

            if (error != null) {
                log.warn("Tool '{}' execution failed: {} ({}ms)", toolName, error.getMessage(), durationMs);
            } else {
                log.debug("Tool '{}' executed successfully ({}ms)", toolName, durationMs);
            }
        }
    }

    /**
     * 从方法参数和 @ToolParam 提取参数 Map。
     */
    private Map<String, Object> extractArguments(ProceedingJoinPoint joinPoint) {
        Map<String, Object> arguments = new HashMap<>();
        Object[] args = joinPoint.getArgs();

        if (args.length == 0) {
            return arguments;
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        String[] parameterNames = signature.getParameterNames();

        for (int i = 0; i < args.length; i++) {
            String paramName = (parameterNames != null && i < parameterNames.length)
                    ? parameterNames[i]
                    : parameters[i].getName();
            arguments.put(paramName, args[i]);
        }

        return arguments;
    }
}
