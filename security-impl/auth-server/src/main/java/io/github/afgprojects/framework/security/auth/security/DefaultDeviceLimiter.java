package io.github.afgprojects.framework.security.auth.security;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.afgprojects.framework.security.core.security.DeviceLimiter;
import io.github.afgprojects.framework.security.core.storage.AfgDeviceStorage;

/**
 * 默认设备限制器实现。
 *
 * <p>基于 AfgDeviceStorage 实现 DeviceLimiter 接口，提供设备管理功能。
 *
 * <h3>功能特性</h3>
 * <ul>
 *   <li>设备注册：记录用户登录设备信息</li>
 *   <li>设备数量限制：限制用户同时登录的设备数量</li>
 *   <li>设备踢出：强制指定设备下线</li>
 *   <li>批量踢出：强制用户所有设备下线</li>
 *   <li>设备状态查询：检查设备是否处于活跃状态</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 创建设备限制器
 * DefaultDeviceLimiter deviceLimiter = new DefaultDeviceLimiter(deviceStorage, 5);
 *
 * // 注册设备
 * boolean registered = deviceLimiter.registerDevice(
 *     "user-123",
 *     "tenant-001",
 *     "device-001",
 *     "iPhone 15",
 *     "Mobile",
 *     "192.168.1.100"
 * );
 *
 * // 获取活跃设备列表
 * List<DeviceLimiter.DeviceInfo> devices = deviceLimiter.getActiveDevices("user-123", "tenant-001");
 *
 * // 踢出设备
 * deviceLimiter.kickDevice("user-123", "tenant-001", "device-001");
 * }</pre>
 *
 * @since 1.0.0
 */
public class DefaultDeviceLimiter implements DeviceLimiter {

    private static final Logger log = LoggerFactory.getLogger(DefaultDeviceLimiter.class);

    private final AfgDeviceStorage deviceStorage;
    private final int maxDevices;

    /**
     * 构造函数。
     *
     * @param deviceStorage 设备存储，永不为 null
     * @param maxDevices     最大设备数量，必须大于 0
     * @throws IllegalArgumentException 如果参数无效
     */
    public DefaultDeviceLimiter(@NonNull AfgDeviceStorage deviceStorage, int maxDevices) {
        if (maxDevices <= 0) {
            throw new IllegalArgumentException("maxDevices must be greater than 0");
        }

        this.deviceStorage = deviceStorage;
        this.maxDevices = maxDevices;
    }

    @Override
    public boolean registerDevice(
            @NonNull String userId,
            @Nullable String tenantId,
            @NonNull String deviceId,
            @Nullable String deviceName,
            @Nullable String deviceType,
            @NonNull String ip) {

        log.debug("Registering device {} for user {}", deviceId, userId);

        // 检查设备是否已存在
        Optional<io.github.afgprojects.framework.security.core.security.model.DeviceInfo> existingDevice =
                deviceStorage.findById(deviceId);

        if (existingDevice.isPresent()) {
            // 设备已存在，更新设备信息
            io.github.afgprojects.framework.security.core.security.model.DeviceInfo device = existingDevice.get();
            device.setUserId(userId);
            device.setTenantId(tenantId);
            device.setDeviceName(deviceName);
            device.setDeviceType(deviceType);
            device.setLastLoginIp(ip);
            device.setLastLoginTime(Instant.now());
            device.setActive(true);

            deviceStorage.save(device);
            log.debug("Updated existing device {} for user {}", deviceId, userId);
            return true;
        }

        // 新设备，检查设备数量限制
        int activeCount = deviceStorage.countActiveByUserId(userId);
        if (activeCount >= maxDevices) {
            log.warn("User {} has reached max device limit {}, rejecting new device {}", userId, maxDevices, deviceId);
            return false;
        }

        // 创建新设备
        io.github.afgprojects.framework.security.core.security.model.DeviceInfo newDevice =
                new io.github.afgprojects.framework.security.core.security.model.DeviceInfo();
        newDevice.setDeviceId(deviceId);
        newDevice.setUserId(userId);
        newDevice.setTenantId(tenantId);
        newDevice.setDeviceName(deviceName);
        newDevice.setDeviceType(deviceType);
        newDevice.setLastLoginIp(ip);
        newDevice.setLastLoginTime(Instant.now());
        newDevice.setActive(true);

        deviceStorage.save(newDevice);
        log.debug("Registered new device {} for user {}", deviceId, userId);
        return true;
    }

    @Override
    @NonNull
    public List<DeviceLimiter.DeviceInfo> getActiveDevices(@NonNull String userId, @Nullable String tenantId) {
        log.debug("Getting active devices for user {}", userId);

        List<io.github.afgprojects.framework.security.core.security.model.DeviceInfo> allDevices =
                deviceStorage.findByUserId(userId);

        // 过滤活跃设备
        return allDevices.stream()
                .filter(io.github.afgprojects.framework.security.core.security.model.DeviceInfo::isActive)
                .map(this::convertToDeviceLimiterInfo)
                .collect(Collectors.toList());
    }

    @Override
    public int getActiveDeviceCount(@NonNull String userId, @Nullable String tenantId) {
        log.debug("Getting active device count for user {}", userId);
        return deviceStorage.countActiveByUserId(userId);
    }

    @Override
    public void kickDevice(@NonNull String userId, @Nullable String tenantId, @NonNull String deviceId) {
        log.debug("Kicking device {} for user {}", deviceId, userId);
        deviceStorage.updateActiveStatus(deviceId, false);
        log.info("Kicked device {} for user {}", deviceId, userId);
    }

    @Override
    public void kickAllDevices(@NonNull String userId, @Nullable String tenantId) {
        log.debug("Kicking all devices for user {}", userId);

        List<io.github.afgprojects.framework.security.core.security.model.DeviceInfo> devices =
                deviceStorage.findByUserId(userId);

        for (io.github.afgprojects.framework.security.core.security.model.DeviceInfo device : devices) {
            if (device.isActive()) {
                deviceStorage.updateActiveStatus(device.getDeviceId(), false);
                log.debug("Kicked device {} for user {}", device.getDeviceId(), userId);
            }
        }

        log.info("Kicked all {} devices for user {}", devices.size(), userId);
    }

    @Override
    public boolean isDeviceActive(@NonNull String userId, @Nullable String tenantId, @NonNull String deviceId) {
        log.debug("Checking if device {} is active for user {}", deviceId, userId);

        Optional<io.github.afgprojects.framework.security.core.security.model.DeviceInfo> device =
                deviceStorage.findById(deviceId);

        if (device.isEmpty()) {
            log.debug("Device {} not found", deviceId);
            return false;
        }

        io.github.afgprojects.framework.security.core.security.model.DeviceInfo deviceInfo = device.get();

        // 检查设备是否属于该用户
        if (!deviceInfo.getUserId().equals(userId)) {
            log.debug("Device {} does not belong to user {}", deviceId, userId);
            return false;
        }

        return deviceInfo.isActive();
    }

    /**
     * 将存储层的 DeviceInfo 转换为 DeviceLimiter.DeviceInfo。
     *
     * @param device 存储层的设备信息
     * @return DeviceLimiter 接口的设备信息
     */
    private DeviceLimiter.DeviceInfo convertToDeviceLimiterInfo(
            io.github.afgprojects.framework.security.core.security.model.DeviceInfo device) {
        Instant loginTime = device.getLastLoginTime();
        LocalDateTime loginLocalTime = loginTime != null
                ? LocalDateTime.ofInstant(loginTime, ZoneId.systemDefault())
                : LocalDateTime.now();

        // 使用最后登录时间作为最后活跃时间（简化实现）
        LocalDateTime lastActiveTime = loginLocalTime;

        return new DeviceLimiter.DeviceInfo(
                device.getDeviceId(),
                device.getDeviceName(),
                device.getDeviceType(),
                device.getLastLoginIp() != null ? device.getLastLoginIp() : "",
                loginLocalTime,
                lastActiveTime
        );
    }
}