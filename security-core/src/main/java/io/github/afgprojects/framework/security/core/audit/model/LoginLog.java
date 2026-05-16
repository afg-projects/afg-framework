package io.github.afgprojects.framework.security.core.audit.model;

import io.github.afgprojects.framework.security.core.audit.LoginLogService.LoginLogInfo;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * 登录日志记录。
 *
 * <p>记录用户登录和登出的详细信息，包括设备信息、地理位置、登录结果等。
 *
 * @param id 日志 ID（由持久层生成）
 * @param userId 用户 ID（登录失败时可能为 null）
 * @param username 用户名
 * @param tenantId 租户 ID（单租户场景可能为 null）
 * @param ip 登录 IP 地址
 * @param deviceId 设备 ID
 * @param deviceName 设备名称
 * @param browser 浏览器信息
 * @param os 操作系统信息
 * @param location 地理位置
 * @param result 登录结果（SUCCESS 或 FAILURE）
 * @param failReason 失败原因（登录成功时为 null）
 * @param loginTime 登录时间
 * @param logoutTime 登出时间（未登出时为 null）
 * @author afg-projects
 * @since 1.0.0
 */
public record LoginLog(
        @Nullable String id,
        @Nullable String userId,
        @NonNull String username,
        @Nullable String tenantId,
        @NonNull String ip,
        @Nullable String deviceId,
        @Nullable String deviceName,
        @Nullable String browser,
        @Nullable String os,
        @Nullable String location,
        @NonNull String result,
        @Nullable String failReason,
        @NonNull Instant loginTime,
        @Nullable Instant logoutTime) implements LoginLogInfo {

    /**
     * 登录成功常量。
     */
    public static final String SUCCESS = "SUCCESS";

    /**
     * 登录失败常量。
     */
    public static final String FAILURE = "FAILURE";

    // Implement LoginLogInfo interface methods with getXxx naming convention
    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public String getIp() {
        return ip;
    }

    @Override
    public String getDeviceId() {
        return deviceId;
    }

    @Override
    public String getDeviceName() {
        return deviceName;
    }

    @Override
    public String getBrowser() {
        return browser;
    }

    @Override
    public String getOs() {
        return os;
    }

    @Override
    public String getLocation() {
        return location;
    }

    @Override
    public String getResult() {
        return result;
    }

    @Override
    public String getFailReason() {
        return failReason;
    }

    @Override
    public Instant getLoginTime() {
        return loginTime;
    }

    @Override
    public Instant getLogoutTime() {
        return logoutTime;
    }

    /**
     * 创建成功登录日志。
     *
     * @param userId 用户 ID
     * @param username 用户名
     * @param tenantId 租户 ID
     * @param ip 登录 IP
     * @param deviceId 设备 ID
     * @param deviceName 设备名称
     * @param browser 浏览器信息
     * @param os 操作系统信息
     * @param location 地理位置
     * @return 登录日志
     */
    public static LoginLog success(
            @NonNull String userId,
            @NonNull String username,
            @Nullable String tenantId,
            @NonNull String ip,
            @Nullable String deviceId,
            @Nullable String deviceName,
            @Nullable String browser,
            @Nullable String os,
            @Nullable String location) {
        return new LoginLog(
                null,
                userId,
                username,
                tenantId,
                ip,
                deviceId,
                deviceName,
                browser,
                os,
                location,
                SUCCESS,
                null,
                Instant.now(),
                null
        );
    }

    /**
     * 创建失败登录日志。
     *
     * @param username 用户名
     * @param tenantId 租户 ID
     * @param ip 登录 IP
     * @param deviceId 设备 ID
     * @param deviceName 设备名称
     * @param browser 浏览器信息
     * @param os 操作系统信息
     * @param location 地理位置
     * @param failReason 失败原因
     * @return 登录日志
     */
    public static LoginLog failure(
            @NonNull String username,
            @Nullable String tenantId,
            @NonNull String ip,
            @Nullable String deviceId,
            @Nullable String deviceName,
            @Nullable String browser,
            @Nullable String os,
            @Nullable String location,
            @NonNull String failReason) {
        return new LoginLog(
                null,
                null,
                username,
                tenantId,
                ip,
                deviceId,
                deviceName,
                browser,
                os,
                location,
                FAILURE,
                failReason,
                Instant.now(),
                null
        );
    }
}