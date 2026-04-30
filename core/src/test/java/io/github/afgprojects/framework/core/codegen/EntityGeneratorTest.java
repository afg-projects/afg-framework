package io.github.afgprojects.framework.core.codegen;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EntityGenerator 测试
 */
@DisplayName("EntityGenerator 测试")
class EntityGeneratorTest {

    private final EntityGenerator generator = new EntityGenerator();

    @Test
    @DisplayName("应该返回正确的名称")
    void shouldReturnCorrectName() {
        assertEquals("EntityGenerator", generator.getName());
    }

    @Test
    @DisplayName("应该返回正确的模板类型")
    void shouldReturnCorrectTemplateType() {
        assertEquals("entity", generator.getTemplateType());
    }

    @Test
    @DisplayName("应该生成基本的 Entity 类")
    void shouldGenerateBasicEntityClass() {
        GeneratorContext context = GeneratorContext.builder()
                .className("User")
                .packageName("com.example.entity")
                .fields(List.of())
                .build();

        String code = generator.generate(context);

        assertTrue(code.contains("package com.example.entity;"));
        assertTrue(code.contains("public class User {"));
        assertTrue(code.contains("}"));
    }

    @Test
    @DisplayName("应该生成带表名的 Entity 类")
    void shouldGenerateEntityWithTableName() {
        GeneratorContext context = GeneratorContext.builder()
                .className("User")
                .packageName("com.example.entity")
                .tableName("t_user")
                .fields(List.of())
                .build();

        String code = generator.generate(context);

        assertTrue(code.contains("@TableName(\"t_user\")"));
    }

    @Test
    @DisplayName("应该生成带注释的 Entity 类")
    void shouldGenerateEntityWithComment() {
        GeneratorContext context = GeneratorContext.builder()
                .className("User")
                .packageName("com.example.entity")
                .classComment("用户实体")
                .fields(List.of())
                .build();

        String code = generator.generate(context);

        assertTrue(code.contains("/**"));
        assertTrue(code.contains("用户实体"));
        assertTrue(code.contains("*/"));
    }

    @Test
    @DisplayName("应该生成带字段的 Entity 类")
    void shouldGenerateEntityWithFields() {
        GeneratorContext.FieldDefinition idField = GeneratorContext.FieldDefinition.builder()
                .name("id")
                .type("Long")
                .primaryKey(true)
                .build();

        GeneratorContext.FieldDefinition nameField = GeneratorContext.FieldDefinition.builder()
                .name("username")
                .type("String")
                .comment("用户名")
                .columnName("user_name")
                .build();

        GeneratorContext context = GeneratorContext.builder()
                .className("User")
                .packageName("com.example.entity")
                .tableName("t_user")
                .fields(List.of(idField, nameField))
                .build();

        String code = generator.generate(context);

        assertTrue(code.contains("private Long id"));
        assertTrue(code.contains("private String username"));
        assertTrue(code.contains("@TableId"));
        assertTrue(code.contains("@TableField(\"user_name\")"));
        assertTrue(code.contains("用户名"));
        assertTrue(code.contains("getId()"));
        assertTrue(code.contains("setId("));
        assertTrue(code.contains("getUsername()"));
        assertTrue(code.contains("setUsername("));
    }

    @Test
    @DisplayName("应该生成带父类的 Entity 类")
    void shouldGenerateEntityWithSuperClass() {
        GeneratorContext context = GeneratorContext.builder()
                .className("User")
                .packageName("com.example.entity")
                .superClass("BaseEntity")
                .fields(List.of())
                .build();

        String code = generator.generate(context);

        assertTrue(code.contains("extends BaseEntity"));
    }

    @Test
    @DisplayName("应该生成带接口的 Entity 类")
    void shouldGenerateEntityWithInterfaces() {
        GeneratorContext context = GeneratorContext.builder()
                .className("User")
                .packageName("com.example.entity")
                .interfaces(List.of("Serializable", "Cloneable"))
                .fields(List.of())
                .build();

        String code = generator.generate(context);

        assertTrue(code.contains("implements Serializable, Cloneable"));
    }

    @Test
    @DisplayName("应该添加必要的导入")
    void shouldAddNecessaryImports() {
        GeneratorContext.FieldDefinition dateField = GeneratorContext.FieldDefinition.builder()
                .name("createdAt")
                .type("LocalDateTime")
                .build();

        GeneratorContext.FieldDefinition amountField = GeneratorContext.FieldDefinition.builder()
                .name("amount")
                .type("BigDecimal")
                .build();

        GeneratorContext context = GeneratorContext.builder()
                .className("Order")
                .packageName("com.example.entity")
                .tableName("t_order")
                .fields(List.of(dateField, amountField))
                .build();

        String code = generator.generate(context);

        assertTrue(code.contains("import java.time.LocalDateTime"));
        assertTrue(code.contains("import java.math.BigDecimal"));
        assertTrue(code.contains("import com.baomidou.mybatisplus.annotation.TableName"));
        assertTrue(code.contains("import lombok.Data"));
    }
}
