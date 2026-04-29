package io.github.afgprojects.framework.data.core.schema;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 三向差异结果
 */
public record ThreeWayDiff(
        String tableName,
        @Nullable SchemaDiff entityVsDatabase,
        @Nullable SchemaDiff entityVsChangeLog,
        @Nullable SchemaDiff databaseVsChangeLog,
        ConflictAnalysis conflicts
) {

    /**
     * 是否有任何差异
     */
    public boolean hasAnyDifferences() {
        return (entityVsDatabase != null && entityVsDatabase.hasDifferences())
                || (entityVsChangeLog != null && entityVsChangeLog.hasDifferences())
                || (databaseVsChangeLog != null && databaseVsChangeLog.hasDifferences());
    }

    /**
     * 是否有冲突
     */
    public boolean hasConflicts() {
        return conflicts != null && conflicts.hasConflicts();
    }
}
