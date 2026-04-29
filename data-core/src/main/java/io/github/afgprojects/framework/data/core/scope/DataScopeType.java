package io.github.afgprojects.framework.data.core.scope;

/**
 * 数据权限类型
 */
public enum DataScopeType {
    /** 全部数据 */
    ALL,
    /** 仅本人数据 */
    SELF,
    /** 本部门数据 */
    DEPT,
    /** 本部门及子部门数据 */
    DEPT_AND_CHILD,
    /** 自定义条件 */
    CUSTOM
}
