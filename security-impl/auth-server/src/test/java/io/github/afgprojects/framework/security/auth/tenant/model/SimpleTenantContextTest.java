package io.github.afgprojects.framework.security.auth.tenant.model;

import io.github.afgprojects.framework.security.core.tenant.TenantContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SimpleTenantContext 测试类
 */
@DisplayName("SimpleTenantContext 测试")
class SimpleTenantContextTest {

    @Nested
    @DisplayName("构造方法测试")
    class ConstructorTests {

        @Test
        @DisplayName("使用 tenantId 构造")
        void shouldCreateWithTenantId() {
            // Given
            String tenantId = "tenant-001";

            // When
            SimpleTenantContext context = new SimpleTenantContext(tenantId);

            // Then
            assertThat(context.getTenantId()).isEqualTo(tenantId);
        }

        @Test
        @DisplayName("tenantId 不能为 null")
        void shouldThrowExceptionWhenTenantIdIsNull() {
            // When & Then
            assertThatThrownBy(() -> new SimpleTenantContext(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("tenantId");
        }
    }

    @Nested
    @DisplayName("TenantContext 接口方法测试")
    class InterfaceMethodTests {

        @Test
        @DisplayName("getTenantId() 返回构造时传入的值")
        void shouldReturnTenantId() {
            // Given
            String tenantId = "tenant-123";
            SimpleTenantContext context = new SimpleTenantContext(tenantId);

            // When
            String result = context.getTenantId();

            // Then
            assertThat(result).isEqualTo(tenantId);
        }

        @Test
        @DisplayName("getTenantCode() 默认返回 tenantId")
        void shouldReturnTenantIdAsCode() {
            // Given
            String tenantId = "tenant-456";
            SimpleTenantContext context = new SimpleTenantContext(tenantId);

            // When
            String code = context.getTenantCode();

            // Then
            assertThat(code).isEqualTo(tenantId);
        }

        @Test
        @DisplayName("getTenantName() 默认返回 null")
        void shouldReturnNullName() {
            // Given
            SimpleTenantContext context = new SimpleTenantContext("tenant-001");

            // When
            String name = context.getTenantName();

            // Then
            assertThat(name).isNull();
        }

        @Test
        @DisplayName("getAttributes() 默认返回空 Map")
        void shouldReturnEmptyAttributes() {
            // Given
            SimpleTenantContext context = new SimpleTenantContext("tenant-001");

            // When
            var attributes = context.getAttributes();

            // Then
            assertThat(attributes).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("isDefault() 默认返回 false")
        void shouldReturnFalseForDefault() {
            // Given
            SimpleTenantContext context = new SimpleTenantContext("tenant-001");

            // When
            boolean isDefault = context.isDefault();

            // Then
            assertThat(isDefault).isFalse();
        }

        @Test
        @DisplayName("isValid() 返回 true")
        void shouldReturnTrueForValid() {
            // Given
            SimpleTenantContext context = new SimpleTenantContext("tenant-001");

            // When
            boolean isValid = context.isValid();

            // Then
            assertThat(isValid).isTrue();
        }
    }

    @Nested
    @DisplayName("equals 和 hashCode 测试")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("相同 tenantId 的对象应该相等")
        void shouldBeEqualForSameTenantId() {
            // Given
            SimpleTenantContext context1 = new SimpleTenantContext("tenant-001");
            SimpleTenantContext context2 = new SimpleTenantContext("tenant-001");

            // When & Then
            assertThat(context1).isEqualTo(context2);
            assertThat(context1.hashCode()).isEqualTo(context2.hashCode());
        }

        @Test
        @DisplayName("不同 tenantId 的对象应该不相等")
        void shouldNotBeEqualForDifferentTenantId() {
            // Given
            SimpleTenantContext context1 = new SimpleTenantContext("tenant-001");
            SimpleTenantContext context2 = new SimpleTenantContext("tenant-002");

            // When & Then
            assertThat(context1).isNotEqualTo(context2);
        }

        @Test
        @DisplayName("与 null 比较应该返回 false")
        void shouldNotBeEqualToNull() {
            // Given
            SimpleTenantContext context = new SimpleTenantContext("tenant-001");

            // When & Then
            assertThat(context).isNotEqualTo(null);
        }

        @Test
        @DisplayName("与不同类型比较应该返回 false")
        void shouldNotBeEqualToDifferentType() {
            // Given
            SimpleTenantContext context = new SimpleTenantContext("tenant-001");
            String other = "tenant-001";

            // When & Then
            assertThat(context).isNotEqualTo(other);
        }

        @Test
        @DisplayName("与自己比较应该返回 true")
        void shouldBeEqualToItself() {
            // Given
            SimpleTenantContext context = new SimpleTenantContext("tenant-001");

            // When & Then
            assertThat(context).isEqualTo(context);
        }
    }

    @Nested
    @DisplayName("toString 测试")
    class ToStringTests {

        @Test
        @DisplayName("toString 应该包含 tenantId")
        void shouldContainTenantId() {
            // Given
            String tenantId = "tenant-001";
            SimpleTenantContext context = new SimpleTenantContext(tenantId);

            // When
            String str = context.toString();

            // Then
            assertThat(str).contains(tenantId);
        }
    }

    @Nested
    @DisplayName("类型检查测试")
    class TypeCheckTests {

        @Test
        @DisplayName("应该实现 TenantContext 接口")
        void shouldImplementTenantContext() {
            // Given
            SimpleTenantContext context = new SimpleTenantContext("tenant-001");

            // When & Then
            assertThat(context).isInstanceOf(TenantContext.class);
        }
    }
}
