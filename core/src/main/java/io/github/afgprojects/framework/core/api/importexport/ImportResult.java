package io.github.afgprojects.framework.core.api.importexport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Data;

/**
 * 数据导入结果。
 * <p>
 * 包含导入的总数、成功数、失败数、解析出的数据列表和错误信息列表。
 *
 * @param <T> 数据类型泛型
 * @since 1.0.0
 */
@Data
@Builder
public class ImportResult<T> {

    /**
     * 总行数（不含标题行）。
     */
    private int totalCount;

    /**
     * 成功导入行数。
     */
    private int successCount;

    /**
     * 导入失败行数。
     */
    private int failureCount;

    /**
     * 成功解析的数据列表。
     */
    @Builder.Default
    private List<T> data = new ArrayList<>();

    /**
     * 导入错误列表。
     */
    @Builder.Default
    private List<ImportError> errors = new ArrayList<>();

    /**
     * 是否存在导入错误。
     *
     * @return 存在错误时返回 true
     */
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    /**
     * 创建空的导入结果。
     *
     * @param <T> 数据类型泛型
     * @return 空的导入结果
     */
    public static <T> ImportResult<T> empty() {
        return ImportResult.<T>builder()
                .totalCount(0)
                .successCount(0)
                .failureCount(0)
                .data(Collections.emptyList())
                .errors(Collections.emptyList())
                .build();
    }

    /**
     * 从数据和错误创建导入结果。
     *
     * @param data   成功解析的数据
     * @param errors 导入错误列表
     * @param <T>    数据类型泛型
     * @return 导入结果
     */
    public static <T> ImportResult<T> of(List<T> data, List<ImportError> errors) {
        int failureCount = errors != null ? errors.size() : 0;
        int successCount = data != null ? data.size() : 0;
        return ImportResult.<T>builder()
                .totalCount(successCount + failureCount)
                .successCount(successCount)
                .failureCount(failureCount)
                .data(data != null ? data : Collections.emptyList())
                .errors(errors != null ? errors : Collections.emptyList())
                .build();
    }
}
