package io.github.afgprojects.framework.security.core.tenant;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TenantStatusTest {

    @Test
    void shouldHaveThreeStatuses() {
        TenantStatus[] statuses = TenantStatus.values();
        assertThat(statuses).hasSize(3);
        assertThat(statuses).containsExactlyInAnyOrder(
            TenantStatus.ACTIVE,
            TenantStatus.SUSPENDED,
            TenantStatus.DISABLED
        );
    }

    @Test
    void activeShouldBeValid() {
        assertThat(TenantStatus.ACTIVE.isActive()).isTrue();
        assertThat(TenantStatus.SUSPENDED.isActive()).isFalse();
        assertThat(TenantStatus.DISABLED.isActive()).isFalse();
    }
}
