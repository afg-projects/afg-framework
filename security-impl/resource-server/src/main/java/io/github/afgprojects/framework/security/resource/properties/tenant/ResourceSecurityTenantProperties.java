package io.github.afgprojects.framework.security.resource.properties.tenant;

import java.util.List;

import lombok.Data;

/**
 * 租户解析配置。
 */
@Data
public class ResourceSecurityTenantProperties {

    /**
     * 租户解析策略列表。
     * 默认按 token, header 顺序解析。
     */
    private List<String> strategies = List.of("token", "header");

    /**
     * 租户请求头名称。
     * 默认 X-Tenant-Id。
     */
    private String headerName = "X-Tenant-Id";

    /**
     * 无法解析租户时是否抛出异常。
     * 默认 true。
     */
    private boolean failIfUnresolved = true;
}
