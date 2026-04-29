package io.github.afgprojects.framework.core.audit;

import java.lang.reflect.Field;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 审计目标表达式解析器
 * <p>
 * 负责解析 SpEL 风格的目标表达式（如 #userId, #request.id）
 * </p>
 */
@SuppressWarnings("PMD.AvoidCatchingGenericException")
class TargetExpressionResolver {

    private static final Logger log = LoggerFactory.getLogger(TargetExpressionResolver.class);

    /**
     * 提取目标对象
     * <p>
     * 目前仅支持简单参数引用，如 "#userId" 或 "#request.id"
     * </p>
     *
     * @param annotation 审计注解
     * @param joinPoint  切点
     * @return 解析后的目标值
     */
    @Nullable String extractTarget(@NonNull Audited annotation, @NonNull ProceedingJoinPoint joinPoint) {
        String targetExpr = annotation.target();
        if (targetExpr.isEmpty()) {
            return null;
        }

        try {
            // 简单的参数引用解析（如 #userId, #request.id）
            if (targetExpr.startsWith("#")) {
                String paramName = targetExpr.substring(1);

                // 处理属性访问（如 request.id）
                int dotIndex = paramName.indexOf('.');
                if (dotIndex > 0) {
                    String rootParam = paramName.substring(0, dotIndex);
                    String property = paramName.substring(dotIndex + 1);
                    Object rootValue = getArgValue(joinPoint, rootParam);
                    if (rootValue != null) {
                        return extractProperty(rootValue, property);
                    }
                } else {
                    Object value = getArgValue(joinPoint, paramName);
                    return value != null ? value.toString() : null;
                }
            }
        } catch (RuntimeException e) {
            log.debug("Failed to extract target: {}", e.getMessage());
        }

        return null;
    }

    /**
     * 获取参数值
     *
     * @param joinPoint 切点
     * @param paramName 参数名
     * @return 参数值
     */
    @Nullable Object getArgValue(@NonNull ProceedingJoinPoint joinPoint, @NonNull String paramName) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                if (paramNames[i].equals(paramName) && i < args.length) {
                    return args[i];
                }
            }
        }
        return null;
    }

    /**
     * 提取对象属性值
     *
     * @param obj      对象实例
     * @param property 属性名
     * @return 属性值字符串
     */
    @Nullable String extractProperty(@NonNull Object obj, @NonNull String property) {
        try {
            Class<?> clazz = obj.getClass();
            Field field = clazz.getDeclaredField(property);
            field.setAccessible(true);
            Object value = field.get(obj);
            return value != null ? value.toString() : null;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.debug("Failed to extract property {}.{}: {}", obj.getClass().getSimpleName(), property, e.getMessage());
            return null;
        }
    }
}
