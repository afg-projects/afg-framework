package io.github.afgprojects.framework.core.model.result;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        records = records != null ? Collections.unmodifiableList(new ArrayList<>(records)) : Collections.emptyList();
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
}
