package io.github.afgprojects.framework.data.liquibase;

import io.github.afgprojects.framework.data.core.dialect.Dialect;
import io.github.afgprojects.framework.data.core.dialect.H2Dialect;
import io.github.afgprojects.framework.data.core.dialect.MySQLDialect;
import io.github.afgprojects.framework.data.core.dialect.PostgreSQLDialect;
import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
import io.github.afgprojects.framework.data.liquibase.extractor.EntitySchemaExtractor;
import io.github.afgprojects.framework.data.liquibase.generator.ChangeLogGenerator;
import io.github.afgprojects.framework.data.liquibase.generator.EntityCodeGenerator;
import io.github.afgprojects.framework.data.liquibase.reader.ChangeLogSchemaReader;
import io.github.afgprojects.framework.data.liquibase.reader.JdbcSchemaReader;
import io.github.afgprojects.framework.data.liquibase.runner.LiquibaseMigrationRunner;
import io.github.afgprojects.framework.data.core.schema.*;
import liquibase.exception.LiquibaseException;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * 迁移服务
 * <p>
 * 整合实体、数据库、ChangeLog 三向转换功能
 */
public class MigrationService {

    private final Dialect dialect;
    private final EntitySchemaExtractor entityExtractor;
    private final JdbcSchemaReader jdbcReader;
    private final ChangeLogSchemaReader changeLogReader;
    private final ChangeLogGenerator changeLogGenerator;
    private final EntityCodeGenerator entityGenerator;
    private final LiquibaseMigrationRunner migrationRunner;
    private final SchemaComparator schemaComparator;

    public MigrationService(Dialect dialect) {
        this.dialect = dialect;
        this.entityExtractor = new EntitySchemaExtractor(dialect);
        this.jdbcReader = new JdbcSchemaReader();
        this.changeLogReader = new ChangeLogSchemaReader();
        this.changeLogGenerator = new ChangeLogGenerator();
        this.entityGenerator = new EntityCodeGenerator();
        this.migrationRunner = new LiquibaseMigrationRunner();
        this.schemaComparator = new DefaultSchemaComparator();
    }

    /**
     * 根据实体生成迁移脚本
     *
     * @param entityMetadata 实体元数据
     * @param author         作者
     * @param outputPath     输出路径
     */
    public void generateMigrationFromEntity(EntityMetadata<?> entityMetadata,
                                             String author, Path outputPath) throws IOException {
        SchemaMetadata schema = entityExtractor.convert(entityMetadata);
        changeLogGenerator.generateCreateTable(schema, author, outputPath);
    }

    /**
     * 根据实体生成迁移脚本（带差异比对）
     *
     * @param entityMetadata 实体元数据
     * @param connection     数据库连接（用于比对）
     * @param changeLogPath  历史 ChangeLog 路径（用于比对）
     * @param author         作者
     * @param outputPath     输出路径
     * @return 三向差异报告
     */
    public ThreeWayDiff generateMigrationWithComparison(
            EntityMetadata<?> entityMetadata,
            Connection connection,
            String changeLogPath,
            String author,
            Path outputPath) throws IOException, SQLException, Exception {

        // 1. 从实体提取 Schema
        SchemaMetadata fromEntity = entityExtractor.convert(entityMetadata);

        // 2. 从数据库读取 Schema（如果连接存在）
        SchemaMetadata fromDatabase = null;
        if (connection != null) {
            fromDatabase = jdbcReader.readTable(connection, fromEntity.getTableName());
        }

        // 3. 从 ChangeLog 读取 Schema（如果文件存在）
        SchemaMetadata fromChangeLog = null;
        if (changeLogPath != null && Path.of(changeLogPath).toFile().exists()) {
            Map<String, SchemaMetadata> schemas = changeLogReader.read(changeLogPath);
            fromChangeLog = schemas.get(fromEntity.getTableName());
        }

        // 4. 三向比对
        ThreeWayDiff diff = schemaComparator.compareThreeWay(fromEntity, fromDatabase, fromChangeLog);

        // 5. 如果有冲突，返回差异报告
        if (diff.hasConflicts()) {
            return diff;
        }

        // 6. 根据差异生成迁移脚本
        if (fromDatabase == null) {
            // 表不存在，生成 createTable
            changeLogGenerator.generateCreateTable(fromEntity, author, outputPath);
        } else if (diff.entityVsDatabase() != null && diff.entityVsDatabase().hasDifferences()) {
            // 表存在但有差异，生成增量变更
            changeLogGenerator.generateIncremental(diff.entityVsDatabase(), author, outputPath);
        }

        return diff;
    }

    /**
     * 从数据库逆向生成实体
     *
     * @param connection  数据库连接
     * @param tableName   表名
     * @param packageName 包名
     * @param outputDir   输出目录
     */
    public void generateEntityFromDatabase(Connection connection, String tableName,
                                            String packageName, Path outputDir) throws SQLException, IOException {
        SchemaMetadata schema = jdbcReader.readTable(connection, tableName);
        entityGenerator.generate(schema, packageName, outputDir);
    }

    /**
     * 从数据库逆向生成所有实体
     *
     * @param connection  数据库连接
     * @param packageName 包名
     * @param outputDir   输出目录
     */
    public void generateAllEntitiesFromDatabase(Connection connection,
                                                 String packageName, Path outputDir) throws SQLException, IOException {
        Map<String, SchemaMetadata> schemas = jdbcReader.readAllTables(connection);
        for (SchemaMetadata schema : schemas.values()) {
            entityGenerator.generate(schema, packageName, outputDir);
        }
    }

    /**
     * 执行数据库迁移
     *
     * @param connection    数据库连接
     * @param changeLogPath ChangeLog 文件路径
     */
    public void executeMigration(Connection connection, String changeLogPath) throws LiquibaseException {
        migrationRunner.migrate(connection, changeLogPath);
    }

    /**
     * 执行数据库迁移到指定版本
     *
     * @param connection    数据库连接
     * @param changeLogPath ChangeLog 文件路径
     * @param targetVersion 目标版本
     */
    public void executeMigration(Connection connection, String changeLogPath,
                                  String targetVersion) throws LiquibaseException {
        migrationRunner.migrate(connection, changeLogPath, targetVersion);
    }

    /**
     * 获取迁移状态
     *
     * @param connection    数据库连接
     * @param changeLogPath ChangeLog 文件路径
     * @return 未执行的 ChangeSet 列表
     */
    public List<LiquibaseMigrationRunner.MigrationStatus> getMigrationStatus(
            Connection connection, String changeLogPath) throws LiquibaseException {
        return migrationRunner.status(connection, changeLogPath);
    }

    /**
     * 比对实体与数据库的差异
     *
     * @param entityMetadata 实体元数据
     * @param connection     数据库连接
     * @return 差异报告
     */
    public SchemaDiff compareEntityWithDatabase(EntityMetadata<?> entityMetadata,
                                                Connection connection) throws SQLException {
        SchemaMetadata fromEntity = entityExtractor.convert(entityMetadata);
        SchemaMetadata fromDatabase = jdbcReader.readTable(connection, fromEntity.getTableName());
        return schemaComparator.compare(fromEntity, fromDatabase);
    }

    /**
     * 创建默认的迁移服务
     */
    public static MigrationService forH2() {
        return new MigrationService(new H2Dialect());
    }

    public static MigrationService forMySQL() {
        return new MigrationService(new MySQLDialect());
    }

    public static MigrationService forPostgreSQL() {
        return new MigrationService(new PostgreSQLDialect());
    }
}