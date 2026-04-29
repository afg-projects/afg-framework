package io.github.afgprojects.framework.core.codegen;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.jspecify.annotations.NonNull;

/**
 * Entity 代码生成器
 *
 * <p>根据表结构生成 JPA Entity 或 MyBatis-Plus Entity
 *
 * <h3>使用示例</h3>
 * <pre>
 * EntityGenerator generator = new EntityGenerator();
 *
 * GeneratorContext context = GeneratorContext.builder()
 *     .className("User")
 *     .packageName("com.example.entity")
 *     .tableName("t_user")
 *     .classComment("用户实体")
 *     .fields(List.of(
 *         FieldDefinition.builder().name("id").type("Long").primaryKey(true).build(),
 *         FieldDefinition.builder().name("username").type("String").required(true).build()
 *     ))
 *     .build();
 *
 * String code = generator.generate(context);
 * </pre>
 *
 * @since 1.0.0
 */
public class EntityGenerator implements CodeGenerator {

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

        // 类注解
        List<String> annotations = collectClassAnnotations(context);
        for (String annotation : annotations) {
            sb.append(annotation).append("\n");
        }

        // 类声明
        sb.append("public class ").append(context.getClassName());

        // 父类
        if (context.getSuperClass() != null) {
            sb.append(" extends ").append(context.getSuperClass());
        }

        // 接口
        if (context.getInterfaces() != null && !context.getInterfaces().isEmpty()) {
            sb.append(" implements ")
              .append(String.join(", ", context.getInterfaces()));
        }

        sb.append(" {\n\n");

        // 字段
        if (context.getFields() != null) {
            for (GeneratorContext.FieldDefinition field : context.getFields()) {
                sb.append(generateField(field)).append("\n");
            }
        }

        // Getter/Setter 方法
        if (context.getFields() != null) {
            for (GeneratorContext.FieldDefinition field : context.getFields()) {
                sb.append(generateGetter(field)).append("\n");
                sb.append(generateSetter(field)).append("\n");
            }
        }

        sb.append("}\n");

        return sb.toString();
    }

    @Override
    @NonNull
    public String getName() {
        return "EntityGenerator";
    }

    @Override
    @NonNull
    public String getTemplateType() {
        return "entity";
    }

    private List<String> collectImports(GeneratorContext context) {
        List<String> imports = new ArrayList<>();

        // 根据字段类型添加导入
        if (context.getFields() != null) {
            for (GeneratorContext.FieldDefinition field : context.getFields()) {
                String type = field.getType();
                if (type.equals("LocalDate") || type.equals("LocalDateTime")) {
                    imports.add("java.time." + type);
                } else if (type.equals("BigDecimal")) {
                    imports.add("java.math.BigDecimal");
                }
            }
        }

        // MyBatis-Plus 注解
        if (context.getTableName() != null) {
            imports.add("com.baomidou.mybatisplus.annotation.TableName");
            imports.add("com.baomidou.mybatisplus.annotation.TableId");
            imports.add("com.baomidou.mybatisplus.annotation.TableField");
        }

        // Lombok 注解
        imports.add("lombok.Data");

        return imports.stream().distinct().sorted().collect(Collectors.toList());
    }

    private List<String> collectClassAnnotations(GeneratorContext context) {
        List<String> annotations = new ArrayList<>();
        annotations.add("@Data");

        if (context.getTableName() != null) {
            annotations.add("@TableName(\"" + context.getTableName() + "\")");
        }

        return annotations;
    }

    private String generateField(GeneratorContext.FieldDefinition field) {
        StringBuilder sb = new StringBuilder();

        // 字段注释
        if (field.getComment() != null) {
            sb.append("    /**\n")
              .append("     * ").append(field.getComment()).append("\n")
              .append("     */\n");
        }

        // 主键注解
        if (field.isPrimaryKey()) {
            sb.append("    @TableId\n");
        }

        // 列名注解
        if (field.getColumnName() != null && !field.isPrimaryKey()) {
            sb.append("    @TableField(\"").append(field.getColumnName()).append("\")\n");
        }

        // 字段声明
        sb.append("    private ")
          .append(field.getType())
          .append(" ")
          .append(field.getName())
          .append(";\n");

        return sb.toString();
    }

    private String generateGetter(GeneratorContext.FieldDefinition field) {
        String capitalizedName = capitalize(field.getName());
        return "    public " + field.getType() + " get" + capitalizedName + "() {\n" +
               "        return this." + field.getName() + ";\n" +
               "    }\n";
    }

    private String generateSetter(GeneratorContext.FieldDefinition field) {
        String capitalizedName = capitalize(field.getName());
        return "    public void set" + capitalizedName + "(" + field.getType() + " " + field.getName() + ") {\n" +
               "        this." + field.getName() + " = " + field.getName() + ";\n" +
               "    }\n";
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}