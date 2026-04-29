package io.github.afgprojects.framework.data.jdbc.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * SQL 执行指标记录器
 * <p>
 * 基于 Micrometer 实现，记录 SQL 执行次数、耗时分布、慢查询等指标。
 * 支持 Prometheus 等监控系统集成。
 *
 * <h3>指标说明</h3>
 * <ul>
 *   <li>{@code afg.jdbc.sql.calls} - SQL 执行次数计数器，按操作类型分类</li>
 *   <li>{@code afg.jdbc.sql.duration} - SQL 执行耗时分布（Timer）</li>
 *   <li>{@code afg.jdbc.sql.rows} - SQL 影响行数计数器</li>
 *   <li>{@code afg.jdbc.sql.errors} - SQL 执行错误次数计数器</li>
 *   <li>{@code afg.jdbc.sql.slow} - 慢查询次数计数器</li>
 * </ul>
 *
 * <h3>标签说明</h3>
 * <ul>
 *   <li>{@code entity} - 实体类名（简单名称）</li>
 *   <li>{@code operation} - 操作类型（SELECT/INSERT/UPDATE/DELETE/OTHER）</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class SqlMetrics {

    /**
     * 指标前缀
     */
    private static final String METRIC_PREFIX = "afg.jdbc.sql.";

    /**
     * 指标名称：SQL 执行次数
     */
    private static final String CALLS_METRIC = METRIC_PREFIX + "calls";

    /**
     * 指标名称：SQL 执行耗时
     */
    private static final String DURATION_METRIC = METRIC_PREFIX + "duration";

    /**
     * 指标名称：影响行数
     */
    private static final String ROWS_METRIC = METRIC_PREFIX + "rows";

    /**
     * 指标名称：错误次数
     */
    private static final String ERRORS_METRIC = METRIC_PREFIX + "errors";

    /**
     * 指标名称：慢查询次数
     */
    private static final String SLOW_METRIC = METRIC_PREFIX + "slow";

    private final MeterRegistry meterRegistry;
    private final SqlMetricsProperties properties;

    /**
     * 慢查询监听器
     */
    private volatile @Nullable Consumer<SlowQueryLog> slowQueryListener;

    /**
     * 缓存的 Timer 实例（按实体类名和操作类型）
     */
    private final ConcurrentHashMap<String, Timer> timerCache = new ConcurrentHashMap<>();

    /**
     * 构造 SQL 指标记录器
     *
     * @param meterRegistry Micrometer 注册表
     * @param properties    指标配置
     */
    public SqlMetrics(@NonNull MeterRegistry meterRegistry, @NonNull SqlMetricsProperties properties) {
        this.meterRegistry = meterRegistry;
        this.properties = properties;
    }

    /**
     * 记录 SQL 执行成功
     *
     * @param entityName    实体类名
     * @param operationType 操作类型
     * @param duration      执行时间
     * @param rowsAffected  影响行数
     */
    public void recordSuccess(@NonNull String entityName,
                              @NonNull SqlOperationType operationType,
                              @NonNull Duration duration,
                              long rowsAffected) {
        if (!properties.isEnabled()) {
            return;
        }

        String[] tags = {"entity", entityName, "operation", operationType.getName()};

        // 记录执行次数
        Counter.builder(CALLS_METRIC)
                .tags(tags)
                .description("SQL execution count")
                .register(meterRegistry)
                .increment();

        // 记录执行时间
        Timer timer = getOrCreateTimer(tags);
        timer.record(duration.toNanos(), TimeUnit.NANOSECONDS);

        // 记录影响行数
        if (rowsAffected > 0) {
            Counter.builder(ROWS_METRIC)
                    .tags(tags)
                    .description("SQL affected rows count")
                    .register(meterRegistry)
                    .increment(rowsAffected);
        }

        // 检查慢查询
        if (duration.toMillis() >= properties.getSlowQueryThreshold().toMillis()) {
            recordSlowQuery(entityName, operationType, duration);
        }
    }

    /**
     * 记录 SQL 执行失败
     *
     * @param entityName    实体类名
     * @param operationType 操作类型
     * @param duration      执行时间
     * @param errorMessage  错误信息
     */
    public void recordError(@NonNull String entityName,
                            @NonNull SqlOperationType operationType,
                            @NonNull Duration duration,
                            @NonNull String errorMessage) {
        if (!properties.isEnabled()) {
            return;
        }

        String[] tags = {"entity", entityName, "operation", operationType.getName()};

        // 记录错误次数
        Counter.builder(ERRORS_METRIC)
                .tags(tags)
                .description("SQL execution error count")
                .register(meterRegistry)
                .increment();

        // 记录执行时间
        Timer timer = getOrCreateTimer(tags);
        timer.record(duration.toNanos(), TimeUnit.NANOSECONDS);
    }

    /**
     * 记录慢查询
     *
     * @param entityName    实体类名
     * @param operationType 操作类型
     * @param duration      执行时间
     */
    private void recordSlowQuery(@NonNull String entityName,
                                 @NonNull SqlOperationType operationType,
                                 @NonNull Duration duration) {
        // 增加慢查询计数
        Counter.builder(SLOW_METRIC)
                .tag("entity", entityName)
                .tag("operation", operationType.getName())
                .description("Slow query count")
                .register(meterRegistry)
                .increment();
    }

    /**
     * 记录慢查询日志
     *
     * @param log 慢查询日志
     */
    public void logSlowQuery(@NonNull SlowQueryLog log) {
        Consumer<SlowQueryLog> listener = this.slowQueryListener;
        if (listener != null && properties.isLogSlowQueries()) {
            listener.accept(log);
        }
    }

    /**
     * 设置慢查询监听器
     *
     * @param listener 慢查询监听器
     */
    public void setSlowQueryListener(@Nullable Consumer<SlowQueryLog> listener) {
        this.slowQueryListener = listener;
    }

    /**
     * 获取或创建 Timer 实例
     *
     * @param tags 标签
     * @return Timer 实例
     */
    @NonNull
    private Timer getOrCreateTimer(@NonNull String[] tags) {
        String cacheKey = buildCacheKey(tags);
        return timerCache.computeIfAbsent(cacheKey, key ->
                Timer.builder(DURATION_METRIC)
                        .tags(tags)
                        .description("SQL execution duration")
                        .publishPercentiles(0.5, 0.95, 0.99)
                        .publishPercentileHistogram(true)
                        .minimumExpectedValue(java.time.Duration.ofMillis(1))
                        .maximumExpectedValue(java.time.Duration.ofSeconds(30))
                        .register(meterRegistry)
        );
    }

    /**
     * 构建缓存键
     *
     * @param tags 标签数组
     * @return 缓存键
     */
    @NonNull
    private String buildCacheKey(@NonNull String[] tags) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tags.length; i++) {
            if (i > 0) {
                sb.append(":");
            }
            sb.append(tags[i]);
        }
        return sb.toString();
    }

    /**
     * 获取配置属性
     *
     * @return 配置属性
     */
    @NonNull
    public SqlMetricsProperties getProperties() {
        return properties;
    }

    /**
     * 获取 MeterRegistry
     *
     * @return MeterRegistry
     */
    @NonNull
    public MeterRegistry getMeterRegistry() {
        return meterRegistry;
    }
}
