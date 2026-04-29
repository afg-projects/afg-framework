package io.github.afgprojects.framework.core.api.storage.model;

import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 列表查询结果
 *
 * @param objects        对象列表
 * @param commonPrefixes 公共前缀（模拟目录）
 * @param isTruncated    是否还有更多数据
 * @param nextMarker     下一页标记
 */
public record ListResult(
        @NonNull List<StorageObject> objects,
        @NonNull List<String> commonPrefixes,
        boolean isTruncated,
        @Nullable String nextMarker
) {

    /**
     * 创建空结果
     */
    @NonNull
    public static ListResult empty() {
        return new ListResult(Collections.emptyList(), Collections.emptyList(), false, null);
    }

    /**
     * 创建结果
     */
    @NonNull
    public static ListResult of(@NonNull List<StorageObject> objects) {
        return new ListResult(objects, Collections.emptyList(), false, null);
    }

    /**
     * 创建带公共前缀的结果
     */
    @NonNull
    public static ListResult of(@NonNull List<StorageObject> objects,
                               @NonNull List<String> commonPrefixes) {
        return new ListResult(objects, commonPrefixes, false, null);
    }

    /**
     * 创建带分页的结果
     */
    @NonNull
    public static ListResult of(@NonNull List<StorageObject> objects,
                               @NonNull List<String> commonPrefixes,
                               boolean isTruncated,
                               @Nullable String nextMarker) {
        return new ListResult(objects, commonPrefixes, isTruncated, nextMarker);
    }

    /**
     * 是否为空
     */
    public boolean isEmpty() {
        return objects.isEmpty() && commonPrefixes.isEmpty();
    }

    /**
     * 对象总数
     */
    public int size() {
        return objects.size();
    }
}
