package io.github.afgprojects.framework.security.core.tenant;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Map;

/**
 * 租户信息接口。
 *
 * <p>定义租户的基本信息，包括 ID、名称、状态、过期时间等。
 * 实现类可以扩展更多属性，通过 {@link #getAttributes()} 提供扩展数据。
 *
 * @since 1.0.0
 */
public interface Tenant {

    /**
     * 获取租户 ID。
     *
     * @return 租户唯一标识（非空）
     */
    @NonNull
    String getTenantId();

    /**
     * 获取租户编码。
     *
     * <p>租户编码是租户的可读标识，如 "acme"、"tenant-001"。
     * 默认返回 {@link #getTenantId()} 的值。
     *
     * @return 租户编码，默认返回租户 ID
     */
    @Nullable
    default String getTenantCode() {
        return getTenantId();
    }

    /**
     * 获取租户名称。
     *
     * @return 租户名称（可空）
     */
    @Nullable
    String getTenantName();

    /**
     * 获取租户状态。
     *
     * @return 租户状态（非空）
     */
    @NonNull
    TenantStatus getStatus();

    /**
     * 获取租户过期时间。
     *
     * <p>如果租户有过期时间，且当前时间已超过过期时间，
     * 则租户应被视为已过期，不允许访问。
     *
     * @return 过期时间，null 表示永不过期
     */
    @Nullable
    Instant getExpiresAt();

    /**
     * 获取租户扩展属性。
     *
     * <p>用于存储租户的自定义属性，如租户类型、配额等。
     * 默认返回空 Map。
     *
     * @return 扩展属性 Map（非空，默认为空 Map）
     */
    @NonNull
    default Map<String, Object> getAttributes() {
        return Map.of();
    }

    /**
     * 判断租户是否为活跃状态。
     *
     * <p>活跃状态表示租户状态为 ACTIVE，且未过期。
     *
     * @return 如果租户状态为 ACTIVE 且未过期则返回 true
     */
    default boolean isActive() {
        return getStatus() != null
            && getStatus().isActive()
            && (getExpiresAt() == null || getExpiresAt().isAfter(Instant.now()));
    }
}
