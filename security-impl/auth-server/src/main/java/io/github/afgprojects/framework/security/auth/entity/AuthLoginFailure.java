package io.github.afgprojects.framework.security.auth.entity;

import lombok.Data;
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
public class AuthLoginFailure {

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
    private @Nullable String lastIp;

    /**
     * 锁定截止时间
     */
    private @Nullable LocalDateTime lockedUntil;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}