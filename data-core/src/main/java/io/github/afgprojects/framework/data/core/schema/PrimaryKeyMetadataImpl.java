package io.github.afgprojects.framework.data.core.schema;

import java.util.List;

/**
 * 主键元数据实现
 */
public final class PrimaryKeyMetadataImpl implements PrimaryKeyMetadata {

    private final String constraintName;
    private final List<String> columnNames;

    public PrimaryKeyMetadataImpl(String constraintName, List<String> columnNames) {
        this.constraintName = constraintName;
        this.columnNames = columnNames;
    }

    @Override
    public String getConstraintName() {
        return constraintName;
    }

    @Override
    public List<String> getColumnNames() {
        return columnNames;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String constraintName;
        private List<String> columnNames = List.of();

        public Builder constraintName(String constraintName) {
            this.constraintName = constraintName;
            return this;
        }

        public Builder columnNames(List<String> columnNames) {
            this.columnNames = columnNames;
            return this;
        }

        public PrimaryKeyMetadataImpl build() {
            return new PrimaryKeyMetadataImpl(constraintName, columnNames);
        }
    }
}
