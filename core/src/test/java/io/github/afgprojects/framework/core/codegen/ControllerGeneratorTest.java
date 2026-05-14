package io.github.afgprojects.framework.core.codegen;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * {@link ControllerGenerator} 的单元测试。
 * <p>
 * 测试 Controller 代码生成器的基本信息、代码生成功能，包括 REST 注解、CRUD 方法等。
 *
 * @see ControllerGenerator
 */
@DisplayName("ControllerGenerator 测试")
class ControllerGeneratorTest {

    private ControllerGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new ControllerGenerator();
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
            assertThat(generator.getName()).isEqualTo("ControllerGenerator");
        }

        /**
         * 测试生成器返回正确的模板类型。
         */
        @Test
        @DisplayName("应该返回正确的模板类型")
        void shouldReturnCorrectTemplateType() {
            assertThat(generator.getTemplateType()).isEqualTo("controller");
        }
    }

    /**
     * 代码生成相关测试。
     * <p>
     * 验证 Controller 代码生成的各项功能。
     */
    @Nested
    @DisplayName("代码生成测试")
    class GenerateTests {

        /**
         * 测试生成基本的 Controller 类结构。
         */
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

        /**
         * 测试生成 REST 相关注解（@RestController、@RequestMapping、@Tag）。
         */
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

        /**
         * 测试生成 CRUD 方法（增删改查）。
         */
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

        /**
         * 测试根据表名生成请求路径。
         */
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

        /**
         * 测试生成 Service 层的依赖注入。
         */
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
