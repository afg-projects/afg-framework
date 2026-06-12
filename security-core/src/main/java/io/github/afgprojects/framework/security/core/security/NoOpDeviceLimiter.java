package io.github.afgprojects.framework.security.core.security;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * NoOp 设备限制降级实现。
 * <p>
 * 总是允许设备注册（不限制设备数量）。
 *
 * @since 1.0.0
 */
public class NoOpDeviceLimiter implements DeviceLimiter {

    @Override
    public boolean registerDevice(String userId, @Nullable String tenantId, String deviceId,
            @Nullable String deviceName, @Nullable String deviceType, String ip) {
        return true;
    }

    @Override
    public List<DeviceInfo> getActiveDevices(String userId, @Nullable String tenantId) {
        return List.of();
    }

    @Override
    public int getActiveDeviceCount(String userId, @Nullable String tenantId) {
        return 0;
    }

    @Override
    public void kickDevice(String userId, @Nullable String tenantId, String deviceId) {
        // no-op
    }

    @Override
    public void kickAllDevices(String userId, @Nullable String tenantId) {
        // no-op
    }

    @Override
    public boolean isDeviceActive(String userId, @Nullable String tenantId, String deviceId) {
        return false;
    }
}
