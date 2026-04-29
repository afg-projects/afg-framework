package io.github.afgprojects.framework.data.core.schema;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 列差异
 */
public record ColumnDiff(
        String columnName,
        DiffType diffType,
        @Nullable ColumnMetadata sourceColumn,
        @Nullable ColumnMetadata targetColumn,
        List<String> differences
) {

    /**
     * 获取差异描述
     */
    public String getDescription() {
        return switch (diffType) {
            case NONE -> "No difference";
            case ADD -> "Column will be added: " + columnName;
            case DROP -> "Column will be dropped: " + columnName;
            case MODIFY -> String.join(", ", differences);
        };
    }
}
