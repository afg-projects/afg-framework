package io.github.afgprojects.framework.data.core.page;

import io.github.afgprojects.framework.data.core.query.Sort;
import org.jspecify.annotations.Nullable;

/**
 * 分页请求参数
 *
 * @param page 当前页码（从1开始）
 * @param size 每页大小
 * @param sort 排序
 */
public record PageRequest(int page, int size, @Nullable Sort sort) {

    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 1000;

    public PageRequest {
        if (page < 1) page = 1;
        if (size < 1) size = DEFAULT_PAGE_SIZE;
        if (size > MAX_PAGE_SIZE) size = MAX_PAGE_SIZE;
    }

    public static PageRequest of(int page, int size) {
        return new PageRequest(page, size, null);
    }

    public static PageRequest of(int page, int size, Sort sort) {
        return new PageRequest(page, size, sort);
    }

    /**
     * 创建首页请求
     *
     * @param size 每页大小
     * @return 首页请求
     */
    public static PageRequest firstPage(int size) {
        return new PageRequest(1, size, null);
    }

    /**
     * 创建默认分页请求
     *
     * @return 默认分页请求（第1页，每页10条）
     */
    public static PageRequest defaultPage() {
        return new PageRequest(1, DEFAULT_PAGE_SIZE, null);
    }

    /**
     * 获取偏移量
     *
     * @return 偏移量
     */
    public long offset() {
        return (long) (page - 1) * size;
    }

    /**
     * 是否有排序
     *
     * @return 有排序返回 true
     */
    public boolean hasSort() {
        return sort != null && sort.isSorted();
    }

    /**
     * 获取下一页请求
     *
     * @return 下一页请求
     */
    public PageRequest nextPage() {
        return new PageRequest(page + 1, size, sort);
    }

    /**
     * 获取上一页请求
     *
     * @return 上一页请求（最小为第1页）
     */
    public PageRequest previousPage() {
        return new PageRequest(Math.max(1, page - 1), size, sort);
    }

    /**
     * 获取指定页码的请求
     *
     * @param newPage 新页码
     * @return 新的分页请求
     */
    public PageRequest withPage(int newPage) {
        return new PageRequest(newPage, size, sort);
    }

    /**
     * 获取指定每页大小的请求
     *
     * @param newSize 新的每页大小
     * @return 新的分页请求
     */
    public PageRequest withSize(int newSize) {
        return new PageRequest(page, newSize, sort);
    }

    /**
     * 获取指定排序的请求
     *
     * @param newSort 新的排序
     * @return 新的分页请求
     */
    public PageRequest withSort(Sort newSort) {
        return new PageRequest(page, size, newSort);
    }

    /**
     * 获取指定排序的请求
     *
     * @param direction 排序方向
     * @param properties 排序属性
     * @return 新的分页请求
     */
    public PageRequest withSort(Sort.Direction direction, String... properties) {
        return new PageRequest(page, size, Sort.by(direction, properties));
    }

    /**
     * 计算总页数
     *
     * @param total 总记录数
     * @return 总页数
     */
    public int totalPages(long total) {
        if (total <= 0) return 0;
        return (int) ((total + size - 1) / size);
    }

    /**
     * 判断是否为首页
     *
     * @return 是首页返回 true
     */
    public boolean isFirst() {
        return page == 1;
    }

    /**
     * 判断是否有上一页
     *
     * @return 有上一页返回 true
     */
    public boolean hasPrevious() {
        return page > 1;
    }
}
