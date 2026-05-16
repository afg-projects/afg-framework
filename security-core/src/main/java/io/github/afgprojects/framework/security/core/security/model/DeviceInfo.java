package io.github.afgprojects.framework.security.core.security.model;

import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * 设备信息。
 *
 * <p>用于记录用户登录设备的相关信息，支持设备管理和安全审计。
 *
 * @since 1.0.0
 */
public class DeviceInfo {

    private String deviceId;

    private String userId;

    @Nullable
    private String tenantId;

    @Nullable
    private String deviceName;

    @Nullable
    private String deviceType;

    @Nullable
    private String lastLoginIp;

    @Nullable
    private Instant lastLoginTime;

    private boolean active = true;

    /**
     * 获取设备 ID。
     *
     * @return 设备唯一标识
     */
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * 设置设备 ID。
     *
     * @param deviceId 设备唯一标识
     */
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * 获取用户 ID。
     *
     * @return 用户唯一标识
     */
    public String getUserId() {
        return userId;
    }

    /**
     * 设置用户 ID。
     *
     * @param userId 用户唯一标识
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * 获取租户 ID。
     *
     * @return 租户 ID，可能为 null
     */
    @Nullable
    public String getTenantId() {
        return tenantId;
    }

    /**
     * 设置租户 ID。
     *
     * @param tenantId 租户 ID
     */
    public void setTenantId(@Nullable String tenantId) {
        this.tenantId = tenantId;
    }

    /**
     * 获取设备名称。
     *
     * @return 设备名称，可能为 null
     */
    @Nullable
    public String getDeviceName() {
        return deviceName;
    }

    /**
     * 设置设备名称。
     *
     * @param deviceName 设备名称
     */
    public void setDeviceName(@Nullable String deviceName) {
        this.deviceName = deviceName;
    }

    /**
     * 获取设备类型。
     *
     * @return 设备类型（如 PC、Mobile、Tablet），可能为 null
     */
    @Nullable
    public String getDeviceType() {
        return deviceType;
    }

    /**
     * 设置设备类型。
     *
     * @param deviceType 设备类型
     */
    public void setDeviceType(@Nullable String deviceType) {
        this.deviceType = deviceType;
    }

    /**
     * 获取最后登录 IP。
     *
     * @return 最后登录 IP 地址，可能为 null
     */
    @Nullable
    public String getLastLoginIp() {
        return lastLoginIp;
    }

    /**
     * 设置最后登录 IP。
     *
     * @param lastLoginIp 最后登录 IP 地址
     */
    public void setLastLoginIp(@Nullable String lastLoginIp) {
        this.lastLoginIp = lastLoginIp;
    }

    /**
     * 获取最后登录时间。
     *
     * @return 最后登录时间，可能为 null
     */
    @Nullable
    public Instant getLastLoginTime() {
        return lastLoginTime;
    }

    /**
     * 设置最后登录时间。
     *
     * @param lastLoginTime 最后登录时间
     */
    public void setLastLoginTime(@Nullable Instant lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    /**
     * 判断设备是否活跃。
     *
     * @return 如果设备活跃则返回 true
     */
    public boolean isActive() {
        return active;
    }

    /**
     * 设置设备活跃状态。
     *
     * @param active 设备活跃状态
     */
    public void setActive(boolean active) {
        this.active = active;
    }
}
