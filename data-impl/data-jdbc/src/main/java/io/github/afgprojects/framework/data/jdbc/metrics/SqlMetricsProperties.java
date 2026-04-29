package io.github.afgprojects.framework.data.jdbc.metrics;

import lombok.Data;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * SQL 指标配置属性
 *
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "afg.jdbc.metrics")
public class SqlMetricsProperties {

    /**
     * 是否启用 SQL 指标
     */
    private boolean enabled = true;

    /**
     * 慢查询阈值（默认 1000ms）
     */
    private Duration slowQueryThreshold = Duration.ofMillis(1000);

    /**
     * 是否记录慢查询日志
     */
    private boolean logSlowQueries = true;

    /**
     * 慢查询日志最大记录条数（用于限制内存使用）
     */
    private int maxSlowQueryLogs = 100;

    /**
     * 是否记录 SQL 参数
     */
    private boolean logSqlParams = false;

    /**
     * 指标标签配置
     */
    private @Nullable TagsConfig tags = new TagsConfig();

    /**
     * 标签配置
     */
    @Data
    public static class TagsConfig {

        /**
         * 是否包含实体类名标签
         */
        private boolean includeEntity = true;

        /**
         * 是否包含操作类型标签
         */
        private boolean includeOperation = true;

        /**
         * 是否包含数据源名称标签
         */
        private boolean includeDataSource = false;
    }
}
