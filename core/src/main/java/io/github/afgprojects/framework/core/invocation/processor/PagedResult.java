package io.github.afgprojects.framework.core.invocation.processor;

import java.util.List;

public record PagedResult<T>(
    List<T> content,
    long totalElements,
    int page,
    int size,
    int totalPages
) {
    public static <T> PagedResult<T> of(List<T> content, long totalElements, int page, int size) {
        return new PagedResult<>(content, totalElements, page, size, (int) Math.ceil((double) totalElements / size));
    }

    public static <T> PagedResult<T> of(List<T> content) {
        return new PagedResult<>(content, content.size(), 1, content.size(), 1);
    }
}
