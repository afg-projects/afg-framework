package io.github.afgprojects.framework.data.core.relation;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * 关联元数据实现
 * <p>
 * 关联关系元数据的不可变实现类。
 */
public record RelationMetadataImpl(
        @NonNull RelationType relationType,
        @NonNull Class<?> entityClass,
        @NonNull Class<?> targetEntityClass,
        @NonNull String fieldName,
        @Nullable String mappedBy,
        @NonNull String foreignKeyColumn,
        @Nullable String joinTable,
        @Nullable String joinColumn,
        @Nullable String inverseJoinColumn,
        @NonNull Set<CascadeType> cascadeTypes,
        @NonNull FetchType fetchType,
        boolean orphanRemoval,
        boolean optional
) implements RelationMetadata {

    /**
     * 创建关联元数据
     */
    public RelationMetadataImpl {
        cascadeTypes = cascadeTypes.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(EnumSet.copyOf(cascadeTypes));
    }

    @Override
    public @NonNull RelationType getRelationType() {
        return relationType;
    }

    @Override
    public @NonNull Class<?> getEntityClass() {
        return entityClass;
    }

    @Override
    public @NonNull Class<?> getTargetEntityClass() {
        return targetEntityClass;
    }

    @Override
    public @NonNull String getFieldName() {
        return fieldName;
    }

    @Override
    public @NonNull String getForeignKeyColumn() {
        return foreignKeyColumn;
    }

    @Override
    public @NonNull Set<CascadeType> getCascadeTypes() {
        return cascadeTypes;
    }

    @Override
    public @NonNull FetchType getFetchType() {
        return fetchType;
    }

    @Override
    public boolean isOwningSide() {
        return mappedBy == null || mappedBy.isEmpty();
    }

    @Override
    public @Nullable String getMappedBy() {
        return mappedBy;
    }

    @Override
    public @Nullable String getJoinTable() {
        return joinTable;
    }

    @Override
    public @Nullable String getJoinColumn() {
        return joinColumn;
    }

    @Override
    public @Nullable String getInverseJoinColumn() {
        return inverseJoinColumn;
    }

    @Override
    public boolean isOrphanRemoval() {
        return orphanRemoval;
    }

    @Override
    public boolean isOptional() {
        return optional;
    }

    /**
     * 创建构建器
     *
     * @return 构建器实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 关联元数据构建器
     */
    public static class Builder {
        private RelationType relationType;
        private Class<?> entityClass;
        private Class<?> targetEntityClass;
        private String fieldName;
        private String mappedBy;
        private String foreignKeyColumn;
        private String joinTable;
        private String joinColumn;
        private String inverseJoinColumn;
        private Set<CascadeType> cascadeTypes = Collections.emptySet();
        private FetchType fetchType = FetchType.LAZY;
        private boolean orphanRemoval = false;
        private boolean optional = true;

        public Builder relationType(@NonNull RelationType relationType) {
            this.relationType = relationType;
            return this;
        }

        public Builder entityClass(@NonNull Class<?> entityClass) {
            this.entityClass = entityClass;
            return this;
        }

        public Builder targetEntityClass(@NonNull Class<?> targetEntityClass) {
            this.targetEntityClass = targetEntityClass;
            return this;
        }

        public Builder fieldName(@NonNull String fieldName) {
            this.fieldName = fieldName;
            return this;
        }

        public Builder mappedBy(@Nullable String mappedBy) {
            this.mappedBy = mappedBy;
            return this;
        }

        public Builder foreignKeyColumn(@NonNull String foreignKeyColumn) {
            this.foreignKeyColumn = foreignKeyColumn;
            return this;
        }

        public Builder joinTable(@Nullable String joinTable) {
            this.joinTable = joinTable;
            return this;
        }

        public Builder joinColumn(@Nullable String joinColumn) {
            this.joinColumn = joinColumn;
            return this;
        }

        public Builder inverseJoinColumn(@Nullable String inverseJoinColumn) {
            this.inverseJoinColumn = inverseJoinColumn;
            return this;
        }

        public Builder cascadeTypes(@NonNull Set<CascadeType> cascadeTypes) {
            this.cascadeTypes = cascadeTypes;
            return this;
        }

        public Builder fetchType(@NonNull FetchType fetchType) {
            this.fetchType = fetchType;
            return this;
        }

        public Builder orphanRemoval(boolean orphanRemoval) {
            this.orphanRemoval = orphanRemoval;
            return this;
        }

        public Builder optional(boolean optional) {
            this.optional = optional;
            return this;
        }

        /**
         * 构建关联元数据
         *
         * @return 关联元数据实例
         */
        public RelationMetadataImpl build() {
            return new RelationMetadataImpl(
                    relationType,
                    entityClass,
                    targetEntityClass,
                    fieldName,
                    mappedBy,
                    foreignKeyColumn,
                    joinTable,
                    joinColumn,
                    inverseJoinColumn,
                    cascadeTypes,
                    fetchType,
                    orphanRemoval,
                    optional
            );
        }
    }
}
