package io.github.afgprojects.framework.core.web.ratelimit;

import java.lang.reflect.Method;
import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.afgprojects.framework.core.model.exception.BusinessException;
import io.github.afgprojects.framework.core.model.exception.CommonErrorCode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * 限流切面
 * <p>
 * 拦截 @{@link RateLimit} 注解，执行分布式限流检查。
 * 支持回退方法处理限流场景。
 * 支持多级限流（同时配置多个限流规则）。
 * </p>
 */
@Aspect
@SuppressWarnings({
    "PMD.SignatureDeclareThrowsException",
    "PMD.AvoidThrowingRawExceptionTypes",
    "PMD.AvoidCatchingGenericException"
})
public class RateLimitInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private final RateLimiter rateLimiter;
    private final RateLimitProperties properties;
    private final @Nullable MeterRegistry meterRegistry;

    /**
     * 构造函数
     *
     * @param rateLimiter   限流器
     * @param properties    限流配置属性
     * @param meterRegistry 指标注册器（可选）
     */
    public RateLimitInterceptor(RateLimiter rateLimiter, RateLimitProperties properties,
            @Nullable MeterRegistry meterRegistry) {
        this.rateLimiter = rateLimiter;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    /**
     * 限流切面（单注解）
     *
     * @param joinPoint  切点
     * @param annotation 限流注解
     * @return 方法执行结果
     * @throws Throwable 方法执行异常
     */
    @Around("@annotation(annotation)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit annotation) throws Throwable {
        return processRateLimit(joinPoint, new RateLimit[] {annotation});
    }

    /**
     * 限流切面（多注解）
     *
     * @param joinPoint  切点
     * @param container 限流注解容器
     * @return 方法执行结果
     * @throws Throwable 方法执行异常
     */
    @Around("@annotation(container)")
    public Object aroundMultiple(ProceedingJoinPoint joinPoint, RateLimits container) throws Throwable {
        return processRateLimit(joinPoint, container.value());
    }

    /**
     * 处理限流逻辑
     *
     * @param joinPoint   切点
     * @param annotations 限流注解数组
     * @return 方法执行结果
     * @throws Throwable 方法执行异常
     */
    private Object processRateLimit(ProceedingJoinPoint joinPoint, RateLimit[] annotations) throws Throwable {
        // 检查是否启用限流
        if (!properties.isEnabled()) {
            return joinPoint.proceed();
        }

        // 遍历所有限流规则
        RateLimitResult lastResult = null;
        RateLimit triggeredAnnotation = null;

        for (RateLimit annotation : annotations) {
            RateLimitResult result = rateLimiter.tryAcquire(annotation);

            // 记录限流指标
            recordMetrics(annotation, result);

            if (!result.allowed()) {
                lastResult = result;
                triggeredAnnotation = annotation;
                log.warn("Rate limit triggered for method: {}, key: {}, dimension: {}",
                        joinPoint.getSignature(), annotation.key(), annotation.dimension());
                break;
            }

            lastResult = result;
        }

        // 所有限流检查通过
        if (lastResult != null && lastResult.allowed()) {
            return joinPoint.proceed();
        }

        // 触发限流
        if (triggeredAnnotation != null) {
            // 尝试执行回退方法
            String fallbackMethod = triggeredAnnotation.fallbackMethod();
            if (fallbackMethod != null && !fallbackMethod.isEmpty() && properties.getFallback().isEnabled()) {
                Object fallbackResult = executeFallback(joinPoint, fallbackMethod);
                if (fallbackResult != null) {
                    return fallbackResult;
                }
            }

            // 无回退方法或回退执行失败，抛出限流异常
            throw new BusinessException(
                    CommonErrorCode.RATE_LIMIT_EXCEEDED,
                    rateLimiter.getRateLimitMessage(triggeredAnnotation));
        }

        return joinPoint.proceed();
    }

    /**
     * 记录限流指标
     *
     * @param annotation 限流注解
     * @param result     限流结果
     */
    private void recordMetrics(RateLimit annotation, RateLimitResult result) {
        if (meterRegistry == null || !properties.getMetrics().isEnabled()) {
            return;
        }

        String prefix = properties.getMetrics().getPrefix();
        String name = prefix + ".requests";

        Counter counter = Counter.builder(name)
                .tag("key", annotation.key())
                .tag("dimension", annotation.dimension().name().toLowerCase())
                .tag("algorithm", annotation.algorithm().name().toLowerCase())
                .tag("result", result.allowed() ? "allowed" : "rejected")
                .description("Rate limit request counter")
                .register(meterRegistry);

        counter.increment();
    }

    /**
     * 执行回退方法
     *
     * @param joinPoint      切点
     * @param fallbackMethod 回退方法名称
     * @return 回退方法执行结果，如果执行失败返回 null
     */
    private @Nullable Object executeFallback(ProceedingJoinPoint joinPoint, String fallbackMethod) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Class<?> targetClass = joinPoint.getTarget().getClass();
            Method targetMethod = signature.getMethod();
            Object[] args = joinPoint.getArgs();

            // 查找回退方法
            Method fallback = findFallbackMethod(targetClass, fallbackMethod, targetMethod.getParameterTypes());

            if (fallback == null) {
                log.warn("Fallback method not found: {} in class {}", fallbackMethod, targetClass.getName());
                return null;
            }

            // 执行回退方法
            fallback.setAccessible(true);
            return fallback.invoke(joinPoint.getTarget(), args);
        } catch (Exception e) {
            log.error("Failed to execute fallback method: {}", fallbackMethod, e);
            return null;
        }
    }

    /**
     * 查找回退方法
     *
     * @param targetClass    目标类
     * @param methodName     方法名称
     * @param parameterTypes 参数类型
     * @return 回退方法，如果找不到返回 null
     */
    private @Nullable Method findFallbackMethod(Class<?> targetClass, String methodName, Class<?>[] parameterTypes) {
        // 先尝试精确匹配
        try {
            return targetClass.getDeclaredMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            // 忽略，尝试模糊匹配
        }

        // 模糊匹配：方法名相同且参数数量相同
        for (Method method : targetClass.getDeclaredMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == parameterTypes.length) {
                return method;
            }
        }

        return null;
    }
}
