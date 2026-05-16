package io.github.afgprojects.framework.security.auth.entity;

import lombok.Data;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;

/**
 * 用户设备绑定实体。
 *
 * <p>用于记录用户登录设备信息。
 *
 * @since 1.0.0
 */
@Data
public class AuthUserDevice {

    /**
     * 设备唯一标识
     */
    private String deviceId;

    /**
     * 用户 ID
     */
    private String userId;

    /**
     * 租户 ID
     */
    private @Nullable String tenantId;

    /**
     * 设备名称
     */
    private @Nullable String deviceName;

    /**
     * 设备类型（MOBILE、TABLET、PC）
     */
    private @Nullable String deviceType;

    /**
     * 最后登录 IP
     */
    private @Nullable String lastLoginIp;

    /**
     * 最后登录时间
     */
    private @Nullable LocalDateTime lastLoginTime;

    /**
     * 活跃状态
     */
    private boolean active;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}