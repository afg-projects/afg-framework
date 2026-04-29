package io.github.afgprojects.framework.integration.redis.lock;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.concurrent.TimeUnit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import io.github.afgprojects.framework.core.lock.DistributedLock;
import io.github.afgprojects.framework.core.lock.Lock;
import io.github.afgprojects.framework.core.lock.LockType;
import io.github.afgprojects.framework.core.lock.exception.LockAcquisitionException;
import io.github.afgprojects.framework.core.lock.exception.LockException;

/**
 * Redis 分布式锁切面
 * <p>
 * 拦截 @{@link Lock} 注解，自动在方法执行前获取锁，执行后释放锁。
 * 支持 SpEL 表达式动态生成锁的键。
 * </p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 基本用法
 * @Lock(key = "user-update")
 * public void updateUser(User user) { ... }
 *
 * // 使用 SpEL 表达式
 * @Lock(key = "#userId")
 * public User getUser(String userId) { ... }
 *
 * // 指定锁类型和超时时间
 * @Lock(key = "order-#orderId", waitTime = 5000, leaseTime = 30000, lockType = LockType.FAIR)
 * public void processOrder(String orderId) { ... }
 * }</pre>
 */
@Aspect
@SuppressWarnings({
    "PMD.SignatureDeclareThrowsException",
    "PMD.AvoidThrowingRawExceptionTypes",
    "PMD.AvoidCatchingGenericException"
})
public class LockAspect {

    private static final Logger log = LoggerFactory.getLogger(LockAspect.class);

    /**
     * SpEL 表达式解析器
     */
    private final SpelExpressionParser parser = new SpelExpressionParser();

    /**
     * 分布式锁服务
     */
    private final DistributedLock distributedLock;

    /**
     * 锁配置属性
     */
    private final LockProperties properties;

    /**
     * 构造锁切面
     *
     * @param distributedLock 分布锁服务
     * @param properties      锁配置属性
     */
    public LockAspect(@NonNull DistributedLock distributedLock, @NonNull LockProperties properties) {
        this.distributedLock = distributedLock;
        this.properties = properties;
    }

    /**
     * 处理 @Lock 注解
     *
     * @param joinPoint  切点
     * @param annotation 注解实例
     * @return 方法执行结果
     * @throws Throwable 方法执行异常
     */
    @Around("@annotation(annotation)")
    public Object aroundLock(ProceedingJoinPoint joinPoint, Lock annotation) throws Throwable {
        String key = generateKey(joinPoint, annotation.key(), annotation.prefix());
        long waitTime = resolveWaitTime(annotation);
        long leaseTime = resolveLeaseTime(annotation);
        LockType lockType = annotation.lockType();

        boolean acquired = false;

        try {
            // 尝试获取锁
            acquired = distributedLock.tryLock(key, waitTime, leaseTime, lockType);

            if (!acquired) {
                log.warn("Failed to acquire lock: key={}", key);

                if (annotation.throwOnFailure()) {
                    // 直接返回失败结果，不使用异常作为流程控制
                    return handleLockFailure(key, annotation.message());
                }

                // 不抛异常，直接执行方法（降级处理）
                log.info("Lock acquisition failed, but proceeding without lock (throwOnFailure=false)");
                return joinPoint.proceed();
            }

            log.debug("Lock acquired: key={}", key);

            // 执行目标方法
            return joinPoint.proceed();

        } finally {
            // 释放锁
            if (acquired) {
                try {
                    distributedLock.unlock(key, lockType);
                    log.debug("Lock released: key={}", key);
                } catch (Exception e) {
                    log.error("Failed to release lock: key={}", key, e);
                }
            }
        }
    }

    /**
     * 处理锁获取失败
     *
     * @param key     锁键
     * @param message 错误消息
     * @return 不返回，直接抛出异常
     */
    private Object handleLockFailure(String key, String message) {
        throw new LockAcquisitionException(key, message);
    }

    /**
     * 生成锁键
     *
     * @param joinPoint 切点
     * @param keyExpr   SpEL 表达式
     * @param prefix    键前缀
     * @return 锁键
     */
    @NonNull
    private String generateKey(ProceedingJoinPoint joinPoint, String keyExpr, String prefix) {
        String key;

        if (keyExpr != null && !keyExpr.isEmpty()) {
            // 使用 SpEL 表达式生成 key
            key = evaluateExpression(joinPoint, keyExpr);
        } else {
            // 使用默认 key（类名.方法名）
            key = generateDefaultKey(joinPoint);
        }

        // 添加前缀
        if (prefix != null && !prefix.isEmpty()) {
            key = prefix + ":" + key;
        }

        return key;
    }

    /**
     * 生成默认锁键
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
            throw new LockException(expression, "Failed to evaluate SpEL expression: " + expression, e);
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
     * 解析等待时间
     *
     * @param annotation 锁注解
     * @return 等待时间（毫秒）
     */
    private long resolveWaitTime(Lock annotation) {
        if (annotation.waitTime() >= 0) {
            return convertToMillis(annotation.waitTime(), annotation.timeUnit());
        }
        return properties.getDefaultWaitTime();
    }

    /**
     * 解析持有时间
     *
     * @param annotation 锁注解
     * @return 持有时间（毫秒），-1 表示使用 watchdog
     */
    private long resolveLeaseTime(Lock annotation) {
        if (annotation.leaseTime() >= 0) {
            return convertToMillis(annotation.leaseTime(), annotation.timeUnit());
        }
        return properties.getDefaultLeaseTime();
    }

    /**
     * 转换时间到毫秒
     *
     * @param time     时间值
     * @param timeUnit 时间单位
     * @return 毫秒数
     */
    private long convertToMillis(long time, Lock.TimeUnit timeUnit) {
        return switch (timeUnit) {
            case MILLISECONDS -> time;
            case SECONDS -> TimeUnit.SECONDS.toMillis(time);
            case MINUTES -> TimeUnit.MINUTES.toMillis(time);
        };
    }
}
