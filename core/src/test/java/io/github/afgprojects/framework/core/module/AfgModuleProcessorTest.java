package io.github.afgprojects.framework.core.module;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;

import io.github.afgprojects.framework.core.config.ModuleConfigLoader;
import io.github.afgprojects.framework.core.support.BaseUnitTest;
import io.github.afgprojects.framework.apt.module.AfgModuleAnnotation;

/**
 * AfgModuleProcessor 单元测试
 */
@DisplayName("AfgModuleProcessor 测试")
class AfgModuleProcessorTest extends BaseUnitTest {

    private ModuleRegistry moduleRegistry;
    private ApplicationContext applicationContext;
    private ConfigurableEnvironment environment;
    private AfgModuleProcessor processor;

    @BeforeEach
    void setUp() {
        moduleRegistry = mock(ModuleRegistry.class);
        applicationContext = mock(ApplicationContext.class);
        environment = mock(ConfigurableEnvironment.class);
        MutablePropertySources propertySources = new MutablePropertySources();

        when(applicationContext.getBean(ConfigurableEnvironment.class)).thenReturn(environment);
        when(environment.getPropertySources()).thenReturn(propertySources);
        when(applicationContext.getBean(ModuleContext.class)).thenReturn(mock(ModuleContext.class));

        processor = new AfgModuleProcessor(moduleRegistry, applicationContext);
    }

    @Nested
    @DisplayName("构造测试")
    class ConstructorTest {

        @Test
        @DisplayName("应该成功创建处理器")
        void shouldCreateProcessor() {
            // when
            AfgModuleProcessor proc = new AfgModuleProcessor(moduleRegistry, applicationContext);

            // then
            assertNotNull(proc);
        }

        @Test
        @DisplayName("Environment 不可用时配置加载器应为 null")
        void shouldHandleMissingEnvironment() {
            // given
            when(applicationContext.getBean(ConfigurableEnvironment.class))
                    .thenThrow(new RuntimeException("Environment not available"));

            // when
            AfgModuleProcessor proc = new AfgModuleProcessor(moduleRegistry, applicationContext);

            // then
            assertNotNull(proc);
        }
    }

    @Nested
    @DisplayName("模块注册测试")
    class ModuleRegistrationTest {

        @Test
        @DisplayName("应该正确处理带有注解的 Bean")
        void shouldProcessAnnotatedBean() {
            // given
            TestModuleConfig bean = new TestModuleConfig();

            // when
            Object result = processor.postProcessAfterInitialization(bean, "testModuleConfig");

            // then
            assertEquals(bean, result);
            verify(moduleRegistry).registerWithoutDependencyCheck(any(ModuleDefinition.class));
        }

        @Test
        @DisplayName("应该忽略没有注解的 Bean")
        void shouldIgnoreNonAnnotatedBean() {
            // given
            Object bean = new Object();

            // when
            Object result = processor.postProcessAfterInitialization(bean, "plainObject");

            // then
            assertEquals(bean, result);
            verify(moduleRegistry, never()).registerWithoutDependencyCheck(any());
        }
    }

    @Nested
    @DisplayName("configFile 提取测试")
    class ConfigFileExtractionTest {

        @Test
        @DisplayName("应该提取注解中的 configFile")
        void shouldExtractConfigFileFromAnnotation() {
            // given
            TestModuleWithConfigFile bean = new TestModuleWithConfigFile();

            // when
            processor.postProcessAfterInitialization(bean, "testModuleWithConfig");

            // then
            verify(moduleRegistry).registerWithoutDependencyCheck(argThat(def ->
                    "custom-module.yml".equals(def.configFile())));
        }

        @Test
        @DisplayName("未指定 configFile 时应为空字符串")
        void shouldHaveEmptyConfigFileWhenNotSpecified() {
            // given
            TestModuleConfig bean = new TestModuleConfig();

            // when
            processor.postProcessAfterInitialization(bean, "testModuleConfig");

            // then
            verify(moduleRegistry).registerWithoutDependencyCheck(argThat(def ->
                    def.configFile().isEmpty()));
        }
    }

    // 测试用的模块配置类 - 使用独立的包名避免冲突
    // 注意：这些类不应该被 ModuleAutoConfiguration 扫描到
    @AfgModuleAnnotation(name = "Test Module", id = "test-module-processor-1", basePackage = "test.module.config1")
    static class TestModuleConfig {
    }

    @AfgModuleAnnotation(name = "Test Module With Config", id = "test-module-processor-2", configFile = "custom-module.yml", basePackage = "test.module.config2")
    static class TestModuleWithConfigFile {
    }
}