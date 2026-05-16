package io.github.afgprojects.framework.security.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.afgprojects.framework.security.core.security.DeviceLimiter;
import io.github.afgprojects.framework.security.core.security.model.DeviceInfo;
import io.github.afgprojects.framework.security.core.storage.AfgDeviceStorage;

/**
 * DefaultDeviceLimiter 测试
 */
@DisplayName("DefaultDeviceLimiter 测试")
@ExtendWith(MockitoExtension.class)
class DefaultDeviceLimiterTest {

    private static final int DEFAULT_MAX_DEVICES = 5;

    @Mock
    private AfgDeviceStorage deviceStorage;

    private DefaultDeviceLimiter deviceLimiter;

    @BeforeEach
    void setUp() {
        deviceLimiter = new DefaultDeviceLimiter(deviceStorage, DEFAULT_MAX_DEVICES);
    }

    @Nested
    @DisplayName("注册设备测试")
    class RegisterDeviceTests {

        @Test
        @DisplayName("应该成功注册新设备")
        void shouldRegisterNewDevice() {
            // given
            String userId = "user-123";
            String tenantId = "tenant-001";
            String deviceId = "device-001";
            String deviceName = "iPhone 15";
            String deviceType = "Mobile";
            String ip = "192.168.1.100";

            when(deviceStorage.findById(deviceId)).thenReturn(Optional.empty());
            when(deviceStorage.countActiveByUserId(userId)).thenReturn(0);

            // when
            boolean result = deviceLimiter.registerDevice(userId, tenantId, deviceId, deviceName, deviceType, ip);

            // then
            assertThat(result).isTrue();
            verify(deviceStorage).save(any(DeviceInfo.class));
        }

        @Test
        @DisplayName("应该成功注册已存在的设备（更新）")
        void shouldRegisterExistingDevice() {
            // given
            String userId = "user-123";
            String tenantId = "tenant-001";
            String deviceId = "device-001";
            String deviceName = "iPhone 15 Pro";
            String deviceType = "Mobile";
            String ip = "192.168.1.101";

            DeviceInfo existingDevice = new DeviceInfo();
            existingDevice.setDeviceId(deviceId);
            existingDevice.setUserId(userId);
            existingDevice.setTenantId(tenantId);
            existingDevice.setActive(true);

            when(deviceStorage.findById(deviceId)).thenReturn(Optional.of(existingDevice));

            // when
            boolean result = deviceLimiter.registerDevice(userId, tenantId, deviceId, deviceName, deviceType, ip);

            // then
            assertThat(result).isTrue();
            verify(deviceStorage).save(any(DeviceInfo.class));
            // 不检查设备数量限制，因为设备已存在
            verify(deviceStorage, never()).countActiveByUserId(anyString());
        }

        @Test
        @DisplayName("当达到最大设备数量时应该拒绝注册新设备")
        void shouldRejectNewDeviceWhenMaxReached() {
            // given
            String userId = "user-123";
            String tenantId = "tenant-001";
            String deviceId = "device-new";
            String deviceName = "New Device";
            String deviceType = "PC";
            String ip = "192.168.1.102";

            when(deviceStorage.findById(deviceId)).thenReturn(Optional.empty());
            when(deviceStorage.countActiveByUserId(userId)).thenReturn(DEFAULT_MAX_DEVICES);

            // when
            boolean result = deviceLimiter.registerDevice(userId, tenantId, deviceId, deviceName, deviceType, ip);

            // then
            assertThat(result).isFalse();
            verify(deviceStorage, never()).save(any(DeviceInfo.class));
        }

        @Test
        @DisplayName("应该支持无租户的设备注册")
        void shouldSupportDeviceRegistrationWithoutTenant() {
            // given
            String userId = "user-456";
            String deviceId = "device-002";
            String deviceName = "MacBook Pro";
            String deviceType = "PC";
            String ip = "192.168.1.103";

            when(deviceStorage.findById(deviceId)).thenReturn(Optional.empty());
            when(deviceStorage.countActiveByUserId(userId)).thenReturn(0);

            // when
            boolean result = deviceLimiter.registerDevice(userId, null, deviceId, deviceName, deviceType, ip);

            // then
            assertThat(result).isTrue();
            ArgumentCaptor<DeviceInfo> captor = ArgumentCaptor.forClass(DeviceInfo.class);
            verify(deviceStorage).save(captor.capture());
            assertThat(captor.getValue().getTenantId()).isNull();
        }

        @Test
        @DisplayName("注册设备时应该正确设置设备信息")
        void shouldSetCorrectDeviceInfoWhenRegistering() {
            // given
            String userId = "user-789";
            String tenantId = "tenant-002";
            String deviceId = "device-003";
            String deviceName = "iPad Pro";
            String deviceType = "Tablet";
            String ip = "192.168.1.104";

            when(deviceStorage.findById(deviceId)).thenReturn(Optional.empty());
            when(deviceStorage.countActiveByUserId(userId)).thenReturn(0);

            // when
            deviceLimiter.registerDevice(userId, tenantId, deviceId, deviceName, deviceType, ip);

            // then
            ArgumentCaptor<DeviceInfo> captor = ArgumentCaptor.forClass(DeviceInfo.class);
            verify(deviceStorage).save(captor.capture());
            DeviceInfo savedDevice = captor.getValue();

            assertThat(savedDevice.getDeviceId()).isEqualTo(deviceId);
            assertThat(savedDevice.getUserId()).isEqualTo(userId);
            assertThat(savedDevice.getTenantId()).isEqualTo(tenantId);
            assertThat(savedDevice.getDeviceName()).isEqualTo(deviceName);
            assertThat(savedDevice.getDeviceType()).isEqualTo(deviceType);
            assertThat(savedDevice.getLastLoginIp()).isEqualTo(ip);
            assertThat(savedDevice.getLastLoginTime()).isNotNull();
            assertThat(savedDevice.isActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("获取活跃设备测试")
    class GetActiveDevicesTests {

        @Test
        @DisplayName("应该返回用户活跃设备列表")
        void shouldReturnActiveDevices() {
            // given
            String userId = "user-123";
            String tenantId = "tenant-001";

            DeviceInfo device1 = createDeviceInfo("device-001", userId, tenantId, "iPhone", "Mobile", "192.168.1.100", true);
            DeviceInfo device2 = createDeviceInfo("device-002", userId, tenantId, "MacBook", "PC", "192.168.1.101", true);
            DeviceInfo device3 = createDeviceInfo("device-003", userId, tenantId, "iPad", "Tablet", "192.168.1.102", false);

            when(deviceStorage.findByUserId(userId)).thenReturn(List.of(device1, device2, device3));

            // when
            List<DeviceLimiter.DeviceInfo> activeDevices = deviceLimiter.getActiveDevices(userId, tenantId);

            // then
            assertThat(activeDevices).hasSize(2);
            assertThat(activeDevices).extracting("deviceId").containsExactlyInAnyOrder("device-001", "device-002");
        }

        @Test
        @DisplayName("当用户无设备时应该返回空列表")
        void shouldReturnEmptyListWhenNoDevices() {
            // given
            String userId = "user-456";
            String tenantId = "tenant-002";

            when(deviceStorage.findByUserId(userId)).thenReturn(List.of());

            // when
            List<DeviceLimiter.DeviceInfo> activeDevices = deviceLimiter.getActiveDevices(userId, tenantId);

            // then
            assertThat(activeDevices).isEmpty();
        }

        @Test
        @DisplayName("应该支持无租户查询活跃设备")
        void shouldSupportGetActiveDevicesWithoutTenant() {
            // given
            String userId = "user-789";

            DeviceInfo device1 = createDeviceInfo("device-001", userId, null, "iPhone", "Mobile", "192.168.1.100", true);

            when(deviceStorage.findByUserId(userId)).thenReturn(List.of(device1));

            // when
            List<DeviceLimiter.DeviceInfo> activeDevices = deviceLimiter.getActiveDevices(userId, null);

            // then
            assertThat(activeDevices).hasSize(1);
        }
    }

    @Nested
    @DisplayName("获取活跃设备数量测试")
    class GetActiveDeviceCountTests {

        @Test
        @DisplayName("应该返回正确的活跃设备数量")
        void shouldReturnCorrectActiveDeviceCount() {
            // given
            String userId = "user-123";
            String tenantId = "tenant-001";

            when(deviceStorage.countActiveByUserId(userId)).thenReturn(3);

            // when
            int count = deviceLimiter.getActiveDeviceCount(userId, tenantId);

            // then
            assertThat(count).isEqualTo(3);
        }

        @Test
        @DisplayName("当用户无活跃设备时应该返回零")
        void shouldReturnZeroWhenNoActiveDevices() {
            // given
            String userId = "user-456";
            String tenantId = "tenant-002";

            when(deviceStorage.countActiveByUserId(userId)).thenReturn(0);

            // when
            int count = deviceLimiter.getActiveDeviceCount(userId, tenantId);

            // then
            assertThat(count).isZero();
        }
    }

    @Nested
    @DisplayName("踢出设备测试")
    class KickDeviceTests {

        @Test
        @DisplayName("应该成功踢出指定设备")
        void shouldKickDevice() {
            // given
            String userId = "user-123";
            String tenantId = "tenant-001";
            String deviceId = "device-001";

            // when
            deviceLimiter.kickDevice(userId, tenantId, deviceId);

            // then
            verify(deviceStorage).updateActiveStatus(deviceId, false);
        }

        @Test
        @DisplayName("应该支持无租户踢出设备")
        void shouldSupportKickDeviceWithoutTenant() {
            // given
            String userId = "user-456";
            String deviceId = "device-002";

            // when
            deviceLimiter.kickDevice(userId, null, deviceId);

            // then
            verify(deviceStorage).updateActiveStatus(deviceId, false);
        }
    }

    @Nested
    @DisplayName("踢出所有设备测试")
    class KickAllDevicesTests {

        @Test
        @DisplayName("应该成功踢出用户所有设备")
        void shouldKickAllDevices() {
            // given
            String userId = "user-123";
            String tenantId = "tenant-001";

            DeviceInfo device1 = createDeviceInfo("device-001", userId, tenantId, "iPhone", "Mobile", "192.168.1.100", true);
            DeviceInfo device2 = createDeviceInfo("device-002", userId, tenantId, "MacBook", "PC", "192.168.1.101", true);

            when(deviceStorage.findByUserId(userId)).thenReturn(List.of(device1, device2));

            // when
            deviceLimiter.kickAllDevices(userId, tenantId);

            // then
            verify(deviceStorage).updateActiveStatus("device-001", false);
            verify(deviceStorage).updateActiveStatus("device-002", false);
        }

        @Test
        @DisplayName("当用户无设备时应该正常处理")
        void shouldHandleNoDevicesWhenKickingAll() {
            // given
            String userId = "user-456";
            String tenantId = "tenant-002";

            when(deviceStorage.findByUserId(userId)).thenReturn(List.of());

            // when
            deviceLimiter.kickAllDevices(userId, tenantId);

            // then
            verify(deviceStorage, never()).updateActiveStatus(anyString(), anyBoolean());
        }

        @Test
        @DisplayName("应该支持无租户踢出所有设备")
        void shouldSupportKickAllDevicesWithoutTenant() {
            // given
            String userId = "user-789";

            DeviceInfo device1 = createDeviceInfo("device-001", userId, null, "iPhone", "Mobile", "192.168.1.100", true);

            when(deviceStorage.findByUserId(userId)).thenReturn(List.of(device1));

            // when
            deviceLimiter.kickAllDevices(userId, null);

            // then
            verify(deviceStorage).updateActiveStatus("device-001", false);
        }
    }

    @Nested
    @DisplayName("检查设备活跃状态测试")
    class IsDeviceActiveTests {

        @Test
        @DisplayName("活跃设备应该返回 true")
        void shouldReturnTrueForActiveDevice() {
            // given
            String userId = "user-123";
            String tenantId = "tenant-001";
            String deviceId = "device-001";

            DeviceInfo device = createDeviceInfo(deviceId, userId, tenantId, "iPhone", "Mobile", "192.168.1.100", true);

            when(deviceStorage.findById(deviceId)).thenReturn(Optional.of(device));

            // when
            boolean isActive = deviceLimiter.isDeviceActive(userId, tenantId, deviceId);

            // then
            assertThat(isActive).isTrue();
        }

        @Test
        @DisplayName("非活跃设备应该返回 false")
        void shouldReturnFalseForInactiveDevice() {
            // given
            String userId = "user-123";
            String tenantId = "tenant-001";
            String deviceId = "device-001";

            DeviceInfo device = createDeviceInfo(deviceId, userId, tenantId, "iPhone", "Mobile", "192.168.1.100", false);

            when(deviceStorage.findById(deviceId)).thenReturn(Optional.of(device));

            // when
            boolean isActive = deviceLimiter.isDeviceActive(userId, tenantId, deviceId);

            // then
            assertThat(isActive).isFalse();
        }

        @Test
        @DisplayName("不存在的设备应该返回 false")
        void shouldReturnFalseForNonExistentDevice() {
            // given
            String userId = "user-123";
            String tenantId = "tenant-001";
            String deviceId = "device-nonexistent";

            when(deviceStorage.findById(deviceId)).thenReturn(Optional.empty());

            // when
            boolean isActive = deviceLimiter.isDeviceActive(userId, tenantId, deviceId);

            // then
            assertThat(isActive).isFalse();
        }

        @Test
        @DisplayName("用户 ID 不匹配的设备应该返回 false")
        void shouldReturnFalseForDeviceWithDifferentUser() {
            // given
            String userId = "user-123";
            String tenantId = "tenant-001";
            String deviceId = "device-001";

            DeviceInfo device = createDeviceInfo(deviceId, "user-456", tenantId, "iPhone", "Mobile", "192.168.1.100", true);

            when(deviceStorage.findById(deviceId)).thenReturn(Optional.of(device));

            // when
            boolean isActive = deviceLimiter.isDeviceActive(userId, tenantId, deviceId);

            // then
            assertThat(isActive).isFalse();
        }

        @Test
        @DisplayName("应该支持无租户检查设备活跃状态")
        void shouldSupportIsDeviceActiveWithoutTenant() {
            // given
            String userId = "user-789";
            String deviceId = "device-001";

            DeviceInfo device = createDeviceInfo(deviceId, userId, null, "iPhone", "Mobile", "192.168.1.100", true);

            when(deviceStorage.findById(deviceId)).thenReturn(Optional.of(device));

            // when
            boolean isActive = deviceLimiter.isDeviceActive(userId, null, deviceId);

            // then
            assertThat(isActive).isTrue();
        }
    }

    @Nested
    @DisplayName("最大设备数量配置测试")
    class MaxDevicesConfigTests {

        @Test
        @DisplayName("应该使用自定义的最大设备数量")
        void shouldUseCustomMaxDevices() {
            // given
            int customMaxDevices = 3;
            DefaultDeviceLimiter customLimiter = new DefaultDeviceLimiter(deviceStorage, customMaxDevices);

            String userId = "user-123";
            String tenantId = "tenant-001";
            String deviceId = "device-new";
            String ip = "192.168.1.100";

            when(deviceStorage.findById(deviceId)).thenReturn(Optional.empty());
            when(deviceStorage.countActiveByUserId(userId)).thenReturn(customMaxDevices);

            // when
            boolean result = customLimiter.registerDevice(userId, tenantId, deviceId, "New Device", "PC", ip);

            // then
            assertThat(result).isFalse();
        }
    }

    /**
     * 创建测试用的 DeviceInfo
     */
    private DeviceInfo createDeviceInfo(
            String deviceId,
            String userId,
            String tenantId,
            String deviceName,
            String deviceType,
            String ip,
            boolean active) {
        DeviceInfo device = new DeviceInfo();
        device.setDeviceId(deviceId);
        device.setUserId(userId);
        device.setTenantId(tenantId);
        device.setDeviceName(deviceName);
        device.setDeviceType(deviceType);
        device.setLastLoginIp(ip);
        device.setLastLoginTime(Instant.now());
        device.setActive(active);
        return device;
    }
}
