package io.github.afgprojects.framework.core.codegen;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * ServiceGenerator 测试
 */
@DisplayName("ServiceGenerator 测试")
class ServiceGeneratorTest {

    private ServiceGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new ServiceGenerator();
    }

    @Nested
    @DisplayName("基本信息测试")
    class BasicInfoTests {

        @Test
        @DisplayName("应该返回正确的名称")
        void shouldReturnCorrectName() {
            assertThat(generator.getName()).isEqualTo("ServiceGenerator");
        }

        @Test
        @DisplayName("应该返回正确的模板类型")
        void shouldReturnCorrectTemplateType() {
            assertThat(generator.getTemplateType()).isEqualTo("service");
        }
    }

    @Nested
    @DisplayName("代码生成测试")
    class GenerateTests {

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
