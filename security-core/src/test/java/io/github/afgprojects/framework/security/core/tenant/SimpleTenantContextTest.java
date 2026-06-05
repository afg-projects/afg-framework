package io.github.afgprojects.framework.security.core.tenant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SimpleTenantContext 测试
 */
@DisplayName("SimpleTenantContext 测试")
class SimpleTenantContextTest {

    @Nested
    @DisplayName("构造函数")
    class ConstructorTests {

        @Test
        @DisplayName("应正确创建简单租户上下文")
        void shouldCreateSimpleTenantContext() {
            SimpleTenantContext context = new SimpleTenantContext("tenant-001");

            assertThat(context.getTenantId()).isEqualTo("tenant-001");
        }

        @Test
        @DisplayName("tenantId 为 null 应抛出 NullPointerException")
        void shouldThrowWhenTenantIdIsNull() {
            assertThatThrownBy(() -> new SimpleTenantContext(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("tenantId");
        }
    }

    @Nested
    @DisplayName("TenantContext 接口默认方法")
    class DefaultMethodTests {

        @Test
        @DisplayName("getTenantCode 应返回 tenantId")
        void shouldReturnTenantIdAsTenantCode() {
            SimpleTenantContext context = new SimpleTenantContext("tenant-001");

            assertThat(context.getTenantCode()).isEqualTo("tenant-001");
        }

        @Test
        @DisplayName("getTenantName 应返回 null")
        void shouldReturnNullTenantName() {
            SimpleTenantContext context = new SimpleTenantContext("tenant-001");

            assertThat(context.getTenantName()).isNull();
        }

        @Test
        @DisplayName("getAttributes 应返回空 Map")
        void shouldReturnEmptyAttributes() {
            SimpleTenantContext context = new SimpleTenantContext("tenant-001");

            assertThat(context.getAttributes()).isEmpty();
        }

        @Test
        @DisplayName("isDefault 应返回 false")
        void shouldReturnFalseForIsDefault() {
            SimpleTenantContext context = new SimpleTenantContext("tenant-001");

            assertThat(context.isDefault()).isFalse();
        }

        @Test
        @DisplayName("isValid 应返回 true")
        void shouldReturnTrueForIsValid() {
            SimpleTenantContext context = new SimpleTenantContext("tenant-001");

            assertThat(context.isValid()).isTrue();
        }
    }

    @Nested
    @DisplayName("equals 和 hashCode")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("相同 tenantId 应相等")
        void shouldBeEqualWithSameTenantId() {
            SimpleTenantContext context1 = new SimpleTenantContext("tenant-001");
            SimpleTenantContext context2 = new SimpleTenantContext("tenant-001");

            assertThat(context1).isEqualTo(context2);
            assertThat(context1).hasSameHashCodeAs(context2);
        }

        @Test
        @DisplayName("不同 tenantId 应不相等")
        void shouldNotBeEqualWithDifferentTenantId() {
            SimpleTenantContext context1 = new SimpleTenantContext("tenant-001");
            SimpleTenantContext context2 = new SimpleTenantContext("tenant-002");

            assertThat(context1).isNotEqualTo(context2);
        }

        @Test
        @DisplayName("与 null 比较应返回 false")
        void shouldNotBeEqualToNull() {
            SimpleTenantContext context = new SimpleTenantContext("tenant-001");

            assertThat(context).isNotEqualTo(null);
        }

        @Test
        @DisplayName("与不同类型比较应返回 false")
        void shouldNotBeEqualToDifferentType() {
            SimpleTenantContext context = new SimpleTenantContext("tenant-001");

            assertThat(context).isNotEqualTo("tenant-001");
        }

        @Test
        @DisplayName("与自身比较应返回 true")
        void shouldBeEqualToItself() {
            SimpleTenantContext context = new SimpleTenantContext("tenant-001");

            assertThat(context).isEqualTo(context);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        @DisplayName("应包含 tenantId")
        void shouldContainTenantId() {
            SimpleTenantContext context = new SimpleTenantContext("tenant-001");

            String str = context.toString();

            assertThat(str).contains("SimpleTenantContext");
            assertThat(str).contains("tenant-001");
        }
    }
}
