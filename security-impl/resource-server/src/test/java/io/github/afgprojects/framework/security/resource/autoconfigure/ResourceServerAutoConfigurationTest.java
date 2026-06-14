package io.github.afgprojects.framework.security.resource.autoconfigure;

import io.github.afgprojects.framework.security.resource.properties.ResourceSecurityProperties;
import io.github.afgprojects.framework.security.resource.properties.jwt.ResourceSecurityJwtProperties;
import io.github.afgprojects.framework.security.resource.properties.permission.ResourceSecurityPermissionProperties;
import io.github.afgprojects.framework.security.resource.properties.tenant.ResourceSecurityTenantProperties;
import io.github.afgprojects.framework.security.core.token.JwtClaimsConfig;
import io.github.afgprojects.framework.security.core.token.JwtClaimsExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ResourceServerAutoConfiguration 单元测试
 *
 * <p>验证 AutoConfiguration 的 Bean 创建逻辑。
 * 由于 AutoConfiguration 条件注册需要 Spring 上下文，此处主要验证
 * 属性绑定到 Bean 的逻辑正确性。
 */
@DisplayName("ResourceServerAutoConfiguration 测试")
class ResourceServerAutoConfigurationTest {

    private ResourceSecurityProperties properties;

    @BeforeEach
    void setUp() {
        properties = new ResourceSecurityProperties();
    }

    @Nested
    @DisplayName("JwtClaimsExtractor 创建逻辑")
    class JwtClaimsExtractorTests {

        @Test
        @DisplayName("应根据 JWT 属性创建 JwtClaimsExtractor")
        void shouldCreateJwtClaimsExtractorFromProperties() {
            ResourceSecurityJwtProperties jwtProps = properties.getJwt();
            jwtProps.setUserIdClaim("custom_user_id");
            jwtProps.setUsernameClaim("custom_username");
            jwtProps.setRolesClaim("custom_roles");
            jwtProps.setPermissionsClaim("custom_permissions");
            jwtProps.setTenantIdClaim("custom_tenant_id");

            // 模拟 AutoConfiguration 的 Bean 创建逻辑
            JwtClaimsConfig claimsConfig = new JwtClaimsConfig();
            claimsConfig.setUserIdClaim(jwtProps.getUserIdClaim());
            claimsConfig.setUsernameClaim(jwtProps.getUsernameClaim());
            claimsConfig.setRolesClaim(jwtProps.getRolesClaim());
            claimsConfig.setPermissionsClaim(jwtProps.getPermissionsClaim());
            claimsConfig.setTenantIdClaim(jwtProps.getTenantIdClaim());
            JwtClaimsExtractor extractor = new JwtClaimsExtractor(claimsConfig);

            assertThat(extractor.getClaimsConfig().getUserIdClaim()).isEqualTo("custom_user_id");
            assertThat(extractor.getClaimsConfig().getUsernameClaim()).isEqualTo("custom_username");
            assertThat(extractor.getClaimsConfig().getRolesClaim()).isEqualTo("custom_roles");
            assertThat(extractor.getClaimsConfig().getPermissionsClaim()).isEqualTo("custom_permissions");
            assertThat(extractor.getClaimsConfig().getTenantIdClaim()).isEqualTo("custom_tenant_id");
        }

        @Test
        @DisplayName("默认 claim 名称应正确")
        void shouldUseDefaultClaimNames() {
            ResourceSecurityJwtProperties jwtProps = properties.getJwt();

            JwtClaimsConfig claimsConfig = new JwtClaimsConfig();
            claimsConfig.setUserIdClaim(jwtProps.getUserIdClaim());
            claimsConfig.setUsernameClaim(jwtProps.getUsernameClaim());
            claimsConfig.setRolesClaim(jwtProps.getRolesClaim());
            claimsConfig.setPermissionsClaim(jwtProps.getPermissionsClaim());
            claimsConfig.setTenantIdClaim(jwtProps.getTenantIdClaim());
            JwtClaimsExtractor extractor = new JwtClaimsExtractor(claimsConfig);

            assertThat(extractor.getClaimsConfig().getUserIdClaim()).isEqualTo("sub");
            assertThat(extractor.getClaimsConfig().getUsernameClaim()).isEqualTo("preferred_username");
            assertThat(extractor.getClaimsConfig().getRolesClaim()).isEqualTo("roles");
            assertThat(extractor.getClaimsConfig().getPermissionsClaim()).isEqualTo("permissions");
            assertThat(extractor.getClaimsConfig().getTenantIdClaim()).isEqualTo("tenant_id");
        }
    }

    @Nested
    @DisplayName("ResourceSecurityProperties 默认值")
    class PropertiesDefaultTests {

        @Test
        @DisplayName("默认应启用资源服务器")
        void shouldEnableByDefault() {
            assertThat(properties.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("默认应启用默认安全配置")
        void shouldEnableDefaultSecurityByDefault() {
            assertThat(properties.isDefaultSecurity()).isTrue();
        }

        @Test
        @DisplayName("JWT 属性应非 null")
        void shouldHaveNonNullJwtProperties() {
            assertThat(properties.getJwt()).isNotNull();
        }

        @Test
        @DisplayName("权限属性应非 null")
        void shouldHaveNonNullPermissionProperties() {
            assertThat(properties.getPermission()).isNotNull();
        }

        @Test
        @DisplayName("租户属性应非 null")
        void shouldHaveNonNullTenantProperties() {
            assertThat(properties.getTenant()).isNotNull();
        }
    }

    @Nested
    @DisplayName("租户配置")
    class TenantConfigTests {

        @Test
        @DisplayName("默认策略应为 token, header")
        void shouldUseTokenAndHeaderByDefault() {
            ResourceSecurityTenantProperties tenantProps = properties.getTenant();

            assertThat(tenantProps.getStrategies()).containsExactly("token", "header");
        }

        @Test
        @DisplayName("默认 header 名称应为 X-Tenant-Id")
        void shouldUseDefaultHeaderName() {
            ResourceSecurityTenantProperties tenantProps = properties.getTenant();

            assertThat(tenantProps.getHeaderName()).isEqualTo("X-Tenant-Id");
        }

        @Test
        @DisplayName("默认应在无法解析时失败")
        void shouldFailIfUnresolvedByDefault() {
            ResourceSecurityTenantProperties tenantProps = properties.getTenant();

            assertThat(tenantProps.isFailIfUnresolved()).isTrue();
        }
    }

    @Nested
    @DisplayName("权限配置")
    class PermissionConfigTests {

        @Test
        @DisplayName("默认 authServerUrl 应为 null")
        void shouldHaveNullAuthServerUrlByDefault() {
            ResourceSecurityPermissionProperties permProps = properties.getPermission();

            assertThat(permProps.getAuthServerUrl()).isNull();
        }

        @Test
        @DisplayName("默认 keyId 应为 null")
        void shouldHaveNullKeyIdByDefault() {
            ResourceSecurityPermissionProperties permProps = properties.getPermission();

            assertThat(permProps.getKeyId()).isNull();
        }

        @Test
        @DisplayName("默认 secret 应为 null")
        void shouldHaveNullSecretByDefault() {
            ResourceSecurityPermissionProperties permProps = properties.getPermission();

            assertThat(permProps.getSecret()).isNull();
        }
    }

    @Nested
    @DisplayName("JWT 配置")
    class JwtConfigTests {

        @Test
        @DisplayName("默认应启用 JWT 验证")
        void shouldEnableJwtByDefault() {
            ResourceSecurityJwtProperties jwtProps = properties.getJwt();

            assertThat(jwtProps.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("默认算法应为 RS256")
        void shouldUseRs256ByDefault() {
            ResourceSecurityJwtProperties jwtProps = properties.getJwt();

            assertThat(jwtProps.getJwsAlgorithm()).isEqualTo("RS256");
        }

        @Test
        @DisplayName("默认缓存 TTL 应为 5 分钟")
        void shouldUse5MinCacheTtlByDefault() {
            ResourceSecurityJwtProperties jwtProps = properties.getJwt();

            assertThat(jwtProps.getCacheTtl()).isEqualTo(java.time.Duration.ofMinutes(5));
        }

        @Test
        @DisplayName("默认 audience 应为空集")
        void shouldHaveEmptyAudienceByDefault() {
            ResourceSecurityJwtProperties jwtProps = properties.getJwt();

            assertThat(jwtProps.getAudience()).isEmpty();
        }
    }
}
