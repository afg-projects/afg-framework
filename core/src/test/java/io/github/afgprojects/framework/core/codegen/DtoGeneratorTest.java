package io.github.afgprojects.framework.core.codegen;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * DtoGenerator 测试
 */
@DisplayName("DtoGenerator 测试")
class DtoGeneratorTest {

    private DtoGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new DtoGenerator();
    }

    @Nested
    @DisplayName("基本信息测试")
    class BasicInfoTests {

        @Test
        @DisplayName("应该返回正确的名称")
        void shouldReturnCorrectName() {
            assertThat(generator.getName()).isEqualTo("DtoGenerator");
        }

        @Test
        @DisplayName("应该返回正确的模板类型")
        void shouldReturnCorrectTemplateType() {
            assertThat(generator.getTemplateType()).isEqualTo("dto");
        }
    }

    @Nested
    @DisplayName("代码生成测试")
    class GenerateTests {

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
