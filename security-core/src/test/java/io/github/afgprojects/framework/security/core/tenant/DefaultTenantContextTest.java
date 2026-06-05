package io.github.afgprojects.framework.security.core.tenant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DefaultTenantContext 测试
 */
@DisplayName("DefaultTenantContext 测试")
class DefaultTenantContextTest {

    @Nested
    @DisplayName("构造函数")
    class ConstructorTests {

        @Test
        @DisplayName("单参数构造函数应正确创建")
        void shouldCreateWithTenantIdOnly() {
            DefaultTenantContext context = new DefaultTenantContext("tenant-001");

            assertThat(context.getTenantId()).isEqualTo("tenant-001");
            assertThat(context.getTenantCode()).isEqualTo("tenant-001");
            assertThat(context.getTenantName()).isNull();
            assertThat(context.getAttributes()).isEmpty();
            assertThat(context.isDefault()).isFalse();
            assertThat(context.isValid()).isTrue();
        }

        @Test
        @DisplayName("三参数构造函数应正确创建")
        void shouldCreateWithTenantIdCodeAndName() {
            DefaultTenantContext context = new DefaultTenantContext("tenant-001", "acme", "ACME Corp");

            assertThat(context.getTenantId()).isEqualTo("tenant-001");
            assertThat(context.getTenantCode()).isEqualTo("acme");
            assertThat(context.getTenantName()).isEqualTo("ACME Corp");
            assertThat(context.getAttributes()).isEmpty();
            assertThat(context.isDefault()).isFalse();
            assertThat(context.isValid()).isTrue();
        }

        @Test
        @DisplayName("全参数构造函数应正确创建")
        void shouldCreateWithAllParameters() {
            Map<String, Object> attrs = new HashMap<>();
            attrs.put("type", "enterprise");
            attrs.put("maxUsers", 100);

            DefaultTenantContext context = new DefaultTenantContext(
                    "tenant-001", "acme", "ACME Corp", attrs, true, true
            );

            assertThat(context.getTenantId()).isEqualTo("tenant-001");
            assertThat(context.getTenantCode()).isEqualTo("acme");
            assertThat(context.getTenantName()).isEqualTo("ACME Corp");
            assertThat(context.getAttributes()).containsEntry("type", "enterprise");
            assertThat(context.getAttributes()).containsEntry("maxUsers", 100);
            assertThat(context.isDefault()).isTrue();
            assertThat(context.isValid()).isTrue();
        }

        @Test
        @DisplayName("全参数构造函数 null attributes 应使用空 Map")
        void shouldUseEmptyMapWhenAttributesIsNull() {
            DefaultTenantContext context = new DefaultTenantContext(
                    "tenant-001", "acme", "ACME Corp", null, false, true
            );

            assertThat(context.getAttributes()).isEmpty();
        }

        @Test
        @DisplayName("tenantId 为 null 应抛出 NullPointerException")
        void shouldThrowWhenTenantIdIsNull() {
            assertThatThrownBy(() -> new DefaultTenantContext(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("tenantId");
        }

        @Test
        @DisplayName("无效租户应 isValid 返回 false")
        void shouldReturnFalseForIsValidWhenInvalid() {
            DefaultTenantContext context = new DefaultTenantContext(
                    "tenant-001", "acme", "ACME Corp", null, false, false
            );

            assertThat(context.isValid()).isFalse();
        }
    }

    @Nested
    @DisplayName("getTenantCode")
    class GetTenantCodeTests {

        @Test
        @DisplayName("有 tenantCode 时应返回 tenantCode")
        void shouldReturnTenantCodeWhenSet() {
            DefaultTenantContext context = new DefaultTenantContext("tenant-001", "acme", "ACME Corp");

            assertThat(context.getTenantCode()).isEqualTo("acme");
        }

        @Test
        @DisplayName("无 tenantCode 时应返回 tenantId")
        void shouldReturnTenantIdWhenTenantCodeIsNull() {
            DefaultTenantContext context = new DefaultTenantContext("tenant-001");

            assertThat(context.getTenantCode()).isEqualTo("tenant-001");
        }
    }

    @Nested
    @DisplayName("addAttribute")
    class AddAttributeTests {

        @Test
        @DisplayName("应正确添加属性")
        void shouldAddAttribute() {
            DefaultTenantContext context = new DefaultTenantContext("tenant-001");
            DefaultTenantContext returned = context.addAttribute("type", "enterprise");

            assertThat(context.getAttributes()).containsEntry("type", "enterprise");
            assertThat(returned).isSameAs(context); // 链式调用
        }

        @Test
        @DisplayName("应支持添加多个属性")
        void shouldAddMultipleAttributes() {
            DefaultTenantContext context = new DefaultTenantContext("tenant-001");
            context.addAttribute("type", "enterprise");
            context.addAttribute("maxUsers", 100);

            assertThat(context.getAttributes()).hasSize(2);
            assertThat(context.getAttributes()).containsEntry("type", "enterprise");
            assertThat(context.getAttributes()).containsEntry("maxUsers", 100);
        }
    }

    @Nested
    @DisplayName("不可变性")
    class ImmutabilityTests {

        @Test
        @DisplayName("getAttributes 应返回防御性拷贝")
        void shouldReturnDefensiveCopyOfAttributes() {
            Map<String, Object> attrs = new HashMap<>();
            attrs.put("type", "enterprise");
            DefaultTenantContext context = new DefaultTenantContext(
                    "tenant-001", "acme", "ACME Corp", attrs, false, true
            );

            // 修改返回的 Map 不应影响原始数据
            context.getAttributes().put("hack", "value");

            assertThat(context.getAttributes()).doesNotContainKey("hack");
        }

        @Test
        @DisplayName("修改原始 attributes 不应影响已创建的上下文")
        void shouldNotBeAffectedByOriginalMapModification() {
            Map<String, Object> attrs = new HashMap<>();
            attrs.put("type", "enterprise");
            DefaultTenantContext context = new DefaultTenantContext(
                    "tenant-001", "acme", "ACME Corp", attrs, false, true
            );

            // 修改原始 Map 不应影响上下文
            attrs.put("newKey", "newValue");

            assertThat(context.getAttributes()).doesNotContainKey("newKey");
        }
    }

    @Nested
    @DisplayName("equals 和 hashCode")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("相同 tenantId 应相等")
        void shouldBeEqualWithSameTenantId() {
            DefaultTenantContext context1 = new DefaultTenantContext("tenant-001", "acme", "ACME");
            DefaultTenantContext context2 = new DefaultTenantContext("tenant-001", "other", "Other");

            assertThat(context1).isEqualTo(context2);
            assertThat(context1).hasSameHashCodeAs(context2);
        }

        @Test
        @DisplayName("不同 tenantId 应不相等")
        void shouldNotBeEqualWithDifferentTenantId() {
            DefaultTenantContext context1 = new DefaultTenantContext("tenant-001");
            DefaultTenantContext context2 = new DefaultTenantContext("tenant-002");

            assertThat(context1).isNotEqualTo(context2);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        @DisplayName("应包含关键信息")
        void shouldContainKeyInfo() {
            DefaultTenantContext context = new DefaultTenantContext(
                    "tenant-001", "acme", "ACME Corp", null, true, true
            );

            String str = context.toString();

            assertThat(str).contains("DefaultTenantContext");
            assertThat(str).contains("tenant-001");
            assertThat(str).contains("acme");
            assertThat(str).contains("ACME Corp");
            assertThat(str).contains("defaultTenant=true");
            assertThat(str).contains("valid=true");
        }
    }
}
