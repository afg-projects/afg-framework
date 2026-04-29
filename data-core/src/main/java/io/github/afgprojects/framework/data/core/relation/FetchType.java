package io.github.afgprojects.framework.data.core.relation;

/**
 * 抓取策略
 * <p>
 * 定义关联实体的加载方式
 */
public enum FetchType {

    /**
     * 懒加载
     * <p>
     * 延迟加载关联数据，仅在首次访问时加载
     * 适用于大数据量或不常用的关联
     */
    LAZY,

    /**
     * 急加载
     * <p>
     * 立即加载关联数据，与主实体一起加载
     * 适用于经常一起使用的关联
     */
    EAGER
}
