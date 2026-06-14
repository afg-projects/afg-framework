package io.github.afgprojects.framework.governance.server.config;

import io.github.afgprojects.framework.governance.server.properties.GovernanceServerProperties;
import io.github.afgprojects.framework.governance.server.properties.GovernanceServerSecurityProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import static org.assertj.core.api.Assertions.*;

/**
 * GovernanceServerAutoConfiguration 结构验证测试
 *
 * <p>验证自动配置类的声明是否符合规范：
 * 1. 使用 @AutoConfiguration 注解
 * 2. 声明 @AutoConfigureAfter 依赖排序
 * 3. 使用 @ConditionalOnProperty 条件
 * 4. 使用 @EnableConfigurationProperties 配置属性注册
 *
 * <p>这些测试是纯反射验证，不需要 Spring 上下文或 gRPC 服务器。
 */
@DisplayName("GovernanceServerAutoConfiguration")
class GovernanceServerAutoConfigurationTest {

    @Nested
    @DisplayName("@AutoConfiguration 注解")
    class AutoConfigurationAnnotation {

        @Test
        @DisplayName("类应使用 @AutoConfiguration 注解而非 @Configuration")
        void shouldUseAutoConfigurationAnnotation() {
            assertThat(GovernanceServerAutoConfiguration.class.isAnnotationPresent(AutoConfiguration.class))
                .isTrue();
        }

        @Test
        @DisplayName("应声明 @AutoConfigureAfter 依赖排序（DataSource 系列）")
        void shouldDeclareAutoConfigureAfterForDataSource() {
            AutoConfiguration annotation = GovernanceServerAutoConfiguration.class
                .getAnnotation(AutoConfiguration.class);

            assertThat(annotation.afterName())
                .contains(
                    "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
                    "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration"
                );
        }

        @Test
        @DisplayName("应声明 @AutoConfigureBefore 依赖排序（GrpcServer）")
        void shouldDeclareAutoConfigureBeforeForGrpcServer() {
            AutoConfiguration annotation = GovernanceServerAutoConfiguration.class
                .getAnnotation(AutoConfiguration.class);

            assertThat(annotation.beforeName())
                .contains(
                    "org.springframework.boot.grpc.server.autoconfigure.GrpcServerAutoConfiguration"
                );
        }
    }

    @Nested
    @DisplayName("@ConditionalOnProperty 条件")
    class ConditionalOnPropertyCheck {

        @Test
        @DisplayName("类应声明 @ConditionalOnProperty 启用开关")
        void shouldDeclareEnabledSwitch() {
            ConditionalOnProperty annotation = GovernanceServerAutoConfiguration.class
                .getAnnotation(ConditionalOnProperty.class);

            assertThat(annotation).isNotNull();
            assertThat(annotation.prefix()).isEqualTo("afg.governance.server");
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
            EnableConfigurationProperties annotation =
                GovernanceServerAutoConfiguration.class
                    .getAnnotation(EnableConfigurationProperties.class);

            assertThat(annotation).isNotNull();
            assertThat(annotation.value())
                .contains(
                    GovernanceServerProperties.class,
                    GovernanceServerSecurityProperties.class
                );
        }
    }

    @Nested
    @DisplayName("Bean 声明验证")
    class BeanDeclaration {

        @Test
        @DisplayName("应声明 GrpcPortMappingBeanPostProcessor Bean")
        void shouldDeclareGrpcPortMappingBeanPostProcessor() throws NoSuchMethodException {
            var method = GovernanceServerAutoConfiguration.class
                .getDeclaredMethod("grpcPortMappingBeanPostProcessor", GovernanceServerProperties.class);

            // Should be a static @Bean method
            assertThat(method.isAnnotationPresent(org.springframework.context.annotation.Bean.class)).isTrue();
        }

        @Test
        @DisplayName("GrpcPortMappingBeanPostProcessor 应为 static 方法")
        void shouldBeStaticBeanMethod() throws NoSuchMethodException {
            var method = GovernanceServerAutoConfiguration.class
                .getDeclaredMethod("grpcPortMappingBeanPostProcessor", GovernanceServerProperties.class);

            assertThat(java.lang.reflect.Modifier.isStatic(method.getModifiers())).isTrue();
        }
    }
}
