package io.github.afgprojects.framework.data.liquibase.generator;

import io.github.afgprojects.framework.data.core.schema.*;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ChangeLog 生成器
 * <p>
 * 将 SchemaMetadata 或 SchemaDiff 转换为 Liquibase XML 格式
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
        StringBuilder xml = new StringBuilder();
        xml.append(XML_DECLARATION);

        String changeSetId = generateChangeSetId();
        xml.append(generateCreateTableChangeSet(schema, author, changeSetId));

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
                        changeSetNum++
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
                        changeSetNum++
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
                        changeSetNum++
                ));
            }
        }

        xml.append(XML_FOOTER);

        try (Writer writer = Files.newBufferedWriter(outputPath)) {
            writer.write(xml.toString());
        }
    }

    private String generateCreateTableChangeSet(SchemaMetadata schema, String author, String id) {
        StringBuilder sb = new StringBuilder();
        sb.append("    <changeSet id=\"").append(id).append("\" author=\"").append(author).append("\">\n");
        sb.append("        <createTable tableName=\"").append(schema.getTableName()).append("\">\n");

        for (ColumnMetadata column : schema.getColumns()) {
            sb.append(generateColumnElement(column, 12));
        }

        sb.append("        </createTable>\n");
        sb.append("    </changeSet>\n");
        return sb.toString();
    }

    private String generateAddColumnChangeSet(String tableName, ColumnMetadata column,
                                               String author, int changeSetNum) {
        StringBuilder sb = new StringBuilder();
        String id = generateChangeSetId() + "_" + changeSetNum;
        sb.append("    <changeSet id=\"").append(id).append("\" author=\"").append(author).append("\">\n");
        sb.append("        <addColumn tableName=\"").append(tableName).append("\">\n");
        sb.append(generateColumnElement(column, 12));
        sb.append("        </addColumn>\n");
        sb.append("    </changeSet>\n");
        return sb.toString();
    }

    private String generateDropColumnChangeSet(String tableName, String columnName,
                                                String author, int changeSetNum) {
        StringBuilder sb = new StringBuilder();
        String id = generateChangeSetId() + "_" + changeSetNum;
        sb.append("    <changeSet id=\"").append(id).append("\" author=\"").append(author).append("\">\n");
        sb.append("        <dropColumn tableName=\"").append(tableName)
                .append("\" columnName=\"").append(columnName).append("\"/>\n");
        sb.append("    </changeSet>\n");
        return sb.toString();
    }

    private String generateModifyColumnChangeSet(String tableName, ColumnMetadata column,
                                                  String author, int changeSetNum) {
        StringBuilder sb = new StringBuilder();
        String id = generateChangeSetId() + "_" + changeSetNum;
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

        boolean hasConstraints = !column.isNullable() || column.isPrimaryKey() || column.isUnique();

        if (!hasConstraints && column.getDefaultValue() == null) {
            sb.append("/>\n");
        } else {
            sb.append(">\n");

            if (hasConstraints) {
                sb.append(spaces).append("    <constraints");
                if (column.isPrimaryKey()) {
                    sb.append(" primaryKey=\"true\"");
                }
                if (!column.isNullable()) {
                    sb.append(" nullable=\"false\"");
                }
                if (column.isUnique()) {
                    sb.append(" unique=\"true\"");
                }
                sb.append("/>\n");
            }

            if (column.getDefaultValue() != null) {
                sb.append(spaces).append("    <defaultValue>")
                        .append(escapeXml(column.getDefaultValue()))
                        .append("</defaultValue>\n");
            }

            sb.append(spaces).append("</column>\n");
        }

        return sb.toString();
    }

    private String generateChangeSetId() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
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
