package io.github.afgprojects.framework.data.core.query;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 分页结果
 *
 * @param <T> 数据类型
 */
public final class Page<T> {

    /**
     * 数据列表
     */
    private final List<T> content;

    /**
     * 总记录数
     */
    private final long total;

    /**
     * 当前页码（从1开始）
     */
    private final long page;

    /**
     * 每页大小
     */
    private final long size;

    /**
     * 构造分页结果
     *
     * @param content 数据列表
     * @param total   总记录数
     * @param page    当前页码
     * @param size    每页大小
     */
    public Page(@NonNull List<T> content, long total, long page, long size) {
        this.content = Collections.unmodifiableList(Objects.requireNonNull(content));
        this.total = Math.max(0, total);
        this.page = Math.max(1, page);
        this.size = Math.max(1, size);
    }

    /**
     * 获取数据列表
     *
     * @return 数据列表
     */
    public @NonNull List<T> getContent() {
        return content;
    }

    /**
     * 获取总记录数
     *
     * @return 总记录数
     */
    public long getTotal() {
        return total;
    }

    /**
     * 获取当前页码
     *
     * @return 当前页码（从1开始）
     */
    public long getPage() {
        return page;
    }

    /**
     * 获取每页大小
     *
     * @return 每页大小
     */
    public long getSize() {
        return size;
    }

    /**
     * 获取总页数
     *
     * @return 总页数
     */
    public long getTotalPages() {
        return size == 0 ? 1 : (total + size - 1) / size;
    }

    /**
     * 获取当前页数据数量
     *
     * @return 当前页数据数量
     */
    public long getNumberOfElements() {
        return content.size();
    }

    /**
     * 是否有内容
     *
     * @return true表示有内容
     */
    public boolean hasContent() {
        return !content.isEmpty();
    }

    /**
     * 是否是第一页
     *
     * @return true表示第一页
     */
    public boolean isFirst() {
        return page == 1;
    }

    /**
     * 是否是最后一页
     *
     * @return true表示最后一页
     */
    public boolean isLast() {
        return page >= getTotalPages();
    }

    /**
     * 是否有下一页
     *
     * @return true表示有下一页
     */
    public boolean hasNext() {
        return page < getTotalPages();
    }

    /**
     * 是否有上一页
     *
     * @return true表示有上一页
     */
    public boolean hasPrevious() {
        return page > 1;
    }

    /**
     * 获取偏移量
     *
     * @return 偏移量
     */
    public long getOffset() {
        return (page - 1) * size;
    }

    /**
     * 转换数据类型
     *
     * @param mapper 转换函数
     * @param <U>    目标类型
     * @return 转换后的分页结果
     */
    public <U> @NonNull Page<U> map(@NonNull Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper, "mapper must not be null");
        List<U> mappedContent = content.stream()
                .map(mapper)
                .collect(Collectors.toList());
        return new Page<>(mappedContent, total, page, size);
    }

    // ========== 静态工厂方法 ==========

    /**
     * 创建空分页
     *
     * @param <T> 数据类型
     * @return 空分页结果
     */
    public static <T> @NonNull Page<T> empty() {
        return new Page<>(Collections.emptyList(), 0, 1, 10);
    }

    /**
     * 创建空分页
     *
     * @param page 页码
     * @param size 每页大小
     * @param <T>  数据类型
     * @return 空分页结果
     */
    public static <T> @NonNull Page<T> empty(long page, long size) {
        return new Page<>(Collections.emptyList(), 0, page, size);
    }

    /**
     * 创建分页结果
     *
     * @param content 数据列表
     * @param total   总记录数
     * @param page    当前页码
     * @param size    每页大小
     * @param <T>     数据类型
     * @return 分页结果
     */
    public static <T> @NonNull Page<T> of(@NonNull List<T> content, long total, long page, long size) {
        return new Page<>(content, total, page, size);
    }

    /**
     * 创建单页结果（所有数据在一页）
     *
     * @param content 数据列表
     * @param <T>     数据类型
     * @return 分页结果
     */
    public static <T> @NonNull Page<T> singlePage(@NonNull List<T> content) {
        return new Page<>(content, content.size(), 1, content.size());
    }

    @Override
    public String toString() {
        return "Page{" +
                "content=" + content +
                ", total=" + total +
                ", page=" + page +
                ", size=" + size +
                '}';
    }
}