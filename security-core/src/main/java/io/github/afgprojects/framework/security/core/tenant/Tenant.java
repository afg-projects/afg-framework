package io.github.afgprojects.framework.security.core.tenant;

import java.util.Optional;

/**
 * 租户信息接口。
 *
 * <p>定义租户的基本信息，包括 ID、名称、状态等。
 * 实现类可以扩展更多属性，如域名、联系方式等。
 *
 * @since 1.0.0
 */
public interface Tenant {

    /**
     * 获取租户 ID。
     *
     * @return 租户唯一标识
     */
    String getId();

    /**
     * 获取租户名称。
     *
     * @return 租户名称
     */
    String getName();

    /**
     * 获取租户状态。
     *
     * @return 租户状态
     */
    TenantStatus getStatus();

    /**
     * 获取租户域名。
     *
     * @return 租户域名（可选）
     */
    default Optional<String> getDomain() {
        return Optional.empty();
    }

    /**
     * 获取租户联系邮箱。
     *
     * @return 联系邮箱（可选）
     */
    default Optional<String> getContactEmail() {
        return Optional.empty();
    }

    /**
     * 判断租户是否为活跃状态。
     *
     * @return 如果租户状态为 ACTIVE 则返回 true
     */
    default boolean isActive() {
        return getStatus() != null && getStatus().isActive();
    }
}
