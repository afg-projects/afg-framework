package io.github.afgprojects.framework.core.properties.datascope;

import lombok.Data;

/**
 * 数据权限配置。
 */
@Data
public class AfgCoreDataScopeProperties {

    /**
     * 是否启用数据权限。
     */
    private boolean enabled = true;

    /**
     * 部门表名。
     */
    private String deptTable = "sys_dept";

    /**
     * 部门表 ID 字段名。
     */
    private String deptIdColumn = "id";

    /**
     * 部门表父级 ID 字段名。
     */
    private String deptParentColumn = "parent_id";

    /**
     * 默认数据范围类型。
     */
    private DataScopeType defaultScopeType = DataScopeType.DEPT;

    /**
     * 用户 ID 字段名。
     */
    private String userIdColumn = "create_by";

    /**
     * 是否缓存部门层级关系。
     */
    private boolean cacheDeptHierarchy = true;

    /**
     * 部门层级缓存过期时间（秒）。
     */
    private long cacheExpireSeconds = 300;

    /**
     * 是否在无上下文时忽略权限过滤。
     */
    private boolean ignoreWhenNoContext = false;

    /**
     * 需要忽略数据权限的表名列表。
     */
    private String[] ignoreTables = {};

    /**
     * 需要忽略数据权限的 Mapper 方法列表。
     */
    private String[] ignoreMethods = {};
}
