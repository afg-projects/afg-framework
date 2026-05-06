package io.github.afgprojects.framework.core.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;

import io.github.afgprojects.framework.core.module.ModuleDefinition;
import io.github.afgprojects.framework.core.support.BaseUnitTest;

/**
 * ModuleConfigLoader 单元测试
 */
@DisplayName("ModuleConfigLoader 测试")
class ModuleConfigLoaderTest extends BaseUnitTest {

    private ConfigurableEnvironment environment;
    private MutablePropertySources propertySources;
    private ModuleConfigLoader loader;

    @BeforeEach
    void setUp() {
        environment = mock(ConfigurableEnvironment.class);
        propertySources = new MutablePropertySources();
        when(environment.getPropertySources()).thenReturn(propertySources);
        loader = new ModuleConfigLoader(environment);
    }

    @Nested
    @DisplayName("配置加载测试")
    class ConfigLoadingTest {

        @Test
        @DisplayName("配置文件不存在时不应抛出异常")
        void shouldNotThrowWhenConfigFileNotExists() {
            // given
            ModuleDefinition module = ModuleDefinition.builder()
                    .id("nonexistent")
                    .name("Nonexistent Module")
                    .build();

            // when & then
            assertDoesNotThrow(() -> loader.loadAndRegisterModuleConfig(module));
        }

        @Test
        @DisplayName("空模块ID应正常处理")
        void shouldHandleEmptyModuleId() {
            // given
            ModuleDefinition module = ModuleDefinition.builder()
                    .id("")
                    .name("Empty Id Module")
                    .build();

            // when & then
            assertDoesNotThrow(() -> loader.loadAndRegisterModuleConfig(module));
        }

        @Test
        @DisplayName("自定义配置文件名应正常处理")
        void shouldHandleCustomConfigFileName() {
            // given
            ModuleDefinition module = ModuleDefinition.builder()
                    .id("auth")
                    .name("Auth Module")
                    .configFile("custom-auth.yml")
                    .build();

            // when & then
            assertDoesNotThrow(() -> loader.loadAndRegisterModuleConfig(module));
        }
    }

    @Nested
    @DisplayName("Environment 集成测试")
    class EnvironmentIntegrationTest {

        @Test
        @DisplayName("Environment 为 null 时应抛出异常")
        void shouldThrowWhenEnvironmentIsNull() {
            // given
            ModuleConfigLoader nullEnvLoader = new ModuleConfigLoader(null);

            ModuleDefinition module = ModuleDefinition.builder()
                    .id("test")
                    .name("Test Module")
                    .build();

            // when & then
            assertThrows(NullPointerException.class, () -> nullEnvLoader.loadAndRegisterModuleConfig(module));
        }

        @Test
        @DisplayName("模块配置应正确添加到 Environment")
        void shouldAddModuleConfigToEnvironment() {
            // given
            ModuleDefinition module = ModuleDefinition.builder()
                    .id("test")
                    .name("Test Module")
                    .build();

            // when
            loader.loadAndRegisterModuleConfig(module);

            // then - 验证 environment 被访问
            verify(environment, atLeastOnce()).getPropertySources();
        }
    }
}
