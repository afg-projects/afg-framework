package io.github.afgprojects.framework.core.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import io.github.afgprojects.framework.core.config.AfgConfigRegistry;
import io.github.afgprojects.framework.core.config.ConfigRefresher;
import io.github.afgprojects.framework.core.module.AfgModule;
import io.github.afgprojects.framework.core.module.ModuleContext;
import io.github.afgprojects.framework.core.module.ModuleDefinition;
import io.github.afgprojects.framework.core.module.ModuleRegistry;
import io.github.afgprojects.framework.core.support.TestApplication;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * AfgAutoConfiguration 集成测试。
 * 测试 AFG 核心自动配置在 Spring Boot 环境中的实际装配效果。
 *
 * @see AfgAutoConfiguration
 * @see ModuleRegistry
 * @see AfgConfigRegistry
 */
@DisplayName("AfgAutoConfiguration 集成测试")
@SpringBootTest(
        classes = TestApplication.class,
        properties = {
                "spring.application.name=test-app"
        }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AfgAutoConfigurationIntegrationTest {

    @Autowired(required = false)
    private ModuleRegistry moduleRegistry;

    @Autowired(required = false)
    private AfgConfigRegistry afgConfigRegistry;

    @Autowired(required = false)
    private ConfigRefresher configRefresher;

    @Autowired(required = false)
    private ModuleContext moduleContext;

    @Autowired(required = false)
    private ObjectMapper objectMapper;

    /**
     * 自动配置测试。
     * 验证核心组件的自动装配。
     */
    @Nested
    @DisplayName("自动配置测试")
    class AutoConfigurationTests {

        /**
         * 测试自动配置 ModuleRegistry。
         */
        @Test
        @DisplayName("应该自动配置 ModuleRegistry")
        void shouldAutoConfigureModuleRegistry() {
            assertThat(moduleRegistry).isNotNull();
        }

        /**
         * 测试自动配置 AfgConfigRegistry。
         */
        @Test
        @DisplayName("应该自动配置 AfgConfigRegistry")
        void shouldAutoConfigureAfgConfigRegistry() {
            assertThat(afgConfigRegistry).isNotNull();
        }

        /**
         * 测试自动配置 ConfigRefresher。
         */
        @Test
        @DisplayName("应该自动配置 ConfigRefresher")
        void shouldAutoConfigureConfigRefresher() {
            assertThat(configRefresher).isNotNull();
        }

        /**
         * 测试自动配置 ModuleContext。
         */
        @Test
        @DisplayName("应该自动配置 ModuleContext")
        void shouldAutoConfigureModuleContext() {
            assertThat(moduleContext).isNotNull();
        }

        /**
         * 测试自动配置 ObjectMapper。
         */
        @Test
        @DisplayName("应该自动配置 ObjectMapper")
        void shouldAutoConfigureObjectMapper() {
            assertThat(objectMapper).isNotNull();
        }
    }

    /**
     * ModuleRegistry 功能测试。
     * 验证模块注册表的基本功能。
     */
    @Nested
    @DisplayName("ModuleRegistry 功能测试")
    class ModuleRegistryTests {

        /**
         * 测试注册模块。
         */
        @Test
        @DisplayName("应该能够注册模块")
        void shouldRegisterModule() {
            var definition = ModuleDefinition.builder()
                    .id("test-module")
                    .name("Test Module")
                    .dependencies(List.of())
                    .build();

            moduleRegistry.registerWithoutDependencyCheck(definition);

            assertThat(moduleRegistry.containsModule("test-module")).isTrue();
        }

        /**
         * 测试获取模块信息。
         */
        @Test
        @DisplayName("应该能够获取模块信息")
        void shouldGetModuleInfo() {
            var definition = ModuleDefinition.builder()
                    .id("info-module")
                    .name("Info Module")
                    .dependencies(List.of())
                    .build();

            moduleRegistry.registerWithoutDependencyCheck(definition);

            var info = moduleRegistry.getModule("info-module");

            assertThat(info).isNotNull();
            assertThat(info.name()).isEqualTo("Info Module");
        }

        /**
         * 测试获取所有模块。
         */
        @Test
        @DisplayName("应该能够获取所有模块")
        void shouldGetAllModules() {
            var defA = ModuleDefinition.builder()
                    .id("module-a")
                    .name("Module A")
                    .dependencies(List.of())
                    .build();
            var defB = ModuleDefinition.builder()
                    .id("module-b")
                    .name("Module B")
                    .dependencies(List.of())
                    .build();

            moduleRegistry.registerWithoutDependencyCheck(defA);
            moduleRegistry.registerWithoutDependencyCheck(defB);

            var modules = moduleRegistry.getAllModules();

            assertThat(modules).isNotEmpty();
        }
    }

    /**
     * AfgConfigRegistry 功能测试。
     * 验证配置注册表的基本功能。
     */
    @Nested
    @DisplayName("AfgConfigRegistry 功能测试")
    class AfgConfigRegistryTests {

        /**
         * 测试注册配置。
         */
        @Test
        @DisplayName("应该能够注册配置")
        void shouldRegisterConfig() {
            afgConfigRegistry.register("test.config.key", "test-value");

            assertThat(afgConfigRegistry.contains("test.config.key")).isTrue();
            assertThat(afgConfigRegistry.getConfig("test.config.key")).isEqualTo("test-value");
        }

        /**
         * 测试更新配置。
         */
        @Test
        @DisplayName("应该能够更新配置")
        void shouldUpdateConfig() {
            afgConfigRegistry.register("update.config.key", "initial-value");
            afgConfigRegistry.updateConfig("update.config.key", "updated-value");

            assertThat(afgConfigRegistry.getConfig("update.config.key")).isEqualTo("updated-value");
        }

        /**
         * 测试删除配置。
         */
        @Test
        @DisplayName("应该能够删除配置")
        void shouldRemoveConfig() {
            afgConfigRegistry.register("remove.config.key", "value");
            afgConfigRegistry.unregister("remove.config.key");

            assertThat(afgConfigRegistry.contains("remove.config.key")).isFalse();
        }
    }

    /**
     * ObjectMapper 功能测试。
     * 验证 JSON 序列化和反序列化功能。
     */
    @Nested
    @DisplayName("ObjectMapper 功能测试")
    class ObjectMapperTests {

        /**
         * 测试序列化对象。
         */
        @Test
        @DisplayName("应该能够序列化对象")
        void shouldSerializeObject() throws Exception {
            var obj = new TestRecord("test", 123);

            String json = objectMapper.writeValueAsString(obj);

            assertThat(json).contains("test");
            assertThat(json).contains("123");
        }

        /**
         * 测试反序列化对象。
         */
        @Test
        @DisplayName("应该能够反序列化对象")
        void shouldDeserializeObject() throws Exception {
            String json = "{\"name\":\"test\",\"value\":123}";

            TestRecord obj = objectMapper.readValue(json, TestRecord.class);

            assertThat(obj.name()).isEqualTo("test");
            assertThat(obj.value()).isEqualTo(123);
        }
    }

    record TestRecord(String name, int value) {}
}
