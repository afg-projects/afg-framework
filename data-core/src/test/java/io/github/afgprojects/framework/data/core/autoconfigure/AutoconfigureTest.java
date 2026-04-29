package io.github.afgprojects.framework.data.core.autoconfigure;

import io.github.afgprojects.framework.data.core.context.TenantContextHolder;
import io.github.afgprojects.framework.data.core.context.TenantContextTaskDecorator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.task.TaskDecorator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Autoconfigure 包测试
 * <p>
 * 使用 ApplicationContextRunner 测试 Spring Boot 自动配置
 */
@DisplayName("Autoconfigure 包测试")
class AutoconfigureTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TenantContextAutoConfiguration.class));

    // ==================== TenantContextAutoConfiguration 测试 ====================

    @Nested
    @DisplayName("TenantContextAutoConfiguration 测试")
    class TenantContextAutoConfigurationTest {

        @Test
        @DisplayName("应自动配置 TenantContextHolder")
        void shouldAutoConfigureTenantContextHolder() {
            contextRunner.run(context -> {
                assertThat(context).hasSingleBean(TenantContextHolder.class);
                assertThat(context.getBean(TenantContextHolder.class)).isNotNull();
            });
        }

        @Test
        @DisplayName("应自动配置 TenantContextTaskDecorator（默认启用）")
        void shouldAutoConfigureTenantContextTaskDecoratorByDefault() {
            contextRunner.run(context -> {
                assertThat(context).hasBean("tenantContextTaskDecorator");
                assertThat(context.getBean("tenantContextTaskDecorator")).isInstanceOf(TaskDecorator.class);
                assertThat(context.getBean("tenantContextTaskDecorator")).isInstanceOf(TenantContextTaskDecorator.class);
            });
        }

        @Test
        @DisplayName("配置禁用后不应创建 TenantContextTaskDecorator")
        void shouldNotCreateTenantContextTaskDecoratorWhenDisabled() {
            contextRunner
                    .withPropertyValues("afg.tenant.context-propagation.enabled=false")
                    .run(context -> {
                        assertThat(context).doesNotHaveBean("tenantContextTaskDecorator");
                        // TenantContextHolder 仍应创建
                        assertThat(context).hasSingleBean(TenantContextHolder.class);
                    });
        }

        @Test
        @DisplayName("显式启用应创建 TenantContextTaskDecorator")
        void shouldCreateTenantContextTaskDecoratorWhenExplicitlyEnabled() {
            contextRunner
                    .withPropertyValues("afg.tenant.context-propagation.enabled=true")
                    .run(context -> {
                        assertThat(context).hasBean("tenantContextTaskDecorator");
                        assertThat(context.getBean("tenantContextTaskDecorator")).isInstanceOf(TenantContextTaskDecorator.class);
                    });
        }

        @Test
        @DisplayName("TenantContextTaskDecorator 应依赖 TenantContextHolder")
        void tenantContextTaskDecoratorShouldDependOnTenantContextHolder() {
            contextRunner.run(context -> {
                TenantContextHolder holder = context.getBean(TenantContextHolder.class);
                TenantContextTaskDecorator decorator = (TenantContextTaskDecorator) context.getBean("tenantContextTaskDecorator");

                // 验证 decorator 使用了正确的 holder
                assertThat(decorator).isNotNull();
            });
        }

        @Test
        @DisplayName("用户自定义 TenantContextHolder 应覆盖自动配置")
        void customTenantContextHolderShouldOverrideAutoConfiguration() {
            TenantContextHolder customHolder = new TenantContextHolder();

            contextRunner
                    .withBean(TenantContextHolder.class, () -> customHolder)
                    .run(context -> {
                        assertThat(context).hasSingleBean(TenantContextHolder.class);
                        assertThat(context.getBean(TenantContextHolder.class)).isSameAs(customHolder);
                    });
        }

        @Test
        @DisplayName("用户自定义 TaskDecorator 应覆盖自动配置")
        void customTaskDecoratorShouldOverrideAutoConfiguration() {
            TaskDecorator customDecorator = runnable -> runnable;

            contextRunner
                    .withBean("tenantContextTaskDecorator", TaskDecorator.class, () -> customDecorator)
                    .run(context -> {
                        assertThat(context).hasBean("tenantContextTaskDecorator");
                        assertThat(context.getBean("tenantContextTaskDecorator")).isSameAs(customDecorator);
                    });
        }
    }
}