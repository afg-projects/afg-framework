package io.github.afgprojects.framework.core.codegen;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * {@link ServiceGenerator} 的单元测试。
 * <p>
 * 测试 Service 代码生成器的基本信息和代码生成功能，包括 Service 注解、CRUD 方法等。
 *
 * @see ServiceGenerator
 */
@DisplayName("ServiceGenerator 测试")
class ServiceGeneratorTest {

    private ServiceGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new ServiceGenerator();
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
            assertThat(generator.getName()).isEqualTo("ServiceGenerator");
        }

        /**
         * 测试生成器返回正确的模板类型。
         */
        @Test
        @DisplayName("应该返回正确的模板类型")
        void shouldReturnCorrectTemplateType() {
            assertThat(generator.getTemplateType()).isEqualTo("service");
        }
    }

    /**
     * 代码生成相关测试。
     * <p>
     * 验证 Service 代码生成的各项功能。
     */
    @Nested
    @DisplayName("代码生成测试")
    class GenerateTests {

        /**
         * 测试生成基本的 Service 类结构。
         */
        @Test
        @DisplayName("应该生成基本的 Service 类")
        void shouldGenerateBasicServiceClass() {
            GeneratorContext context = GeneratorContext.builder()
                    .className("User")
                    .packageName("com.example.service")
                    .build();

            String code = generator.generate(context);

            assertThat(code).contains("package com.example.service;");
            assertThat(code).contains("public class UserService {");
            assertThat(code).contains("}");
        }

        /**
         * 测试生成 Service 相关注解（@Service、@RequiredArgsConstructor）。
         */
        @Test
        @DisplayName("应该生成 Service 注解")
        void shouldGenerateServiceAnnotations() {
            GeneratorContext context = GeneratorContext.builder()
                    .className("User")
                    .packageName("com.example.service")
                    .build();

            String code = generator.generate(context);

            assertThat(code).contains("@Service");
            assertThat(code).contains("@RequiredArgsConstructor");
        }

        /**
         * 测试生成 CRUD 方法（增删改查）。
         */
        @Test
        @DisplayName("应该生成 CRUD 方法")
        void shouldGenerateCrudMethods() {
            GeneratorContext context = GeneratorContext.builder()
                    .className("User")
                    .packageName("com.example.service")
                    .build();

            String code = generator.generate(context);

            assertThat(code).contains("public User create(");
            assertThat(code).contains("public User getById(");
            assertThat(code).contains("public List<User> list()");
            assertThat(code).contains("public User update(");
            assertThat(code).contains("public void delete(");
        }

        /**
         * 测试生成 Mapper 层的依赖注入。
         */
        @Test
        @DisplayName("应该生成 Mapper 注入")
        void shouldGenerateMapperInjection() {
            GeneratorContext context = GeneratorContext.builder()
                    .className("User")
                    .packageName("com.example.service")
                    .build();

            String code = generator.generate(context);

            assertThat(code).contains("UserMapper userMapper");
        }
    }
}
