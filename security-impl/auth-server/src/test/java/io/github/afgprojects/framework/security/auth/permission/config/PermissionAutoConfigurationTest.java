package io.github.afgprojects.framework.security.auth.permission.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.afgprojects.framework.data.core.scope.DataScope;
import io.github.afgprojects.framework.data.core.scope.DataScopeType;
import io.github.afgprojects.framework.security.auth.permission.DataScopeInterceptor;
import io.github.afgprojects.framework.security.auth.permission.DefaultRbacService;
import io.github.afgprojects.framework.security.auth.permission.InMemoryRolePermissionStorage;
import io.github.afgprojects.framework.security.auth.permission.RbacPermissionService;
import io.github.afgprojects.framework.security.core.permission.DataScopeService;
import io.github.afgprojects.framework.security.core.permission.PermissionService;
import io.github.afgprojects.framework.security.core.permission.RbacService;
import io.github.afgprojects.framework.security.core.permission.RolePermissionStorage;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * PermissionAutoConfiguration 测试。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@DisplayName("PermissionAutoConfiguration 测试")
class PermissionAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PermissionAutoConfiguration.class));

    @Nested
    @DisplayName("Bean 创建测试")
    class BeanCreationTests {

        @Test
        @DisplayName("应创建 RolePermissionStorage Bean")
        void shouldCreateRolePermissionStorageBean() {
            contextRunner.run(context -> {
                assertThat(context).hasSingleBean(RolePermissionStorage.class);
                assertThat(context.getBean(RolePermissionStorage.class))
                        .isInstanceOf(InMemoryRolePermissionStorage.class);
            });
        }

        @Test
        @DisplayName("应创建 PermissionService Bean")
        void shouldCreatePermissionServiceBean() {
            contextRunner.run(context -> {
                assertThat(context).hasSingleBean(PermissionService.class);
                assertThat(context.getBean(PermissionService.class))
                        .isInstanceOf(RbacPermissionService.class);
            });
        }

        @Test
        @DisplayName("应创建 RbacService Bean")
        void shouldCreateRbacServiceBean() {
            contextRunner.run(context -> {
                assertThat(context).hasSingleBean(RbacService.class);
                assertThat(context.getBean(RbacService.class))
                        .isInstanceOf(DefaultRbacService.class);
            });
        }

        @Test
        @DisplayName("应创建 DataScopeService Bean")
        void shouldCreateDataScopeServiceBean() {
            contextRunner.run(context -> {
                assertThat(context).hasSingleBean(DataScopeService.class);
            });
        }

        @Test
        @DisplayName("应创建 DataScopeInterceptor Bean")
        void shouldCreateDataScopeInterceptorBean() {
            contextRunner.run(context -> {
                assertThat(context).hasSingleBean(DataScopeInterceptor.class);
            });
        }

        @Test
        @DisplayName("应创建 PermissionProperties Bean")
        void shouldCreatePermissionPropertiesBean() {
            contextRunner.run(context -> {
                assertThat(context).hasSingleBean(PermissionProperties.class);
            });
        }
    }

    @Nested
    @DisplayName("条件配置测试")
    class ConditionalConfigTests {

        @Test
        @DisplayName("禁用权限功能时不应创建 Bean")
        void shouldNotCreateBeansWhenDisabled() {
            contextRunner
                    .withPropertyValues("afg.permission.enabled=false")
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(RolePermissionStorage.class);
                        assertThat(context).doesNotHaveBean(PermissionService.class);
                        assertThat(context).doesNotHaveBean(RbacService.class);
                        assertThat(context).doesNotHaveBean(DataScopeService.class);
                        assertThat(context).doesNotHaveBean(DataScopeInterceptor.class);
                    });
        }

        @Test
        @DisplayName("禁用数据权限拦截器时不应创建拦截器 Bean")
        void shouldNotCreateInterceptorWhenDisabled() {
            contextRunner
                    .withPropertyValues("afg.permission.data-scope-interceptor-enabled=false")
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(DataScopeInterceptor.class);
                        // 其他 Bean 仍应创建
                        assertThat(context).hasSingleBean(RolePermissionStorage.class);
                        assertThat(context).hasSingleBean(PermissionService.class);
                        assertThat(context).hasSingleBean(RbacService.class);
                        assertThat(context).hasSingleBean(DataScopeService.class);
                    });
        }
    }

    @Nested
    @DisplayName("属性配置测试")
    class PropertiesConfigTests {

        @Test
        @DisplayName("应使用默认属性值")
        void shouldUseDefaultProperties() {
            contextRunner.run(context -> {
                PermissionProperties properties = context.getBean(PermissionProperties.class);
                assertThat(properties.isEnabled()).isTrue();
                assertThat(properties.getDefaultDataScope()).isEqualTo("ALL");
                assertThat(properties.isDataScopeInterceptorEnabled()).isTrue();
            });
        }

        @Test
        @DisplayName("应使用配置的属性值")
        void shouldUseConfiguredProperties() {
            contextRunner
                    .withPropertyValues(
                            "afg.permission.default-data-scope=DEPT",
                            "afg.permission.data-scope-interceptor-enabled=true"
                    )
                    .run(context -> {
                        PermissionProperties properties = context.getBean(PermissionProperties.class);
                        assertThat(properties.getDefaultDataScope()).isEqualTo("DEPT");
                        assertThat(properties.isDataScopeInterceptorEnabled()).isTrue();
                    });
        }
    }

    @Nested
    @DisplayName("自定义 Bean 测试")
    class CustomBeanTests {

        @Test
        @DisplayName("自定义 RolePermissionStorage 应覆盖默认实现")
        void customRolePermissionStorageShouldOverrideDefault() {
            contextRunner
                    .withBean(RolePermissionStorage.class, () -> new InMemoryRolePermissionStorage())
                    .run(context -> {
                        assertThat(context).hasSingleBean(RolePermissionStorage.class);
                        // 验证使用的是自定义 Bean
                        RolePermissionStorage storage = context.getBean(RolePermissionStorage.class);
                        assertThat(storage).isInstanceOf(InMemoryRolePermissionStorage.class);
                    });
        }

        @Test
        @DisplayName("自定义 DataScopeService 应覆盖默认实现")
        void customDataScopeServiceShouldOverrideDefault() {
            contextRunner
                    .withBean(DataScopeService.class, () -> new CustomDataScopeService())
                    .run(context -> {
                        assertThat(context).hasSingleBean(DataScopeService.class);
                    });
        }
    }

    /**
     * 自定义数据权限服务实现（用于测试）。
     */
    static class CustomDataScopeService implements DataScopeService {
        @Override
        @NonNull
        public DataScope getDataScope(@NonNull String userId, @Nullable String tenantId) {
            return DataScope.of("custom", "id", DataScopeType.ALL);
        }

        @Override
        public void setDataScope(@NonNull String userId, @Nullable String tenantId, @NonNull DataScope scope) {
            // 测试实现，不持久化
        }

        @Override
        public void removeDataScope(@NonNull String userId, @Nullable String tenantId) {
            // 测试实现，不持久化
        }
    }
}
