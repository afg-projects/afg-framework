package io.github.afgprojects.framework.data.core.schema;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Schema 差异结果
 */
public record SchemaDiff(
        String tableName,
        boolean tableExists,
        List<ColumnDiff> columnDiffs,
        @Nullable IndexDiff indexDiff,
        @Nullable ForeignKeyDiff foreignKeyDiff
) {

    /**
     * 是否有差异
     */
    public boolean hasDifferences() {
        return !columnDiffs.isEmpty()
                || (indexDiff != null && indexDiff.hasDifferences())
                || (foreignKeyDiff != null && foreignKeyDiff.hasDifferences());
    }

    /**
     * 是否有新增列
     */
    public boolean hasAddedColumns() {
        return columnDiffs.stream().anyMatch(d -> d.diffType() == DiffType.ADD);
    }

    /**
     * 是否有删除列
     */
    public boolean hasDroppedColumns() {
        return columnDiffs.stream().anyMatch(d -> d.diffType() == DiffType.DROP);
    }

    /**
     * 是否有修改列
     */
    public boolean hasModifiedColumns() {
        return columnDiffs.stream().anyMatch(d -> d.diffType() == DiffType.MODIFY);
    }
}
