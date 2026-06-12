package io.github.afgprojects.framework.security.core.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NoOpDeviceLimiter 测试
 */
@DisplayName("NoOpDeviceLimiter 测试")
class NoOpDeviceLimiterTest {

    private NoOpDeviceLimiter deviceLimiter;

    @BeforeEach
    void setUp() {
        deviceLimiter = new NoOpDeviceLimiter();
    }

    @Test
    @DisplayName("registerDevice 应返回 true（总是允许）")
    void shouldAlwaysAllowDeviceRegistration() {
        boolean result = deviceLimiter.registerDevice("user1", "tenant1", "device1", "PC", "Desktop", "127.0.0.1");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("getActiveDevices 应返回空列表")
    void shouldReturnEmptyDeviceList() {
        assertThat(deviceLimiter.getActiveDevices("user1", "tenant1")).isEmpty();
    }

    @Test
    @DisplayName("getActiveDeviceCount 应返回 0")
    void shouldReturnZeroDeviceCount() {
        assertThat(deviceLimiter.getActiveDeviceCount("user1", "tenant1")).isZero();
    }

    @Test
    @DisplayName("kickDevice 应不抛异常")
    void shouldNotThrowOnKickDevice() {
        deviceLimiter.kickDevice("user1", "tenant1", "device1");
    }

    @Test
    @DisplayName("kickAllDevices 应不抛异常")
    void shouldNotThrowOnKickAllDevices() {
        deviceLimiter.kickAllDevices("user1", "tenant1");
    }

    @Test
    @DisplayName("isDeviceActive 应返回 false")
    void shouldReturnFalseOnDeviceActive() {
        assertThat(deviceLimiter.isDeviceActive("user1", "tenant1", "device1")).isFalse();
    }
}
