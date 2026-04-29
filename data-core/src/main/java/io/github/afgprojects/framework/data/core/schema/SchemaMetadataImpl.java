package io.github.afgprojects.framework.data.core.schema;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Schema 元数据实现
 */
public final class SchemaMetadataImpl implements SchemaMetadata {

    private final String tableName;
    private final @Nullable String comment;
    private final List<ColumnMetadata> columns;
    private final @Nullable PrimaryKeyMetadata primaryKey;
    private final List<IndexMetadata> indexes;
    private final List<ForeignKeyMetadata> foreignKeys;

    public SchemaMetadataImpl(String tableName, @Nullable String comment,
                               List<ColumnMetadata> columns, @Nullable PrimaryKeyMetadata primaryKey,
                               List<IndexMetadata> indexes, List<ForeignKeyMetadata> foreignKeys) {
        this.tableName = tableName;
        this.comment = comment;
        this.columns = columns;
        this.primaryKey = primaryKey;
        this.indexes = indexes;
        this.foreignKeys = foreignKeys;
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public @Nullable String getComment() {
        return comment;
    }

    @Override
    public List<ColumnMetadata> getColumns() {
        return columns;
    }

    @Override
    public @Nullable PrimaryKeyMetadata getPrimaryKey() {
        return primaryKey;
    }

    @Override
    public List<IndexMetadata> getIndexes() {
        return indexes;
    }

    @Override
    public List<ForeignKeyMetadata> getForeignKeys() {
        return foreignKeys;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String tableName;
        private @Nullable String comment;
        private java.util.List<ColumnMetadata> columns = new java.util.ArrayList<>();
        private @Nullable PrimaryKeyMetadata primaryKey;
        private java.util.List<IndexMetadata> indexes = new java.util.ArrayList<>();
        private java.util.List<ForeignKeyMetadata> foreignKeys = new java.util.ArrayList<>();

        public Builder tableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        public Builder comment(@Nullable String comment) {
            this.comment = comment;
            return this;
        }

        public Builder columns(List<ColumnMetadata> columns) {
            this.columns = columns;
            return this;
        }

        public Builder addColumn(ColumnMetadata column) {
            this.columns.add(column);
            return this;
        }

        public Builder primaryKey(@Nullable PrimaryKeyMetadata primaryKey) {
            this.primaryKey = primaryKey;
            return this;
        }

        public Builder indexes(List<IndexMetadata> indexes) {
            this.indexes = indexes;
            return this;
        }

        public Builder addIndex(IndexMetadata index) {
            this.indexes.add(index);
            return this;
        }

        public Builder foreignKeys(List<ForeignKeyMetadata> foreignKeys) {
            this.foreignKeys = foreignKeys;
            return this;
        }

        public Builder addForeignKey(ForeignKeyMetadata foreignKey) {
            this.foreignKeys.add(foreignKey);
            return this;
        }

        public SchemaMetadataImpl build() {
            return new SchemaMetadataImpl(
                    tableName, comment,
                    List.copyOf(columns),
                    primaryKey,
                    List.copyOf(indexes),
                    List.copyOf(foreignKeys)
            );
        }
    }
}
