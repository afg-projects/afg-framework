package io.github.afgprojects.framework.data.liquibase.generator;

import io.github.afgprojects.framework.data.core.schema.*;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * ChangeLog 生成器
 * <p>
 * 将 SchemaMetadata 或 SchemaDiff 转换为 Liquibase XML 格式
 * <p>
 * 生成的 XML 符合项目规范：
 * <ul>
 *   <li>changeSet id 格式：{表名}-{描述}</li>
 *   <li>author 统一为 afg</li>
 *   <li>包含 remarks 注释</li>
 *   <li>自动生成索引</li>
 * </ul>
 */
public class ChangeLogGenerator {

    private static final String XML_DECLARATION = """
            <?xml version="1.0" encoding="UTF-8"?>
            <databaseChangeLog
                xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.26.xsd">
            """;

    private static final String XML_FOOTER = "</databaseChangeLog>";

    /**
     * 生成完整的 ChangeLog 文件
     *
     * @param schema   Schema 元数据
     * @param author   作者
     * @param outputPath 输出路径
     */
    public void generateCreateTable(SchemaMetadata schema, String author, Path outputPath) throws IOException {
        generateCreateTable(schema, author, outputPath, null);
    }

    /**
     * 生成完整的 ChangeLog 文件（带表注释）
     *
     * @param schema     Schema 元数据
     * @param author     作者
     * @param outputPath 输出路径
     * @param remarks    表注释（中文说明）
     */
    public void generateCreateTable(SchemaMetadata schema, String author, Path outputPath, String remarks) throws IOException {
        StringBuilder xml = new StringBuilder();
        xml.append(XML_DECLARATION);

        String changeSetId = generateChangeSetId(schema.getTableName(), "init");
        xml.append(generateCreateTableChangeSet(schema, author, changeSetId, remarks));

        // 生成索引
        if (schema.getIndexes() != null && !schema.getIndexes().isEmpty()) {
            xml.append(generateIndexChangeSet(schema, author, changeSetId + "-idx"));
        }

        xml.append(XML_FOOTER);

        try (Writer writer = Files.newBufferedWriter(outputPath)) {
            writer.write(xml.toString());
        }
    }

    /**
     * 根据差异生成增量 ChangeLog
     *
     * @param diff       Schema 差异
     * @param author     作者
     * @param outputPath 输出路径
     */
    public void generateIncremental(SchemaDiff diff, String author, Path outputPath) throws IOException {
        StringBuilder xml = new StringBuilder();
        xml.append(XML_DECLARATION);

        int changeSetNum = 1;

        // 生成新增列的 ChangeSet
        for (ColumnDiff columnDiff : diff.columnDiffs()) {
            if (columnDiff.diffType() == DiffType.ADD && columnDiff.sourceColumn() != null) {
                xml.append(generateAddColumnChangeSet(
                        diff.tableName(),
                        columnDiff.sourceColumn(),
                        author,
                        generateChangeSetId(diff.tableName(), "add-" + columnDiff.columnName())
                ));
            }
        }

        // 生成删除列的 ChangeSet
        for (ColumnDiff columnDiff : diff.columnDiffs()) {
            if (columnDiff.diffType() == DiffType.DROP) {
                xml.append(generateDropColumnChangeSet(
                        diff.tableName(),
                        columnDiff.columnName(),
                        author,
                        generateChangeSetId(diff.tableName(), "drop-" + columnDiff.columnName())
                ));
            }
        }

        // 生成修改列的 ChangeSet
        for (ColumnDiff columnDiff : diff.columnDiffs()) {
            if (columnDiff.diffType() == DiffType.MODIFY && columnDiff.sourceColumn() != null) {
                xml.append(generateModifyColumnChangeSet(
                        diff.tableName(),
                        columnDiff.sourceColumn(),
                        author,
                        generateChangeSetId(diff.tableName(), "modify-" + columnDiff.columnName())
                ));
            }
        }

        xml.append(XML_FOOTER);

        try (Writer writer = Files.newBufferedWriter(outputPath)) {
            writer.write(xml.toString());
        }
    }

    private String generateCreateTableChangeSet(SchemaMetadata schema, String author, String id, String remarks) {
        StringBuilder sb = new StringBuilder();
        sb.append("    <changeSet id=\"").append(id).append("\" author=\"").append(author).append("\">\n");

        // 添加表注释
        sb.append("        <createTable tableName=\"").append(schema.getTableName()).append("\"");
        if (remarks != null && !remarks.isEmpty()) {
            sb.append(" remarks=\"").append(escapeXml(remarks)).append("\"");
        }
        sb.append(">\n");

        for (ColumnMetadata column : schema.getColumns()) {
            sb.append(generateColumnElement(column, 12));
        }

        sb.append("        </createTable>\n");
        sb.append("    </changeSet>\n");
        return sb.toString();
    }

    private String generateIndexChangeSet(SchemaMetadata schema, String author, String id) {
        StringBuilder sb = new StringBuilder();
        sb.append("    <changeSet id=\"").append(id).append("\" author=\"").append(author).append("\">\n");

        for (IndexMetadata index : schema.getIndexes()) {
            sb.append("        <createIndex indexName=\"").append(index.getIndexName())
                    .append("\" tableName=\"").append(schema.getTableName()).append("\">\n");
            for (String columnName : index.getColumnNames()) {
                sb.append("            <column name=\"").append(columnName).append("\"/>\n");
            }
            sb.append("        </createIndex>\n");
        }

        sb.append("    </changeSet>\n");
        return sb.toString();
    }

    private String generateAddColumnChangeSet(String tableName, ColumnMetadata column,
                                               String author, String id) {
        StringBuilder sb = new StringBuilder();
        sb.append("    <changeSet id=\"").append(id).append("\" author=\"").append(author).append("\">\n");
        sb.append("        <addColumn tableName=\"").append(tableName).append("\">\n");
        sb.append(generateColumnElement(column, 12));
        sb.append("        </addColumn>\n");
        sb.append("    </changeSet>\n");
        return sb.toString();
    }

    private String generateDropColumnChangeSet(String tableName, String columnName,
                                                String author, String id) {
        StringBuilder sb = new StringBuilder();
        sb.append("    <changeSet id=\"").append(id).append("\" author=\"").append(author).append("\">\n");
        sb.append("        <dropColumn tableName=\"").append(tableName)
                .append("\" columnName=\"").append(columnName).append("\"/>\n");
        sb.append("    </changeSet>\n");
        return sb.toString();
    }

    private String generateModifyColumnChangeSet(String tableName, ColumnMetadata column,
                                                  String author, String id) {
        StringBuilder sb = new StringBuilder();
        sb.append("    <changeSet id=\"").append(id).append("\" author=\"").append(author).append("\">\n");
        sb.append("        <modifyDataType tableName=\"").append(tableName)
                .append("\" columnName=\"").append(column.getColumnName())
                .append("\" newDataType=\"").append(column.getDataType()).append("\"/>\n");
        sb.append("    </changeSet>\n");
        return sb.toString();
    }

    private String generateColumnElement(ColumnMetadata column, int indent) {
        String spaces = " ".repeat(indent);
        StringBuilder sb = new StringBuilder();
        sb.append(spaces).append("<column name=\"").append(column.getColumnName())
                .append("\" type=\"").append(column.getDataType()).append("\"");

        // 添加字段注释（remarks）
        if (column.getComment() != null && !column.getComment().isEmpty()) {
            sb.append(" remarks=\"").append(escapeXml(column.getComment())).append("\"");
        }

        boolean hasConstraints = !column.isNullable() || column.isPrimaryKey() || column.isUnique();
        boolean hasDefaultValue = column.getDefaultValue() != null && !column.getDefaultValue().isEmpty();

        if (!hasConstraints && !hasDefaultValue && !column.isAutoIncrement()) {
            sb.append("/>\n");
        } else {
            sb.append(">\n");

            if (column.isAutoIncrement()) {
                sb.append(spaces).append("    <constraints");
                if (column.isPrimaryKey()) {
                    sb.append(" primaryKey=\"true\" primaryKeyName=\"pk_").append(column.getColumnName()).append("\"");
                }
                sb.append("/>\n");
                sb.append(spaces).append("    <autoIncrement/>\n");
            } else if (hasConstraints) {
                sb.append(spaces).append("    <constraints");
                if (column.isPrimaryKey()) {
                    sb.append(" primaryKey=\"true\" primaryKeyName=\"pk_").append(column.getColumnName()).append("\"");
                }
                if (!column.isNullable()) {
                    sb.append(" nullable=\"false\"");
                }
                if (column.isUnique()) {
                    sb.append(" unique=\"true\" uniqueConstraintName=\"uk_").append(column.getColumnName()).append("\"");
                }
                sb.append("/>\n");
            }

            if (hasDefaultValue) {
                String defaultValue = column.getDefaultValue();
                if (defaultValue.equalsIgnoreCase("CURRENT_TIMESTAMP")) {
                    sb.append(spaces).append("    <defaultValueDate>CURRENT_TIMESTAMP</defaultValueDate>\n");
                } else if (defaultValue.matches("-?\\d+")) {
                    sb.append(spaces).append("    <defaultValueNumeric>").append(defaultValue).append("</defaultValueNumeric>\n");
                } else if (defaultValue.equalsIgnoreCase("true") || defaultValue.equalsIgnoreCase("false")) {
                    sb.append(spaces).append("    <defaultValueBoolean>").append(defaultValue).append("</defaultValueBoolean>\n");
                } else {
                    sb.append(spaces).append("    <defaultValue>").append(escapeXml(defaultValue)).append("</defaultValue>\n");
                }
            }

            sb.append(spaces).append("</column>\n");
        }

        return sb.toString();
    }

    /**
     * 生成 changeSet id
     * <p>
     * 格式：{表名}-{描述}
     * <p>
     * 示例：sys-user-init, sys-user-add-phone
     */
    private String generateChangeSetId(String tableName, String description) {
        // 将表名转换为 kebab-case 格式
        String normalizedTableName = tableName.replace("_", "-");
        return normalizedTableName + "-" + description;
    }

    private String escapeXml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
