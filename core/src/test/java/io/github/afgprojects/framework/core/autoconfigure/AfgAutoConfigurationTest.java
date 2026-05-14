package io.github.afgprojects.framework.core.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import io.github.afgprojects.framework.core.config.AfgConfigRegistry;
import io.github.afgprojects.framework.core.config.ConfigRefresher;
import io.github.afgprojects.framework.core.module.ModuleContext;
import io.github.afgprojects.framework.core.module.ModuleRegistry;
import io.github.afgprojects.framework.core.util.JacksonUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * AfgAutoConfiguration 单元测试。
 * 测试 AFG 核心自动配置类的 Bean 创建功能。
 *
 * @see AfgAutoConfiguration
 */
@DisplayName("AfgAutoConfiguration 测试")
class AfgAutoConfigurationTest {

    private AfgAutoConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new AfgAutoConfiguration();
    }

    /**
     * 模块注册表配置测试。
     * 验证 moduleRegistry Bean 的创建。
     */
    @Nested
    @DisplayName("moduleRegistry 配置测试")
    class ModuleRegistryTests {

        /**
         * 测试创建模块注册表。
         */
        @Test
        @DisplayName("应该创建模块注册表")
        void shouldCreateModuleRegistry() {
            ModuleRegistry registry = configuration.moduleRegistry();

            assertThat(registry).isNotNull();
        }
    }

    /**
     * 配置注册表配置测试。
     * 验证 afgConfigRegistry Bean 的创建。
     */
    @Nested
    @DisplayName("afgConfigRegistry 配置测试")
    class AfgConfigRegistryTests {

        /**
         * 测试创建配置注册表。
         */
        @Test
        @DisplayName("应该创建配置注册表")
        void shouldCreateAfgConfigRegistry() {
            AfgConfigRegistry registry = configuration.afgConfigRegistry();

            assertThat(registry).isNotNull();
        }
    }

    /**
     * 配置刷新器配置测试。
     * 验证 configRefresher Bean 的创建。
     */
    @Nested
    @DisplayName("configRefresher 配置测试")
    class ConfigRefresherTests {

        /**
         * 测试创建配置刷新器。
         */
        @Test
        @DisplayName("应该创建配置刷新器")
        void shouldCreateConfigRefresher() {
            AfgConfigRegistry configRegistry = new AfgConfigRegistry();

            ConfigRefresher refresher = configuration.configRefresher(configRegistry);

            assertThat(refresher).isNotNull();
        }
    }

    /**
     * 模块上下文配置测试。
     * 验证 moduleContext Bean 的创建。
     */
    @Nested
    @DisplayName("moduleContext 配置测试")
    class ModuleContextTests {

        /**
         * 测试创建模块上下文。
         */
        @Test
        @DisplayName("应该创建模块上下文")
        void shouldCreateModuleContext() {
            ModuleRegistry registry = new ModuleRegistry();
            ApplicationContext applicationContext = mock(ApplicationContext.class);

            ModuleContext context = configuration.moduleContext(registry, applicationContext);

            assertThat(context).isNotNull();
        }
    }

    /**
     * ObjectMapper 配置测试。
     * 验证 objectMapper Bean 的创建。
     */
    @Nested
    @DisplayName("objectMapper 配置测试")
    class ObjectMapperTests {

        /**
         * 测试创建 ObjectMapper。
         */
        @Test
        @DisplayName("应该创建 ObjectMapper")
        void shouldCreateObjectMapper() {
            ObjectMapper mapper = configuration.objectMapper();

            assertThat(mapper).isNotNull();
        }
    }

    /**
     * JacksonUtilsBeanPostProcessor 配置测试。
     * 验证 BeanPostProcessor 的创建和处理逻辑。
     */
    @Nested
    @DisplayName("JacksonUtilsBeanPostProcessor 测试")
    class JacksonUtilsBeanPostProcessorTests {

        /**
         * 测试创建 BeanPostProcessor。
         */
        @Test
        @DisplayName("应该创建 BeanPostProcessor")
        void shouldCreateBeanPostProcessor() {
            var processor = configuration.jacksonUtilsBeanPostProcessor();

            assertThat(processor).isNotNull();
        }

        /**
         * 测试处理 ObjectMapper bean 时设置 JacksonUtils。
         */
        @Test
        @DisplayName("处理 ObjectMapper bean 应该设置 JacksonUtils")
        void shouldSetJacksonUtilsWhenProcessingObjectMapper() {
            var processor = new AfgAutoConfiguration.JacksonUtilsBeanPostProcessor();
            ObjectMapper mapper = new ObjectMapper();

            Object result = processor.postProcessAfterInitialization(mapper, "objectMapper");

            assertThat(result).isSameAs(mapper);
        }

        /**
         * 测试处理非 ObjectMapper bean 时直接返回。
         */
        @Test
        @DisplayName("处理非 ObjectMapper bean 应该直接返回")
        void shouldReturnOtherBeansUnchanged() {
            var processor = new AfgAutoConfiguration.JacksonUtilsBeanPostProcessor();
            String otherBean = "test";

            Object result = processor.postProcessAfterInitialization(otherBean, "stringBean");

            assertThat(result).isSameAs(otherBean);
        }
    }
}
