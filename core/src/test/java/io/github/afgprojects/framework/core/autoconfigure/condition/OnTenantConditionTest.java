package io.github.afgprojects.framework.core.autoconfigure.condition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * OnTenantCondition 测试类
 */
class OnTenantConditionTest {

    private OnTenantCondition condition;
    private ConditionContext context;
    private Environment environment;
    private AnnotatedTypeMetadata metadata;

    @BeforeEach
    void setUp() {
        condition = new OnTenantCondition();
        context = mock(ConditionContext.class);
        environment = mock(Environment.class);
        metadata = mock(AnnotatedTypeMetadata.class);
        when(context.getEnvironment()).thenReturn(environment);
    }

    @Test
    @DisplayName("租户 ID 匹配时应该匹配")
    void shouldMatchWhenTenantIdMatches() {
        // given
        when(environment.getProperty("afg.tenant.id")).thenReturn("tenant-001");
        mockAnnotationAttributes(new String[]{"tenant-001", "tenant-002"}, false);

        // when
        boolean matches = condition.matches(context, metadata);

        // then
        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("租户 ID 不匹配时应该不匹配")
    void shouldNotMatchWhenTenantIdNotMatches() {
        // given
        when(environment.getProperty("afg.tenant.id")).thenReturn("tenant-003");
        mockAnnotationAttributes(new String[]{"tenant-001", "tenant-002"}, false);

        // when
        boolean matches = condition.matches(context, metadata);

        // then
        assertThat(matches).isFalse();
    }

    @Test
    @DisplayName("租户 ID 缺失且 matchIfMissing 为 true 时应该匹配")
    void shouldMatchWhenTenantIdMissingAndMatchIfMissingTrue() {
        // given
        when(environment.getProperty("afg.tenant.id")).thenReturn(null);
        when(environment.getProperty("AFG_TENANT_ID")).thenReturn(null);
        mockAnnotationAttributes(new String[]{"tenant-001"}, true);

        // when
        boolean matches = condition.matches(context, metadata);

        // then
        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("租户 ID 缺失且 matchIfMissing 为 false 时应该不匹配")
    void shouldNotMatchWhenTenantIdMissingAndMatchIfMissingFalse() {
        // given
        when(environment.getProperty("afg.tenant.id")).thenReturn(null);
        when(environment.getProperty("AFG_TENANT_ID")).thenReturn(null);
        mockAnnotationAttributes(new String[]{"tenant-001"}, false);

        // when
        boolean matches = condition.matches(context, metadata);

        // then
        assertThat(matches).isFalse();
    }

    @Test
    @DisplayName("从环境变量获取租户 ID")
    void shouldGetTenantIdFromEnvironmentVariable() {
        // given
        when(environment.getProperty("afg.tenant.id")).thenReturn(null);
        when(environment.getProperty("AFG_TENANT_ID")).thenReturn("tenant-from-env");
        mockAnnotationAttributes(new String[]{"tenant-from-env"}, false);

        // when
        boolean matches = condition.matches(context, metadata);

        // then
        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("配置属性优先于环境变量")
    void propertyShouldTakePrecedenceOverEnvVariable() {
        // given
        when(environment.getProperty("afg.tenant.id")).thenReturn("tenant-from-property");
        when(environment.getProperty("AFG_TENANT_ID")).thenReturn("tenant-from-env");
        mockAnnotationAttributes(new String[]{"tenant-from-property"}, false);

        // when
        boolean matches = condition.matches(context, metadata);

        // then
        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("单一租户 ID 匹配")
    void shouldMatchSingleTenantId() {
        // given
        when(environment.getProperty("afg.tenant.id")).thenReturn("tenant-001");
        mockAnnotationAttributes(new String[]{"tenant-001"}, false);

        // when
        boolean matches = condition.matches(context, metadata);

        // then
        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("注解属性为 null 时应该不匹配")
    void shouldNotMatchWhenAnnotationAttributesNull() {
        // given
        when(metadata.getAnnotationAttributes(ConditionalOnTenant.class.getName())).thenReturn(null);

        // when
        boolean matches = condition.matches(context, metadata);

        // then
        assertThat(matches).isFalse();
    }

    @Test
    @DisplayName("租户 ID 数组为空时应该不匹配")
    void shouldNotMatchWhenTenantIdArrayEmpty() {
        // given
        when(environment.getProperty("afg.tenant.id")).thenReturn("tenant-001");
        mockAnnotationAttributes(new String[]{}, false);

        // when
        boolean matches = condition.matches(context, metadata);

        // then
        assertThat(matches).isFalse();
    }

    private void mockAnnotationAttributes(String[] tenantIds, boolean matchIfMissing) {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("tenantId", tenantIds);
        attrs.put("matchIfMissing", matchIfMissing);
        when(metadata.getAnnotationAttributes(ConditionalOnTenant.class.getName())).thenReturn(attrs);
    }
}