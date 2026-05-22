package io.github.afgprojects.framework.data.liquibase.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Liquibase 配置属性
 * <p>
 * 所有配置项都有合理的默认值，开箱即用。
 * 默认使用统一的 changeLog 路径：classpath:db/changelog/changelog.xml
 */
@ConfigurationProperties(prefix = "afg.liquibase")
public class LiquibaseProperties {

    /**
     * 是否启用 Liquibase
     */
    private boolean enabled = true;

    /**
     * ChangeLog 文件路径
     * <p>
     * 默认值：classpath:db/changelog/changelog.xml
     * <p>
     * 各模块在 db/changelog/ 目录下创建模块目录和迁移脚本，
     * 主 changelog.xml 文件引用所有模块的迁移脚本。
     */
    private String changeLog = "classpath:db/changelog/changelog.xml";

    /**
     * 是否先删除数据库再执行迁移
     * <p>
     * 仅用于开发环境，生产环境必须为 false
     */
    private boolean dropFirst = false;

    /**
     * 执行的上下文（逗号分隔）
     * <p>
     * 用于过滤执行特定的 changeSet，例如：dev,prod
     */
    private String contexts;

    /**
     * 执行的标签（逗号分隔）
     * <p>
     * 用于过滤执行特定的 changeSet，例如：v1.0.0,v1.1.0
     */
    private String labels;

    /**
     * 默认 Schema 名称
     * <p>
     * 用于指定数据库默认 Schema，例如：public（PostgreSQL）
     */
    private String defaultSchema;

    /**
     * Liquibase 变更记录表名
     * <p>
     * 默认：DB_CHANGELOG
     */
    private String databaseChangeLogTable = "DB_CHANGELOG";

    /**
     * Liquibase 锁表名
     * <p>
     * 默认：DB_CHANGELOG_LOCK
     */
    private String databaseChangeLogLockTable = "DB_CHANGELOG_LOCK";

    // Getters and Setters

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getChangeLog() {
        return changeLog;
    }

    public void setChangeLog(String changeLog) {
        this.changeLog = changeLog;
    }

    public boolean isDropFirst() {
        return dropFirst;
    }

    public void setDropFirst(boolean dropFirst) {
        this.dropFirst = dropFirst;
    }

    public String getContexts() {
        return contexts;
    }

    public void setContexts(String contexts) {
        this.contexts = contexts;
    }

    public String getLabels() {
        return labels;
    }

    public void setLabels(String labels) {
        this.labels = labels;
    }

    public String getDefaultSchema() {
        return defaultSchema;
    }

    public void setDefaultSchema(String defaultSchema) {
        this.defaultSchema = defaultSchema;
    }

    public String getDatabaseChangeLogTable() {
        return databaseChangeLogTable;
    }

    public void setDatabaseChangeLogTable(String databaseChangeLogTable) {
        this.databaseChangeLogTable = databaseChangeLogTable;
    }

    public String getDatabaseChangeLogLockTable() {
        return databaseChangeLogLockTable;
    }

    public void setDatabaseChangeLogLockTable(String databaseChangeLogLockTable) {
        this.databaseChangeLogLockTable = databaseChangeLogLockTable;
    }
}
