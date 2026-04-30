package io.github.afgprojects.framework.core.codegen;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CodeGeneratorManager 测试
 */
@DisplayName("CodeGeneratorManager 测试")
class CodeGeneratorManagerTest {

    @Test
    @DisplayName("应该返回单例实例")
    void shouldReturnSingletonInstance() {
        CodeGeneratorManager instance1 = CodeGeneratorManager.getInstance();
        CodeGeneratorManager instance2 = CodeGeneratorManager.getInstance();

        assertSame(instance1, instance2);
    }

    @Test
    @DisplayName("应该包含内置生成器")
    void shouldContainBuiltInGenerators() {
        CodeGeneratorManager manager = CodeGeneratorManager.getInstance();
        List<String> types = manager.getSupportedTemplateTypes();

        assertTrue(types.contains("entity"));
        assertTrue(types.contains("dto"));
        assertTrue(types.contains("controller"));
        assertTrue(types.contains("service"));
    }

    @Test
    @DisplayName("应该生成 Entity 代码")
    void shouldGenerateEntityCode() {
        CodeGeneratorManager manager = CodeGeneratorManager.getInstance();

        GeneratorContext context = GeneratorContext.builder()
                .className("User")
                .packageName("com.example.entity")
                .tableName("t_user")
                .classComment("用户实体")
                .fields(List.of(
                        GeneratorContext.FieldDefinition.builder()
                                .name("id")
                                .type("Long")
                                .primaryKey(true)
                                .build()
                ))
                .build();

        String code = manager.generate("entity", context);

        assertNotNull(code);
        assertTrue(code.contains("package com.example.entity"));
        assertTrue(code.contains("public class User"));
        assertTrue(code.contains("private Long id"));
    }

    @Test
    @DisplayName("应该抛出异常当模板类型不支持")
    void shouldThrowWhenTemplateTypeNotSupported() {
        CodeGeneratorManager manager = CodeGeneratorManager.getInstance();

        GeneratorContext context = GeneratorContext.builder()
                .className("Test")
                .packageName("com.example")
                .fields(List.of())
                .build();

        assertThrows(IllegalArgumentException.class, () ->
            manager.generate("unsupported", context)
        );
    }

    @Test
    @DisplayName("应该获取生成器")
    void shouldGetGenerator() {
        CodeGeneratorManager manager = CodeGeneratorManager.getInstance();

        CodeGenerator generator = manager.getGenerator("entity");
        assertNotNull(generator);
        assertEquals("entity", generator.getTemplateType());
    }

    @Test
    @DisplayName("应该返回 null 当生成器不存在")
    void shouldReturnNullWhenGeneratorNotExists() {
        CodeGeneratorManager manager = CodeGeneratorManager.getInstance();

        CodeGenerator generator = manager.getGenerator("nonexistent");
        assertNull(generator);
    }

    @Test
    @DisplayName("应该批量生成代码")
    void shouldGenerateAll() {
        CodeGeneratorManager manager = CodeGeneratorManager.getInstance();

        GeneratorContext context = GeneratorContext.builder()
                .className("User")
                .packageName("com.example")
                .fields(List.of())
                .build();

        var result = manager.generateAll(List.of("entity", "dto", "nonexistent"), context);

        assertTrue(result.containsKey("entity"));
        assertTrue(result.containsKey("dto"));
        assertFalse(result.containsKey("nonexistent"));
    }
}
