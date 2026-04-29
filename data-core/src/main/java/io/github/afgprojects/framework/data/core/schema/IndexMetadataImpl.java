package io.github.afgprojects.framework.data.core.schema;

import java.util.List;

/**
 * 索引元数据实现
 */
public final class IndexMetadataImpl implements IndexMetadata {

    private final String indexName;
    private final List<String> columnNames;
    private final boolean unique;
    private final String indexType;

    public IndexMetadataImpl(String indexName, List<String> columnNames, boolean unique, String indexType) {
        this.indexName = indexName;
        this.columnNames = columnNames;
        this.unique = unique;
        this.indexType = indexType;
    }

    @Override
    public String getIndexName() {
        return indexName;
    }

    @Override
    public List<String> getColumnNames() {
        return columnNames;
    }

    @Override
    public boolean isUnique() {
        return unique;
    }

    @Override
    public String getIndexType() {
        return indexType;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String indexName;
        private List<String> columnNames = List.of();
        private boolean unique;
        private String indexType = "BTREE";

        public Builder indexName(String indexName) {
            this.indexName = indexName;
            return this;
        }

        public Builder columnNames(List<String> columnNames) {
            this.columnNames = columnNames;
            return this;
        }

        public Builder unique(boolean unique) {
            this.unique = unique;
            return this;
        }

        public Builder indexType(String indexType) {
            this.indexType = indexType;
            return this;
        }

        public IndexMetadataImpl build() {
            return new IndexMetadataImpl(indexName, columnNames, unique, indexType);
        }
    }
}
