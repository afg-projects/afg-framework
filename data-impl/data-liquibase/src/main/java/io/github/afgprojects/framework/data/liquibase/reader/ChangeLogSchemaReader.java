package io.github.afgprojects.framework.data.liquibase.reader;

import io.github.afgprojects.framework.data.core.schema.*;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.changelog.ChangeLogParameters;
import liquibase.parser.ChangeLogParser;
import liquibase.parser.ChangeLogParserFactory;
import liquibase.resource.DirectoryResourceAccessor;
import liquibase.change.Change;
import liquibase.change.core.CreateTableChange;
import liquibase.change.core.AddColumnChange;
import liquibase.change.ColumnConfig;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

/**
 * ChangeLog Schema 读取器
 * <p>
 * 解析 Liquibase XML ChangeLog 提取 SchemaMetadata
 */
public class ChangeLogSchemaReader {

    /**
     * 读取 ChangeLog 文件，提取所有表的 Schema
     *
     * @param changeLogPath ChangeLog 文件路径
     * @return 表名 -> SchemaMetadata 映射
     */
    public Map<String, SchemaMetadata> read(String changeLogPath) throws Exception {
        return read(Path.of(changeLogPath));
    }

    /**
     * 读取 ChangeLog 文件，提取所有表的 Schema
     *
     * @param changeLogPath ChangeLog 文件路径
     * @return 表名 -> SchemaMetadata 映射
     */
    public Map<String, SchemaMetadata> read(Path changeLogPath) throws Exception {
        Map<String, SchemaMetadataImpl.Builder> schemaBuilders = new HashMap<>();

        File changeLogFile = changeLogPath.toFile();
        File parentDir = changeLogFile.getParentFile();

        try (DirectoryResourceAccessor resourceAccessor = new DirectoryResourceAccessor(parentDir)) {
            ChangeLogParser parser = ChangeLogParserFactory.getInstance()
                    .getParser(changeLogFile.getName(), resourceAccessor);

            DatabaseChangeLog changeLog = parser.parse(
                    changeLogFile.getName(),
                    new ChangeLogParameters(),
                    resourceAccessor
            );

            // 遍历所有 ChangeSet
            for (ChangeSet changeSet : changeLog.getChangeSets()) {
                for (Change change : changeSet.getChanges()) {
                    processChange(change, schemaBuilders);
                }
            }
        }

        // 构建最终的 SchemaMetadata
        Map<String, SchemaMetadata> result = new HashMap<>();
        for (Map.Entry<String, SchemaMetadataImpl.Builder> entry : schemaBuilders.entrySet()) {
            result.put(entry.getKey(), entry.getValue().build());
        }

        return result;
    }

    private void processChange(Change change, Map<String, SchemaMetadataImpl.Builder> schemaBuilders) {
        if (change instanceof CreateTableChange createTable) {
            processCreateTable(createTable, schemaBuilders);
        } else if (change instanceof AddColumnChange addColumn) {
            processAddColumn(addColumn, schemaBuilders);
        }
        // 可以添加更多 Change 类型的处理
    }

    private void processCreateTable(CreateTableChange change,
                                     Map<String, SchemaMetadataImpl.Builder> schemaBuilders) {
        String tableName = change.getTableName();
        SchemaMetadataImpl.Builder builder = schemaBuilders.computeIfAbsent(
                tableName, k -> SchemaMetadataImpl.builder().tableName(tableName)
        );

        for (ColumnConfig columnConfig : change.getColumns()) {
            ColumnMetadata column = toColumnMetadata(columnConfig);
            builder.addColumn(column);

            // 检查主键
            if (columnConfig.getConstraints() != null
                    && Boolean.TRUE.equals(columnConfig.getConstraints().isPrimaryKey())) {
                PrimaryKeyMetadata pk = PrimaryKeyMetadataImpl.builder()
                        .constraintName("pk_" + tableName)
                        .columnNames(List.of(columnConfig.getName()))
                        .build();
                builder.primaryKey(pk);
            }
        }
    }

    private void processAddColumn(AddColumnChange change,
                                   Map<String, SchemaMetadataImpl.Builder> schemaBuilders) {
        String tableName = change.getTableName();
        SchemaMetadataImpl.Builder builder = schemaBuilders.get(tableName);

        if (builder == null) {
            // 表不存在，可能是增量更新
            builder = SchemaMetadataImpl.builder().tableName(tableName);
            schemaBuilders.put(tableName, builder);
        }

        for (ColumnConfig columnConfig : change.getColumns()) {
            ColumnMetadata column = toColumnMetadata(columnConfig);
            builder.addColumn(column);
        }
    }

    private ColumnMetadata toColumnMetadata(ColumnConfig config) {
        ColumnMetadataImpl.Builder builder = ColumnMetadataImpl.builder()
                .columnName(config.getName())
                .dataType(config.getType() != null ? config.getType() : "VARCHAR(255)");

        if (config.getConstraints() != null) {
            builder.nullable(!Boolean.FALSE.equals(config.getConstraints().isNullable()));
            builder.unique(Boolean.TRUE.equals(config.getConstraints().isUnique()));
            builder.primaryKey(Boolean.TRUE.equals(config.getConstraints().isPrimaryKey()));
        } else {
            builder.nullable(true);
        }

        if (config.getDefaultValue() != null) {
            Object defaultValue = config.getDefaultValue();
            builder.defaultValue(defaultValue instanceof String ? (String) defaultValue : defaultValue.toString());
        }

        return builder.build();
    }
}
