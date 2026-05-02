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
 * AfgAutoConfiguration 测试
 */
@DisplayName("AfgAutoConfiguration 测试")
class AfgAutoConfigurationTest {

    private AfgAutoConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new AfgAutoConfiguration();
    }

    @Nested
    @DisplayName("moduleRegistry 配置测试")
    class ModuleRegistryTests {

        @Test
        @DisplayName("应该创建模块注册表")
        void shouldCreateModuleRegistry() {
            ModuleRegistry registry = configuration.moduleRegistry();

            assertThat(registry).isNotNull();
        }
    }

    @Nested
    @DisplayName("afgConfigRegistry 配置测试")
    class AfgConfigRegistryTests {

        @Test
        @DisplayName("应该创建配置注册表")
        void shouldCreateAfgConfigRegistry() {
            AfgConfigRegistry registry = configuration.afgConfigRegistry();

            assertThat(registry).isNotNull();
        }
    }

    @Nested
    @DisplayName("configRefresher 配置测试")
    class ConfigRefresherTests {

        @Test
        @DisplayName("应该创建配置刷新器")
        void shouldCreateConfigRefresher() {
            AfgConfigRegistry configRegistry = new AfgConfigRegistry();

            ConfigRefresher refresher = configuration.configRefresher(configRegistry);

            assertThat(refresher).isNotNull();
        }
    }

    @Nested
    @DisplayName("moduleContext 配置测试")
    class ModuleContextTests {

        @Test
        @DisplayName("应该创建模块上下文")
        void shouldCreateModuleContext() {
            ModuleRegistry registry = new ModuleRegistry();
            ApplicationContext applicationContext = mock(ApplicationContext.class);

            ModuleContext context = configuration.moduleContext(registry, applicationContext);

            assertThat(context).isNotNull();
        }
    }

    @Nested
    @DisplayName("objectMapper 配置测试")
    class ObjectMapperTests {

        @Test
        @DisplayName("应该创建 ObjectMapper")
        void shouldCreateObjectMapper() {
            ObjectMapper mapper = configuration.objectMapper();

            assertThat(mapper).isNotNull();
        }
    }

    @Nested
    @DisplayName("JacksonUtilsBeanPostProcessor 测试")
    class JacksonUtilsBeanPostProcessorTests {

        @Test
        @DisplayName("应该创建 BeanPostProcessor")
        void shouldCreateBeanPostProcessor() {
            var processor = configuration.jacksonUtilsBeanPostProcessor();

            assertThat(processor).isNotNull();
        }

        @Test
        @DisplayName("处理 ObjectMapper bean 应该设置 JacksonUtils")
        void shouldSetJacksonUtilsWhenProcessingObjectMapper() {
            var processor = new AfgAutoConfiguration.JacksonUtilsBeanPostProcessor();
            ObjectMapper mapper = new ObjectMapper();

            Object result = processor.postProcessAfterInitialization(mapper, "objectMapper");

            assertThat(result).isSameAs(mapper);
        }

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
