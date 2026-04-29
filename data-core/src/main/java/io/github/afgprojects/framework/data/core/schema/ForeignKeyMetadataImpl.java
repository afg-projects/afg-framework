package io.github.afgprojects.framework.data.core.schema;

import java.util.List;

/**
 * 外键元数据实现
 */
public final class ForeignKeyMetadataImpl implements ForeignKeyMetadata {

    private final String constraintName;
    private final List<String> columnNames;
    private final String referencedTableName;
    private final List<String> referencedColumnNames;
    private final String updateRule;
    private final String deleteRule;

    public ForeignKeyMetadataImpl(String constraintName, List<String> columnNames,
                                   String referencedTableName, List<String> referencedColumnNames,
                                   String updateRule, String deleteRule) {
        this.constraintName = constraintName;
        this.columnNames = columnNames;
        this.referencedTableName = referencedTableName;
        this.referencedColumnNames = referencedColumnNames;
        this.updateRule = updateRule;
        this.deleteRule = deleteRule;
    }

    @Override
    public String getConstraintName() {
        return constraintName;
    }

    @Override
    public List<String> getColumnNames() {
        return columnNames;
    }

    @Override
    public String getReferencedTableName() {
        return referencedTableName;
    }

    @Override
    public List<String> getReferencedColumnNames() {
        return referencedColumnNames;
    }

    @Override
    public String getUpdateRule() {
        return updateRule;
    }

    @Override
    public String getDeleteRule() {
        return deleteRule;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String constraintName;
        private List<String> columnNames = List.of();
        private String referencedTableName;
        private List<String> referencedColumnNames = List.of();
        private String updateRule = "NO ACTION";
        private String deleteRule = "NO ACTION";

        public Builder constraintName(String constraintName) {
            this.constraintName = constraintName;
            return this;
        }

        public Builder columnNames(List<String> columnNames) {
            this.columnNames = columnNames;
            return this;
        }

        public Builder referencedTableName(String referencedTableName) {
            this.referencedTableName = referencedTableName;
            return this;
        }

        public Builder referencedColumnNames(List<String> referencedColumnNames) {
            this.referencedColumnNames = referencedColumnNames;
            return this;
        }

        public Builder updateRule(String updateRule) {
            this.updateRule = updateRule;
            return this;
        }

        public Builder deleteRule(String deleteRule) {
            this.deleteRule = deleteRule;
            return this;
        }

        public ForeignKeyMetadataImpl build() {
            return new ForeignKeyMetadataImpl(
                    constraintName, columnNames, referencedTableName,
                    referencedColumnNames, updateRule, deleteRule
            );
        }
    }
}
