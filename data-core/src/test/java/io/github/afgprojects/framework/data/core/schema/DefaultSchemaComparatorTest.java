package io.github.afgprojects.framework.data.core.schema;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DefaultSchemaComparator 单元测试
 */
@DisplayName("DefaultSchemaComparator 测试")
class DefaultSchemaComparatorTest {

    private final DefaultSchemaComparator comparator = new DefaultSchemaComparator();

    // ========== 列 ADD 检测 ==========

    @Nested
    @DisplayName("列 ADD 检测")
    class ColumnAddTests {

        @Test
        @DisplayName("检测新增列")
        void shouldDetectAddedColumn() {
            SchemaMetadata source = schemaWithColumns(
                    col("id", "BIGINT", false),
                    col("name", "VARCHAR(50)", false),
                    col("email", "VARCHAR(100)", true)  // 新增列
            );
            SchemaMetadata target = schemaWithColumns(
                    col("id", "BIGINT", false),
                    col("name", "VARCHAR(50)", false)
            );

            SchemaDiff diff = comparator.compare(source, target);

            assertThat(diff.columnDiffs()).anyMatch(cd ->
                    cd.diffType() == DiffType.ADD && cd.columnName().equals("email"));
        }

        @Test
        @DisplayName("新增多个列")
        void shouldDetectMultipleAddedColumns() {
            SchemaMetadata source = schemaWithColumns(
                    col("id", "BIGINT", false),
                    col("name", "VARCHAR(50)", false),
                    col("email", "VARCHAR(100)", true),
                    col("phone", "VARCHAR(20)", true)
            );
            SchemaMetadata target = schemaWithColumns(
                    col("id", "BIGINT", false),
                    col("name", "VARCHAR(50)", false)
            );

            SchemaDiff diff = comparator.compare(source, target);

            assertThat(diff.hasAddedColumns()).isTrue();
            assertThat(diff.columnDiffs().stream()
                    .filter(cd -> cd.diffType() == DiffType.ADD)
                    .map(ColumnDiff::columnName))
                    .containsExactlyInAnyOrder("email", "phone");
        }
    }

    // ========== 列 MODIFY 检测 ==========

    @Nested
    @DisplayName("列 MODIFY 检测")
    class ColumnModifyTests {

        @Test
        @DisplayName("检测列类型变更")
        void shouldDetectColumnTypeChange() {
            SchemaMetadata source = schemaWithColumns(
                    col("id", "BIGINT", false),
                    col("name", "TEXT", false)  // 从 VARCHAR 变为 TEXT
            );
            SchemaMetadata target = schemaWithColumns(
                    col("id", "BIGINT", false),
                    col("name", "VARCHAR(50)", false)
            );

            SchemaDiff diff = comparator.compare(source, target);

            assertThat(diff.hasModifiedColumns()).isTrue();
            assertThat(diff.columnDiffs()).anyMatch(cd ->
                    cd.diffType() == DiffType.MODIFY && cd.columnName().equals("name"));
        }

        @Test
        @DisplayName("检测列 nullable 变更")
        void shouldDetectColumnNullableChange() {
            SchemaMetadata source = schemaWithColumns(
                    col("id", "BIGINT", false),
                    col("name", "VARCHAR(50)", true)  // 从 not null 变为 nullable
            );
            SchemaMetadata target = schemaWithColumns(
                    col("id", "BIGINT", false),
                    col("name", "VARCHAR(50)", false)
            );

            SchemaDiff diff = comparator.compare(source, target);

            assertThat(diff.hasModifiedColumns()).isTrue();
        }

        @Test
        @DisplayName("检测列默认值变更")
        void shouldDetectColumnDefaultValueChange() {
            SchemaMetadata source = schemaWithColumns(
                    colWithDefault("status", "INT", false, "1"),
                    col("id", "BIGINT", false)
            );
            SchemaMetadata target = schemaWithColumns(
                    colWithDefault("status", "INT", false, "0"),
                    col("id", "BIGINT", false)
            );

            SchemaDiff diff = comparator.compare(source, target);

            assertThat(diff.hasModifiedColumns()).isTrue();
        }
    }

    // ========== 列 DROP 检测 ==========

    @Nested
    @DisplayName("列 DROP 检测")
    class ColumnDropTests {

        @Test
        @DisplayName("检测删除列")
        void shouldDetectDroppedColumn() {
            SchemaMetadata source = schemaWithColumns(
                    col("id", "BIGINT", false),
                    col("name", "VARCHAR(50)", false)
            );
            SchemaMetadata target = schemaWithColumns(
                    col("id", "BIGINT", false),
                    col("name", "VARCHAR(50)", false),
                    col("email", "VARCHAR(100)", true)  // 目标有但源没有
            );

            SchemaDiff diff = comparator.compare(source, target);

            assertThat(diff.hasDroppedColumns()).isTrue();
            assertThat(diff.columnDiffs()).anyMatch(cd ->
                    cd.diffType() == DiffType.DROP && cd.columnName().equals("email"));
        }
    }

    // ========== 索引变更 ==========

    @Nested
    @DisplayName("索引变更检测")
    class IndexDiffTests {

        @Test
        @DisplayName("检测新增索引")
        void shouldDetectAddedIndex() {
            SchemaMetadata source = schemaWithColumnsAndIndexes(
                    List.of(col("id", "BIGINT", false), col("name", "VARCHAR(50)", false)),
                    List.of(idx("idx_user_name", "name", false))  // 新增索引
            );
            SchemaMetadata target = schemaWithColumnsAndIndexes(
                    List.of(col("id", "BIGINT", false), col("name", "VARCHAR(50)", false)),
                    Collections.emptyList()
            );

            SchemaDiff diff = comparator.compare(source, target);

            assertThat(diff.indexDiff()).isNotNull();
            assertThat(diff.indexDiff().addedIndexes()).anyMatch(i ->
                    i.getIndexName().equals("idx_user_name"));
        }

        @Test
        @DisplayName("检测删除索引")
        void shouldDetectDroppedIndex() {
            SchemaMetadata source = schemaWithColumnsAndIndexes(
                    List.of(col("id", "BIGINT", false), col("name", "VARCHAR(50)", false)),
                    Collections.emptyList()
            );
            SchemaMetadata target = schemaWithColumnsAndIndexes(
                    List.of(col("id", "BIGINT", false), col("name", "VARCHAR(50)", false)),
                    List.of(idx("idx_user_name", "name", false))
            );

            SchemaDiff diff = comparator.compare(source, target);

            assertThat(diff.indexDiff()).isNotNull();
            assertThat(diff.indexDiff().droppedIndexes()).anyMatch(i ->
                    i.getIndexName().equals("idx_user_name"));
        }

        @Test
        @DisplayName("检测索引修改（唯一性变更）")
        void shouldDetectModifiedIndex() {
            SchemaMetadata source = schemaWithColumnsAndIndexes(
                    List.of(col("id", "BIGINT", false), col("name", "VARCHAR(50)", false)),
                    List.of(idx("idx_user_name", "name", true))  // 唯一索引
            );
            SchemaMetadata target = schemaWithColumnsAndIndexes(
                    List.of(col("id", "BIGINT", false), col("name", "VARCHAR(50)", false)),
                    List.of(idx("idx_user_name", "name", false))  // 非唯一索引
            );

            SchemaDiff diff = comparator.compare(source, target);

            assertThat(diff.indexDiff()).isNotNull();
            assertThat(diff.indexDiff().modifiedIndexes()).anyMatch(i ->
                    i.getIndexName().equals("idx_user_name"));
        }
    }

    // ========== 空比较 ==========

    @Nested
    @DisplayName("空比较")
    class EmptyComparisonTests {

        @Test
        @DisplayName("相同 Schema 无差异")
        void shouldReturnNoDifferencesForIdenticalSchemas() {
            SchemaMetadata source = schemaWithColumns(
                    col("id", "BIGINT", false),
                    col("name", "VARCHAR(50)", false)
            );
            SchemaMetadata target = schemaWithColumns(
                    col("id", "BIGINT", false),
                    col("name", "VARCHAR(50)", false)
            );

            SchemaDiff diff = comparator.compare(source, target);

            assertThat(diff.hasDifferences()).isFalse();
        }

        @Test
        @DisplayName("空 Schema 无差异")
        void shouldReturnNoDifferencesForEmptySchemas() {
            SchemaMetadata source = emptySchema();
            SchemaMetadata target = emptySchema();

            SchemaDiff diff = comparator.compare(source, target);

            assertThat(diff.hasDifferences()).isFalse();
        }

        @Test
        @DisplayName("target 为 null 时所有列都是新增")
        void shouldMarkAllColumnsAsAdded_whenTargetIsNull() {
            SchemaMetadata source = schemaWithColumns(
                    col("id", "BIGINT", false),
                    col("name", "VARCHAR(50)", false)
            );

            SchemaDiff diff = comparator.compare(source, null);

            assertThat(diff.hasAddedColumns()).isTrue();
            assertThat(diff.columnDiffs()).hasSize(2);
            assertThat(diff.columnDiffs()).allMatch(cd -> cd.diffType() == DiffType.ADD);
        }
    }

    // ========== 冲突分析 ==========

    @Nested
    @DisplayName("冲突分析")
    class ConflictAnalysisTests {

        @Test
        @DisplayName("三路比较检测冲突 - 同列不同修改")
        void shouldDetectConflictsInThreeWayComparison() {
            // entity: name TEXT (实体修改了类型)
            // database: name VARCHAR(200) (数据库修改了类型)
            // changeLog: name VARCHAR(50) (ChangeLog 原始)
            // 注意：normalizeDataType 去掉括号内容，所以 VARCHAR(200) 和 VARCHAR(50) 归一化为 VARCHAR
            // TEXT vs VARCHAR 是不同的归一化类型，会触发 MODIFY diff
            SchemaMetadata entity = schemaWithColumns(
                    col("id", "BIGINT", false),
                    col("name", "TEXT", false)
            );
            SchemaMetadata database = schemaWithColumns(
                    col("id", "BIGINT", false),
                    col("name", "VARCHAR(200)", false)
            );
            SchemaMetadata changeLog = schemaWithColumns(
                    col("id", "BIGINT", false),
                    col("name", "VARCHAR(50)", false)
            );

            ThreeWayDiff threeWayDiff = comparator.compareThreeWay(entity, database, changeLog);

            assertThat(threeWayDiff.hasConflicts()).isTrue();
        }

        @Test
        @DisplayName("三路比较无冲突 - 不同列修改")
        void shouldNotDetectConflictWhenDifferentColumnsModified() {
            SchemaMetadata entity = schemaWithColumns(
                    col("id", "BIGINT", false),
                    col("name", "VARCHAR(100)", false),  // 实体修改 name
                    col("age", "INT", true)
            );
            SchemaMetadata database = schemaWithColumns(
                    col("id", "BIGINT", false),
                    col("name", "VARCHAR(50)", false),
                    col("age", "BIGINT", true)  // 数据库修改 age
            );
            SchemaMetadata changeLog = schemaWithColumns(
                    col("id", "BIGINT", false),
                    col("name", "VARCHAR(50)", false),
                    col("age", "INT", true)
            );

            ThreeWayDiff threeWayDiff = comparator.compareThreeWay(entity, database, changeLog);

            assertThat(threeWayDiff.hasConflicts()).isFalse();
        }

        @Test
        @DisplayName("database 为 null 时无冲突")
        void shouldNotDetectConflict_whenDatabaseIsNull() {
            SchemaMetadata entity = schemaWithColumns(
                    col("id", "BIGINT", false),
                    col("name", "VARCHAR(100)", false)
            );
            SchemaMetadata changeLog = schemaWithColumns(
                    col("id", "BIGINT", false),
                    col("name", "VARCHAR(50)", false)
            );

            ThreeWayDiff threeWayDiff = comparator.compareThreeWay(entity, null, changeLog);

            assertThat(threeWayDiff.hasConflicts()).isFalse();
        }
    }

    // ========== 辅助方法 ==========

    private SchemaMetadata emptySchema() {
        return new SimpleSchemaMetadata("test_table", Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }

    private SchemaMetadata schemaWithColumns(ColumnMetadata... columns) {
        return new SimpleSchemaMetadata("test_table", List.of(columns), Collections.emptyList(), Collections.emptyList());
    }

    private SchemaMetadata schemaWithColumnsAndIndexes(List<ColumnMetadata> columns, List<IndexMetadata> indexes) {
        return new SimpleSchemaMetadata("test_table", columns, indexes, Collections.emptyList());
    }

    private ColumnMetadata col(String name, String type, boolean nullable) {
        return new SimpleColumnMetadata(name, type, nullable, null, false);
    }

    private ColumnMetadata colWithDefault(String name, String type, boolean nullable, String defaultValue) {
        return new SimpleColumnMetadata(name, type, nullable, defaultValue, false);
    }

    private IndexMetadata idx(String name, String columnName, boolean unique) {
        return new SimpleIndexMetadata(name, List.of(columnName), unique);
    }

    // ========== 简单实现类 ==========

    private record SimpleSchemaMetadata(
            String tableName,
            List<ColumnMetadata> columns,
            List<IndexMetadata> indexes,
            List<ForeignKeyMetadata> foreignKeys
    ) implements SchemaMetadata {
        @Override
        public String getTableName() { return tableName; }
        @Override
        public String getComment() { return null; }
        @Override
        public List<ColumnMetadata> getColumns() { return columns; }
        @Override
        public PrimaryKeyMetadata getPrimaryKey() { return null; }
        @Override
        public List<IndexMetadata> getIndexes() { return indexes; }
        @Override
        public List<ForeignKeyMetadata> getForeignKeys() { return foreignKeys; }
    }

    private record SimpleColumnMetadata(
            String columnName,
            String dataType,
            boolean nullable,
            String defaultValue,
            boolean unique
    ) implements ColumnMetadata {
        @Override
        public String getColumnName() { return columnName; }
        @Override
        public String getDataType() { return dataType; }
        @Override
        public boolean isNullable() { return nullable; }
        @Override
        public String getDefaultValue() { return defaultValue; }
        @Override
        public String getComment() { return null; }
        @Override
        public boolean isUnique() { return unique; }
        @Override
        public boolean isPrimaryKey() { return false; }
        @Override
        public boolean isAutoIncrement() { return false; }
    }

    private record SimpleIndexMetadata(
            String indexName,
            List<String> columnNames,
            boolean unique
    ) implements IndexMetadata {
        @Override
        public String getIndexName() { return indexName; }
        @Override
        public List<String> getColumnNames() { return columnNames; }
        @Override
        public boolean isUnique() { return unique; }
        @Override
        public String getIndexType() { return "BTREE"; }
    }
}
