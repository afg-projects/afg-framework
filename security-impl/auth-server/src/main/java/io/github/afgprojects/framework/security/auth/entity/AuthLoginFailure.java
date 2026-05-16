package io.github.afgprojects.framework.security.auth.entity;

import io.github.afgprojects.framework.data.core.entity.BaseEntity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;

/**
 * 登录失败记录实体。
 *
 * <p>用于记录用户登录失败次数和锁定状态。
 *
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "auth_login_failure")
public class AuthLoginFailure extends BaseEntity<Long> {

    /**
     * 用户 ID
     */
    private String userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 租户 ID
     */
    private @Nullable String tenantId;

    /**
     * 失败次数
     */
    private int failureCount;

    /**
     * 最后失败的 IP 地址
     */
    private @Nullable String lastFailureIp;

    /**
     * 最后失败时间
     */
    private @Nullable LocalDateTime lastFailureTime;

    /**
     * 锁定截止时间
     */
    private @Nullable LocalDateTime lockedUntil;
}