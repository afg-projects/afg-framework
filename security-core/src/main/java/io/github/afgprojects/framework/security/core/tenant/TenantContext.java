package io.github.afgprojects.framework.security.core.tenant;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * 租户上下文接口。
 *
 * <p>提供当前租户的信息，包括租户 ID、租户编码和扩展属性。
 *
 * @since 1.0.0
 */
public interface TenantContext {

    /**
     * 获取租户 ID。
     *
     * @return 租户唯一标识，永不为 null
     */
    @NonNull
    String getTenantId();

    /**
     * 获取租户编码。
     *
     * <p>租户编码通常用于 URL、请求头等场景，比 ID 更易读。
     *
     * @return 租户编码，如未设置则返回租户 ID
     */
    @Nullable
    default String getTenantCode() {
        return getTenantId();
    }

    /**
     * 获取租户名称。
     *
     * @return 租户名称
     */
    @Nullable
    default String getTenantName() {
        return null;
    }

    /**
     * 获取租户扩展属性。
     *
     * <p>可用于存储租户的额外信息，如租户类型、配置等。
     *
     * @return 属性 Map，可能为空 Map
     */
    @NonNull
    default Map<String, Object> getAttributes() {
        return Map.of();
    }

    /**
     * 判断是否为默认租户。
     *
     * @return 如果是默认租户则返回 true
     */
    default boolean isDefault() {
        return false;
    }

    /**
     * 判断租户是否有效。
     *
     * @return 如果租户有效则返回 true
     */
    default boolean isValid() {
        return true;
    }
}