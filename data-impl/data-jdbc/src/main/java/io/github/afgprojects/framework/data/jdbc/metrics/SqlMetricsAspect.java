package io.github.afgprojects.framework.data.jdbc.metrics;

import io.github.afgprojects.framework.data.core.EntityProxy;
import io.github.afgprojects.framework.data.core.query.Page;
import io.github.afgprojects.framework.data.jdbc.JdbcEntityProxy;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * SQL 执行指标切面
 * <p>
 * 拦截 {@link EntityProxy} 的 CRUD 操作，记录执行时间和指标。
 *
 * <h3>拦截的方法</h3>
 * <ul>
 *   <li>save/saveAll - INSERT 或 UPDATE</li>
 *   <li>insert/insertAll - INSERT</li>
 *   <li>update/updateAll - UPDATE</li>
 *   <li>findById/findAll/findAllById - SELECT</li>
 *   <li>count/existsById - SELECT (COUNT)</li>
 *   <li>deleteById/delete/deleteAllById/deleteAll - DELETE</li>
 *   <li>findAll(Condition) - SELECT</li>
 *   <li>updateAll(Condition, Map) - UPDATE</li>
 *   <li>deleteAll(Condition) - DELETE</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Aspect
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public class SqlMetricsAspect {

    private static final Logger log = LoggerFactory.getLogger(SqlMetricsAspect.class);

    private final SqlMetrics sqlMetrics;
    private final SqlMetricsProperties properties;

    /**
     * 构造 SQL 指标切面
     *
     * @param sqlMetrics SQL 指标记录器
     * @param properties 配置属性
     */
    public SqlMetricsAspect(@NonNull SqlMetrics sqlMetrics, @NonNull SqlMetricsProperties properties) {
        this.sqlMetrics = sqlMetrics;
        this.properties = properties;
        // 设置默认的慢查询监听器
        sqlMetrics.setSlowQueryListener(this::handleSlowQuery);
    }

    /**
     * 拦截 save 方法
     */
    @Around("execution(* io.github.afgprojects.framework.data.jdbc.JdbcEntityProxy.save(..))")
    public @NonNull Object aroundSave(ProceedingJoinPoint pjp) throws Throwable {
        return executeWithMetrics(pjp, SqlOperationType.OTHER, "save", true);
    }

    /**
     * 拦截 saveAll 方法
     */
    @Around("execution(* io.github.afgprojects.framework.data.jdbc.JdbcEntityProxy.saveAll(..))")
    public @NonNull Object aroundSaveAll(ProceedingJoinPoint pjp) throws Throwable {
        return executeWithMetrics(pjp, SqlOperationType.OTHER, "saveAll", true);
    }

    /**
     * 拦截 insert 方法
     */
    @Around("execution(* io.github.afgprojects.framework.data.jdbc.JdbcEntityProxy.insert(..))")
    public @NonNull Object aroundInsert(ProceedingJoinPoint pjp) throws Throwable {
        return executeWithMetrics(pjp, SqlOperationType.INSERT, "insert", true);
    }

    /**
     * 拦截 insertAll 方法
     */
    @Around("execution(* io.github.afgprojects.framework.data.jdbc.JdbcEntityProxy.insertAll(..))")
    public @NonNull Object aroundInsertAll(ProceedingJoinPoint pjp) throws Throwable {
        return executeWithMetrics(pjp, SqlOperationType.INSERT, "insertAll", true);
    }

    /**
     * 拦截 update 方法
     */
    @Around("execution(* io.github.afgprojects.framework.data.jdbc.JdbcEntityProxy.update(..))")
    public @NonNull Object aroundUpdate(ProceedingJoinPoint pjp) throws Throwable {
        return executeWithMetrics(pjp, SqlOperationType.UPDATE, "update", true);
    }

    /**
     * 拦截 updateAll(entities) 方法
     */
    @Around("execution(* io.github.afgprojects.framework.data.jdbc.JdbcEntityProxy.updateAll(java.lang.Iterable))")
    public @NonNull Object aroundUpdateAllEntities(ProceedingJoinPoint pjp) throws Throwable {
        return executeWithMetrics(pjp, SqlOperationType.UPDATE, "updateAll", true);
    }

    /**
     * 拦截 findById 方法
     */
    @Around("execution(* io.github.afgprojects.framework.data.jdbc.JdbcEntityProxy.findById(..))")
    public @NonNull Object aroundFindById(ProceedingJoinPoint pjp) throws Throwable {
        return executeWithMetrics(pjp, SqlOperationType.SELECT, "findById", false);
    }

    /**
     * 拦截 findAll() 方法
     */
    @Around("execution(* io.github.afgprojects.framework.data.jdbc.JdbcEntityProxy.findAll())")
    public @NonNull Object aroundFindAll(ProceedingJoinPoint pjp) throws Throwable {
        return executeWithMetrics(pjp, SqlOperationType.SELECT, "findAll", false);
    }

    /**
     * 拦截 findAllById 方法
     */
    @Around("execution(* io.github.afgprojects.framework.data.jdbc.JdbcEntityProxy.findAllById(..))")
    public @NonNull Object aroundFindAllById(ProceedingJoinPoint pjp) throws Throwable {
        return executeWithMetrics(pjp, SqlOperationType.SELECT, "findAllById", false);
    }

    /**
     * 拦截 count() 方法
     */
    @Around("execution(* io.github.afgprojects.framework.data.jdbc.JdbcEntityProxy.count())")
    public @NonNull Object aroundCount(ProceedingJoinPoint pjp) throws Throwable {
        return executeWithMetrics(pjp, SqlOperationType.SELECT, "count", false);
    }

    /**
     * 拦截 existsById 方法
     */
    @Around("execution(* io.github.afgprojects.framework.data.jdbc.JdbcEntityProxy.existsById(..))")
    public @NonNull Object aroundExistsById(ProceedingJoinPoint pjp) throws Throwable {
        return executeWithMetrics(pjp, SqlOperationType.SELECT, "existsById", false);
    }

    /**
     * 拦截 deleteById 方法
     */
    @Around("execution(* io.github.afgprojects.framework.data.jdbc.JdbcEntityProxy.deleteById(..))")
    public @NonNull Object aroundDeleteById(ProceedingJoinPoint pjp) throws Throwable {
        return executeWithMetrics(pjp, SqlOperationType.DELETE, "deleteById", true);
    }

    /**
     * 拦截 delete(entity) 方法
     */
    @Around("execution(* io.github.afgprojects.framework.data.jdbc.JdbcEntityProxy.delete(..))")
    public @NonNull Object aroundDelete(ProceedingJoinPoint pjp) throws Throwable {
        return executeWithMetrics(pjp, SqlOperationType.DELETE, "delete", true);
    }

    /**
     * 拦截 deleteAllById 方法
     */
    @Around("execution(* io.github.afgprojects.framework.data.jdbc.JdbcEntityProxy.deleteAllById(..))")
    public @NonNull Object aroundDeleteAllById(ProceedingJoinPoint pjp) throws Throwable {
        return executeWithMetrics(pjp, SqlOperationType.DELETE, "deleteAllById", true);
    }

    /**
     * 拦截 deleteAll(entities) 方法
     */
    @Around("execution(* io.github.afgprojects.framework.data.jdbc.JdbcEntityProxy.deleteAll(java.lang.Iterable))")
    public @NonNull Object aroundDeleteAllEntities(ProceedingJoinPoint pjp) throws Throwable {
        return executeWithMetrics(pjp, SqlOperationType.DELETE, "deleteAll", true);
    }

    /**
     * 拦截 findAll(Condition) 方法
     */
    @Around("execution(* io.github.afgprojects.framework.data.jdbc.JdbcEntityProxy.findAll(io.github.afgprojects.framework.data.core.query.Condition))")
    public @NonNull Object aroundFindAllByCondition(ProceedingJoinPoint pjp) throws Throwable {
        return executeWithMetrics(pjp, SqlOperationType.SELECT, "findAll", false);
    }

    /**
     * 拦截 findAll(Condition, PageRequest) 方法
     */
    @Around("execution(* io.github.afgprojects.framework.data.jdbc.JdbcEntityProxy.findAll(io.github.afgprojects.framework.data.core.query.Condition, io.github.afgprojects.framework.data.core.page.PageRequest))")
    public @NonNull Object aroundFindAllByConditionWithPaging(ProceedingJoinPoint pjp) throws Throwable {
        return executeWithMetrics(pjp, SqlOperationType.SELECT, "findAllPaged", false);
    }

    /**
     * 拦截 count(Condition) 方法
     */
    @Around("execution(* io.github.afgprojects.framework.data.jdbc.JdbcEntityProxy.count(io.github.afgprojects.framework.data.core.query.Condition))")
    public @NonNull Object aroundCountByCondition(ProceedingJoinPoint pjp) throws Throwable {
        return executeWithMetrics(pjp, SqlOperationType.SELECT, "count", false);
    }

    /**
     * 拦截 exists(Condition) 方法
     */
    @Around("execution(* io.github.afgprojects.framework.data.jdbc.JdbcEntityProxy.exists(io.github.afgprojects.framework.data.core.query.Condition))")
    public @NonNull Object aroundExistsByCondition(ProceedingJoinPoint pjp) throws Throwable {
        return executeWithMetrics(pjp, SqlOperationType.SELECT, "exists", false);
    }

    /**
     * 拦截 findOne 方法
     */
    @Around("execution(* io.github.afgprojects.framework.data.jdbc.JdbcEntityProxy.findOne(..))")
    public @NonNull Object aroundFindOne(ProceedingJoinPoint pjp) throws Throwable {
        return executeWithMetrics(pjp, SqlOperationType.SELECT, "findOne", false);
    }

    /**
     * 拦截 findFirst 方法
     */
    @Around("execution(* io.github.afgprojects.framework.data.jdbc.JdbcEntityProxy.findFirst(..))")
    public @NonNull Object aroundFindFirst(ProceedingJoinPoint pjp) throws Throwable {
        return executeWithMetrics(pjp, SqlOperationType.SELECT, "findFirst", false);
    }

    /**
     * 拦截 updateAll(Condition, Map) 方法
     */
    @Around("execution(* io.github.afgprojects.framework.data.jdbc.JdbcEntityProxy.updateAll(io.github.afgprojects.framework.data.core.query.Condition, java.util.Map))")
    public @NonNull Object aroundUpdateAllByCondition(ProceedingJoinPoint pjp) throws Throwable {
        return executeWithMetrics(pjp, SqlOperationType.UPDATE, "updateAll", true);
    }

    /**
     * 拦截 deleteAll(Condition) 方法
     */
    @Around("execution(* io.github.afgprojects.framework.data.jdbc.JdbcEntityProxy.deleteAll(io.github.afgprojects.framework.data.core.query.Condition))")
    public @NonNull Object aroundDeleteAllByCondition(ProceedingJoinPoint pjp) throws Throwable {
        return executeWithMetrics(pjp, SqlOperationType.DELETE, "deleteAll", true);
    }

    /**
     * 拦截 restoreById 方法
     */
    @Around("execution(* io.github.afgprojects.framework.data.jdbc.JdbcEntityProxy.restoreById(..))")
    public @NonNull Object aroundRestoreById(ProceedingJoinPoint pjp) throws Throwable {
        return executeWithMetrics(pjp, SqlOperationType.UPDATE, "restoreById", true);
    }

    /**
     * 拦截 restoreAllById 方法
     */
    @Around("execution(* io.github.afgprojects.framework.data.jdbc.JdbcEntityProxy.restoreAllById(..))")
    public @NonNull Object aroundRestoreAllById(ProceedingJoinPoint pjp) throws Throwable {
        return executeWithMetrics(pjp, SqlOperationType.UPDATE, "restoreAllById", true);
    }

    /**
     * 带指标记录的方法执行
     *
     * @param pjp           切入点
     * @param operationType 操作类型
     * @param methodName    方法名
     * @param countRows     是否统计影响行数
     * @return 方法返回值
     * @throws Throwable 执行异常
     */
    @NonNull
    private Object executeWithMetrics(ProceedingJoinPoint pjp,
                                      SqlOperationType operationType,
                                      String methodName,
                                      boolean countRows) throws Throwable {
        if (!properties.isEnabled()) {
            return pjp.proceed();
        }

        String entityName = getEntityName(pjp);
        Instant start = Instant.now();

        try {
            Object result = pjp.proceed();
            Duration duration = Duration.between(start, Instant.now());

            long rowsAffected = 0;
            if (countRows) {
                rowsAffected = getRowsAffected(result, methodName);
            }

            sqlMetrics.recordSuccess(entityName, operationType, duration, rowsAffected);

            // 检查慢查询
            if (duration.toMillis() >= properties.getSlowQueryThreshold().toMillis() && properties.isLogSlowQueries()) {
                SlowQueryLog slowQuery = SlowQueryLog.of(
                        buildOperationDescription(entityName, methodName),
                        operationType,
                        duration,
                        null,
                        entityName
                );
                sqlMetrics.logSlowQuery(slowQuery);
            }

            return result;
        } catch (Throwable e) {
            Duration duration = Duration.between(start, Instant.now());
            sqlMetrics.recordError(entityName, operationType, duration, e.getMessage());
            throw e;
        }
    }

    /**
     * 获取实体类名
     *
     * @param pjp 切入点
     * @return 实体类名
     */
    @NonNull
    private String getEntityName(ProceedingJoinPoint pjp) {
        try {
            MethodSignature signature = (MethodSignature) pjp.getSignature();
            Class<?> declaringType = signature.getDeclaringType();
            if (declaringType.getSimpleName().equals("JdbcEntityProxy")) {
                // 尝试从泛型获取实体类名
                Object target = pjp.getTarget();
                if (target instanceof JdbcEntityProxy<?> proxy) {
                    return proxy.getEntityClass().getSimpleName();
                }
            }
            return declaringType.getSimpleName();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    /**
     * 获取影响行数
     *
     * @param result     方法返回值
     * @param methodName 方法名
     * @return 影响行数
     */
    private long getRowsAffected(@Nullable Object result, String methodName) {
        if (result == null) {
            return 0;
        }

        // 单个实体操作返回 1
        if (isEntityOperation(methodName)) {
            return 1;
        }

        // List 类型返回 size
        if (result instanceof List<?> list) {
            return list.size();
        }

        // long 类型（如 updateAll/deleteAll 返回值）
        if (result instanceof Long l) {
            return l;
        }

        // int 类型
        if (result instanceof Integer i) {
            return i;
        }

        // Page 类型返回记录数
        if (result instanceof Page<?> page) {
            return page.getContent().size();
        }

        return 0;
    }

    /**
     * 判断是否为单个实体操作
     *
     * @param methodName 方法名
     * @return 是否为实体操作
     */
    private boolean isEntityOperation(String methodName) {
        return methodName.equals("save") ||
                methodName.equals("insert") ||
                methodName.equals("update") ||
                methodName.equals("delete") ||
                methodName.equals("restoreById");
    }

    /**
     * 构建操作描述
     *
     * @param entityName 实体名
     * @param methodName 方法名
     * @return 操作描述
     */
    @NonNull
    private String buildOperationDescription(String entityName, String methodName) {
        return entityName + "." + methodName;
    }

    /**
     * 处理慢查询
     *
     * @param log 慢查询日志
     */
    private void handleSlowQuery(SlowQueryLog log) {
        SqlMetricsAspect.log.warn("Slow query detected: {}", log);
    }
}
