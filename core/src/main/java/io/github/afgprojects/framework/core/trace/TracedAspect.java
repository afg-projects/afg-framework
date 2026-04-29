package io.github.afgprojects.framework.core.trace;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

/**
 * 追踪切面
 * <p>
 * 拦截 @{@link Traced} 注解的方法，自动创建 Span 并记录执行信息。
 * </p>
 *
 * <h3>功能特性</h3>
 * <ul>
 *   <li>自动创建和关闭 Span</li>
 *   <li>支持多种 Span 类型（CLIENT/SERVER/PRODUCER/CONSUMER/INTERNAL）</li>
 *   <li>支持采样策略</li>
 *   <li>支持参数和返回值记录</li>
 *   <li>自动记录异常信息</li>
 *   <li>支持 baggage 传播</li>
 * </ul>
 */
@Aspect
@SuppressWarnings({
    "PMD.SignatureDeclareThrowsException",
    "PMD.AvoidThrowingRawExceptionTypes",
    "PMD.AvoidCatchingGenericException"
})
public class TracedAspect {

    private static final Logger log = LoggerFactory.getLogger(TracedAspect.class);

    private final @Nullable Tracer tracer;
    private final TracingProperties properties;
    private final @Nullable TracingSampler sampler;
    private final @Nullable SpanCreator spanCreator;
    private final TracingLogRecorder logRecorder;

    /**
     * 构造函数
     *
     * @param tracer     Micrometer Tracer（可为 null）
     * @param properties 追踪配置属性
     */
    public TracedAspect(@Nullable Tracer tracer, @NonNull TracingProperties properties) {
        this.tracer = tracer;
        this.properties = properties;
        this.sampler = properties.isEnabled() && properties.getSampling().getStrategy() != SamplingStrategy.ALWAYS
                ? new TracingSampler(properties.getSampling())
                : null;
        this.spanCreator = tracer != null ? new SpanCreator(tracer) : null;
        this.logRecorder = new TracingLogRecorder();
    }

    /**
     * 追踪切面方法
     *
     * @param joinPoint  切点
     * @param annotation 追踪注解
     * @return 方法执行结果
     * @throws Throwable 方法执行异常
     */
    @Around("@annotation(annotation)")
    public Object traceAround(ProceedingJoinPoint joinPoint, Traced annotation) throws Throwable {
        // 检查是否启用
        if (!properties.isEnabled() || !properties.getAnnotations().isEnabled()) {
            return joinPoint.proceed();
        }

        // 采样检查
        if (sampler != null && !sampler.shouldSample()) {
            return joinPoint.proceed();
        }

        // 无 Tracer 时仅记录日志
        if (tracer == null || spanCreator == null) {
            return proceedWithoutTracer(joinPoint, annotation);
        }

        // 解析操作名称
        String operationName = resolveOperationName(joinPoint, annotation);

        // 创建 Span
        Span span = spanCreator.createSpan(annotation.kind(), operationName);

        try {
            // 记录参数
            if (annotation.logParameters()) {
                logRecorder.logParameters(span, joinPoint);
            }

            // 执行方法
            Object result = joinPoint.proceed();

            // 记录返回值
            if (annotation.logResult() && result != null) {
                logRecorder.logResult(span, result);
            }

            span.tag("success", "true");
            return result;

        } catch (Throwable ex) {
            // 记录异常
            span.tag("success", "false");
            span.tag("exception", ex.getClass().getSimpleName());

            if (annotation.exceptionLogLevel() != ExceptionLogLevel.NONE) {
                logRecorder.logException(span, ex, annotation.exceptionLogLevel());
            }

            throw ex;

        } finally {
            span.end();
        }
    }

    /**
     * 无 Tracer 时的处理
     */
    private Object proceedWithoutTracer(ProceedingJoinPoint joinPoint, Traced annotation) throws Throwable {
        String operationName = resolveOperationName(joinPoint, annotation);

        if (log.isDebugEnabled()) {
            log.debug("[Traced] {} - starting", operationName);
        }

        long startTime = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();

            if (log.isDebugEnabled()) {
                long duration = System.currentTimeMillis() - startTime;
                log.debug("[Traced] {} - completed in {}ms", operationName, duration);
            }

            return result;

        } catch (Throwable ex) {
            if (log.isDebugEnabled()) {
                long duration = System.currentTimeMillis() - startTime;
                log.debug("[Traced] {} - failed in {}ms: {}",
                        operationName, duration, ex.getMessage());
            }

            if (annotation.exceptionLogLevel() == ExceptionLogLevel.STACK_TRACE) {
                log.warn("[Traced] {} - exception details", operationName, ex);
            }

            throw ex;
        }
    }

    /**
     * 解析操作名称
     */
    private String resolveOperationName(ProceedingJoinPoint joinPoint, Traced annotation) {
        if (annotation.operationName() != null && !annotation.operationName().isEmpty()) {
            return annotation.operationName();
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        return className + "." + methodName;
    }
}
