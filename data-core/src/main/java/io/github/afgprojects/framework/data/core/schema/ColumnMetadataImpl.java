package io.github.afgprojects.framework.data.core.schema;

import org.jspecify.annotations.Nullable;

/**
 * 列元数据实现
 */
public final class ColumnMetadataImpl implements ColumnMetadata {

    private final String columnName;
    private final String dataType;
    private final boolean nullable;
    private final @Nullable String defaultValue;
    private final @Nullable String comment;
    private final boolean unique;
    private final boolean primaryKey;
    private final boolean autoIncrement;

    public ColumnMetadataImpl(String columnName, String dataType, boolean nullable,
                               @Nullable String defaultValue, @Nullable String comment,
                               boolean unique, boolean primaryKey, boolean autoIncrement) {
        this.columnName = columnName;
        this.dataType = dataType;
        this.nullable = nullable;
        this.defaultValue = defaultValue;
        this.comment = comment;
        this.unique = unique;
        this.primaryKey = primaryKey;
        this.autoIncrement = autoIncrement;
    }

    @Override
    public String getColumnName() {
        return columnName;
    }

    @Override
    public String getDataType() {
        return dataType;
    }

    @Override
    public boolean isNullable() {
        return nullable;
    }

    @Override
    public @Nullable String getDefaultValue() {
        return defaultValue;
    }

    @Override
    public @Nullable String getComment() {
        return comment;
    }

    @Override
    public boolean isUnique() {
        return unique;
    }

    @Override
    public boolean isPrimaryKey() {
        return primaryKey;
    }

    @Override
    public boolean isAutoIncrement() {
        return autoIncrement;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String columnName;
        private String dataType;
        private boolean nullable = true;
        private @Nullable String defaultValue;
        private @Nullable String comment;
        private boolean unique;
        private boolean primaryKey;
        private boolean autoIncrement;

        public Builder columnName(String columnName) {
            this.columnName = columnName;
            return this;
        }

        public Builder dataType(String dataType) {
            this.dataType = dataType;
            return this;
        }

        public Builder nullable(boolean nullable) {
            this.nullable = nullable;
            return this;
        }

        public Builder defaultValue(@Nullable String defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder comment(@Nullable String comment) {
            this.comment = comment;
            return this;
        }

        public Builder unique(boolean unique) {
            this.unique = unique;
            return this;
        }

        public Builder primaryKey(boolean primaryKey) {
            this.primaryKey = primaryKey;
            return this;
        }

        public Builder autoIncrement(boolean autoIncrement) {
            this.autoIncrement = autoIncrement;
            return this;
        }

        public ColumnMetadataImpl build() {
            return new ColumnMetadataImpl(
                    columnName, dataType, nullable, defaultValue, comment, unique, primaryKey, autoIncrement
            );
        }
    }
}
