package io.github.afgprojects.framework.commons.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 不可变分页数据
 *
 * @param <T> 记录类型
 */
@SuppressWarnings("PMD.UnusedAssignment")
public record PageData<T>(
        List<T> records, long total, long page, long size, long pages, boolean hasNext, boolean hasPrevious)
        implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // NOPMD - record 紧凑构造器中重新赋值参数是正常行为
    public PageData {
        records = records != null ? List.copyOf(records) : Collections.emptyList();
        pages = size > 0 ? (total + size - 1) / size : 0;
        hasNext = page < pages;
        hasPrevious = page > 1;
    }

    /**
     * 创建空分页结果
     */
    public static <T> PageData<T> empty() {
        return new PageData<>(Collections.emptyList(), 0, 1, 10, 0, false, false);
    }

    /**
     * 创建分页结果
     */
    public static <T> PageData<T> of(List<T> records, long total, long page, long size) {
        return new PageData<>(records, total, page, size, 0, false, false);
    }

    /**
     * 创建空分页（指定页码和大小）
     */
    public static <T> PageData<T> empty(long page, long size) {
        return new PageData<>(Collections.emptyList(), 0, page, size, 0, false, false);
    }

    /**
     * 创建单页结果（所有数据在一页）
     */
    public static <T> PageData<T> singlePage(List<T> records) {
        return new PageData<>(records, records.size(), 1, Math.max(1, records.size()), 0, false, false);
    }

    /**
     * 转换数据类型
     */
    public <U> PageData<U> map(Function<? super T, ? extends U> mapper) {
        List<U> mapped = records.stream().map(mapper).collect(Collectors.toList());
        return new PageData<>(mapped, total, page, size, 0, false, false);
    }

    /**
     * 是否是第一页
     */
    public boolean isFirst() {
        return page == 1;
    }

    /**
     * 是否是最后一页
     */
    public boolean isLast() {
        return page >= pages;
    }

    /**
     * 是否有内容
     */
    public boolean hasContent() {
        return !records.isEmpty();
    }

    /**
     * 获取当前页数据数量
     */
    public int getNumberOfElements() {
        return records.size();
    }

    /**
     * 获取偏移量
     */
    public long getOffset() {
        return (page - 1) * size;
    }
}
