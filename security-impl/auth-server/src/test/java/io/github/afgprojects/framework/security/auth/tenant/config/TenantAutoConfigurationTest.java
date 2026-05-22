package io.github.afgprojects.framework.security.auth.tenant.config;

import io.github.afgprojects.framework.security.auth.autoconfigure.AuthSecurityProperties;
import io.github.afgprojects.framework.security.auth.autoconfigure.TenantAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Cache;

import io.github.afgprojects.framework.data.core.context.TenantContextHolder;
import io.github.afgprojects.framework.security.auth.tenant.filter.TenantFilter;
import io.github.afgprojects.framework.security.auth.tenant.resolver.TenantResolverChain;
import io.github.afgprojects.framework.security.auth.tenant.validator.DefaultTenantValidator;
import io.github.afgprojects.framework.security.auth.tenant.validator.NoOpTenantValidator;
import io.github.afgprojects.framework.security.auth.tenant.validator.TenantValidator;
import io.github.afgprojects.framework.security.core.tenant.AfgTenantService;

/**
 * TenantAutoConfiguration 测试类。
 *
 * @since 1.0.0
 */
class TenantAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TenantAutoConfiguration.class));

    @Nested
    @DisplayName("条件装配测试")
    class ConditionalTests {

        @Test
        @DisplayName("当 afg.security.auth-server.tenant.enabled=true 时应该创建所有 Bean")
        void shouldCreateBeansWhenEnabled() {
            contextRunner
                    .withPropertyValues("afg.security.auth-server.tenant.enabled=true")
                    .run(context -> {
                        assertThat(context).hasSingleBean(TenantContextHolder.class);
                        assertThat(context).hasSingleBean(TenantResolverChain.class);
                        assertThat(context).hasSingleBean(TenantFilter.class);
                        assertThat(context).hasSingleBean(TenantValidator.class);
                        assertThat(context).hasSingleBean(AuthSecurityProperties.class);
                    });
        }

        @Test
        @DisplayName("当 afg.security.auth-server.tenant.enabled=false 时不应该创建 Bean")
        void shouldNotCreateBeansWhenDisabled() {
            contextRunner
                    .withPropertyValues("afg.security.auth-server.tenant.enabled=false")
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(TenantContextHolder.class);
                        assertThat(context).doesNotHaveBean(TenantResolverChain.class);
                        assertThat(context).doesNotHaveBean(TenantFilter.class);
                        assertThat(context).doesNotHaveBean(TenantValidator.class);
                    });
        }

        @Test
        @DisplayName("默认应该启用租户功能")
        void shouldBeEnabledByDefault() {
            contextRunner
                    .run(context -> {
                        assertThat(context).hasSingleBean(TenantContextHolder.class);
                        assertThat(context).hasSingleBean(TenantFilter.class);
                    });
        }
    }

    @Nested
    @DisplayName("TenantValidator 创建测试")
    class TenantValidatorTests {

        @Test
        @DisplayName("当 AfgTenantService 存在时应该创建 DefaultTenantValidator")
        void shouldCreateDefaultValidatorWhenTenantServiceExists() {
            contextRunner
                    .withPropertyValues("afg.security.auth-server.tenant.enabled=true")
                    .withUserConfiguration(TestTenantServiceConfig.class)
                    .run(context -> {
                        assertThat(context).hasSingleBean(TenantValidator.class);
                        assertThat(context).hasSingleBean(DefaultTenantValidator.class);
                        assertThat(context.getBean(TenantValidator.class))
                                .isInstanceOf(DefaultTenantValidator.class);
                    });
        }

        @Test
        @DisplayName("当 AfgTenantService 不存在时应该创建 NoOpTenantValidator")
        void shouldCreateNoOpValidatorWhenTenantServiceNotExists() {
            contextRunner
                    .withPropertyValues("afg.security.auth-server.tenant.enabled=true")
                    .run(context -> {
                        assertThat(context).hasSingleBean(TenantValidator.class);
                        assertThat(context).hasSingleBean(NoOpTenantValidator.class);
                        assertThat(context.getBean(TenantValidator.class))
                                .isInstanceOf(NoOpTenantValidator.class);
                    });
        }
    }

    @Nested
    @DisplayName("TenantResolverChain 配置测试")
    class TenantResolverChainTests {

        @Test
        @DisplayName("应该根据配置创建解析器链")
        void shouldCreateResolverChainFromConfig() {
            contextRunner
                    .withPropertyValues(
                            "afg.security.auth-server.tenant.enabled=true",
                            "afg.security.auth-server.tenant.fail-if-unresolved=false")
                    .run(context -> {
                        assertThat(context).hasSingleBean(TenantResolverChain.class);

                        TenantResolverChain chain = context.getBean(TenantResolverChain.class);
                        assertThat(chain).isNotNull();
                    });
        }

        @Test
        @DisplayName("应该配置 failIfUnresolved 属性")
        void shouldConfigureFailIfUnresolved() {
            contextRunner
                    .withPropertyValues(
                            "afg.security.auth-server.tenant.enabled=true",
                            "afg.security.auth-server.tenant.fail-if-unresolved=true")
                    .run(context -> {
                        AuthSecurityProperties properties = context.getBean(AuthSecurityProperties.class);
                        assertThat(properties.getTenant().isFailIfUnresolved()).isTrue();
                    });
        }
    }

    @Nested
    @DisplayName("TenantFilter 创建测试")
    class TenantFilterTests {

        @Test
        @DisplayName("应该创建 TenantFilter 并注入依赖")
        void shouldCreateTenantFilterWithDependencies() {
            contextRunner
                    .withPropertyValues("afg.security.auth-server.tenant.enabled=true")
                    .run(context -> {
                        assertThat(context).hasSingleBean(TenantFilter.class);

                        TenantFilter filter = context.getBean(TenantFilter.class);
                        assertThat(filter).isNotNull();
                    });
        }
    }

    @Nested
    @DisplayName("缓存配置测试")
    class CacheConfigTests {

        @Test
        @DisplayName("应该创建验证缓存")
        void shouldCreateValidationCache() {
            contextRunner
                    .withPropertyValues("afg.security.auth-server.tenant.enabled=true")
                    .withUserConfiguration(TestTenantServiceConfig.class)
                    .run(context -> {
                        assertThat(context).hasSingleBean(Cache.class);
                    });
        }
    }

    /**
     * 测试用的 AfgTenantService 配置。
     */
    @Configuration
    static class TestTenantServiceConfig {

        @Bean
        public AfgTenantService afgTenantService() {
            return tenantId -> null;
        }
    }
}
