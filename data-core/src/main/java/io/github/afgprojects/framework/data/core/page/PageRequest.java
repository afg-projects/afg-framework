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

    public static PageRequest defaultPage() {
        return new PageRequest(1, DEFAULT_PAGE_SIZE, null);
    }

    public long offset() {
        return (long) (page - 1) * size;
    }

    public boolean hasSort() {
        return sort != null && sort.isSorted();
    }
}
