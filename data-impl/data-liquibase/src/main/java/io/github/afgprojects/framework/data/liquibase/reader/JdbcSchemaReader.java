package io.github.afgprojects.framework.data.liquibase.reader;

import io.github.afgprojects.framework.data.core.schema.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JDBC Schema 读取器
 * <p>
 * 通过 JDBC DatabaseMetaData 读取数据库表结构，支持多种数据库：
 * <ul>
 *   <li>MySQL / MariaDB - 使用 catalog</li>
 *   <li>PostgreSQL - 使用 schema (public)</li>
 *   <li>Oracle - 使用 schema (用户名)</li>
 *   <li>SQL Server - 使用 schema (dbo)</li>
 *   <li>H2 - 使用 catalog</li>
 *   <li>DM (达梦) - 使用 schema</li>
 *   <li>KingBase (金仓) - 使用 schema</li>
 *   <li>OceanBase - 使用 catalog</li>
 *   <li>GaussDB / openGauss - 使用 schema</li>
 * </ul>
 */
public class JdbcSchemaReader {

    /**
     * 读取指定表的结构
     *
     * @param conn      数据库连接
     * @param tableName 表名
     * @return SchemaMetadata
     */
    public SchemaMetadata readTable(Connection conn, String tableName) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        String catalog = getCatalog(conn, metaData);
        String schema = getSchemaName(conn, metaData);

        // 处理表名大小写问题
        String actualTableName = resolveTableName(metaData, catalog, schema, tableName);

        SchemaMetadataImpl.Builder builder = SchemaMetadataImpl.builder()
                .tableName(actualTableName);

        // 读取列
        readColumns(metaData, catalog, schema, actualTableName, builder);

        // 读取主键
        readPrimaryKey(metaData, catalog, schema, actualTableName, builder);

        // 读取索引
        readIndexes(metaData, catalog, schema, actualTableName, builder);

        // 读取外键
        readForeignKeys(metaData, catalog, schema, actualTableName, builder);

        return builder.build();
    }

    /**
     * 读取所有表的结构
     *
     * @param conn 数据库连接
     * @return 表名 -> SchemaMetadata 映射
     */
    public Map<String, SchemaMetadata> readAllTables(Connection conn) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        String catalog = getCatalog(conn, metaData);
        String schema = getSchemaName(conn, metaData);

        Map<String, SchemaMetadata> tables = new HashMap<>();

        try (ResultSet rs = metaData.getTables(catalog, schema, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                tables.put(tableName, readTable(conn, tableName));
            }
        }

        return tables;
    }

    /**
     * 获取 catalog 名称
     * <p>
     * MySQL、MariaDB、H2、OceanBase 使用 catalog
     */
    private String getCatalog(Connection conn, DatabaseMetaData metaData) throws SQLException {
        String dbName = metaData.getDatabaseProductName().toLowerCase();

        // 这些数据库使用 catalog
        if (dbName.contains("mysql") || dbName.contains("mariadb") ||
            dbName.contains("h2") || dbName.contains("oceanbase")) {
            return conn.getCatalog();
        }

        // 其他数据库不使用 catalog
        return null;
    }

    /**
     * 获取 schema 名称
     * <p>
     * PostgreSQL、Oracle、SQL Server、DM、KingBase、GaussDB 使用 schema
     */
    private String getSchemaName(Connection conn, DatabaseMetaData metaData) throws SQLException {
        String dbName = metaData.getDatabaseProductName().toLowerCase();

        if (dbName.contains("postgresql")) {
            return "public";
        }

        if (dbName.contains("oracle")) {
            // Oracle 使用用户名作为 schema
            return conn.getSchema();
        }

        if (dbName.contains("sql server")) {
            return "dbo";
        }

        if (dbName.contains("dm") || dbName.contains("达梦")) {
            return conn.getSchema();
        }

        if (dbName.contains("kingbase") || dbName.contains("金仓")) {
            return conn.getSchema();
        }

        if (dbName.contains("gaussdb") || dbName.contains("opengauss")) {
            return conn.getSchema();
        }

        // 默认不使用 schema
        return null;
    }

    /**
     * 解析实际表名（处理大小写问题）
     * <p>
     * 不同数据库对表名大小写处理不同：
     * <ul>
     *   <li>MySQL - 默认小写，取决于 lower_case_table_names 配置</li>
     *   <li>PostgreSQL - 默认小写，除非用双引号</li>
     *   <li>Oracle - 默认大写</li>
     *   <li>SQL Server - 取决于排序规则</li>
     * </ul>
     */
    private String resolveTableName(DatabaseMetaData metaData, String catalog, String schema,
                                     String tableName) throws SQLException {
        // 先尝试原始表名
        try (ResultSet rs = metaData.getTables(catalog, schema, tableName, new String[]{"TABLE"})) {
            if (rs.next()) {
                return tableName;
            }
        }

        // 尝试小写
        String lowerName = tableName.toLowerCase();
        try (ResultSet rs = metaData.getTables(catalog, schema, lowerName, new String[]{"TABLE"})) {
            if (rs.next()) {
                return lowerName;
            }
        }

        // 尝试大写
        String upperName = tableName.toUpperCase();
        try (ResultSet rs = metaData.getTables(catalog, schema, upperName, new String[]{"TABLE"})) {
            if (rs.next()) {
                return upperName;
            }
        }

        // 都找不到，返回原始表名（后续操作可能会失败）
        return tableName;
    }

    private void readColumns(DatabaseMetaData metaData, String catalog, String schema,
                             String tableName, SchemaMetadataImpl.Builder builder) throws SQLException {
        try (ResultSet rs = metaData.getColumns(catalog, schema, tableName, "%")) {
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                String typeName = rs.getString("TYPE_NAME");
                int columnSize = rs.getInt("COLUMN_SIZE");
                int nullable = rs.getInt("NULLABLE");
                String defaultValue = rs.getString("COLUMN_DEF");
                String remarks = rs.getString("REMARKS");

                String dataType = columnSize > 0
                        ? typeName + "(" + columnSize + ")"
                        : typeName;

                ColumnMetadata column = ColumnMetadataImpl.builder()
                        .columnName(columnName)
                        .dataType(dataType)
                        .nullable(nullable == DatabaseMetaData.columnNullable)
                        .defaultValue(defaultValue)
                        .comment(remarks)
                        .build();

                builder.addColumn(column);
            }
        }
    }

    private void readPrimaryKey(DatabaseMetaData metaData, String catalog, String schema,
                                 String tableName, SchemaMetadataImpl.Builder builder) throws SQLException {
        try (ResultSet rs = metaData.getPrimaryKeys(catalog, schema, tableName)) {
            String constraintName = null;
            List<String> columnNames = new ArrayList<>();

            while (rs.next()) {
                constraintName = rs.getString("PK_NAME");
                columnNames.add(rs.getString("COLUMN_NAME"));
            }

            if (!columnNames.isEmpty()) {
                PrimaryKeyMetadata primaryKey = PrimaryKeyMetadataImpl.builder()
                        .constraintName(constraintName != null ? constraintName : "pk_" + tableName)
                        .columnNames(columnNames)
                        .build();
                builder.primaryKey(primaryKey);
            }
        }
    }

    private void readIndexes(DatabaseMetaData metaData, String catalog, String schema,
                              String tableName, SchemaMetadataImpl.Builder builder) throws SQLException {
        try (ResultSet rs = metaData.getIndexInfo(catalog, schema, tableName, false, true)) {
            Map<String, List<String>> indexColumns = new HashMap<>();
            Map<String, Boolean> indexUnique = new HashMap<>();

            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                if (indexName == null) continue;

                String columnName = rs.getString("COLUMN_NAME");
                boolean unique = !rs.getBoolean("NON_UNIQUE");

                indexColumns.computeIfAbsent(indexName, k -> new ArrayList<>()).add(columnName);
                indexUnique.put(indexName, unique);
            }

            for (Map.Entry<String, List<String>> entry : indexColumns.entrySet()) {
                IndexMetadata index = IndexMetadataImpl.builder()
                        .indexName(entry.getKey())
                        .columnNames(entry.getValue())
                        .unique(indexUnique.get(entry.getKey()))
                        .build();
                builder.addIndex(index);
            }
        }
    }

    private void readForeignKeys(DatabaseMetaData metaData, String catalog, String schema,
                                  String tableName, SchemaMetadataImpl.Builder builder) throws SQLException {
        try (ResultSet rs = metaData.getImportedKeys(catalog, schema, tableName)) {
            Map<String, ForeignKeyMetadataImpl.Builder> fkBuilders = new HashMap<>();

            while (rs.next()) {
                String fkName = rs.getString("FK_NAME");
                if (fkName == null) continue;

                String columnName = rs.getString("FKCOLUMN_NAME");
                String referencedTable = rs.getString("PKTABLE_NAME");
                String referencedColumn = rs.getString("PKCOLUMN_NAME");
                int updateRule = rs.getInt("UPDATE_RULE");
                int deleteRule = rs.getInt("DELETE_RULE");

                ForeignKeyMetadataImpl.Builder fkBuilder = fkBuilders.computeIfAbsent(
                        fkName, k -> ForeignKeyMetadataImpl.builder()
                                .constraintName(fkName)
                                .referencedTableName(referencedTable)
                                .updateRule(ruleToString(updateRule))
                                .deleteRule(ruleToString(deleteRule))
                );

                fkBuilder.columnNames(List.of(columnName));
                fkBuilder.referencedColumnNames(List.of(referencedColumn));
            }

            for (ForeignKeyMetadataImpl.Builder fkBuilder : fkBuilders.values()) {
                builder.addForeignKey(fkBuilder.build());
            }
        }
    }

    private String ruleToString(int rule) {
        return switch (rule) {
            case DatabaseMetaData.importedKeyCascade -> "CASCADE";
            case DatabaseMetaData.importedKeySetNull -> "SET NULL";
            case DatabaseMetaData.importedKeySetDefault -> "SET DEFAULT";
            case DatabaseMetaData.importedKeyRestrict -> "RESTRICT";
            default -> "NO ACTION";
        };
    }
}
