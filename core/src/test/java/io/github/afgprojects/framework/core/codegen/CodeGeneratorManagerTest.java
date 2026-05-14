package io.github.afgprojects.framework.core.codegen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * {@link CodeGeneratorManager} 的单元测试。
 * <p>
 * 测试代码生成器管理器的单例模式、生成器注册、代码生成、批量生成等功能。
 *
 * @see CodeGeneratorManager
 */
@DisplayName("CodeGeneratorManager 测试")
class CodeGeneratorManagerTest {

    private CodeGeneratorManager manager;

    @BeforeEach
    void setUp() {
        manager = CodeGeneratorManager.getInstance();
    }

    /**
     * 单例模式相关测试。
     * <p>
     * 验证 CodeGeneratorManager 的单例实现是否正确。
     */
    @Nested
    @DisplayName("单例模式测试")
    class SingletonTests {

        /**
         * 测试多次调用 getInstance 应返回同一个实例。
         */
        @Test
        @DisplayName("应该返回同一个实例")
        void shouldReturnSameInstance() {
            CodeGeneratorManager instance1 = CodeGeneratorManager.getInstance();
            CodeGeneratorManager instance2 = CodeGeneratorManager.getInstance();

            assertThat(instance1).isSameAs(instance2);
        }
    }

    /**
     * 生成器注册相关测试。
     * <p>
     * 验证自定义生成器的注册和获取功能。
     */
    @Nested
    @DisplayName("生成器注册测试")
    class RegisterTests {

        /**
         * 测试注册自定义生成器后可以正常获取。
         */
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

    /**
     * 代码生成相关测试。
     * <p>
     * 验证使用管理器生成各类代码的功能。
     */
    @Nested
    @DisplayName("代码生成测试")
    class GenerateTests {

        /**
         * 测试生成 Entity 代码。
         */
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

        /**
         * 测试生成 DTO 代码。
         */
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

        /**
         * 测试对不支持的模板类型抛出异常。
         */
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

    /**
     * 生成器获取相关测试。
     * <p>
     * 验证根据模板类型获取已注册生成器的功能。
     */
    @Nested
    @DisplayName("生成器获取测试")
    class GetGeneratorTests {

        /**
         * 测试获取已注册的生成器。
         */
        @Test
        @DisplayName("应该获取已注册的生成器")
        void shouldGetRegisteredGenerator() {
            CodeGenerator generator = manager.getGenerator("entity");

            assertThat(generator).isNotNull();
            assertThat(generator.getName()).isEqualTo("EntityGenerator");
        }

        /**
         * 测试对未注册的类型返回 null。
         */
        @Test
        @DisplayName("应该对未注册的类型返回 null")
        void shouldReturnNullForUnregisteredType() {
            CodeGenerator generator = manager.getGenerator("nonexistent");

            assertThat(generator).isNull();
        }
    }

    /**
     * 支持的模板类型相关测试。
     * <p>
     * 验证获取所有支持的模板类型列表。
     */
    @Nested
    @DisplayName("支持的模板类型测试")
    class SupportedTemplateTypesTests {

        /**
         * 测试返回所有支持的模板类型。
         */
        @Test
        @DisplayName("应该返回所有支持的模板类型")
        void shouldReturnSupportedTemplateTypes() {
            List<String> types = manager.getSupportedTemplateTypes();

            assertThat(types).contains("entity", "dto", "controller", "service");
        }
    }

    /**
     * 批量生成相关测试。
     * <p>
     * 验证批量生成多种类型代码的功能。
     */
    @Nested
    @DisplayName("批量生成测试")
    class GenerateAllTests {

        /**
         * 测试批量生成代码。
         */
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

        /**
         * 测试批量生成时跳过不支持的类型。
         */
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