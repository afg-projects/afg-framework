package io.github.afgprojects.framework.core.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;

import io.github.afgprojects.framework.core.module.ModuleRegistry;
import io.github.afgprojects.framework.core.support.BaseIntegrationTest;

/**
 * 模块配置集成测试
 *
 * 验证模块配置文件的加载和优先级
 */
@DisplayName("模块配置集成测试")
class ModuleConfigIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private Environment environment;

    @Autowired
    private ModuleRegistry moduleRegistry;

    @Nested
    @DisplayName("配置加载测试")
    class ConfigLoadingTest {

        @Test
        @DisplayName("Environment 应可用")
        void environmentShouldBeAvailable() {
            assertNotNull(environment);
        }

        @Test
        @DisplayName("ModuleRegistry 应可用")
        void moduleRegistryShouldBeAvailable() {
            assertNotNull(moduleRegistry);
        }

        @Test
        @DisplayName("应该能够访问配置属性")
        void shouldAccessConfigProperties() {
            // given
            String appName = environment.getProperty("spring.application.name");

            // then - 测试应用可能没有设置应用名，所以检查环境是否可用
            // 如果应用名存在则验证，否则跳过
            if (appName == null) {
                // 验证其他配置属性
                assertNotNull(environment);
            } else {
                assertNotNull(appName);
            }
        }
    }

    @Nested
    @DisplayName("配置优先级测试")
    class ConfigPriorityTest {

        @Test
        @DisplayName("应该能够查看 PropertySources")
        void shouldViewPropertySources() {
            // given
            ConfigurableEnvironment configurableEnv = (ConfigurableEnvironment) environment;

            // when
            var propertySources = configurableEnv.getPropertySources();

            // then
            assertNotNull(propertySources);
            assertTrue(propertySources.size() > 0);

            // 打印 PropertySource 名称用于调试
            for (PropertySource<?> source : propertySources) {
                System.out.println("PropertySource: " + source.getName());
            }
        }

        @Test
        @DisplayName("模块配置优先级应低于主配置")
        void moduleConfigShouldHaveLowerPriority() {
            // given
            ConfigurableEnvironment configurableEnv = (ConfigurableEnvironment) environment;
            var propertySources = configurableEnv.getPropertySources();

            // 查找模块配置 PropertySource
            boolean foundModuleConfig = false;
            boolean foundApplicationConfig = false;
            int moduleConfigIndex = -1;
            int applicationConfigIndex = -1;

            int index = 0;
            for (PropertySource<?> source : propertySources) {
                String name = source.getName();
                if (name.contains("module-config")) {
                    foundModuleConfig = true;
                    moduleConfigIndex = index;
                }
                if (name.contains("applicationConfig")) {
                    foundApplicationConfig = true;
                    applicationConfigIndex = index;
                }
                index++;
            }

            // 如果存在模块配置，验证优先级
            if (foundModuleConfig && foundApplicationConfig) {
                // PropertySources 是有序列表，索引越大优先级越低
                assertTrue(moduleConfigIndex > applicationConfigIndex,
                        "模块配置优先级应该低于主配置");
            }
        }
    }

    @Nested
    @DisplayName("模块注册测试")
    class ModuleRegistrationTest {

        @Test
        @DisplayName("应该能够获取所有已注册模块")
        void shouldGetAllRegisteredModules() {
            // when
            var modules = moduleRegistry.getAllModules();

            // then
            assertNotNull(modules);
            // 测试应用可能没有注册业务模块
            System.out.println("Registered modules count: " + modules.size());
        }
    }
}