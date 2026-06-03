package io.github.afgprojects.framework.data.jdbc.properties.metrics;

import lombok.Data;

/**
 * SQL 指标标签配置。
 */
@Data
public class SqlMetricsTagsProperties {

    private boolean includeEntity = true;
    private boolean includeOperation = true;
    private boolean includeDataSource = false;
}
