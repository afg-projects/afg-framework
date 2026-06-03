package io.github.afgprojects.framework.data.jdbc.properties.metrics;

import java.time.Duration;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * SQL 指标配置属性
 */
@Data
@ConfigurationProperties(prefix = "afg.jdbc.metrics")
public class SqlMetricsProperties {

    private boolean enabled = true;
    private Duration slowQueryThreshold = Duration.ofMillis(1000);
    private boolean logSlowQueries = true;
    private int maxSlowQueryLogs = 100;
    private boolean logSqlParams = false;
    private @Nullable SqlMetricsTagsProperties tags = new SqlMetricsTagsProperties();
}
