package io.github.afgprojects.framework.security.core.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NoOpIpRestrictionChecker 测试
 */
@DisplayName("NoOpIpRestrictionChecker 测试")
class NoOpIpRestrictionCheckerTest {

    private NoOpIpRestrictionChecker ipChecker;

    @BeforeEach
    void setUp() {
        ipChecker = new NoOpIpRestrictionChecker();
    }

    @Test
    @DisplayName("isAllowed 应返回 true（总是允许）")
    void shouldAlwaysAllowAccess() {
        assertThat(ipChecker.isAllowed("192.168.1.1", "user1", "tenant1")).isTrue();
    }

    @Test
    @DisplayName("isBlacklisted 应返回 false")
    void shouldReturnFalseOnBlacklisted() {
        assertThat(ipChecker.isBlacklisted("192.168.1.1")).isFalse();
    }

    @Test
    @DisplayName("isWhitelisted 应返回 false")
    void shouldReturnFalseOnWhitelisted() {
        assertThat(ipChecker.isWhitelisted("192.168.1.1")).isFalse();
    }
}
