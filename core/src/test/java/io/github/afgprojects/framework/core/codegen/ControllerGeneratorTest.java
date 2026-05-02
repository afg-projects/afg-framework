package io.github.afgprojects.framework.core.codegen;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * ControllerGenerator 测试
 */
@DisplayName("ControllerGenerator 测试")
class ControllerGeneratorTest {

    private ControllerGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new ControllerGenerator();
    }

    @Nested
    @DisplayName("基本信息测试")
    class BasicInfoTests {

        @Test
        @DisplayName("应该返回正确的名称")
        void shouldReturnCorrectName() {
            assertThat(generator.getName()).isEqualTo("ControllerGenerator");
        }

        @Test
        @DisplayName("应该返回正确的模板类型")
        void shouldReturnCorrectTemplateType() {
            assertThat(generator.getTemplateType()).isEqualTo("controller");
        }
    }

    @Nested
    @DisplayName("代码生成测试")
    class GenerateTests {

        @Test
        @DisplayName("应该生成基本的 Controller 类")
        void shouldGenerateBasicControllerClass() {
            GeneratorContext context = GeneratorContext.builder()
                    .className("User")
                    .packageName("com.example.controller")
                    .build();

            String code = generator.generate(context);

            assertThat(code).contains("package com.example.controller;");
            assertThat(code).contains("public class UserController {");
            assertThat(code).contains("}");
        }

        @Test
        @DisplayName("应该生成 REST 注解")
        void shouldGenerateRestAnnotations() {
            GeneratorContext context = GeneratorContext.builder()
                    .className("User")
                    .packageName("com.example.controller")
                    .build();

            String code = generator.generate(context);

            assertThat(code).contains("@RestController");
            assertThat(code).contains("@RequestMapping");
            assertThat(code).contains("@Tag");
        }

        @Test
        @DisplayName("应该生成 CRUD 方法")
        void shouldGenerateCrudMethods() {
            GeneratorContext context = GeneratorContext.builder()
                    .className("User")
                    .packageName("com.example.controller")
                    .build();

            String code = generator.generate(context);

            assertThat(code).contains("@PostMapping");
            assertThat(code).contains("@GetMapping");
            assertThat(code).contains("@PutMapping");
            assertThat(code).contains("@DeleteMapping");
        }

        @Test
        @DisplayName("应该生成带表名的路径")
        void shouldGeneratePathWithTableName() {
            GeneratorContext context = GeneratorContext.builder()
                    .className("User")
                    .packageName("com.example.controller")
                    .tableName("t_user")
                    .build();

            String code = generator.generate(context);

            assertThat(code).contains("@RequestMapping(\"/user\")");
        }

        @Test
        @DisplayName("应该生成 Service 注入")
        void shouldGenerateServiceInjection() {
            GeneratorContext context = GeneratorContext.builder()
                    .className("User")
                    .packageName("com.example.controller")
                    .build();

            String code = generator.generate(context);

            assertThat(code).contains("UserService userService");
            assertThat(code).contains("UserController(UserService userService)");
        }
    }
}
