package io.github.afgprojects.framework.data.core.relation;

/**
 * 关联类型枚举
 * <p>
 * 定义实体之间的关联关系类型
 */
public enum RelationType {

    /**
     * 一对一关联
     */
    ONE_TO_ONE,

    /**
     * 一对多关联
     */
    ONE_TO_MANY,

    /**
     * 多对一关联
     */
    MANY_TO_ONE,

    /**
     * 多对多关联
     */
    MANY_TO_MANY
}
