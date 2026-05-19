package io.github.afgprojects.framework.security.resource.permission;

import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * 接口权限配置。
 *
 * <p>定义单个接口的权限要求，可通过治理中心动态调整。
 *
 * @since 1.0.0
 */
@Getter
@Setter
public class ApiPermissionConfig {

    /**
     * 接口路径模式（支持 Ant 风格）。
     * 例如：/api/users/**、/api/orders/{id}
     */
    private String pattern;

    /**
     * HTTP 方法（可选，为空表示所有方法）。
     * 例如：GET、POST、PUT、DELETE
     */
    @Nullable
    private String method;

    /**
     * 是否需要登录。
     * 默认 true。
     */
    private boolean requireAuth = true;

    /**
     * 是否需要权限校验。
     * 默认 false（只校验登录）。
     */
    private boolean requirePermission = false;

    /**
     * 需要的角色（可选）。
     */
    @Nullable
    private Set<String> roles;

    /**
     * 需要的权限（可选）。
     */
    @Nullable
    private Set<String> permissions;

    /**
     * 角色逻辑关系。
     * 默认 AND（所有角色都需要）。
     */
    private Logical roleLogical = Logical.AND;

    /**
     * 权限逻辑关系。
     * 默认 AND（所有权限都需要）。
     */
    private Logical permissionLogical = Logical.AND;

    /**
     * 是否启用。
     * 默认 true。
     */
    private boolean enabled = true;

    /**
     * 优先级（数字越小优先级越高）。
     * 默认 100。
     */
    private int priority = 100;

    /**
     * 描述。
     */
    @Nullable
    private String description;
}