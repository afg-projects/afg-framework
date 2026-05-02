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
 * AfgAutoConfiguration 集成测试
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

    @Nested
    @DisplayName("自动配置测试")
    class AutoConfigurationTests {

        @Test
        @DisplayName("应该自动配置 ModuleRegistry")
        void shouldAutoConfigureModuleRegistry() {
            assertThat(moduleRegistry).isNotNull();
        }

        @Test
        @DisplayName("应该自动配置 AfgConfigRegistry")
        void shouldAutoConfigureAfgConfigRegistry() {
            assertThat(afgConfigRegistry).isNotNull();
        }

        @Test
        @DisplayName("应该自动配置 ConfigRefresher")
        void shouldAutoConfigureConfigRefresher() {
            assertThat(configRefresher).isNotNull();
        }

        @Test
        @DisplayName("应该自动配置 ModuleContext")
        void shouldAutoConfigureModuleContext() {
            assertThat(moduleContext).isNotNull();
        }

        @Test
        @DisplayName("应该自动配置 ObjectMapper")
        void shouldAutoConfigureObjectMapper() {
            assertThat(objectMapper).isNotNull();
        }
    }

    @Nested
    @DisplayName("ModuleRegistry 功能测试")
    class ModuleRegistryTests {

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

    @Nested
    @DisplayName("AfgConfigRegistry 功能测试")
    class AfgConfigRegistryTests {

        @Test
        @DisplayName("应该能够注册配置")
        void shouldRegisterConfig() {
            afgConfigRegistry.register("test.config.key", "test-value");

            assertThat(afgConfigRegistry.contains("test.config.key")).isTrue();
            assertThat(afgConfigRegistry.getConfig("test.config.key")).isEqualTo("test-value");
        }

        @Test
        @DisplayName("应该能够更新配置")
        void shouldUpdateConfig() {
            afgConfigRegistry.register("update.config.key", "initial-value");
            afgConfigRegistry.updateConfig("update.config.key", "updated-value");

            assertThat(afgConfigRegistry.getConfig("update.config.key")).isEqualTo("updated-value");
        }

        @Test
        @DisplayName("应该能够删除配置")
        void shouldRemoveConfig() {
            afgConfigRegistry.register("remove.config.key", "value");
            afgConfigRegistry.unregister("remove.config.key");

            assertThat(afgConfigRegistry.contains("remove.config.key")).isFalse();
        }
    }

    @Nested
    @DisplayName("ObjectMapper 功能测试")
    class ObjectMapperTests {

        @Test
        @DisplayName("应该能够序列化对象")
        void shouldSerializeObject() throws Exception {
            var obj = new TestRecord("test", 123);

            String json = objectMapper.writeValueAsString(obj);

            assertThat(json).contains("test");
            assertThat(json).contains("123");
        }

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
