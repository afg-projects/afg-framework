package io.github.afgprojects.framework.core.codegen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * CodeGeneratorManager 测试
 */
@DisplayName("CodeGeneratorManager 测试")
class CodeGeneratorManagerTest {

    private CodeGeneratorManager manager;

    @BeforeEach
    void setUp() {
        manager = CodeGeneratorManager.getInstance();
    }

    @Nested
    @DisplayName("单例模式测试")
    class SingletonTests {

        @Test
        @DisplayName("应该返回同一个实例")
        void shouldReturnSameInstance() {
            CodeGeneratorManager instance1 = CodeGeneratorManager.getInstance();
            CodeGeneratorManager instance2 = CodeGeneratorManager.getInstance();

            assertThat(instance1).isSameAs(instance2);
        }
    }

    @Nested
    @DisplayName("生成器注册测试")
    class RegisterTests {

        @Test
        @DisplayName("应该注册自定义生成器")
        void shouldRegisterCustomGenerator() {
            CodeGenerator customGenerator = new CodeGenerator() {
                @Override
                public String generate(GeneratorContext context) {
                    return "custom code";
                }

                @Override
                public String getName() {
                    return "CustomGenerator";
                }

                @Override
                public String getTemplateType() {
                    return "custom";
                }
            };

            manager.register(customGenerator);

            assertThat(manager.getGenerator("custom")).isSameAs(customGenerator);
        }
    }

    @Nested
    @DisplayName("代码生成测试")
    class GenerateTests {

        @Test
        @DisplayName("应该生成 Entity 代码")
        void shouldGenerateEntityCode() {
            GeneratorContext context = GeneratorContext.builder()
                    .className("User")
                    .packageName("com.example.entity")
                    .build();

            String code = manager.generate("entity", context);

            assertThat(code).contains("package com.example.entity");
            assertThat(code).contains("public class User");
        }

        @Test
        @DisplayName("应该生成 DTO 代码")
        void shouldGenerateDtoCode() {
            GeneratorContext context = GeneratorContext.builder()
                    .className("UserDTO")
                    .packageName("com.example.dto")
                    .build();

            String code = manager.generate("dto", context);

            assertThat(code).contains("package com.example.dto");
        }

        @Test
        @DisplayName("应该对不支持的模板类型抛出异常")
        void shouldThrowForUnsupportedTemplateType() {
            GeneratorContext context = GeneratorContext.builder()
                    .className("Test")
                    .packageName("com.example")
                    .build();

            assertThatThrownBy(() -> manager.generate("unsupported", context))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unsupported template type");
        }
    }

    @Nested
    @DisplayName("生成器获取测试")
    class GetGeneratorTests {

        @Test
        @DisplayName("应该获取已注册的生成器")
        void shouldGetRegisteredGenerator() {
            CodeGenerator generator = manager.getGenerator("entity");

            assertThat(generator).isNotNull();
            assertThat(generator.getName()).isEqualTo("EntityGenerator");
        }

        @Test
        @DisplayName("应该对未注册的类型返回 null")
        void shouldReturnNullForUnregisteredType() {
            CodeGenerator generator = manager.getGenerator("nonexistent");

            assertThat(generator).isNull();
        }
    }

    @Nested
    @DisplayName("支持的模板类型测试")
    class SupportedTemplateTypesTests {

        @Test
        @DisplayName("应该返回所有支持的模板类型")
        void shouldReturnSupportedTemplateTypes() {
            List<String> types = manager.getSupportedTemplateTypes();

            assertThat(types).contains("entity", "dto", "controller", "service");
        }
    }

    @Nested
    @DisplayName("批量生成测试")
    class GenerateAllTests {

        @Test
        @DisplayName("应该批量生成代码")
        void shouldGenerateAll() {
            GeneratorContext context = GeneratorContext.builder()
                    .className("User")
                    .packageName("com.example")
                    .build();

            var result = manager.generateAll(List.of("entity", "dto"), context);

            assertThat(result).containsKeys("entity", "dto");
            assertThat(result.get("entity")).contains("public class User");
        }

        @Test
        @DisplayName("应该跳过不支持的类型")
        void shouldSkipUnsupportedTypes() {
            GeneratorContext context = GeneratorContext.builder()
                    .className("User")
                    .packageName("com.example")
                    .build();

            var result = manager.generateAll(List.of("entity", "invalid", "dto"), context);

            assertThat(result).containsKeys("entity", "dto");
            assertThat(result).doesNotContainKey("invalid");
        }
    }
}