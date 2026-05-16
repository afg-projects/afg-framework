package io.github.afgprojects.framework.security.core.security;

import java.time.LocalDateTime;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * 设备限制器接口。
 *
 * <p>用于管理用户登录设备，实现设备数量限制和设备踢出功能。
 *
 * <p>典型应用场景：
 * <ul>
 *   <li>限制同一账号同时登录的设备数量</li>
 *   <li>查看当前登录设备列表</li>
 *   <li>踢出可疑设备或强制下线</li>
 *   <li>设备信任管理</li>
 * </ul>
 *
 * @since 1.0.0
 */
public interface DeviceLimiter {

    /**
     * 注册设备登录。
     *
     * <p>用户登录成功后调用此方法记录设备信息。
     * 如果设备数量超过限制，可能返回 false。
     *
     * @param userId 用户 ID，永不为 null
     * @param tenantId 租户 ID，单租户场景可为 null
     * @param deviceId 设备唯一标识，永不为 null
     * @param deviceName 设备名称，可为 null
     * @param deviceType 设备类型（如 PC、Mobile、Tablet），可为 null
     * @param ip 登录 IP 地址，永不为 null
     * @return 如果注册成功则返回 true，如果超过设备数量限制则返回 false
     */
    boolean registerDevice(
            String userId,
            @Nullable String tenantId,
            String deviceId,
            @Nullable String deviceName,
            @Nullable String deviceType,
            String ip);

    /**
     * 获取用户当前活跃的设备列表。
     *
     * @param userId 用户 ID，永不为 null
     * @param tenantId 租户 ID，单租户场景可为 null
     * @return 活跃设备列表
     */
    List<DeviceInfo> getActiveDevices(String userId, @Nullable String tenantId);

    /**
     * 获取用户当前活跃设备数量。
     *
     * @param userId 用户 ID，永不为 null
     * @param tenantId 租户 ID，单租户场景可为 null
     * @return 活跃设备数量
     */
    int getActiveDeviceCount(String userId, @Nullable String tenantId);

    /**
     * 踢出指定设备。
     *
     * <p>使指定设备的登录状态失效。
     *
     * @param userId 用户 ID，永不为 null
     * @param tenantId 租户 ID，单租户场景可为 null
     * @param deviceId 设备唯一标识，永不为 null
     */
    void kickDevice(String userId, @Nullable String tenantId, String deviceId);

    /**
     * 踢出所有设备。
     *
     * <p>使用户在所有设备上的登录状态失效，通常用于强制重新登录。
     *
     * @param userId 用户 ID，永不为 null
     * @param tenantId 租户 ID，单租户场景可为 null
     */
    void kickAllDevices(String userId, @Nullable String tenantId);

    /**
     * 检查设备是否处于活跃状态。
     *
     * @param userId 用户 ID，永不为 null
     * @param tenantId 租户 ID，单租户场景可为 null
     * @param deviceId 设备唯一标识，永不为 null
     * @return 如果设备活跃则返回 true
     */
    boolean isDeviceActive(String userId, @Nullable String tenantId, String deviceId);

    /**
     * 设备信息。
     *
     * @param deviceId 设备唯一标识
     * @param deviceName 设备名称
     * @param deviceType 设备类型
     * @param ip 登录 IP 地址
     * @param loginTime 登录时间
     * @param lastActiveTime 最后活跃时间
     */
    record DeviceInfo(
            String deviceId,
            @Nullable String deviceName,
            @Nullable String deviceType,
            String ip,
            LocalDateTime loginTime,
            LocalDateTime lastActiveTime) {}
}
