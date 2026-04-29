package io.github.afgprojects.framework.data.core.schema;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 索引差异
 */
public record IndexDiff(
        List<IndexMetadata> addedIndexes,
        List<IndexMetadata> droppedIndexes,
        List<IndexMetadata> modifiedIndexes
) {

    public boolean hasDifferences() {
        return !addedIndexes.isEmpty() || !droppedIndexes.isEmpty() || !modifiedIndexes.isEmpty();
    }
}
