package io.github.afgprojects.framework.core.codegen;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * {@link DtoGenerator} 的单元测试。
 * <p>
 * 测试 DTO 代码生成器的基本信息和代码生成功能。
 *
 * @see DtoGenerator
 */
@DisplayName("DtoGenerator 测试")
class DtoGeneratorTest {

    private DtoGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new DtoGenerator();
    }

    /**
     * 生成器基本信息测试。
     * <p>
     * 验证生成器的名称和模板类型是否正确。
     */
    @Nested
    @DisplayName("基本信息测试")
    class BasicInfoTests {

        /**
         * 测试生成器返回正确的名称。
         */
        @Test
        @DisplayName("应该返回正确的名称")
        void shouldReturnCorrectName() {
            assertThat(generator.getName()).isEqualTo("DtoGenerator");
        }

        /**
         * 测试生成器返回正确的模板类型。
         */
        @Test
        @DisplayName("应该返回正确的模板类型")
        void shouldReturnCorrectTemplateType() {
            assertThat(generator.getTemplateType()).isEqualTo("dto");
        }
    }

    /**
     * 代码生成相关测试。
     * <p>
     * 验证 DTO 代码生成的各项功能。
     */
    @Nested
    @DisplayName("代码生成测试")
    class GenerateTests {

        /**
         * 测试生成基本的 DTO 类结构（使用 record 类型）。
         */
        @Test
        @DisplayName("应该生成基本的 DTO 类")
        void shouldGenerateBasicDtoClass() {
            GeneratorContext context = GeneratorContext.builder()
                    .className("User")
                    .packageName("com.example.dto")
                    .build();

            String code = generator.generate(context);

            assertThat(code).contains("package com.example.dto;");
            assertThat(code).contains("public record User(");
            assertThat(code).contains("}");
        }

        /**
         * 测试生成 Swagger Schema 注解。
         */
        @Test
        @DisplayName("应该生成 Schema 注解")
        void shouldGenerateSchemaAnnotations() {
            GeneratorContext context = GeneratorContext.builder()
                    .className("User")
                    .packageName("com.example.dto")
                    .build();

            String code = generator.generate(context);

            assertThat(code).contains("import io.swagger.v3.oas.annotations.media.Schema");
        }
    }
}
