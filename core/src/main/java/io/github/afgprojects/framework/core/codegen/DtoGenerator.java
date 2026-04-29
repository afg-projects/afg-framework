package io.github.afgprojects.framework.core.codegen;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.jspecify.annotations.NonNull;

/**
 * DTO 代码生成器
 *
 * <p>根据 Entity 或字段定义生成 DTO 类
 *
 * @since 1.0.0
 */
public class DtoGenerator implements CodeGenerator {

    @Override
    @NonNull
    public String generate(@NonNull GeneratorContext context) {
        StringBuilder sb = new StringBuilder();

        // 包声明
        sb.append("package ").append(context.getPackageName()).append(";\n\n");

        // 导入
        List<String> imports = collectImports(context);
        for (String imp : imports) {
            sb.append("import ").append(imp).append(";\n");
        }
        if (!imports.isEmpty()) {
            sb.append("\n");
        }

        // 类注释
        if (context.getClassComment() != null) {
            sb.append("/**\n")
              .append(" * ").append(context.getClassComment()).append("\n")
              .append(" */\n");
        }

        // Schema 注解（OpenAPI）
        if (context.getClassComment() != null) {
            sb.append("@Schema(description = \"").append(context.getClassComment()).append("\")\n");
        }

        // record 类型声明
        sb.append("public record ").append(context.getClassName()).append("(");

        // 字段
        if (context.getFields() != null && !context.getFields().isEmpty()) {
            sb.append("\n");
            List<String> fieldStrings = new ArrayList<>();
            for (GeneratorContext.FieldDefinition field : context.getFields()) {
                fieldStrings.add(generateRecordField(field));
            }
            sb.append(String.join(",\n", fieldStrings));
            sb.append("\n");
        }

        sb.append(") {\n");
        sb.append("}\n");

        return sb.toString();
    }

    @Override
    @NonNull
    public String getName() {
        return "DtoGenerator";
    }

    @Override
    @NonNull
    public String getTemplateType() {
        return "dto";
    }

    private List<String> collectImports(GeneratorContext context) {
        List<String> imports = new ArrayList<>();

        imports.add("io.swagger.v3.oas.annotations.media.Schema");

        if (context.getFields() != null) {
            for (GeneratorContext.FieldDefinition field : context.getFields()) {
                String type = field.getType();
                if (type.equals("LocalDate") || type.equals("LocalDateTime")) {
                    imports.add("java.time." + type);
                } else if (type.equals("BigDecimal")) {
                    imports.add("java.math.BigDecimal");
                } else if (type.contains("List<")) {
                    imports.add("java.util.List");
                }
            }
        }

        return imports.stream().distinct().sorted().collect(Collectors.toList());
    }

    private String generateRecordField(GeneratorContext.FieldDefinition field) {
        StringBuilder sb = new StringBuilder();
        sb.append("        ");

        // Schema 注解
        sb.append("@Schema(");
        if (field.getComment() != null) {
            sb.append("description = \"").append(field.getComment()).append("\"");
        }
        if (field.isRequired()) {
            if (field.getComment() != null) {
                sb.append(", ");
            }
            sb.append("requiredMode = Schema.RequiredMode.REQUIRED");
        }
        sb.append(")\n");
        sb.append("        ");

        sb.append(field.getType()).append(" ").append(field.getName());

        return sb.toString();
    }
}