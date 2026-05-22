package io.github.afgprojects.framework.security.resource.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.afgprojects.framework.security.resource.introspection.IntrospectionProperties;
import io.github.afgprojects.framework.security.resource.jwt.JwtAuthenticationConverter;
import io.github.afgprojects.framework.security.resource.tenant.HeaderTenantResolver;
import io.github.afgprojects.framework.security.resource.tenant.TokenTenantResolver;
import io.github.afgprojects.framework.security.resource.tenant.TenantResolverChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * ResourceServerAutoConfiguration 测试类。
 *
 * @since 1.0.0
 */
class ResourceServerAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ResourceServerAutoConfiguration.class));

    @Nested
    @DisplayName("Bean 创建测试")
    class BeanCreationTests {

        @Test
        @DisplayName("应创建 JwtAuthenticationConverter Bean")
        void shouldCreateJwtAuthenticationConverterBean() {
            contextRunner.run(context -> {
                assertThat(context).hasSingleBean(JwtAuthenticationConverter.class);
                assertThat(context.getBean(JwtAuthenticationConverter.class)).isNotNull();
            });
        }

        @Test
        @DisplayName("应创建 TokenTenantResolver Bean")
        void shouldCreateTokenTenantResolverBean() {
            contextRunner.run(context -> {
                assertThat(context).hasSingleBean(TokenTenantResolver.class);
                TokenTenantResolver resolver = context.getBean(TokenTenantResolver.class);
                assertThat(resolver.getTenantIdClaim()).isEqualTo("tenant_id");
                assertThat(resolver.getOrder()).isEqualTo(100);
            });
        }

        @Test
        @DisplayName("应创建 HeaderTenantResolver Bean")
        void shouldCreateHeaderTenantResolverBean() {
            contextRunner.run(context -> {
                assertThat(context).hasSingleBean(HeaderTenantResolver.class);
                HeaderTenantResolver resolver = context.getBean(HeaderTenantResolver.class);
                assertThat(resolver.getHeaderName()).isEqualTo("X-Tenant-Id");
                assertThat(resolver.getOrder()).isEqualTo(200);
            });
        }

        @Test
        @DisplayName("应创建 TenantResolverChain Bean")
        void shouldCreateTenantResolverChainBean() {
            contextRunner.run(context -> {
                assertThat(context).hasSingleBean(TenantResolverChain.class);
                TenantResolverChain chain = context.getBean(TenantResolverChain.class);
                assertThat(chain.isEmpty()).isFalse();
                assertThat(chain.getResolvers()).hasSize(2);
            });
        }

        @Test
        @DisplayName("应创建配置属性 Bean")
        void shouldCreatePropertiesBeans() {
            contextRunner.run(context -> {
                assertThat(context).hasSingleBean(ResourceSecurityProperties.class);
                assertThat(context).hasSingleBean(IntrospectionProperties.class);
            });
        }
    }

    @Nested
    @DisplayName("条件配置测试")
    class ConditionalTests {

        @Test
        @DisplayName("禁用资源服务器时不应创建 Bean")
        void shouldNotCreateBeansWhenDisabled() {
            contextRunner
                    .withPropertyValues("afg.security.resource-server.enabled=false")
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(JwtAuthenticationConverter.class);
                        assertThat(context).doesNotHaveBean(TokenTenantResolver.class);
                        assertThat(context).doesNotHaveBean(HeaderTenantResolver.class);
                        assertThat(context).doesNotHaveBean(TenantResolverChain.class);
                    });
        }

        @Test
        @DisplayName("禁用 JWT 时不应创建 JwtAuthenticationConverter 和 TokenTenantResolver")
        void shouldNotCreateJwtBeansWhenDisabled() {
            contextRunner
                    .withPropertyValues("afg.security.resource-server.jwt.enabled=false")
                    .run(context -> {
                        // 当 JWT 禁用时，JwtAuthenticationConverter 和 TokenTenantResolver 不应创建
                        assertThat(context).doesNotHaveBean(JwtAuthenticationConverter.class);
                        assertThat(context).doesNotHaveBean(TokenTenantResolver.class);
                        // TenantResolverChain 应该创建，但只包含 HeaderTenantResolver
                        assertThat(context).hasSingleBean(TenantResolverChain.class);
                        TenantResolverChain chain = context.getBean(TenantResolverChain.class);
                        assertThat(chain.getResolvers()).hasSize(1);
                        assertThat(chain.getResolvers().get(0)).isInstanceOf(HeaderTenantResolver.class);
                        // HeaderTenantResolver 应该创建
                        assertThat(context).hasSingleBean(HeaderTenantResolver.class);
                    });
        }
    }

    @Nested
    @DisplayName("自定义配置测试")
    class CustomConfigurationTests {

        @Test
        @DisplayName("应使用自定义 tenantId claim")
        void shouldUseCustomTenantIdClaim() {
            contextRunner
                    .withPropertyValues("afg.security.resource-server.jwt.tenant-id-claim=custom_tenant")
                    .run(context -> {
                        TokenTenantResolver resolver = context.getBean(TokenTenantResolver.class);
                        assertThat(resolver.getTenantIdClaim()).isEqualTo("custom_tenant");
                    });
        }

        @Test
        @DisplayName("应使用自定义租户请求头名称")
        void shouldUseCustomTenantHeaderName() {
            contextRunner
                    .withPropertyValues("afg.security.resource-server.tenant.header-name=X-Custom-Tenant")
                    .run(context -> {
                        HeaderTenantResolver resolver = context.getBean(HeaderTenantResolver.class);
                        assertThat(resolver.getHeaderName()).isEqualTo("X-Custom-Tenant");
                    });
        }

        @Test
        @DisplayName("应使用自定义租户策略")
        void shouldUseCustomTenantStrategies() {
            contextRunner
                    .withPropertyValues("afg.security.resource-server.tenant.strategies=header")
                    .run(context -> {
                        TenantResolverChain chain = context.getBean(TenantResolverChain.class);
                        assertThat(chain.getResolvers()).hasSize(1);
                        assertThat(chain.getResolvers().get(0)).isInstanceOf(HeaderTenantResolver.class);
                    });
        }

        @Test
        @DisplayName("应配置 failIfTenantUnresolved")
        void shouldConfigureFailIfTenantUnresolved() {
            contextRunner
                    .withPropertyValues("afg.security.resource-server.tenant.fail-if-unresolved=false")
                    .run(context -> {
                        TenantResolverChain chain = context.getBean(TenantResolverChain.class);
                        assertThat(chain.isFailIfUnresolved()).isFalse();
                    });
        }

        @Test
        @DisplayName("使用未知策略时应跳过")
        void shouldSkipUnknownStrategy() {
            contextRunner
                    .withPropertyValues("afg.security.resource-server.tenant.strategies=header,unknown,token")
                    .run(context -> {
                        TenantResolverChain chain = context.getBean(TenantResolverChain.class);
                        // unknown 策略会被跳过，只有 header 和 token
                        assertThat(chain.getResolvers()).hasSize(2);
                    });
        }

        @Test
        @DisplayName("使用 token 策略但 JWT 禁用时应跳过")
        void shouldSkipTokenStrategyWhenJwtDisabled() {
            contextRunner
                    .withPropertyValues(
                            "afg.security.resource-server.jwt.enabled=false",
                            "afg.security.resource-server.tenant.strategies=token,header")
                    .run(context -> {
                        TenantResolverChain chain = context.getBean(TenantResolverChain.class);
                        // token 策略会被跳过，因为 TokenTenantResolver 不存在
                        assertThat(chain.getResolvers()).hasSize(1);
                        assertThat(chain.getResolvers().get(0)).isInstanceOf(HeaderTenantResolver.class);
                    });
        }

        @Test
        @DisplayName("空策略列表时应使用默认解析器")
        void shouldUseDefaultResolversWhenEmptyStrategies() {
            contextRunner
                    .withPropertyValues("afg.security.resource-server.tenant.strategies=")
                    .run(context -> {
                        TenantResolverChain chain = context.getBean(TenantResolverChain.class);
                        // 空策略列表时使用默认的 token 和 header
                        assertThat(chain.getResolvers()).hasSize(2);
                    });
        }
    }

    @Nested
    @DisplayName("默认值测试")
    class DefaultValueTests {

        @Test
        @DisplayName("ResourceSecurityProperties.JwtConfig 默认值应正确")
        void jwtPropertiesDefaultValuesShouldBeCorrect() {
            contextRunner.run(context -> {
                ResourceSecurityProperties props = context.getBean(ResourceSecurityProperties.class);
                ResourceSecurityProperties.JwtConfig jwtConfig = props.getJwt();
                assertThat(jwtConfig.isEnabled()).isTrue();
                assertThat(jwtConfig.getTenantIdClaim()).isEqualTo("tenant_id");
                assertThat(jwtConfig.getUserIdClaim()).isEqualTo("sub");
                assertThat(jwtConfig.getUsernameClaim()).isEqualTo("preferred_username");
                assertThat(jwtConfig.getRolesClaim()).isEqualTo("roles");
                assertThat(jwtConfig.getPermissionsClaim()).isEqualTo("permissions");
            });
        }

        @Test
        @DisplayName("IntrospectionProperties 默认值应正确")
        void introspectionPropertiesDefaultValuesShouldBeCorrect() {
            contextRunner.run(context -> {
                IntrospectionProperties props = context.getBean(IntrospectionProperties.class);
                assertThat(props.isEnabled()).isFalse();
                assertThat(props.isVerifyActive()).isTrue();
            });
        }

        @Test
        @DisplayName("ResourceSecurityProperties.TenantConfig 默认值应正确")
        void tenantConfigDefaultValuesShouldBeCorrect() {
            contextRunner.run(context -> {
                ResourceSecurityProperties props = context.getBean(ResourceSecurityProperties.class);
                ResourceSecurityProperties.TenantConfig tenantConfig = props.getTenant();
                assertThat(tenantConfig.getStrategies()).containsExactly("token", "header");
                assertThat(tenantConfig.getHeaderName()).isEqualTo("X-Tenant-Id");
                assertThat(tenantConfig.isFailIfUnresolved()).isTrue();
            });
        }
    }
}