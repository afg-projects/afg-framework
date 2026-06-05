package io.github.afgprojects.framework.security.core.security.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DeviceInfo 测试
 */
@DisplayName("DeviceInfo 测试")
class DeviceInfoTest {

    @Nested
    @DisplayName("Getter 和 Setter")
    class GetterSetterTests {

        @Test
        @DisplayName("应正确设置和获取 deviceId")
        void shouldSetAndGetDeviceId() {
            DeviceInfo device = new DeviceInfo();
            device.setDeviceId("device-001");

            assertThat(device.getDeviceId()).isEqualTo("device-001");
        }

        @Test
        @DisplayName("应正确设置和获取 userId")
        void shouldSetAndGetUserId() {
            DeviceInfo device = new DeviceInfo();
            device.setUserId("user-001");

            assertThat(device.getUserId()).isEqualTo("user-001");
        }

        @Test
        @DisplayName("应正确设置和获取 tenantId")
        void shouldSetAndGetTenantId() {
            DeviceInfo device = new DeviceInfo();
            device.setTenantId("tenant-001");

            assertThat(device.getTenantId()).isEqualTo("tenant-001");
        }

        @Test
        @DisplayName("应正确设置和获取 deviceName")
        void shouldSetAndGetDeviceName() {
            DeviceInfo device = new DeviceInfo();
            device.setDeviceName("iPhone 15");

            assertThat(device.getDeviceName()).isEqualTo("iPhone 15");
        }

        @Test
        @DisplayName("应正确设置和获取 deviceType")
        void shouldSetAndGetDeviceType() {
            DeviceInfo device = new DeviceInfo();
            device.setDeviceType("Mobile");

            assertThat(device.getDeviceType()).isEqualTo("Mobile");
        }

        @Test
        @DisplayName("应正确设置和获取 lastLoginIp")
        void shouldSetAndGetLastLoginIp() {
            DeviceInfo device = new DeviceInfo();
            device.setLastLoginIp("192.168.1.100");

            assertThat(device.getLastLoginIp()).isEqualTo("192.168.1.100");
        }

        @Test
        @DisplayName("应正确设置和获取 lastLoginTime")
        void shouldSetAndGetLastLoginTime() {
            DeviceInfo device = new DeviceInfo();
            Instant now = Instant.now();
            device.setLastLoginTime(now);

            assertThat(device.getLastLoginTime()).isEqualTo(now);
        }

        @Test
        @DisplayName("应正确设置和获取 active")
        void shouldSetAndGetActive() {
            DeviceInfo device = new DeviceInfo();

            assertThat(device.isActive()).isTrue(); // 默认值

            device.setActive(false);
            assertThat(device.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("默认值")
    class DefaultValueTests {

        @Test
        @DisplayName("新实例 active 默认为 true")
        void shouldHaveDefaultActiveTrue() {
            DeviceInfo device = new DeviceInfo();

            assertThat(device.isActive()).isTrue();
        }
    }
}
