package io.github.afgprojects.framework.data.jdbc.autoconfigure;

import io.github.afgprojects.framework.data.jdbc.metrics.SqlMetrics;
import io.github.afgprojects.framework.data.jdbc.metrics.SqlMetricsAspect;
import io.github.afgprojects.framework.data.jdbc.metrics.SqlMetricsProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * SQL 指标自动配置
 * <p>
 * 自动配置 SQL 执行指标监控功能。
 *
 * <h3>启用条件</h3>
 * <ul>
 *   <li>存在 MeterRegistry bean（Spring Boot Actuator 提供）</li>
 *   <li>{@code afg.jdbc.metrics.enabled=true}（默认为 true）</li>
 * </ul>
 *
 * <h3>配置示例</h3>
 * <pre>
 * afg:
 *   jdbc:
 *     metrics:
 *       enabled: true
 *       slow-query-threshold: 1000ms
 *       log-slow-queries: true
 *       log-sql-params: false
 * </pre>
 *
 * <h3>提供的指标</h3>
 * <ul>
 *   <li>{@code afg.jdbc.sql.calls} - SQL 执行次数计数器</li>
 *   <li>{@code afg.jdbc.sql.duration} - SQL 执行耗时分布（Timer）</li>
 *   <li>{@code afg.jdbc.sql.rows} - SQL 影响行数计数器</li>
 *   <li>{@code afg.jdbc.sql.errors} - SQL 执行错误次数计数器</li>
 *   <li>{@code afg.jdbc.sql.slow} - 慢查询次数计数器</li>
 * </ul>
 *
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnBean(MeterRegistry.class)
@ConditionalOnProperty(prefix = "afg.jdbc.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(SqlMetricsProperties.class)
public class SqlMetricsAutoConfiguration {

    /**
     * 配置 SQL 指标记录器
     *
     * @param meterRegistry Micrometer 注册表
     * @param properties    SQL 指标配置属性
     * @return SQL 指标记录器实例
     */
    @Bean
    @ConditionalOnMissingBean
    @NonNull
    public SqlMetrics sqlMetrics(MeterRegistry meterRegistry, SqlMetricsProperties properties) {
        return new SqlMetrics(meterRegistry, properties);
    }

    /**
     * 配置 SQL 指标切面
     *
     * @param sqlMetrics SQL 指标记录器
     * @param properties SQL 指标配置属性
     * @return SQL 指标切面实例
     */
    @Bean
    @ConditionalOnMissingBean
    @NonNull
    public SqlMetricsAspect sqlMetricsAspect(SqlMetrics sqlMetrics, SqlMetricsProperties properties) {
        return new SqlMetricsAspect(sqlMetrics, properties);
    }
}
