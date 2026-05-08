package io.github.afgprojects.framework.security.resource.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * DefaultTenantContext 测试类。
 *
 * @since 1.0.0
 */
class DefaultTenantContextTest {

    @Nested
    @DisplayName("构造函数测试")
    class ConstructorTests {

        @Test
        @DisplayName("使用 tenantId 构造")
        void shouldConstructWithTenantId() {
            DefaultTenantContext context = new DefaultTenantContext("tenant-123");
            assertThat(context.getTenantId()).isEqualTo("tenant-123");
            assertThat(context.getTenantCode()).isEqualTo("tenant-123");
            assertThat(context.getTenantName()).isNull();
            assertThat(context.isDefault()).isFalse();
            assertThat(context.isValid()).isTrue();
        }

        @Test
        @DisplayName("使用 tenantId, tenantCode, tenantName 构造")
        void shouldConstructWithTenantIdCodeName() {
            DefaultTenantContext context = new DefaultTenantContext(
                    "tenant-123", "acme", "ACME Corp");
            assertThat(context.getTenantId()).isEqualTo("tenant-123");
            assertThat(context.getTenantCode()).isEqualTo("acme");
            assertThat(context.getTenantName()).isEqualTo("ACME Corp");
        }

        @Test
        @DisplayName("使用完整参数构造")
        void shouldConstructWithFullParameters() {
            Map<String, Object> attributes = Map.of("type", "enterprise", "region", "cn");
            DefaultTenantContext context = new DefaultTenantContext(
                    "tenant-123", "acme", "ACME Corp", attributes, true, true);
            assertThat(context.getTenantId()).isEqualTo("tenant-123");
            assertThat(context.getTenantCode()).isEqualTo("acme");
            assertThat(context.getTenantName()).isEqualTo("ACME Corp");
            assertThat(context.getAttributes()).containsEntry("type", "enterprise");
            assertThat(context.isDefault()).isTrue();
            assertThat(context.isValid()).isTrue();
        }

        @Test
        @DisplayName("tenantId 为 null 时应抛出异常")
        void shouldThrowExceptionWhenTenantIdIsNull() {
            assertThatThrownBy(() -> new DefaultTenantContext(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("tenantId must not be null");
        }

        @Test
        @DisplayName("attributes 为 null 时应使用空 Map")
        void shouldUseEmptyMapWhenAttributesIsNull() {
            DefaultTenantContext context = new DefaultTenantContext(
                    "tenant-123", null, null, null, false, true);
            assertThat(context.getAttributes()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getTenantCode 测试")
    class TenantCodeTests {

        @Test
        @DisplayName("tenantCode 为 null 时返回 tenantId")
        void shouldReturnTenantIdWhenTenantCodeIsNull() {
            DefaultTenantContext context = new DefaultTenantContext("tenant-123");
            assertThat(context.getTenantCode()).isEqualTo("tenant-123");
        }

        @Test
        @DisplayName("tenantCode 有值时返回 tenantCode")
        void shouldReturnTenantCodeWhenSet() {
            DefaultTenantContext context = new DefaultTenantContext("tenant-123", "acme", null);
            assertThat(context.getTenantCode()).isEqualTo("acme");
        }
    }

    @Nested
    @DisplayName("addAttribute 测试")
    class AddAttributeTests {

        @Test
        @DisplayName("添加属性")
        void shouldAddAttribute() {
            DefaultTenantContext context = new DefaultTenantContext("tenant-123");
            context.addAttribute("type", "enterprise");
            assertThat(context.getAttributes()).containsEntry("type", "enterprise");
        }

        @Test
        @DisplayName("添加多个属性")
        void shouldAddMultipleAttributes() {
            DefaultTenantContext context = new DefaultTenantContext("tenant-123");
            context.addAttribute("type", "enterprise");
            context.addAttribute("region", "cn");
            assertThat(context.getAttributes())
                    .containsEntry("type", "enterprise")
                    .containsEntry("region", "cn");
        }
    }

    @Nested
    @DisplayName("equals 和 hashCode 测试")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("相同 tenantId 的对象应相等")
        void shouldBeEqualWhenTenantIdIsSame() {
            DefaultTenantContext context1 = new DefaultTenantContext("tenant-123");
            DefaultTenantContext context2 = new DefaultTenantContext("tenant-123", "acme", "ACME");
            assertThat(context1).isEqualTo(context2);
            assertThat(context1.hashCode()).isEqualTo(context2.hashCode());
        }

        @Test
        @DisplayName("不同 tenantId 的对象应不相等")
        void shouldNotBeEqualWhenTenantIdIsDifferent() {
            DefaultTenantContext context1 = new DefaultTenantContext("tenant-123");
            DefaultTenantContext context2 = new DefaultTenantContext("tenant-456");
            assertThat(context1).isNotEqualTo(context2);
        }

        @Test
        @DisplayName("与 null 比较应返回 false")
        void shouldNotBeEqualToNull() {
            DefaultTenantContext context = new DefaultTenantContext("tenant-123");
            assertThat(context).isNotEqualTo(null);
        }

        @Test
        @DisplayName("与自身比较应返回 true")
        void shouldBeEqualToSelf() {
            DefaultTenantContext context = new DefaultTenantContext("tenant-123");
            assertThat(context).isEqualTo(context);
        }
    }

    @Nested
    @DisplayName("toString 测试")
    class ToStringTests {

        @Test
        @DisplayName("toString 应包含 tenantId")
        void shouldContainTenantId() {
            DefaultTenantContext context = new DefaultTenantContext("tenant-123");
            String str = context.toString();
            assertThat(str).contains("tenantId='tenant-123'");
            assertThat(str).contains("DefaultTenantContext");
        }
    }

    @Nested
    @DisplayName("getAttributes 测试")
    class GetAttributesTests {

        @Test
        @DisplayName("返回的 attributes 应是副本")
        void shouldReturnCopyOfAttributes() {
            DefaultTenantContext context = new DefaultTenantContext("tenant-123");
            context.addAttribute("key", "value");
            Map<String, Object> attrs1 = context.getAttributes();
            Map<String, Object> attrs2 = context.getAttributes();
            assertThat(attrs1).isNotSameAs(attrs2);
            assertThat(attrs1).isEqualTo(attrs2);
        }
    }
}