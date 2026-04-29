package io.github.afgprojects.framework.data.core.schema;

import java.util.List;

/**
 * 外键差异
 */
public record ForeignKeyDiff(
        List<ForeignKeyMetadata> addedForeignKeys,
        List<ForeignKeyMetadata> droppedForeignKeys,
        List<ForeignKeyMetadata> modifiedForeignKeys
) {

    public boolean hasDifferences() {
        return !addedForeignKeys.isEmpty() || !droppedForeignKeys.isEmpty() || !modifiedForeignKeys.isEmpty();
    }
}
