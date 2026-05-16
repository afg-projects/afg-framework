package io.github.afgprojects.framework.security.auth.permission.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 权限配置属性。
 *
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "afg.permission")
public class PermissionProperties {

    /**
     * 是否启用权限功能
     */
    private boolean enabled = true;

    /**
     * 默认数据范围类型
     * <p>
     * 可选值：ALL, SELF, DEPT, DEPT_AND_CHILD, CUSTOM
     */
    private String defaultDataScope = "ALL";

    /**
     * 是否启用数据权限拦截器
     */
    private boolean dataScopeInterceptorEnabled = true;
}
