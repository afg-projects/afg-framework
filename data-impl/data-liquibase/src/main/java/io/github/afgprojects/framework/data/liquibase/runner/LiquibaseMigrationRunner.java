package io.github.afgprojects.framework.data.liquibase.runner;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.DirectoryResourceAccessor;
import liquibase.resource.ResourceAccessor;

import java.io.File;
import java.sql.Connection;
import java.util.List;

/**
 * Liquibase 迁移执行器
 * <p>
 * 使用 Liquibase API 执行数据库迁移
 */
public class LiquibaseMigrationRunner {

    /**
     * 执行迁移到最新版本
     *
     * @param connection    数据库连接
     * @param changeLogPath ChangeLog 文件路径
     */
    public void migrate(Connection connection, String changeLogPath) throws LiquibaseException {
        migrate(connection, changeLogPath, null, null, null);
    }

    /**
     * 执行迁移到指定版本
     *
     * @param connection    数据库连接
     * @param changeLogPath ChangeLog 文件路径
     * @param targetVersion 目标版本（为空则迁移到最新）
     */
    public void migrate(Connection connection, String changeLogPath, String targetVersion) throws LiquibaseException {
        migrate(connection, changeLogPath, targetVersion, null, null);
    }

    /**
     * 执行迁移
     *
     * @param connection    数据库连接
     * @param changeLogPath ChangeLog 文件路径
     * @param targetVersion 目标版本
     * @param contexts      上下文过滤
     * @param labels        标签过滤
     */
    public void migrate(Connection connection, String changeLogPath, String targetVersion,
                        String contexts, String labels) throws LiquibaseException {
        Liquibase liquibase = createLiquibase(connection, changeLogPath);

        Contexts contextsObj = contexts != null && !contexts.isEmpty()
                ? new Contexts(contexts.split(","))
                : new Contexts();

        LabelExpression labelsObj = labels != null && !labels.isEmpty()
                ? new LabelExpression(labels.split(","))
                : new LabelExpression();

        if (targetVersion != null && !targetVersion.isEmpty()) {
            liquibase.update(targetVersion, contextsObj, labelsObj);
        } else {
            liquibase.update(contextsObj, labelsObj);
        }
    }

    /**
     * 回滚指定数量的 ChangeSet
     *
     * @param connection    数据库连接
     * @param changeLogPath ChangeLog 文件路径
     * @param steps         回滚步数
     */
    public void rollback(Connection connection, String changeLogPath, int steps) throws LiquibaseException {
        Liquibase liquibase = createLiquibase(connection, changeLogPath);
        liquibase.rollback(String.valueOf(steps), new Contexts());
    }

    /**
     * 回滚到指定版本
     *
     * @param connection    数据库连接
     * @param changeLogPath ChangeLog 文件路径
     * @param targetVersion 目标版本
     */
    public void rollbackToVersion(Connection connection, String changeLogPath, String targetVersion) throws LiquibaseException {
        Liquibase liquibase = createLiquibase(connection, changeLogPath);
        liquibase.rollback(targetVersion, new Contexts());
    }

    /**
     * 获取迁移状态
     *
     * @param connection    数据库连接
     * @param changeLogPath ChangeLog 文件路径
     * @return 迁移状态列表
     */
    public List<MigrationStatus> status(Connection connection, String changeLogPath) throws LiquibaseException {
        Liquibase liquibase = createLiquibase(connection, changeLogPath);
        // 返回未执行的 ChangeSet 列表
        List<ChangeSet> unrunChangeSets = liquibase.listUnrunChangeSets(new Contexts(), new LabelExpression());
        return unrunChangeSets.stream()
                .map(cs -> new MigrationStatus(
                        cs.getId(),
                        cs.getAuthor(),
                        cs.getDescription(),
                        false
                ))
                .toList();
    }

    /**
     * 验证 ChangeLog
     *
     * @param connection    数据库连接
     * @param changeLogPath ChangeLog 文件路径
     */
    public void validate(Connection connection, String changeLogPath) throws LiquibaseException {
        Liquibase liquibase = createLiquibase(connection, changeLogPath);
        liquibase.validate();
    }

    /**
     * 标记所有 ChangeSet 为已执行（不实际执行）
     *
     * @param connection    数据库连接
     * @param changeLogPath ChangeLog 文件路径
     */
    public void markAllExecuted(Connection connection, String changeLogPath) throws LiquibaseException {
        Liquibase liquibase = createLiquibase(connection, changeLogPath);
        liquibase.changeLogSync(new Contexts(), new LabelExpression());
    }

    private Liquibase createLiquibase(Connection connection, String changeLogPath) throws LiquibaseException {
        try {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));

            File changeLogFile = new File(changeLogPath);
            ResourceAccessor resourceAccessor;

            if (changeLogFile.isAbsolute()) {
                resourceAccessor = new DirectoryResourceAccessor(changeLogFile.getParentFile());
            } else {
                resourceAccessor = new DirectoryResourceAccessor(new File("."));
            }

            String changeLogFileName = changeLogFile.getName();

            return new Liquibase(changeLogFileName, resourceAccessor, database);
        } catch (java.io.FileNotFoundException e) {
            throw new LiquibaseException("ChangeLog file not found: " + changeLogPath, e);
        }
    }

    /**
     * 迁移状态
     */
    public record MigrationStatus(
            String changeSetId,
            String author,
            String description,
            boolean executed
    ) {}
}
