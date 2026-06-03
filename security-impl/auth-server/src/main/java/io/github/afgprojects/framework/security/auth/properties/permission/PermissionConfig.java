package io.github.afgprojects.framework.security.auth.properties.permission;

import lombok.Data;

/**
 * 权限配置。
 */
@Data
public class PermissionConfig {

    /**
     * 是否启用权限功能。
     */
    private boolean enabled = true;

    /**
     * 默认数据范围类型。
     * 可选值：ALL, SELF, DEPT, DEPT_AND_CHILD, CUSTOM。
     */
    private String defaultDataScope = "ALL";

    /**
     * 是否启用数据权限拦截器。
     */
    private boolean dataScopeInterceptorEnabled = true;
}
