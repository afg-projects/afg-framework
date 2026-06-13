package io.github.afgprojects.framework.core.duplicatesubmit;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.jspecify.annotations.NonNull;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import io.github.afgprojects.framework.core.api.duplicatesubmit.DuplicateSubmitChecker;
import io.github.afgprojects.framework.core.config.AfgCoreProperties;
import io.github.afgprojects.framework.core.duplicatesubmit.exception.DuplicateSubmitException;
import lombok.extern.slf4j.Slf4j;

/**
 * 防重复提交切面
 * <p>
 * 拦截 @{@link DuplicateSubmit} 注解，自动在方法执行前进行去重检查。
 * 支持 SpEL 表达式动态生成去重键。
 * </p>
 *
 * @since 1.0.0
 */
@Aspect
@Slf4j
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public class DuplicateSubmitAspect {

    /**
     * SpEL 表达式解析器
     */
    private final SpelExpressionParser parser = new SpelExpressionParser();

    /**
     * 重复提交检查器
     */
    private final DuplicateSubmitChecker checker;

    /**
     * 核心配置属性
     */
    private final AfgCoreProperties properties;

    /**
     * 构造防重复提交切面
     *
     * @param checker    重复提交检查器
     * @param properties 核心配置属性
     */
    public DuplicateSubmitAspect(@NonNull DuplicateSubmitChecker checker,
                                 @NonNull AfgCoreProperties properties) {
        this.checker = checker;
        this.properties = properties;
    }

    /**
     * 处理 @DuplicateSubmit 注解
     *
     * @param joinPoint  切点
     * @param annotation 注解实例
     * @return 方法执行结果
     * @throws DuplicateSubmitException 重复提交被拦截
     * @throws RuntimeException         方法执行异常
     */
    @Around("@annotation(annotation)")
    public Object aroundDuplicateSubmit(ProceedingJoinPoint joinPoint, DuplicateSubmit annotation)
            throws DuplicateSubmitException, RuntimeException {
        String key = generateKey(joinPoint, annotation.key(), annotation.prefix());
        long interval = resolveInterval(annotation);

        // 尝试获取去重标记
        boolean acquired = checker.tryAcquire(key, interval);

        if (!acquired) {
            log.warn("Duplicate submit detected: key={}, interval={}ms", key, interval);
            throw new DuplicateSubmitException(key, annotation.message());
        }

        log.debug("Duplicate submit check passed: key={}, interval={}ms", key, interval);

        // 执行目标方法
        return proceedSafely(joinPoint);
    }

    /**
     * 安全执行目标方法
     *
     * @param joinPoint 切点
     * @return 方法执行结果
     * @throws RuntimeException 方法执行异常
     */
    @SuppressWarnings("PMD.IdenticalCatchBranches")
    private Object proceedSafely(ProceedingJoinPoint joinPoint) {
        try {
            return joinPoint.proceed();
        } catch (RuntimeException e) {
            throw e;
        } catch (Error e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException("Unexpected exception in duplicate-submit protected method", e);
        }
    }

    /**
     * 生成去重键
     *
     * @param joinPoint 切点
     * @param keyExpr   SpEL 表达式或静态键
     * @param prefix    键前缀
     * @return 去重键
     */
    @NonNull
    private String generateKey(ProceedingJoinPoint joinPoint, String keyExpr, String prefix) {
        String key;

        if (keyExpr != null && !keyExpr.isEmpty()) {
            // 如果是 SpEL 表达式（以 # 开头），则解析
            if (keyExpr.startsWith("#")) {
                key = evaluateExpression(joinPoint, keyExpr);
            } else {
                // 静态键，直接使用
                key = keyExpr;
            }
        } else {
            // 使用默认 key（类名.方法名）
            key = generateDefaultKey(joinPoint);
        }

        // 添加前缀
        if (prefix != null && !prefix.isEmpty()) {
            key = prefix + ":" + key;
        } else {
            // 使用配置中的默认前缀
            key = properties.getDuplicateSubmit().getKeyPrefix() + ":" + key;
        }

        return key;
    }

    /**
     * 生成默认去重键
     */
    @NonNull
    private String generateDefaultKey(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        return className + "." + methodName;
    }

    /**
     * 评估 SpEL 表达式
     *
     * @param joinPoint  切点
     * @param expression SpEL 表达式
     * @return 表达式计算结果
     */
    @NonNull
    private String evaluateExpression(ProceedingJoinPoint joinPoint, String expression) {
        try {
            EvaluationContext context = createEvaluationContext(joinPoint);
            Expression expr = parser.parseExpression(expression);
            Object value = expr.getValue(context);
            return value == null ? "null" : value.toString();
        } catch (Exception e) {
            log.error("Failed to evaluate SpEL expression: {}", expression, e);
            throw new DuplicateSubmitException(expression,
                    "防重复提交键表达式解析失败: " + expression, e);
        }
    }

    /**
     * 创建 SpEL 评估上下文
     *
     * @param joinPoint 切点
     * @return 评估上下文
     */
    @NonNull
    private EvaluationContext createEvaluationContext(ProceedingJoinPoint joinPoint) {
        StandardEvaluationContext context = new StandardEvaluationContext();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        // 设置参数变量
        for (int i = 0; i < parameters.length; i++) {
            String paramName = parameters[i].getName();
            context.setVariable(paramName, args[i]);
            context.setVariable("p" + i, args[i]);
            context.setVariable("a" + i, args[i]);
        }

        return context;
    }

    /**
     * 解析去重间隔
     *
     * @param annotation 防重复提交注解
     * @return 去重间隔（毫秒）
     */
    private long resolveInterval(DuplicateSubmit annotation) {
        if (annotation.interval() > 0) {
            return annotation.interval();
        }
        return properties.getDuplicateSubmit().getDefaultInterval();
    }
}
