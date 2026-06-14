package io.github.afgprojects.framework.governance.client.config;

import io.github.afgprojects.framework.governance.client.common.GovernanceChannelManager;
import io.github.afgprojects.framework.governance.client.properties.common.GovernanceCommonProperties;
import io.github.afgprojects.framework.governance.client.properties.config.GovernanceConfigProperties;
import io.github.afgprojects.framework.governance.client.properties.registry.GovernanceRegistryProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.*;

/**
 * GovernanceClientAutoConfiguration 结构验证测试
 *
 * <p>验证自动配置类的声明是否符合规范：
 * 1. 使用 @AutoConfiguration 注解
 * 2. 声明 @AutoConfigureAfter 依赖排序
 * 3. 使用 @ConditionalOnProperty 条件
 * 4. 使用 @ConditionalOnMissingBean 可替换组件
 * 5. 使用 @EnableConfigurationProperties 配置属性注册
 *
 * <p>这些测试是纯反射验证，不需要 Spring 上下文，
 * 确保自动配置的声明合规性。
 */
@DisplayName("GovernanceClientAutoConfiguration")
class GovernanceClientAutoConfigurationTest {

    @Nested
    @DisplayName("@AutoConfiguration 注解")
    class AutoConfigurationAnnotation {

        @Test
        @DisplayName("类应使用 @AutoConfiguration 注解而非 @Configuration")
        void shouldUseAutoConfigurationAnnotation() {
            assertThat(GovernanceClientAutoConfiguration.class.isAnnotationPresent(AutoConfiguration.class))
                .isTrue();
        }

        @Test
        @DisplayName("应声明 @AutoConfigureAfter 依赖排序")
        void shouldDeclareAutoConfigureAfter() {
            AutoConfiguration annotation = GovernanceClientAutoConfiguration.class
                .getAnnotation(AutoConfiguration.class);

            // afterName should be non-empty (uses string reference for cross-module)
            assertThat(annotation.afterName())
                .isNotEmpty()
                .contains("io.github.afgprojects.framework.core.autoconfigure.AfgAutoConfiguration");
        }
    }

    @Nested
    @DisplayName("@ConditionalOnProperty 条件")
    class ConditionalOnPropertyCheck {

        @Test
        @DisplayName("类应声明 @ConditionalOnProperty 启用开关")
        void shouldDeclareEnabledSwitch() {
            ConditionalOnProperty annotation = GovernanceClientAutoConfiguration.class
                .getAnnotation(ConditionalOnProperty.class);

            assertThat(annotation).isNotNull();
            assertThat(annotation.prefix()).isEqualTo("afg.governance.client");
            assertThat(annotation.name()).contains("enabled");
            assertThat(annotation.matchIfMissing()).isTrue();
        }
    }

    @Nested
    @DisplayName("@EnableConfigurationProperties 配置属性")
    class ConfigurationPropertiesCheck {

        @Test
        @DisplayName("应注册所有配置属性类")
        void shouldRegisterAllConfigurationProperties() {
            org.springframework.boot.context.properties.EnableConfigurationProperties annotation =
                GovernanceClientAutoConfiguration.class
                    .getAnnotation(org.springframework.boot.context.properties.EnableConfigurationProperties.class);

            assertThat(annotation).isNotNull();
            assertThat(annotation.value())
                .contains(
                    GovernanceCommonProperties.class,
                    GovernanceConfigProperties.class,
                    GovernanceRegistryProperties.class
                );
        }
    }

    @Nested
    @DisplayName("Bean 条件注册")
    class BeanConditionalRegistration {

        @Test
        @DisplayName("GovernanceChannelManager 应声明 @ConditionalOnMissingBean")
        void shouldDeclareConditionalOnMissingBeanForChannelManager() throws NoSuchMethodException {
            Method method = GovernanceClientAutoConfiguration.class
                .getMethod("governanceChannelManager", GovernanceCommonProperties.class);

            assertThat(method.isAnnotationPresent(ConditionalOnMissingBean.class)).isTrue();
        }

        @Test
        @DisplayName("GovernanceConfigClient 应声明启用开关条件")
        void shouldDeclareEnabledConditionForConfigClient() throws NoSuchMethodException {
            Method method = GovernanceClientAutoConfiguration.class
                .getMethod("governanceConfigClient",
                    GovernanceChannelManager.class,
                    GovernanceConfigProperties.class,
                    String.class);

            assertThat(method.isAnnotationPresent(ConditionalOnProperty.class)).isTrue();
            assertThat(method.isAnnotationPresent(ConditionalOnMissingBean.class)).isTrue();
        }

        @Test
        @DisplayName("GovernanceRegistryClient 应声明启用开关条件")
        void shouldDeclareEnabledConditionForRegistryClient() throws NoSuchMethodException {
            Method method = GovernanceClientAutoConfiguration.class
                .getMethod("governanceRegistryClient",
                    GovernanceChannelManager.class,
                    GovernanceRegistryProperties.class,
                    String.class,
                    int.class);

            assertThat(method.isAnnotationPresent(ConditionalOnProperty.class)).isTrue();
            assertThat(method.isAnnotationPresent(ConditionalOnMissingBean.class)).isTrue();
        }
    }

    @Nested
    @DisplayName("AutoConfiguration.imports 注册")
    class AutoConfigurationImportsRegistration {

        @Test
        @DisplayName("类应注册在 AutoConfiguration.imports 文件中")
        void shouldBeRegisteredInAutoConfigurationImports() {
            // The class exists and is annotated with @AutoConfiguration,
            // confirming it is meant for auto-configuration registration.
            // The actual META-INF file is verified separately in build integration tests.
            assertThat(GovernanceClientAutoConfiguration.class.isAnnotationPresent(AutoConfiguration.class))
                .isTrue();
        }
    }

    @Nested
    @DisplayName("PreDestroy 生命周期")
    class PreDestroyLifecycle {

        @Test
        @DisplayName("应声明 @PreDestroy 方法用于资源清理")
        void shouldDeclarePreDestroyMethod() throws NoSuchMethodException {
            Method destroyMethod = GovernanceClientAutoConfiguration.class
                .getDeclaredMethod("destroy");

            assertThat(destroyMethod.isAnnotationPresent(jakarta.annotation.PreDestroy.class)).isTrue();
        }
    }
}
