package io.github.afgprojects.framework.ai.core.resilience;

import io.github.afgprojects.framework.ai.core.api.resilience.FallbackStrategy;
import io.github.afgprojects.framework.ai.core.api.resilience.ResilienceExecutor;
import io.github.afgprojects.framework.ai.core.config.AfgAiProperties;
import io.github.afgprojects.framework.ai.core.resilience.annotation.AiResilient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * @AiResilient 注解的 AOP 切面，拦截标注了 @AiResilient 的方法，提供重试、熔断和降级能力。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Aspect
@Slf4j
@RequiredArgsConstructor
public class AiResilientAspect {

    private final ResilienceExecutor resilienceExecutor;
    private final AfgAiProperties properties;

    @Around("@annotation(aiResilient)")
    public Object aroundAiResilient(ProceedingJoinPoint joinPoint, AiResilient aiResilient) throws Throwable {
        log.debug("AiResilient aspect: retry={}, circuitBreaker={}", aiResilient.retry(), aiResilient.circuitBreaker());

        // 构建降级策略
        String fallbackMethodName = aiResilient.fallbackMethod();
        FallbackStrategy<Object> fallback = fallbackMethodName.isEmpty()
                ? new ThrowingFallbackStrategy()
                : new MethodFallbackStrategy(joinPoint, fallbackMethodName);

        // 使用 ResilienceExecutor 执行带韧性保护的方法
        return resilienceExecutor.executeWithFallback(
                (ResilienceExecutor.ResilientOperation<Object>) () -> {
                    try {
                        return joinPoint.proceed();
                    } catch (Exception e) {
                        throw e;
                    } catch (Throwable t) {
                        throw new RuntimeException(t);
                    }
                },
                fallback
        );
    }

    /**
     * 抛出异常的降级策略（无降级）
     */
    private static class ThrowingFallbackStrategy implements FallbackStrategy<Object> {
        @Override
        @Nullable
        public Object fallback(@org.jspecify.annotations.NonNull Exception exception,
                               @org.jspecify.annotations.NonNull FallbackContext context) {
            return null;
        }

        @Override
        public boolean shouldFallback(@org.jspecify.annotations.NonNull Exception exception) {
            return false;
        }
    }

    /**
     * 基于方法名的降级策略
     */
    private static class MethodFallbackStrategy implements FallbackStrategy<Object> {

        private final ProceedingJoinPoint joinPoint;
        private final String fallbackMethodName;

        MethodFallbackStrategy(ProceedingJoinPoint joinPoint, String fallbackMethodName) {
            this.joinPoint = joinPoint;
            this.fallbackMethodName = fallbackMethodName;
        }

        @Override
        @Nullable
        public Object fallback(@org.jspecify.annotations.NonNull Exception exception,
                               @org.jspecify.annotations.NonNull FallbackContext context) {
            try {
                MethodSignature signature = (MethodSignature) joinPoint.getSignature();
                Method fallbackMethod = findFallbackMethod(signature.getDeclaringType(),
                        fallbackMethodName, signature.getParameterTypes());
                if (fallbackMethod != null) {
                    fallbackMethod.setAccessible(true);
                    return fallbackMethod.invoke(joinPoint.getTarget(), joinPoint.getArgs());
                }
                log.warn("Fallback method not found: {}", fallbackMethodName);
            } catch (Exception e) {
                log.error("Fallback method execution failed: {}", fallbackMethodName, e);
            }
            return null;
        }

        @Override
        public boolean shouldFallback(@org.jspecify.annotations.NonNull Exception exception) {
            return true;
        }

        @Nullable
        private Method findFallbackMethod(Class<?> targetClass, String methodName, Class<?>[] paramTypes) {
            try {
                return targetClass.getDeclaredMethod(methodName, paramTypes);
            } catch (NoSuchMethodException e) {
                Class<?> superClass = targetClass.getSuperclass();
                if (superClass != null) {
                    return findFallbackMethod(superClass, methodName, paramTypes);
                }
                return null;
            }
        }
    }
}
