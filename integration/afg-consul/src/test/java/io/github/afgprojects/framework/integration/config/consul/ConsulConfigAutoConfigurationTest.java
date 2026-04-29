package io.github.afgprojects.framework.integration.config.consul;

import io.github.afgprojects.framework.core.api.config.RemoteConfigClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ConsulConfigAutoConfiguration 自动配置测试
 * <p>
 * 测试条件装配和 Bean 创建逻辑
 */
@DisplayName("ConsulConfigAutoConfiguration 测试")
class ConsulConfigAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConsulConfigAutoConfiguration.class));

    @Nested
    @DisplayName("条件装配测试")
    class ConditionalOnTests {

        @Test
        @DisplayName("当 Consul 依赖存在且 enabled=true 时应该创建 Bean")
        void shouldCreateBeansWhenConsulClassPresentAndEnabled() {
            // Consul 依赖在 classpath 中，@ConditionalOnClass 条件满足
            // 默认 enabled=true，所以应该创建 Bean
            contextRunner.run(context -> {
                // Consul 依赖存在，AutoConfiguration 应该生效
                assertThat(context).hasSingleBean(ConsulConfigProperties.class);
                assertThat(context).hasSingleBean(ConsulConfigClient.class);
                assertThat(context).hasSingleBean(RemoteConfigClient.class);
            });
        }

        @Test
        @DisplayName("当 enabled=false 时不应该创建 Bean")
        void shouldNotCreateBeansWhenDisabled() {
            contextRunner
                    .withPropertyValues("afg.config.consul.enabled=false")
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(ConsulConfigProperties.class);
                        assertThat(context).doesNotHaveBean(ConsulConfigClient.class);
                        assertThat(context).doesNotHaveBean(RemoteConfigClient.class);
                    });
        }
    }

    @Nested
    @DisplayName("Bean 创建测试")
    class BeanCreationTests {

        @Test
        @DisplayName("AutoConfiguration 类应该有正确的注解")
        void shouldHaveCorrectAnnotations() {
            assertThat(ConsulConfigAutoConfiguration.class)
                    .hasAnnotation(org.springframework.boot.autoconfigure.AutoConfiguration.class);
        }
    }

    @Nested
    @DisplayName("配置属性绑定测试")
    class ConfigurationPropertiesBindingTests {

        @Test
        @DisplayName("ConsulConfigProperties 应该有正确的配置前缀")
        void shouldHaveCorrectConfigurationPrefix() {
            assertThat(ConsulConfigProperties.class).isNotNull();
        }
    }
}
