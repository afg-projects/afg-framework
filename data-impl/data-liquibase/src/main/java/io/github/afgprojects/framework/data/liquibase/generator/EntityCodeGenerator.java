package io.github.afgprojects.framework.data.liquibase.generator;

import io.github.afgprojects.framework.data.core.schema.ColumnMetadata;
import io.github.afgprojects.framework.data.core.schema.SchemaMetadata;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Entity 代码生成器
 * <p>
 * 将 SchemaMetadata 转换为 Entity Java 代码
 */
public class EntityCodeGenerator {

    private final boolean useLombok;
    private final boolean useJsr305;

    public EntityCodeGenerator() {
        this(true, true);
    }

    public EntityCodeGenerator(boolean useLombok, boolean useJsr305) {
        this.useLombok = useLombok;
        this.useJsr305 = useJsr305;
    }

    /**
     * 生成 Entity Java 代码
     *
     * @param schema      Schema 元数据
     * @param packageName 包名
     * @param outputDir   输出目录
     */
    public void generate(SchemaMetadata schema, String packageName, Path outputDir) throws IOException {
        String className = toClassName(schema.getTableName());
        String filePath = packageName.replace('.', '/') + "/" + className + ".java";
        Path outputFile = outputDir.resolve(filePath);

        Files.createDirectories(outputFile.getParent());

        try (Writer writer = Files.newBufferedWriter(outputFile)) {
            writer.write(generateClass(schema, packageName, className));
        }
    }

    private String generateClass(SchemaMetadata schema, String packageName, String className) {
        StringBuilder sb = new StringBuilder();

        // 包声明
        sb.append("package ").append(packageName).append(";\n\n");

        // 导入
        sb.append(generateImports(schema));

        // 类注释
        sb.append("/**\n");
        sb.append(" * ").append(className).append(" 实体类\n");
        sb.append(" * 对应数据库表: ").append(schema.getTableName()).append("\n");
        sb.append(" */\n");

        // 类声明
        if (useLombok) {
            sb.append("@Data\n");
        }
        sb.append("public class ").append(className).append(" extends BaseEntity<Long> {\n\n");

        // 字段
        for (ColumnMetadata column : schema.getColumns()) {
            // 跳过 BaseEntity 中的字段
            if (isBaseEntityField(column.getColumnName())) {
                continue;
            }
            sb.append(generateField(column));
        }

        // 如果不使用 Lombok，生成 getter/setter
        if (!useLombok) {
            for (ColumnMetadata column : schema.getColumns()) {
                if (isBaseEntityField(column.getColumnName())) {
                    continue;
                }
                sb.append(generateGetterSetter(column));
            }
        }

        sb.append("}\n");

        return sb.toString();
    }

    private String generateImports(SchemaMetadata schema) {
        StringBuilder sb = new StringBuilder();

        // BaseEntity
        sb.append("import entity.io.github.afgprojects.data.framework.core.BaseEntity;\n");

        // 时间类型
        boolean hasDateTime = schema.getColumns().stream()
                .anyMatch(c -> c.getDataType().toUpperCase().contains("DATE")
                        || c.getDataType().toUpperCase().contains("TIME"));
        if (hasDateTime) {
            sb.append("import java.time.LocalDateTime;\n");
        }

        // BigDecimal
        boolean hasDecimal = schema.getColumns().stream()
                .anyMatch(c -> c.getDataType().toUpperCase().contains("DECIMAL")
                        || c.getDataType().toUpperCase().contains("NUMERIC"));
        if (hasDecimal) {
            sb.append("import java.math.BigDecimal;\n");
        }

        // JSR-305
        if (useJsr305) {
            sb.append("import org.jspecify.annotations.Nullable;\n");
        }

        // Lombok
        if (useLombok) {
            sb.append("import lombok.Data;\n");
        }

        sb.append("\n");
        return sb.toString();
    }

    private String generateField(ColumnMetadata column) {
        StringBuilder sb = new StringBuilder();
        sb.append("    /**\n");
        sb.append("     * ").append(column.getColumnName());
        if (column.getComment() != null && !column.getComment().isEmpty()) {
            sb.append(" - ").append(column.getComment());
        }
        sb.append("\n");
        sb.append("     */\n");

        String javaType = toJavaType(column.getDataType());

        if (useJsr305 && column.isNullable()) {
            sb.append("    private ").append("@Nullable ").append(javaType)
                    .append(" ").append(toFieldName(column.getColumnName())).append(";\n\n");
        } else {
            sb.append("    private ").append(javaType)
                    .append(" ").append(toFieldName(column.getColumnName())).append(";\n\n");
        }

        return sb.toString();
    }

    private String generateGetterSetter(ColumnMetadata column) {
        StringBuilder sb = new StringBuilder();
        String fieldName = toFieldName(column.getColumnName());
        String javaType = toJavaType(column.getDataType());
        String capitalizedName = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

        // Getter
        sb.append("    public ").append(javaType).append(" get").append(capitalizedName).append("() {\n");
        sb.append("        return ").append(fieldName).append(";\n");
        sb.append("    }\n\n");

        // Setter
        sb.append("    public void set").append(capitalizedName).append("(").append(javaType).append(" ").append(fieldName).append(") {\n");
        sb.append("        this.").append(fieldName).append(" = ").append(fieldName).append(";\n");
        sb.append("    }\n\n");

        return sb.toString();
    }

    private String toClassName(String tableName) {
        String[] parts = tableName.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }

    private String toFieldName(String columnName) {
        String[] parts = columnName.split("_");
        StringBuilder sb = new StringBuilder(parts[0].toLowerCase());
        for (int i = 1; i < parts.length; i++) {
            sb.append(Character.toUpperCase(parts[i].charAt(0)))
                    .append(parts[i].substring(1).toLowerCase());
        }
        return sb.toString();
    }

    private String toJavaType(String sqlType) {
        String upper = sqlType.toUpperCase();

        if (upper.contains("BIGINT")) return "Long";
        if (upper.contains("INT")) return "Integer";
        if (upper.contains("SMALLINT")) return "Short";
        if (upper.contains("TINYINT")) return "Boolean";
        if (upper.contains("BIT")) return "Boolean";
        if (upper.contains("BOOLEAN")) return "Boolean";
        if (upper.contains("DECIMAL") || upper.contains("NUMERIC")) return "BigDecimal";
        if (upper.contains("DOUBLE")) return "Double";
        if (upper.contains("FLOAT") || upper.contains("REAL")) return "Float";
        if (upper.contains("DATE") && !upper.contains("TIME")) return "LocalDate";
        if (upper.contains("TIME") || upper.contains("DATETIME") || upper.contains("TIMESTAMP")) return "LocalDateTime";
        if (upper.contains("BLOB") || upper.contains("BINARY") || upper.contains("VARBINARY")) return "byte[]";
        if (upper.contains("CLOB") || upper.contains("TEXT")) return "String";

        return "String";
    }

    private boolean isBaseEntityField(String columnName) {
        return List.of("id", "create_time", "update_time", "created_at", "updated_at").contains(columnName.toLowerCase());
    }
}
