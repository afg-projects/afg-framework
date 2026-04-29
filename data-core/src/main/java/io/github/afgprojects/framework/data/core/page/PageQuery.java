package io.github.afgprojects.framework.data.core.page;

import org.jspecify.annotations.Nullable;

/**
 * 分页查询参数
 *
 * @param page    当前页码（从1开始）
 * @param size    每页大小
 * @param orderBy 排序字段
 * @param asc     是否升序
 */
public record PageQuery(long page, long size, @Nullable String orderBy, boolean asc) {

    /**
     * 默认每页大小
     */
    public static final long DEFAULT_SIZE = 10;

    /**
     * 最大每页大小
     */
    public static final long MAX_SIZE = 1000;

    /**
     * 紧凑构造器，进行参数校验和规范化
     */
    public PageQuery {
        // 页码最小为1
        if (page < 1) {
            page = 1;
        }
        // 每页大小最小为1
        if (size < 1) {
            size = DEFAULT_SIZE;
        }
        // 每页大小最大为MAX_SIZE
        if (size > MAX_SIZE) {
            size = MAX_SIZE;
        }
    }

    /**
     * 创建分页参数（使用默认排序）
     *
     * @param page 当前页码
     * @param size 每页大小
     * @return PageQuery实例
     */
    public static PageQuery of(long page, long size) {
        return new PageQuery(page, size, null, true);
    }

    /**
     * 创建分页参数（指定排序）
     *
     * @param page    当前页码
     * @param size    每页大小
     * @param orderBy 排序字段
     * @param asc     是否升序
     * @return PageQuery实例
     */
    public static PageQuery of(long page, long size, String orderBy, boolean asc) {
        return new PageQuery(page, size, orderBy, asc);
    }

    /**
     * 创建默认分页参数（第1页，每页10条）
     *
     * @return PageQuery实例
     */
    public static PageQuery defaultPage() {
        return new PageQuery(1, DEFAULT_SIZE, null, true);
    }

    /**
     * 计算偏移量（用于SQL LIMIT offset, size）
     *
     * @return 偏移量
     */
    public long offset() {
        return (page - 1) * size;
    }

    /**
     * 是否有排序
     *
     * @return true表示有排序
     */
    public boolean hasOrder() {
        return orderBy != null && !orderBy.isEmpty();
    }

    /**
     * 获取排序方向字符串
     *
     * @return "ASC" 或 "DESC"
     */
    public String orderDirection() {
        return asc ? "ASC" : "DESC";
    }
}
