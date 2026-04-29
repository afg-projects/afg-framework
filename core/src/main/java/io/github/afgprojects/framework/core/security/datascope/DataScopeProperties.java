package io.github.afgprojects.framework.core.security.datascope;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * 数据权限配置属性
 * <p>
 * 配置示例：
 * <pre>
 * afg:
 *   data-scope:
 *     enabled: true
 *     dept-table: sys_dept
 *     default-scope-type: DEPT
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "afg.data-scope")
public class DataScopeProperties {

    /**
     * 是否启用数据权限
     */
    private boolean enabled = true;

    /**
     * 部门表名
     */
    private String deptTable = "sys_dept";

    /**
     * 部门表 ID 字段名
     */
    private String deptIdColumn = "id";

    /**
     * 部门表父级 ID 字段名
     */
    private String deptParentColumn = "parent_id";

    /**
     * 默认数据范围类型
     */
    private DataScopeType defaultScopeType = DataScopeType.DEPT;

    /**
     * 用户 ID 字段名（用于 SELF 类型）
     */
    private String userIdColumn = "create_by";

    /**
     * 是否缓存部门层级关系
     */
    private boolean cacheDeptHierarchy = true;

    /**
     * 部门层级缓存过期时间（秒）
     */
    private long cacheExpireSeconds = 300;

    /**
     * 是否在无上下文时忽略权限过滤
     * <p>
     * 如果设置为 false，无上下文时将抛出异常
     */
    private boolean ignoreWhenNoContext = false;

    /**
     * 需要忽略数据权限的表名列表
     */
    private String[] ignoreTables = {};

    /**
     * 需要忽略数据权限的 Mapper 方法列表
     * <p>
     * 支持通配符，如 "select*", "list*"
     */
    private String[] ignoreMethods = {};
}