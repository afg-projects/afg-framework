package io.github.afgprojects.framework.core.feature;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 功能开关切面
 * <p>
 * 拦截 @{@link FeatureToggle} 注解，根据功能开关状态决定是否执行方法
 * </p>
 */
@Aspect
@SuppressWarnings({
    "PMD.SignatureDeclareThrowsException",
    "PMD.AvoidThrowingRawExceptionTypes",
    "PMD.AvoidCatchingGenericException"
})
public class FeatureToggleAspect {

    private static final Logger log = LoggerFactory.getLogger(FeatureToggleAspect.class);

    private final FeatureFlagManager featureFlagManager;

    /**
     * 构造函数
     *
     * @param featureFlagManager 功能开关管理器
     */
    public FeatureToggleAspect(@NonNull FeatureFlagManager featureFlagManager) {
        this.featureFlagManager = featureFlagManager;
    }

    /**
     * 功能开关切面
     *
     * @param joinPoint  切点
     * @param annotation 注解实例
     * @return 方法执行结果
     * @throws Throwable 方法执行异常
     */
    @Around("@annotation(annotation)")
    public Object around(ProceedingJoinPoint joinPoint, FeatureToggle annotation) throws Throwable {
        String featureName = annotation.feature();
        boolean enabledByDefault = annotation.enabledByDefault();
        String fallbackMethodName = annotation.fallbackMethod();

        // 判断功能是否启用
        boolean enabled = featureFlagManager.isEnabled(featureName, enabledByDefault);

        if (enabled) {
            log.debug("功能开关已启用: {}", featureName);
            return joinPoint.proceed();
        }

        log.info("功能开关已禁用: {}", featureName);

        // 功能禁用，尝试执行回退方法
        if (!fallbackMethodName.isEmpty()) {
            return executeFallback(joinPoint, fallbackMethodName, featureName);
        }

        // 无回退方法，抛出异常
        throw new FeatureDisabledException(featureName);
    }

    /**
     * 执行回退方法
     *
     * @param joinPoint         原方法切点
     * @param fallbackMethodName 回退方法名称
     * @param featureName       功能名称
     * @return 回退方法执行结果
     * @throws Throwable 执行异常
     */
    private @Nullable Object executeFallback(
            ProceedingJoinPoint joinPoint, String fallbackMethodName, String featureName) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Class<?> targetClass = joinPoint.getTarget().getClass();
        Object[] args = joinPoint.getArgs();
        Class<?>[] parameterTypes = signature.getParameterTypes();

        try {
            // 查找回退方法
            Method fallbackMethod = findFallbackMethod(targetClass, fallbackMethodName, parameterTypes);
            if (fallbackMethod == null) {
                log.error(
                        "回退方法不存在: {}.{}({})",
                        targetClass.getSimpleName(),
                        fallbackMethodName,
                        Arrays.toString(parameterTypes));
                throw new FeatureDisabledException(
                        featureName, "回退方法不存在: " + fallbackMethodName);
            }

            // 执行回退方法
            fallbackMethod.setAccessible(true);
            log.debug("执行回退方法: {}", fallbackMethodName);
            return fallbackMethod.invoke(joinPoint.getTarget(), args);
        } catch (Exception e) {
            log.error("执行回退方法失败: {}", fallbackMethodName, e);
            if (e.getCause() != null) {
                // 保留原始异常作为 suppressed 异常以便调试
                FeatureDisabledException fde = new FeatureDisabledException(
                        featureName, "回退方法执行失败: " + e.getCause().getMessage(), e.getCause());
                fde.addSuppressed(e);
                throw fde;
            }
            throw new FeatureDisabledException(featureName, "回退方法执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 查找回退方法
     *
     * @param targetClass     目标类
     * @param methodName      方法名称
     * @param parameterTypes  参数类型
     * @return 回退方法，不存在则返回 null
     */
    private @Nullable Method findFallbackMethod(
            Class<?> targetClass, String methodName, Class<?>[] parameterTypes) {
        // 先尝试精确匹配
        try {
            return targetClass.getDeclaredMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            // 精确匹配失败，尝试宽松匹配
            return findMethodByNameAndParamCount(targetClass, methodName, parameterTypes.length);
        }
    }

    /**
     * 按方法名和参数数量查找方法
     *
     * @param targetClass 目标类
     * @param methodName  方法名称
     * @param paramCount  参数数量
     * @return 方法，不存在则返回 null
     */
    private @Nullable Method findMethodByNameAndParamCount(Class<?> targetClass, String methodName, int paramCount) {
        for (Method method : targetClass.getDeclaredMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == paramCount) {
                return method;
            }
        }
        // 递归查找父类
        Class<?> superClass = targetClass.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            return findMethodByNameAndParamCount(superClass, methodName, paramCount);
        }
        return null;
    }
}