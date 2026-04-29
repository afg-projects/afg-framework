package io.github.afgprojects.framework.data.core.schema;

import java.util.ArrayList;
import java.util.List;

/**
 * 冲突分析结果
 */
public record ConflictAnalysis(
        boolean hasConflicts,
        List<Conflict> conflicts
) {

    public static ConflictAnalysis noConflicts() {
        return new ConflictAnalysis(false, List.of());
    }

    public static ConflictAnalysis of(List<Conflict> conflicts) {
        return new ConflictAnalysis(!conflicts.isEmpty(), conflicts);
    }

    /**
     * 冲突详情
     */
    public record Conflict(
            String columnName,
            String description,
            ConflictType type,
            ConflictResolution suggestion
    ) {}

    /**
     * 冲突类型
     */
    public enum ConflictType {
        /**
         * 类型冲突
         */
        TYPE_MISMATCH,
        /**
         * 约束冲突
         */
        CONSTRAINT_MISMATCH,
        /**
         * 列存在性冲突
         */
        COLUMN_EXISTENCE,
        /**
         * 默认值冲突
         */
        DEFAULT_VALUE_MISMATCH
    }

    /**
     * 冲突解决方案
     */
    public enum ConflictResolution {
        /**
         * 使用实体定义
         */
        USE_ENTITY,
        /**
         * 使用数据库定义
         */
        USE_DATABASE,
        /**
         * 使用 ChangeLog 定义
         */
        USE_CHANGELOG,
        /**
         * 手动解决
         */
        MANUAL_RESOLUTION
    }

    /**
     * Builder
     */
    public static class Builder {
        private final List<Conflict> conflicts = new ArrayList<>();

        public Builder addConflict(String columnName, String description,
                                   ConflictType type, ConflictResolution suggestion) {
            conflicts.add(new Conflict(columnName, description, type, suggestion));
            return this;
        }

        public ConflictAnalysis build() {
            return new ConflictAnalysis(!conflicts.isEmpty(), List.copyOf(conflicts));
        }
    }
}
