package io.github.afgprojects.framework.core.model.result;

import java.util.List;

/**
 * @deprecated 使用 {@link io.github.afgprojects.framework.commons.model.PageData} 代替
 */
@Deprecated(since = "1.1.0", forRemoval = true)
public record PageData<T>(
        List<T> records, long total, long page, long size, long pages, boolean hasNext, boolean hasPrevious) {
    
    public static <T> PageData<T> empty() {
        return new PageData<>(List.of(), 0, 1, 10, 0, false, false);
    }

    public static <T> PageData<T> of(List<T> records, long total, long page, long size) {
        long pages = size > 0 ? (total + size - 1) / size : 0;
        return new PageData<>(records, total, page, size, pages, page < pages, page > 1);
    }
}
