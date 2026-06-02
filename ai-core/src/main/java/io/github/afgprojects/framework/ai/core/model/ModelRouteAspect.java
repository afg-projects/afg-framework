package io.github.afgprojects.framework.ai.core.model;

import io.github.afgprojects.framework.ai.core.api.model.ModelRegistry;
import io.github.afgprojects.framework.ai.core.model.annotation.ModelRoute;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * @ModelRoute 注解的 AOP 切面，将模型路由信息放入 ThreadLocal 上下文。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Aspect
@Slf4j
@RequiredArgsConstructor
public class ModelRouteAspect {

    private final ModelRegistry modelRegistry;

    @Around("@annotation(modelRoute)")
    public Object aroundModelRoute(ProceedingJoinPoint joinPoint, ModelRoute modelRoute) throws Throwable {
        String modelName = modelRoute.value();
        String fallbackModel = modelRoute.fallback();

        log.debug("ModelRoute aspect: routing to model {} (fallback: {})", modelName, fallbackModel);

        // 验证模型是否存在
        modelRegistry.getModel(modelName)
                .orElseThrow(() -> new IllegalArgumentException("Model not found: " + modelName));

        // 将模型路由信息放入 ThreadLocal
        try {
            ModelRouteContext.set(modelName, fallbackModel);
            return joinPoint.proceed();
        } finally {
            ModelRouteContext.clear();
        }
    }
}
