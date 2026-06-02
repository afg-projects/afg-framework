package io.github.afgprojects.framework.security.auth.entity;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import io.github.afgprojects.framework.data.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * 用户设备绑定实体。
 *
 * <p>用于记录用户登录设备信息。
 *
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AfEntity
@Table(name = "auth_user_device")
public class AuthUserDevice extends BaseEntity {

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
     * 设备操作系统
     */
    private @Nullable String deviceOs;

    /**
     * 浏览器
     */
    private @Nullable String browser;

    /**
     * 最后登录 IP
     */
    private @Nullable String lastLoginIp;

    /**
     * 最后登录时间
     */
    private @Nullable Instant lastLoginTime;

    /**
     * 首次登录时间
     */
    private @Nullable Instant firstLoginTime;

    /**
     * 活跃状态
     */
    @Column(name = "is_active")
    private boolean active;

    /**
     * 是否信任设备
     */
    @Column(name = "is_trusted")
    private boolean trusted;
}