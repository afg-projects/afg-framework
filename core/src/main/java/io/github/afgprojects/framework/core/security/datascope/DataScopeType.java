package io.github.afgprojects.framework.core.security.datascope;

/**
 * 数据范围类型
 * <p>
 * 定义数据权限的粒度级别
 */
public enum DataScopeType {

    /**
     * 全部数据
     * <p>
     * 不做任何数据权限过滤
     */
    ALL,

    /**
     * 本部门数据
     * <p>
     * 仅能查看所属部门的数据
     */
    DEPT,

    /**
     * 本部门及子部门数据
     * <p>
     * 能查看所属部门及其下级部门的数据
     */
    DEPT_AND_CHILD,

    /**
     * 仅本人数据
     * <p>
     * 只能查看自己创建的数据
     */
    SELF,

    /**
     * 自定义条件
     * <p>
     * 通过自定义 SQL 条件进行过滤
     */
    CUSTOM
}