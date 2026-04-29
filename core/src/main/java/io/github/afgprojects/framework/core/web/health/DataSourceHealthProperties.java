package io.github.afgprojects.framework.core.web.health;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * 数据源健康检查配置属性
 * 配置数据库连接池健康检查参数
 *
 * @since 1.0.0
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "afg.health.datasource")
@SuppressWarnings({"PMD.UncommentedEmptyConstructor", "PMD.FieldDeclarationsShouldBeAtStartOfClass"})
public class DataSourceHealthProperties {

    /**
     * 是否启用数据源健康检查
     */
    private boolean enabled = true;

    /**
     * 验证连接的 SQL 语句
     * 不同数据库使用不同的验证语句：
     * <ul>
     *   <li>MySQL/PostgreSQL: SELECT 1</li>
     *   <li>Oracle: SELECT 1 FROM DUAL</li>
     *   <li>SQL Server: SELECT 1</li>
     * </ul>
     */
    private String validationQuery = "SELECT 1";

    /**
     * 连接池使用率警告阈值（百分比）
     * 超过此阈值将报告 WARNING 状态
     */
    private int poolUsageWarningThreshold = 70;

    /**
     * 连接池使用率严重阈值（百分比）
     * 超过此阈值将报告 CRITICAL 状态
     */
    private int poolUsageCriticalThreshold = 90;

    /**
     * 等待线程数警告阈值
     * 超过此阈值将报告 WARNING 状态
     */
    private int threadsAwaitingWarningThreshold = 5;

    /**
     * 等待线程数严重阈值
     * 超过此阈值将报告 CRITICAL 状态
     */
    private int threadsAwaitingCriticalThreshold = 10;

    /**
     * 连接获取超时时间（毫秒）
     * 用于验证连接是否可用
     */
    private int connectionTimeout = 3000;
}
