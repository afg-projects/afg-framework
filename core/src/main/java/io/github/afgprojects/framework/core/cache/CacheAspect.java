package io.github.afgprojects.framework.core.cache;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.stream.Collectors;

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

import io.github.afgprojects.framework.core.cache.exception.CacheException;

/**
 * 缓存切面
 * <p>
 * 拦截 @{@link Cached}、@{@link CacheEvict}、@{@link CachePut} 注解，
 * 自动执行缓存操作
 * </p>
 */
@Aspect
@SuppressWarnings({
    "PMD.SignatureDeclareThrowsException",
    "PMD.AvoidThrowingRawExceptionTypes",
    "PMD.AvoidCatchingGenericException"
})
public class CacheAspect {

    private static final Logger log = LoggerFactory.getLogger(CacheAspect.class);

    /**
     * SpEL 表达式解析器
     */
    private final SpelExpressionParser parser = new SpelExpressionParser();

    /**
     * 缓存管理器
     */
    private final DefaultCacheManager cacheManager;

    /**
     * 构造缓存切面
     *
     * @param cacheManager 缓存管理器
     */
    public CacheAspect(@NonNull DefaultCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * 处理 @Cached 注解
     *
     * @param joinPoint  切点
     * @param annotation 注解
     * @return 方法返回值（可能是缓存值）
     * @throws Throwable 方法执行异常
     */
    @Around("@annotation(annotation)")
    public Object aroundCached(ProceedingJoinPoint joinPoint, Cached annotation) throws Throwable {
        String cacheName = annotation.cacheName();
        String key = generateKey(joinPoint, annotation.key(), annotation.keyPrefix());
        long ttl = annotation.timeUnit().toMillis(annotation.ttl());

        AfgCache<Object> cache = cacheManager.getCache(cacheName);

        // 检查条件
        if (!evaluateCondition(joinPoint, annotation.condition(), null, true)) {
            return joinPoint.proceed();
        }

        // 尝试从缓存获取
        Object cachedValue = cache.get(key);
        if (cachedValue != null) {
            log.debug("Cache hit: cacheName={}, key={}", cacheName, key);
            return cachedValue;
        }

        // 缓存未命中，执行方法
        log.debug("Cache miss: cacheName={}, key={}", cacheName, key);
        Object result = joinPoint.proceed();

        // 检查 unless 条件
        if (evaluateUnless(joinPoint, annotation.unless(), result)) {
            return result;
        }

        // 缓存结果
        if (result != null || annotation.cacheNull()) {
            cache.put(key, result, ttl);
        }

        return result;
    }

    /**
     * 处理 @CacheEvict 注解
     *
     * @param joinPoint  切点
     * @param annotation 注解
     * @return 方法返回值
     * @throws Throwable 方法执行异常
     */
    @Around("@annotation(annotation)")
    public Object aroundCacheEvict(ProceedingJoinPoint joinPoint, CacheEvict annotation) throws Throwable {
        String cacheName = annotation.cacheName();
        AfgCache<Object> cache = cacheManager.getCache(cacheName);

        // 检查条件
        if (!evaluateCondition(joinPoint, annotation.condition(), null, true)) {
            return joinPoint.proceed();
        }

        // beforeInvocation 为 true 时，在方法执行前清除缓存
        if (annotation.beforeInvocation()) {
            evictCache(cache, joinPoint, annotation);
        }

        // 执行方法
        Object result = joinPoint.proceed();

        // beforeInvocation 为 false 时，在方法执行后清除缓存
        if (!annotation.beforeInvocation()) {
            evictCache(cache, joinPoint, annotation);
        }

        return result;
    }

    /**
     * 处理 @CachePut 注解
     *
     * @param joinPoint  切点
     * @param annotation 注解
     * @return 方法返回值
     * @throws Throwable 方法执行异常
     */
    @Around("@annotation(annotation)")
    public Object aroundCachePut(ProceedingJoinPoint joinPoint, CachePut annotation) throws Throwable {
        String cacheName = annotation.cacheName();
        String key = generateKey(joinPoint, annotation.key(), annotation.keyPrefix());
        long ttl = annotation.timeUnit().toMillis(annotation.ttl());

        AfgCache<Object> cache = cacheManager.getCache(cacheName);

        // 执行方法
        Object result = joinPoint.proceed();

        // 检查条件
        if (!evaluateCondition(joinPoint, annotation.condition(), result, true)) {
            return result;
        }

        // 检查 unless 条件
        if (evaluateUnless(joinPoint, annotation.unless(), result)) {
            return result;
        }

        // 更新缓存
        if (result != null || annotation.cacheNull()) {
            cache.put(key, result, ttl);
            log.debug("Cache put: cacheName={}, key={}", cacheName, key);
        }

        return result;
    }

    /**
     * 清除缓存
     */
    private void evictCache(AfgCache<Object> cache, ProceedingJoinPoint joinPoint, CacheEvict annotation) {
        if (annotation.allEntries()) {
            cache.clear();
            log.debug("Cache clear: cacheName={}", annotation.cacheName());
        } else {
            String key = generateKey(joinPoint, annotation.key(), annotation.keyPrefix());
            cache.evict(key);
            log.debug("Cache evict: cacheName={}, key={}", annotation.cacheName(), key);
        }
    }

    /**
     * 生成缓存键
     *
     * @param joinPoint  切点
     * @param keyExpr    SpEL 表达式
     * @param keyPrefix  键前缀
     * @return 缓存键
     */
    @NonNull
    private String generateKey(ProceedingJoinPoint joinPoint, String keyExpr, String keyPrefix) {
        String key;

        if (keyExpr != null && !keyExpr.isEmpty()) {
            // 使用 SpEL 表达式生成 key
            key = evaluateExpression(joinPoint, keyExpr, null);
        } else {
            // 默认使用所有参数生成 key
            key = generateDefaultKey(joinPoint);
        }

        // 添加前缀
        if (keyPrefix != null && !keyPrefix.isEmpty()) {
            key = keyPrefix + key;
        }

        return key;
    }

    /**
     * 生成默认缓存键
     */
    @NonNull
    private String generateDefaultKey(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        // 使用方法名和参数生成 key
        StringBuilder sb = new StringBuilder(method.getName());

        if (args.length > 0) {
            sb.append(":");
            sb.append(Arrays.stream(args)
                    .map(arg -> arg == null ? "null" : arg.toString())
                    .collect(Collectors.joining(":")));
        }

        return sb.toString();
    }

    /**
     * 评估 SpEL 表达式
     *
     * @param joinPoint 切点
     * @param expression SpEL 表达式
     * @param result     方法返回值
     * @return 表达式计算结果
     */
    @NonNull
    private String evaluateExpression(ProceedingJoinPoint joinPoint, String expression, Object result) {
        try {
            EvaluationContext context = createEvaluationContext(joinPoint, result);
            Expression expr = parser.parseExpression(expression);
            Object value = expr.getValue(context);
            return value == null ? "null" : value.toString();
        } catch (Exception e) {
            throw new CacheException("Failed to evaluate SpEL expression: " + expression, e);
        }
    }

    /**
     * 评估条件表达式
     *
     * @param joinPoint  切点
     * @param condition  条件表达式
     * @param result     方法返回值
     * @param defaultVal 默认值（表达式为空时返回）
     * @return 条件是否满足
     */
    private boolean evaluateCondition(ProceedingJoinPoint joinPoint, String condition, Object result, boolean defaultVal) {
        if (condition == null || condition.isEmpty()) {
            return defaultVal;
        }
        try {
            EvaluationContext context = createEvaluationContext(joinPoint, result);
            Expression expr = parser.parseExpression(condition);
            Boolean value = expr.getValue(context, Boolean.class);
            return value != null && value;
        } catch (Exception e) {
            log.warn("Failed to evaluate condition expression: {}", condition, e);
            return defaultVal;
        }
    }

    /**
     * 评估 unless 表达式
     *
     * @param joinPoint 切点
     * @param unless    unless 表达式
     * @param result    方法返回值
     * @return 是否排除缓存
     */
    private boolean evaluateUnless(ProceedingJoinPoint joinPoint, String unless, Object result) {
        if (unless == null || unless.isEmpty()) {
            return false;
        }
        try {
            EvaluationContext context = createEvaluationContext(joinPoint, result);
            Expression expr = parser.parseExpression(unless);
            Boolean value = expr.getValue(context, Boolean.class);
            return value != null && value;
        } catch (Exception e) {
            log.warn("Failed to evaluate unless expression: {}", unless, e);
            return false;
        }
    }

    /**
     * 创建 SpEL 评估上下文
     *
     * @param joinPoint 切点
     * @param result    方法返回值（可选）
     * @return 评估上下文
     */
    @NonNull
    private EvaluationContext createEvaluationContext(ProceedingJoinPoint joinPoint, Object result) {
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

        // 设置返回值变量
        if (result != null) {
            context.setVariable("result", result);
        }

        return context;
    }
}