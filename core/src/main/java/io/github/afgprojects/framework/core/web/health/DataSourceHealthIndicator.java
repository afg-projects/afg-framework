package io.github.afgprojects.framework.core.web.health;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

/**
 * 数据源健康检查指示器
 * 检查数据库连接池状态和连接可用性
 *
 * <p>检查内容：
 * <ul>
 *   <li>数据库连接是否可用</li>
 *   <li>连接池使用率（活跃连接数/最大连接数）</li>
 *   <li>等待获取连接的线程数</li>
 *   <li>连接池详细状态信息</li>
 * </ul>
 *
 * <p>支持 HikariCP 连接池，其他连接池仅提供基础连接检查。
 *
 * @since 1.0.0
 */
@SuppressWarnings({"PMD.CloseResource", "PMD.AvoidCatchingGenericException"})
public class DataSourceHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(DataSourceHealthIndicator.class);

    private final DataSource dataSource;
    private final DataSourceHealthProperties properties;

    /**
     * 构造函数
     *
     * @param dataSource 数据源
     * @param properties 健康检查配置
     */
    public DataSourceHealthIndicator(@NonNull DataSource dataSource, @NonNull DataSourceHealthProperties properties) {
        this.dataSource = dataSource;
        this.properties = properties;
    }

    @Override
    public Health health() {
        Health.Builder builder = Health.up();
        boolean connectionHealthy = true;

        // 检查数据库连接是否可用
        connectionHealthy = checkConnection(builder);

        // 如果是 HikariCP，检查连接池状态
        if (dataSource instanceof HikariDataSource hikariDataSource) {
            checkHikariPool(hikariDataSource, builder, connectionHealthy);
        } else {
            builder.withDetail("poolType", dataSource.getClass().getSimpleName());
            builder.withDetail("hikariSupported", false);
        }

        return builder.build();
    }

    /**
     * 检查数据库连接是否可用
     *
     * @param builder Health 构建器
     * @return true 表示连接正常，false 表示连接异常
     */
    private boolean checkConnection(Health.Builder builder) {
        long startTime = System.currentTimeMillis();

        try (Connection connection = dataSource.getConnection()) {
            // 使用配置的超时时间验证连接
            boolean valid = connection.isValid(
                    (int) TimeUnit.MILLISECONDS.toSeconds(properties.getConnectionTimeout()));
            long duration = System.currentTimeMillis() - startTime;

            if (valid) {
                builder.withDetail("database", "UP")
                        .withDetail("validationQuery", properties.getValidationQuery())
                        .withDetail("responseTime", duration + "ms");
                return true;
            } else {
                log.error("数据库连接验证失败");
                builder.status(Status.DOWN)
                        .withDetail("database", "DOWN")
                        .withDetail("error", "Connection validation failed")
                        .withDetail("responseTime", duration + "ms");
                return false;
            }
        } catch (SQLException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("数据库连接检查失败: {}", e.getMessage(), e);
            builder.status(Status.DOWN)
                    .withDetail("database", "DOWN")
                    .withDetail("error", e.getMessage())
                    .withDetail("responseTime", duration + "ms");
            return false;
        }
    }

    /**
     * 检查 HikariCP 连接池状态
     *
     * @param hikariDataSource  HikariCP 数据源
     * @param builder           Health 构建器
     * @param connectionHealthy 连接是否健康
     */
    private void checkHikariPool(HikariDataSource hikariDataSource, Health.Builder builder, boolean connectionHealthy) {
        try {
            HikariPoolMXBean pool = hikariDataSource.getHikariPoolMXBean();
            if (pool == null) {
                builder.withDetail("poolType", "HikariCP")
                        .withDetail("poolStatus", "NOT_INITIALIZED");
                return;
            }

            // 获取连接池状态
            int activeConnections = pool.getActiveConnections();
            int idleConnections = pool.getIdleConnections();
            int totalConnections = pool.getTotalConnections();
            int threadsAwaitingConnection = pool.getThreadsAwaitingConnection();
            int maximumPoolSize = hikariDataSource.getMaximumPoolSize();
            int minimumIdle = hikariDataSource.getMinimumIdle();

            // 计算连接池使用率
            int poolUsagePercent = maximumPoolSize > 0 ? (activeConnections * 100) / maximumPoolSize : 0;

            // 添加连接池详情
            builder.withDetail("poolType", "HikariCP")
                    .withDetail("poolStatus", "RUNNING")
                    .withDetail("activeConnections", activeConnections)
                    .withDetail("idleConnections", idleConnections)
                    .withDetail("totalConnections", totalConnections)
                    .withDetail("maximumPoolSize", maximumPoolSize)
                    .withDetail("minimumIdle", minimumIdle)
                    .withDetail("poolUsagePercent", poolUsagePercent + "%")
                    .withDetail("threadsAwaitingConnection", threadsAwaitingConnection);

            // 评估连接池状态
            evaluatePoolStatus(builder, poolUsagePercent, threadsAwaitingConnection, connectionHealthy);

        } catch (Exception e) {
            log.warn("获取 HikariCP 连接池状态失败: {}", e.getMessage());
            builder.withDetail("poolType", "HikariCP")
                    .withDetail("poolStatus", "ERROR")
                    .withDetail("poolError", e.getMessage());
        }
    }

    /**
     * 评估连接池状态并设置健康状态
     *
     * @param builder                   Health 构建器
     * @param poolUsagePercent          连接池使用率
     * @param threadsAwaitingConnection 等待线程数
     * @param connectionHealthy         连接是否健康
     */
    private void evaluatePoolStatus(Health.Builder builder, int poolUsagePercent,
                                    int threadsAwaitingConnection, boolean connectionHealthy) {
        // 如果连接不健康，状态已经是 DOWN
        if (!connectionHealthy) {
            builder.withDetail("poolHealth", "CRITICAL");
            return;
        }

        // 检查连接池使用率
        boolean usageCritical = poolUsagePercent >= properties.getPoolUsageCriticalThreshold();
        boolean usageWarning = poolUsagePercent >= properties.getPoolUsageWarningThreshold();

        // 检查等待线程数
        boolean threadsCritical = threadsAwaitingConnection >= properties.getThreadsAwaitingCriticalThreshold();
        boolean threadsWarning = threadsAwaitingConnection >= properties.getThreadsAwaitingWarningThreshold();

        // 确定整体状态
        if (usageCritical || threadsCritical) {
            builder.status(Status.DOWN);
            builder.withDetail("poolHealth", "CRITICAL");
            log.warn("连接池状态严重: 使用率={}%, 等待线程数={}", poolUsagePercent, threadsAwaitingConnection);
        } else if (usageWarning || threadsWarning) {
            // 使用自定义状态表示警告
            builder.status(new Status("WARNING", "Connection pool usage is high"));
            builder.withDetail("poolHealth", "WARNING");
            log.warn("连接池状态警告: 使用率={}%, 等待线程数={}", poolUsagePercent, threadsAwaitingConnection);
        } else {
            builder.withDetail("poolHealth", "NORMAL");
        }

        // 添加具体的警告信息
        if (usageWarning) {
            builder.withDetail("poolUsageWarning",
                    String.format("Pool usage %d%% exceeds warning threshold %d%%",
                            poolUsagePercent, properties.getPoolUsageWarningThreshold()));
        }
        if (threadsWarning) {
            builder.withDetail("threadsAwaitingWarning",
                    String.format("%d threads awaiting connection exceeds warning threshold %d",
                            threadsAwaitingConnection, properties.getThreadsAwaitingWarningThreshold()));
        }
    }
}