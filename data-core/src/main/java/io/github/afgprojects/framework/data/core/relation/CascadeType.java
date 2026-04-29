package io.github.afgprojects.framework.data.core.relation;

/**
 * 级联操作类型
 * <p>
 * 定义关联实体的级联操作行为
 */
public enum CascadeType {

    /**
     * 级联持久化
     * <p>
     * 当保存父实体时，同时保存关联的子实体
     */
    PERSIST,

    /**
     * 级联合并
     * <p>
     * 当合并父实体时，同时合并关联的子实体
     */
    MERGE,

    /**
     * 级联删除
     * <p>
     * 当删除父实体时，同时删除关联的子实体
     */
    REMOVE,

    /**
     * 级联刷新
     * <p>
     * 当刷新父实体时，同时刷新关联的子实体
     */
    REFRESH,

    /**
     * 级联分离
     * <p>
     * 当分离父实体时，同时分离关联的子实体
     */
    DETACH,

    /**
     * 全部级联操作
     * <p>
     * 包含所有级联操作类型
     */
    ALL
}
