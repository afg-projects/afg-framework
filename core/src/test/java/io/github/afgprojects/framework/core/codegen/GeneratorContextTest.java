package io.github.afgprojects.framework.core.codegen;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GeneratorContext 测试
 */
@DisplayName("GeneratorContext 测试")
class GeneratorContextTest {

    @Test
    @DisplayName("应该使用 Builder 创建 GeneratorContext")
    void shouldCreateWithBuilder() {
        GeneratorContext context = GeneratorContext.builder()
                .className("UserEntity")
                .packageName("com.example.entity")
                .tableName("user")
                .classComment("用户实体")
                .fields(List.of())
                .build();

        assertEquals("UserEntity", context.getClassName());
        assertEquals("com.example.entity", context.getPackageName());
        assertEquals("user", context.getTableName());
        assertEquals("用户实体", context.getClassComment());
    }

    @Test
    @DisplayName("应该正确设置和获取 fields")
    void shouldSetAndGetFields() {
        GeneratorContext.FieldDefinition field = GeneratorContext.FieldDefinition.builder()
                .name("id")
                .type("Long")
                .primaryKey(true)
                .build();

        GeneratorContext context = GeneratorContext.builder()
                .className("TestEntity")
                .packageName("com.example")
                .fields(List.of(field))
                .build();

        assertEquals(1, context.getFields().size());
        assertEquals("id", context.getFields().get(0).getName());
        assertTrue(context.getFields().get(0).isPrimaryKey());
    }

    @Test
    @DisplayName("应该正确设置和获取 methods")
    void shouldSetAndGetMethods() {
        GeneratorContext.MethodDefinition method = GeneratorContext.MethodDefinition.builder()
                .name("getId")
                .returnType("Long")
                .build();

        GeneratorContext context = GeneratorContext.builder()
                .className("TestEntity")
                .packageName("com.example")
                .fields(List.of())
                .methods(List.of(method))
                .build();

        assertNotNull(context.getMethods());
        assertEquals(1, context.getMethods().size());
        assertEquals("getId", context.getMethods().get(0).getName());
    }

    @Test
    @DisplayName("应该正确设置和获取 extraProperties")
    void shouldSetAndGetExtraProperties() {
        GeneratorContext context = GeneratorContext.builder()
                .className("TestEntity")
                .packageName("com.example")
                .fields(List.of())
                .extraProperties(Map.of("key", "value"))
                .build();

        assertEquals("value", context.getExtraProperties().get("key"));
    }

    @Test
    @DisplayName("应该正确设置和获取 superClass")
    void shouldSetAndGetSuperClass() {
        GeneratorContext context = GeneratorContext.builder()
                .className("TestEntity")
                .packageName("com.example")
                .fields(List.of())
                .superClass("BaseEntity")
                .build();

        assertEquals("BaseEntity", context.getSuperClass());
    }

    @Test
    @DisplayName("应该正确设置和获取 interfaces")
    void shouldSetAndGetInterfaces() {
        GeneratorContext context = GeneratorContext.builder()
                .className("TestEntity")
                .packageName("com.example")
                .fields(List.of())
                .interfaces(List.of("Serializable", "Cloneable"))
                .build();

        assertNotNull(context.getInterfaces());
        assertEquals(2, context.getInterfaces().size());
    }

    @Test
    @DisplayName("应该正确设置和获取 annotations")
    void shouldSetAndGetAnnotations() {
        GeneratorContext context = GeneratorContext.builder()
                .className("TestEntity")
                .packageName("com.example")
                .fields(List.of())
                .annotations(List.of("@Entity", "@Table(name = \"test\")"))
                .build();

        assertNotNull(context.getAnnotations());
        assertEquals(2, context.getAnnotations().size());
    }

    @Test
    @DisplayName("FieldDefinition 应该正确设置所有属性")
    void fieldDefinitionShouldSetAllProperties() {
        GeneratorContext.FieldDefinition field = GeneratorContext.FieldDefinition.builder()
                .name("username")
                .type("String")
                .comment("用户名")
                .primaryKey(false)
                .required(true)
                .defaultValue("guest")
                .length(50)
                .columnName("user_name")
                .annotations(List.of("@NotBlank"))
                .build();

        assertEquals("username", field.getName());
        assertEquals("String", field.getType());
        assertEquals("用户名", field.getComment());
        assertFalse(field.isPrimaryKey());
        assertTrue(field.isRequired());
        assertEquals("guest", field.getDefaultValue());
        assertEquals(50, field.getLength());
        assertEquals("user_name", field.getColumnName());
        assertNotNull(field.getAnnotations());
    }

    @Test
    @DisplayName("MethodDefinition 应该正确设置所有属性")
    void methodDefinitionShouldSetAllProperties() {
        GeneratorContext.ParameterDefinition param = GeneratorContext.ParameterDefinition.builder()
                .name("id")
                .type("Long")
                .build();

        GeneratorContext.MethodDefinition method = GeneratorContext.MethodDefinition.builder()
                .name("findById")
                .returnType("Optional<User>")
                .parameters(List.of(param))
                .body("return repository.findById(id);")
                .comment("根据ID查找用户")
                .annotations(List.of("@Override"))
                .build();

        assertEquals("findById", method.getName());
        assertEquals("Optional<User>", method.getReturnType());
        assertNotNull(method.getParameters());
        assertEquals(1, method.getParameters().size());
        assertEquals("return repository.findById(id);", method.getBody());
        assertEquals("根据ID查找用户", method.getComment());
        assertNotNull(method.getAnnotations());
    }

    @Test
    @DisplayName("ParameterDefinition 应该正确设置属性")
    void parameterDefinitionShouldSetProperties() {
        GeneratorContext.ParameterDefinition param = GeneratorContext.ParameterDefinition.builder()
                .name("name")
                .type("String")
                .build();

        assertEquals("name", param.getName());
        assertEquals("String", param.getType());
    }
}
