package io.github.afgprojects.framework.security.core.tenant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TenantStatus 测试
 */
@DisplayName("TenantStatus 测试")
class TenantStatusTest {

    @Nested
    @DisplayName("枚举值")
    class EnumValueTests {

        @Test
        @DisplayName("应包含所有状态")
        void shouldContainAllStatuses() {
            TenantStatus[] statuses = TenantStatus.values();

            assertThat(statuses).hasSize(3);
            assertThat(statuses).containsExactlyInAnyOrder(
                    TenantStatus.ACTIVE,
                    TenantStatus.SUSPENDED,
                    TenantStatus.DISABLED
            );
        }
    }

    @Nested
    @DisplayName("isActive")
    class IsActiveTests {

        @Test
        @DisplayName("ACTIVE 状态应为活跃")
        void shouldBeActiveForActiveStatus() {
            assertThat(TenantStatus.ACTIVE.isActive()).isTrue();
        }

        @Test
        @DisplayName("SUSPENDED 状态应为非活跃")
        void shouldNotBeActiveForSuspendedStatus() {
            assertThat(TenantStatus.SUSPENDED.isActive()).isFalse();
        }

        @Test
        @DisplayName("DISABLED 状态应为非活跃")
        void shouldNotBeActiveForDisabledStatus() {
            assertThat(TenantStatus.DISABLED.isActive()).isFalse();
        }
    }
}
