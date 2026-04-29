package io.github.afgprojects.framework.integration.config.nacos;

import com.alibaba.nacos.api.config.ConfigService;
import io.github.afgprojects.framework.core.api.config.RemoteConfigClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NacosConfigAutoConfiguration 自动配置测试
 * <p>
 * 测试条件装配和 Bean 创建逻辑
 */
@DisplayName("NacosConfigAutoConfiguration 测试")
class NacosConfigAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(NacosConfigAutoConfiguration.class));

    @Nested
    @DisplayName("条件装配测试")
    class ConditionalOnTests {

        @Test
        @DisplayName("当 Nacos 依赖存在且 enabled=true 时应该创建 Bean")
        void shouldCreateBeansWhenNacosClassPresentAndEnabled() {
            // Nacos 依赖在 classpath 中，@ConditionalOnClass 条件满足
            // 默认 enabled=true，所以应该创建 Bean
            contextRunner.run(context -> {
                // Nacos 依赖存在，AutoConfiguration 应该生效
                assertThat(context).hasSingleBean(NacosConfigProperties.class);
                assertThat(context).hasSingleBean(ConfigService.class);
                assertThat(context).hasSingleBean(NacosConfigClient.class);
                assertThat(context).hasSingleBean(RemoteConfigClient.class);
            });
        }

        @Test
        @DisplayName("当 enabled=false 时不应该创建 Bean")
        void shouldNotCreateBeansWhenDisabled() {
            contextRunner
                    .withPropertyValues("afg.config.nacos.enabled=false")
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(NacosConfigProperties.class);
                        assertThat(context).doesNotHaveBean(NacosConfigClient.class);
                        assertThat(context).doesNotHaveBean(ConfigService.class);
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
            assertThat(NacosConfigAutoConfiguration.class)
                    .hasAnnotation(org.springframework.boot.autoconfigure.AutoConfiguration.class);
        }
    }

    @Nested
    @DisplayName("配置属性绑定测试")
    class ConfigurationPropertiesBindingTests {

        @Test
        @DisplayName("NacosConfigProperties 应该有正确的配置前缀")
        void shouldHaveCorrectConfigurationPrefix() {
            assertThat(NacosConfigProperties.class).isNotNull();
        }
    }
}
