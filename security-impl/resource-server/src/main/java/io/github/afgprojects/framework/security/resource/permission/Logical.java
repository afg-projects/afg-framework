package io.github.afgprojects.framework.security.resource.permission;

/**
 * 多条件逻辑关系。
 */
public enum Logical {

    /**
     * 所有条件都必须满足。
     */
    AND,

    /**
     * 满足任意一个条件即可。
     */
    OR
}