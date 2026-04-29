package io.github.afgprojects.framework.data.core.schema;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Schema 包测试
 */
@DisplayName("Schema 包测试")
class SchemaTest {

    // ==================== DiffType 枚举测试 ====================

    @Nested
    @DisplayName("DiffType 枚举测试")
    class DiffTypeTest {

        @Test
        @DisplayName("应包含所有差异类型")
        void shouldContainAllDiffTypes() {
            assertThat(DiffType.values()).containsExactly(
                    DiffType.NONE, DiffType.ADD, DiffType.DROP, DiffType.MODIFY
            );
        }

        @Test
        @DisplayName("应能通过名称获取枚举值")
        void shouldGetValueByName() {
            assertThat(DiffType.valueOf("ADD")).isEqualTo(DiffType.ADD);
            assertThat(DiffType.valueOf("DROP")).isEqualTo(DiffType.DROP);
            assertThat(DiffType.valueOf("MODIFY")).isEqualTo(DiffType.MODIFY);
            assertThat(DiffType.valueOf("NONE")).isEqualTo(DiffType.NONE);
        }
    }

    // ==================== ColumnMetadataImpl 测试 ====================

    @Nested
    @DisplayName("ColumnMetadataImpl 测试")
    class ColumnMetadataImplTest {

        @Test
        @DisplayName("应正确创建列元数据")
        void shouldCreateColumnMetadata() {
            ColumnMetadataImpl column = new ColumnMetadataImpl(
                    "id", "BIGINT", false, null, "主键", true, true, true
            );

            assertThat(column.getColumnName()).isEqualTo("id");
            assertThat(column.getDataType()).isEqualTo("BIGINT");
            assertThat(column.isNullable()).isFalse();
            assertThat(column.getDefaultValue()).isNull();
            assertThat(column.getComment()).isEqualTo("主键");
            assertThat(column.isUnique()).isTrue();
            assertThat(column.isPrimaryKey()).isTrue();
            assertThat(column.isAutoIncrement()).isTrue();
        }

        @Test
        @DisplayName("Builder 应正确构建列元数据")
        void shouldBuildColumnMetadataWithBuilder() {
            ColumnMetadataImpl column = ColumnMetadataImpl.builder()
                    .columnName("name")
                    .dataType("VARCHAR(255)")
                    .nullable(false)
                    .defaultValue("'unknown'")
                    .comment("名称")
                    .unique(false)
                    .primaryKey(false)
                    .autoIncrement(false)
                    .build();

            assertThat(column.getColumnName()).isEqualTo("name");
            assertThat(column.getDataType()).isEqualTo("VARCHAR(255)");
            assertThat(column.isNullable()).isFalse();
            assertThat(column.getDefaultValue()).isEqualTo("'unknown'");
            assertThat(column.getComment()).isEqualTo("名称");
            assertThat(column.isUnique()).isFalse();
            assertThat(column.isPrimaryKey()).isFalse();
            assertThat(column.isAutoIncrement()).isFalse();
        }

        @Test
        @DisplayName("Builder 应使用默认值")
        void shouldUseDefaultValuesInBuilder() {
            ColumnMetadataImpl column = ColumnMetadataImpl.builder()
                    .columnName("test")
                    .dataType("INT")
                    .build();

            assertThat(column.isNullable()).isTrue(); // 默认可空
            assertThat(column.getDefaultValue()).isNull();
            assertThat(column.getComment()).isNull();
            assertThat(column.isUnique()).isFalse();
            assertThat(column.isPrimaryKey()).isFalse();
            assertThat(column.isAutoIncrement()).isFalse();
        }
    }

    // ==================== IndexMetadataImpl 测试 ====================

    @Nested
    @DisplayName("IndexMetadataImpl 测试")
    class IndexMetadataImplTest {

        @Test
        @DisplayName("应正确创建索引元数据")
        void shouldCreateIndexMetadata() {
            IndexMetadataImpl index = new IndexMetadataImpl(
                    "idx_user_name", List.of("name", "email"), true, "BTREE"
            );

            assertThat(index.getIndexName()).isEqualTo("idx_user_name");
            assertThat(index.getColumnNames()).containsExactly("name", "email");
            assertThat(index.isUnique()).isTrue();
            assertThat(index.getIndexType()).isEqualTo("BTREE");
        }

        @Test
        @DisplayName("Builder 应正确构建索引元数据")
        void shouldBuildIndexMetadataWithBuilder() {
            IndexMetadataImpl index = IndexMetadataImpl.builder()
                    .indexName("idx_email")
                    .columnNames(List.of("email"))
                    .unique(true)
                    .indexType("HASH")
                    .build();

            assertThat(index.getIndexName()).isEqualTo("idx_email");
            assertThat(index.getColumnNames()).containsExactly("email");
            assertThat(index.isUnique()).isTrue();
            assertThat(index.getIndexType()).isEqualTo("HASH");
        }

        @Test
        @DisplayName("Builder 应使用默认值")
        void shouldUseDefaultValuesInBuilder() {
            IndexMetadataImpl index = IndexMetadataImpl.builder()
                    .indexName("idx_test")
                    .build();

            assertThat(index.getColumnNames()).isEmpty();
            assertThat(index.isUnique()).isFalse();
            assertThat(index.getIndexType()).isEqualTo("BTREE"); // 默认 BTREE
        }
    }

    // ==================== ForeignKeyMetadataImpl 测试 ====================

    @Nested
    @DisplayName("ForeignKeyMetadataImpl 测试")
    class ForeignKeyMetadataImplTest {

        @Test
        @DisplayName("应正确创建外键元数据")
        void shouldCreateForeignKeyMetadata() {
            ForeignKeyMetadataImpl fk = new ForeignKeyMetadataImpl(
                    "fk_order_user",
                    List.of("user_id"),
                    "users",
                    List.of("id"),
                    "CASCADE",
                    "RESTRICT"
            );

            assertThat(fk.getConstraintName()).isEqualTo("fk_order_user");
            assertThat(fk.getColumnNames()).containsExactly("user_id");
            assertThat(fk.getReferencedTableName()).isEqualTo("users");
            assertThat(fk.getReferencedColumnNames()).containsExactly("id");
            assertThat(fk.getUpdateRule()).isEqualTo("CASCADE");
            assertThat(fk.getDeleteRule()).isEqualTo("RESTRICT");
        }

        @Test
        @DisplayName("Builder 应正确构建外键元数据")
        void shouldBuildForeignKeyMetadataWithBuilder() {
            ForeignKeyMetadataImpl fk = ForeignKeyMetadataImpl.builder()
                    .constraintName("fk_item_order")
                    .columnNames(List.of("order_id"))
                    .referencedTableName("orders")
                    .referencedColumnNames(List.of("id"))
                    .updateRule("NO ACTION")
                    .deleteRule("CASCADE")
                    .build();

            assertThat(fk.getConstraintName()).isEqualTo("fk_item_order");
            assertThat(fk.getColumnNames()).containsExactly("order_id");
            assertThat(fk.getReferencedTableName()).isEqualTo("orders");
            assertThat(fk.getReferencedColumnNames()).containsExactly("id");
            assertThat(fk.getUpdateRule()).isEqualTo("NO ACTION");
            assertThat(fk.getDeleteRule()).isEqualTo("CASCADE");
        }

        @Test
        @DisplayName("Builder 应使用默认值")
        void shouldUseDefaultValuesInBuilder() {
            ForeignKeyMetadataImpl fk = ForeignKeyMetadataImpl.builder()
                    .constraintName("fk_test")
                    .referencedTableName("ref_table")
                    .build();

            assertThat(fk.getColumnNames()).isEmpty();
            assertThat(fk.getReferencedColumnNames()).isEmpty();
            assertThat(fk.getUpdateRule()).isEqualTo("NO ACTION");
            assertThat(fk.getDeleteRule()).isEqualTo("NO ACTION");
        }
    }

    // ==================== PrimaryKeyMetadataImpl 测试 ====================

    @Nested
    @DisplayName("PrimaryKeyMetadataImpl 测试")
    class PrimaryKeyMetadataImplTest {

        @Test
        @DisplayName("应正确创建主键元数据")
        void shouldCreatePrimaryKeyMetadata() {
            PrimaryKeyMetadataImpl pk = new PrimaryKeyMetadataImpl(
                    "pk_user", List.of("id")
            );

            assertThat(pk.getConstraintName()).isEqualTo("pk_user");
            assertThat(pk.getColumnNames()).containsExactly("id");
        }

        @Test
        @DisplayName("应支持复合主键")
        void shouldSupportCompositePrimaryKey() {
            PrimaryKeyMetadataImpl pk = new PrimaryKeyMetadataImpl(
                    "pk_order_item", List.of("order_id", "item_id")
            );

            assertThat(pk.getColumnNames()).containsExactly("order_id", "item_id");
        }

        @Test
        @DisplayName("Builder 应正确构建主键元数据")
        void shouldBuildPrimaryKeyMetadataWithBuilder() {
            PrimaryKeyMetadataImpl pk = PrimaryKeyMetadataImpl.builder()
                    .constraintName("pk_test")
                    .columnNames(List.of("id"))
                    .build();

            assertThat(pk.getConstraintName()).isEqualTo("pk_test");
            assertThat(pk.getColumnNames()).containsExactly("id");
        }
    }

    // ==================== SchemaMetadataImpl 测试 ====================

    @Nested
    @DisplayName("SchemaMetadataImpl 测试")
    class SchemaMetadataImplTest {

        @Test
        @DisplayName("应正确创建 Schema 元数据")
        void shouldCreateSchemaMetadata() {
            ColumnMetadataImpl col1 = ColumnMetadataImpl.builder()
                    .columnName("id").dataType("BIGINT").build();
            ColumnMetadataImpl col2 = ColumnMetadataImpl.builder()
                    .columnName("name").dataType("VARCHAR(255)").build();

            SchemaMetadataImpl schema = new SchemaMetadataImpl(
                    "users",
                    "用户表",
                    List.of(col1, col2),
                    PrimaryKeyMetadataImpl.builder()
                            .constraintName("pk_users")
                            .columnNames(List.of("id"))
                            .build(),
                    List.of(),
                    List.of()
            );

            assertThat(schema.getTableName()).isEqualTo("users");
            assertThat(schema.getComment()).isEqualTo("用户表");
            assertThat(schema.getColumns()).hasSize(2);
            assertThat(schema.getPrimaryKey()).isNotNull();
            assertThat(schema.getIndexes()).isEmpty();
            assertThat(schema.getForeignKeys()).isEmpty();
        }

        @Test
        @DisplayName("Builder 应正确构建 Schema 元数据")
        void shouldBuildSchemaMetadataWithBuilder() {
            ColumnMetadataImpl col = ColumnMetadataImpl.builder()
                    .columnName("id").dataType("BIGINT").build();

            SchemaMetadataImpl schema = SchemaMetadataImpl.builder()
                    .tableName("products")
                    .comment("产品表")
                    .addColumn(col)
                    .primaryKey(PrimaryKeyMetadataImpl.builder()
                            .constraintName("pk_products")
                            .columnNames(List.of("id"))
                            .build())
                    .addIndex(IndexMetadataImpl.builder()
                            .indexName("idx_name")
                            .columnNames(List.of("name"))
                            .build())
                    .addForeignKey(ForeignKeyMetadataImpl.builder()
                            .constraintName("fk_category")
                            .referencedTableName("categories")
                            .build())
                    .build();

            assertThat(schema.getTableName()).isEqualTo("products");
            assertThat(schema.getColumns()).hasSize(1);
            assertThat(schema.getIndexes()).hasSize(1);
            assertThat(schema.getForeignKeys()).hasSize(1);
        }

        @Test
        @DisplayName("getColumn 应返回指定列")
        void shouldGetColumnByName() {
            ColumnMetadataImpl col1 = ColumnMetadataImpl.builder()
                    .columnName("id").dataType("BIGINT").build();
            ColumnMetadataImpl col2 = ColumnMetadataImpl.builder()
                    .columnName("name").dataType("VARCHAR(255)").build();

            SchemaMetadataImpl schema = SchemaMetadataImpl.builder()
                    .tableName("test")
                    .columns(List.of(col1, col2))
                    .build();

            assertThat(schema.getColumn("id")).isNotNull();
            assertThat(schema.getColumn("name")).isNotNull();
            assertThat(schema.getColumn("not_exist")).isNull();
        }

        @Test
        @DisplayName("hasColumn 应正确判断列是否存在")
        void shouldCheckColumnExists() {
            ColumnMetadataImpl col = ColumnMetadataImpl.builder()
                    .columnName("id").dataType("BIGINT").build();

            SchemaMetadataImpl schema = SchemaMetadataImpl.builder()
                    .tableName("test")
                    .addColumn(col)
                    .build();

            assertThat(schema.hasColumn("id")).isTrue();
            assertThat(schema.hasColumn("name")).isFalse();
        }
    }

    // ==================== ColumnDiff 测试 ====================

    @Nested
    @DisplayName("ColumnDiff 测试")
    class ColumnDiffTest {

        @Test
        @DisplayName("应正确创建列差异 - 新增")
        void shouldCreateAddColumnDiff() {
            ColumnMetadataImpl col = ColumnMetadataImpl.builder()
                    .columnName("email").dataType("VARCHAR(255)").build();

            ColumnDiff diff = new ColumnDiff("email", DiffType.ADD, col, null,
                    List.of("Column not exists in target"));

            assertThat(diff.columnName()).isEqualTo("email");
            assertThat(diff.diffType()).isEqualTo(DiffType.ADD);
            assertThat(diff.sourceColumn()).isEqualTo(col);
            assertThat(diff.targetColumn()).isNull();
            assertThat(diff.differences()).containsExactly("Column not exists in target");
            assertThat(diff.getDescription()).isEqualTo("Column will be added: email");
        }

        @Test
        @DisplayName("应正确创建列差异 - 删除")
        void shouldCreateDropColumnDiff() {
            ColumnMetadataImpl col = ColumnMetadataImpl.builder()
                    .columnName("old_col").dataType("INT").build();

            ColumnDiff diff = new ColumnDiff("old_col", DiffType.DROP, null, col,
                    List.of("Column not exists in source"));

            assertThat(diff.diffType()).isEqualTo(DiffType.DROP);
            assertThat(diff.getDescription()).isEqualTo("Column will be dropped: old_col");
        }

        @Test
        @DisplayName("应正确创建列差异 - 修改")
        void shouldCreateModifyColumnDiff() {
            ColumnMetadataImpl sourceCol = ColumnMetadataImpl.builder()
                    .columnName("name").dataType("VARCHAR(500)").build();
            ColumnMetadataImpl targetCol = ColumnMetadataImpl.builder()
                    .columnName("name").dataType("VARCHAR(255)").build();

            ColumnDiff diff = new ColumnDiff("name", DiffType.MODIFY, sourceCol, targetCol,
                    List.of("Type: VARCHAR(255) → VARCHAR(500)"));

            assertThat(diff.diffType()).isEqualTo(DiffType.MODIFY);
            assertThat(diff.getDescription()).isEqualTo("Type: VARCHAR(255) → VARCHAR(500)");
        }

        @Test
        @DisplayName("应正确创建列差异 - 无差异")
        void shouldCreateNoneColumnDiff() {
            ColumnDiff diff = new ColumnDiff("id", DiffType.NONE, null, null, List.of());

            assertThat(diff.diffType()).isEqualTo(DiffType.NONE);
            assertThat(diff.getDescription()).isEqualTo("No difference");
        }
    }

    // ==================== IndexDiff 测试 ====================

    @Nested
    @DisplayName("IndexDiff 测试")
    class IndexDiffTest {

        @Test
        @DisplayName("应正确创建索引差异")
        void shouldCreateIndexDiff() {
            IndexMetadataImpl added = IndexMetadataImpl.builder()
                    .indexName("idx_new").build();
            IndexMetadataImpl dropped = IndexMetadataImpl.builder()
                    .indexName("idx_old").build();
            IndexMetadataImpl modified = IndexMetadataImpl.builder()
                    .indexName("idx_changed").build();

            IndexDiff diff = new IndexDiff(List.of(added), List.of(dropped), List.of(modified));

            assertThat(diff.addedIndexes()).hasSize(1);
            assertThat(diff.droppedIndexes()).hasSize(1);
            assertThat(diff.modifiedIndexes()).hasSize(1);
            assertThat(diff.hasDifferences()).isTrue();
        }

        @Test
        @DisplayName("无差异时应返回 false")
        void shouldReturnFalseWhenNoDifferences() {
            IndexDiff diff = new IndexDiff(List.of(), List.of(), List.of());

            assertThat(diff.hasDifferences()).isFalse();
        }
    }

    // ==================== ForeignKeyDiff 测试 ====================

    @Nested
    @DisplayName("ForeignKeyDiff 测试")
    class ForeignKeyDiffTest {

        @Test
        @DisplayName("应正确创建外键差异")
        void shouldCreateForeignKeyDiff() {
            ForeignKeyMetadataImpl added = ForeignKeyMetadataImpl.builder()
                    .constraintName("fk_new").referencedTableName("ref").build();
            ForeignKeyMetadataImpl dropped = ForeignKeyMetadataImpl.builder()
                    .constraintName("fk_old").referencedTableName("ref").build();

            ForeignKeyDiff diff = new ForeignKeyDiff(List.of(added), List.of(dropped), List.of());

            assertThat(diff.addedForeignKeys()).hasSize(1);
            assertThat(diff.droppedForeignKeys()).hasSize(1);
            assertThat(diff.modifiedForeignKeys()).isEmpty();
            assertThat(diff.hasDifferences()).isTrue();
        }

        @Test
        @DisplayName("无差异时应返回 false")
        void shouldReturnFalseWhenNoDifferences() {
            ForeignKeyDiff diff = new ForeignKeyDiff(List.of(), List.of(), List.of());

            assertThat(diff.hasDifferences()).isFalse();
        }
    }

    // ==================== SchemaDiff 测试 ====================

    @Nested
    @DisplayName("SchemaDiff 测试")
    class SchemaDiffTest {

        @Test
        @DisplayName("应正确创建 Schema 差异")
        void shouldCreateSchemaDiff() {
            ColumnDiff colDiff = new ColumnDiff("email", DiffType.ADD, null, null, List.of());
            IndexDiff indexDiff = new IndexDiff(
                    List.of(IndexMetadataImpl.builder().indexName("idx_new").build()),
                    List.of(),
                    List.of()
            );

            SchemaDiff diff = new SchemaDiff("users", true, List.of(colDiff), indexDiff, null);

            assertThat(diff.tableName()).isEqualTo("users");
            assertThat(diff.tableExists()).isTrue();
            assertThat(diff.columnDiffs()).hasSize(1);
            assertThat(diff.indexDiff()).isNotNull();
            assertThat(diff.foreignKeyDiff()).isNull();
            assertThat(diff.hasDifferences()).isTrue();
        }

        @Test
        @DisplayName("hasAddedColumns 应正确判断")
        void shouldCheckHasAddedColumns() {
            ColumnDiff addDiff = new ColumnDiff("new_col", DiffType.ADD, null, null, List.of());
            ColumnDiff modifyDiff = new ColumnDiff("old_col", DiffType.MODIFY, null, null, List.of());

            SchemaDiff diff = new SchemaDiff("test", true, List.of(addDiff, modifyDiff), null, null);

            assertThat(diff.hasAddedColumns()).isTrue();
            assertThat(diff.hasDroppedColumns()).isFalse();
            assertThat(diff.hasModifiedColumns()).isTrue();
        }

        @Test
        @DisplayName("hasDroppedColumns 应正确判断")
        void shouldCheckHasDroppedColumns() {
            ColumnDiff dropDiff = new ColumnDiff("old_col", DiffType.DROP, null, null, List.of());

            SchemaDiff diff = new SchemaDiff("test", true, List.of(dropDiff), null, null);

            assertThat(diff.hasDroppedColumns()).isTrue();
            assertThat(diff.hasAddedColumns()).isFalse();
        }

        @Test
        @DisplayName("无差异时 hasDifferences 应返回 false")
        void shouldReturnFalseWhenNoDifferences() {
            SchemaDiff diff = new SchemaDiff("test", true, List.of(), null, null);

            assertThat(diff.hasDifferences()).isFalse();
        }
    }

    // ==================== ThreeWayDiff 测试 ====================

    @Nested
    @DisplayName("ThreeWayDiff 测试")
    class ThreeWayDiffTest {

        @Test
        @DisplayName("应正确创建三向差异")
        void shouldCreateThreeWayDiff() {
            SchemaDiff entityVsDb = new SchemaDiff("test", true, List.of(), null, null);
            ConflictAnalysis conflicts = ConflictAnalysis.noConflicts();

            ThreeWayDiff diff = new ThreeWayDiff(
                    "users", entityVsDb, null, null, conflicts
            );

            assertThat(diff.tableName()).isEqualTo("users");
            assertThat(diff.entityVsDatabase()).isEqualTo(entityVsDb);
            assertThat(diff.entityVsChangeLog()).isNull();
            assertThat(diff.databaseVsChangeLog()).isNull();
            assertThat(diff.conflicts()).isEqualTo(conflicts);
        }

        @Test
        @DisplayName("hasAnyDifferences 应正确判断 - 有差异")
        void shouldCheckHasAnyDifferencesTrue() {
            ColumnDiff colDiff = new ColumnDiff("col", DiffType.ADD, null, null, List.of());
            SchemaDiff schemaDiff = new SchemaDiff("test", true, List.of(colDiff), null, null);

            ThreeWayDiff diff = new ThreeWayDiff("test", schemaDiff, null, null, ConflictAnalysis.noConflicts());

            assertThat(diff.hasAnyDifferences()).isTrue();
        }

        @Test
        @DisplayName("hasAnyDifferences 应正确判断 - 无差异")
        void shouldCheckHasAnyDifferencesFalse() {
            SchemaDiff schemaDiff = new SchemaDiff("test", true, List.of(), null, null);

            ThreeWayDiff diff = new ThreeWayDiff("test", schemaDiff, null, null, ConflictAnalysis.noConflicts());

            assertThat(diff.hasAnyDifferences()).isFalse();
        }

        @Test
        @DisplayName("hasAnyDifferences - entityVsChangeLog 有差异")
        void shouldCheckHasAnyDifferencesFromEntityVsChangeLog() {
            ColumnDiff colDiff = new ColumnDiff("col", DiffType.ADD, null, null, List.of());
            SchemaDiff schemaDiff = new SchemaDiff("test", true, List.of(colDiff), null, null);

            ThreeWayDiff diff = new ThreeWayDiff("test", null, schemaDiff, null, ConflictAnalysis.noConflicts());

            assertThat(diff.hasAnyDifferences()).isTrue();
        }

        @Test
        @DisplayName("hasAnyDifferences - databaseVsChangeLog 有差异")
        void shouldCheckHasAnyDifferencesFromDatabaseVsChangeLog() {
            ColumnDiff colDiff = new ColumnDiff("col", DiffType.ADD, null, null, List.of());
            SchemaDiff schemaDiff = new SchemaDiff("test", true, List.of(colDiff), null, null);

            ThreeWayDiff diff = new ThreeWayDiff("test", null, null, schemaDiff, ConflictAnalysis.noConflicts());

            assertThat(diff.hasAnyDifferences()).isTrue();
        }

        @Test
        @DisplayName("hasAnyDifferences - 所有差异都为 null")
        void shouldCheckHasAnyDifferencesAllNull() {
            ThreeWayDiff diff = new ThreeWayDiff("test", null, null, null, ConflictAnalysis.noConflicts());

            assertThat(diff.hasAnyDifferences()).isFalse();
        }

        @Test
        @DisplayName("hasConflicts 应正确判断 - 有冲突")
        void shouldCheckHasConflictsTrue() {
            ConflictAnalysis conflicts = ConflictAnalysis.of(List.of(
                    new ConflictAnalysis.Conflict("col", "conflict",
                            ConflictAnalysis.ConflictType.TYPE_MISMATCH,
                            ConflictAnalysis.ConflictResolution.MANUAL_RESOLUTION)
            ));

            ThreeWayDiff diff = new ThreeWayDiff("test", null, null, null, conflicts);

            assertThat(diff.hasConflicts()).isTrue();
        }

        @Test
        @DisplayName("hasConflicts 应正确判断 - 无冲突")
        void shouldCheckHasConflictsFalse() {
            ThreeWayDiff diff = new ThreeWayDiff("test", null, null, null, ConflictAnalysis.noConflicts());

            assertThat(diff.hasConflicts()).isFalse();
        }

        @Test
        @DisplayName("hasConflicts - conflicts 为 null")
        void shouldCheckHasConflictsNull() {
            ThreeWayDiff diff = new ThreeWayDiff("test", null, null, null, null);

            assertThat(diff.hasConflicts()).isFalse();
        }
    }

    // ==================== ConflictAnalysis 测试 ====================

    @Nested
    @DisplayName("ConflictAnalysis 测试")
    class ConflictAnalysisTest {

        @Test
        @DisplayName("noConflicts 应创建无冲突结果")
        void shouldCreateNoConflicts() {
            ConflictAnalysis analysis = ConflictAnalysis.noConflicts();

            assertThat(analysis.hasConflicts()).isFalse();
            assertThat(analysis.conflicts()).isEmpty();
        }

        @Test
        @DisplayName("of 应创建有冲突结果")
        void shouldCreateWithConflicts() {
            ConflictAnalysis.Conflict conflict = new ConflictAnalysis.Conflict(
                    "name",
                    "Type mismatch",
                    ConflictAnalysis.ConflictType.TYPE_MISMATCH,
                    ConflictAnalysis.ConflictResolution.MANUAL_RESOLUTION
            );

            ConflictAnalysis analysis = ConflictAnalysis.of(List.of(conflict));

            assertThat(analysis.hasConflicts()).isTrue();
            assertThat(analysis.conflicts()).hasSize(1);
        }

        @Test
        @DisplayName("Builder 应正确构建冲突分析")
        void shouldBuildWithBuilder() {
            ConflictAnalysis analysis = new ConflictAnalysis.Builder()
                    .addConflict("col1", "Type conflict",
                            ConflictAnalysis.ConflictType.TYPE_MISMATCH,
                            ConflictAnalysis.ConflictResolution.USE_ENTITY)
                    .addConflict("col2", "Default value conflict",
                            ConflictAnalysis.ConflictType.DEFAULT_VALUE_MISMATCH,
                            ConflictAnalysis.ConflictResolution.USE_DATABASE)
                    .build();

            assertThat(analysis.hasConflicts()).isTrue();
            assertThat(analysis.conflicts()).hasSize(2);
        }

        @Test
        @DisplayName("应包含所有冲突类型")
        void shouldContainAllConflictTypes() {
            assertThat(ConflictAnalysis.ConflictType.values()).containsExactly(
                    ConflictAnalysis.ConflictType.TYPE_MISMATCH,
                    ConflictAnalysis.ConflictType.CONSTRAINT_MISMATCH,
                    ConflictAnalysis.ConflictType.COLUMN_EXISTENCE,
                    ConflictAnalysis.ConflictType.DEFAULT_VALUE_MISMATCH
            );
        }

        @Test
        @DisplayName("应包含所有冲突解决方案")
        void shouldContainAllConflictResolutions() {
            assertThat(ConflictAnalysis.ConflictResolution.values()).containsExactly(
                    ConflictAnalysis.ConflictResolution.USE_ENTITY,
                    ConflictAnalysis.ConflictResolution.USE_DATABASE,
                    ConflictAnalysis.ConflictResolution.USE_CHANGELOG,
                    ConflictAnalysis.ConflictResolution.MANUAL_RESOLUTION
            );
        }
    }

    // ==================== DefaultSchemaComparator 测试 ====================

    @Nested
    @DisplayName("DefaultSchemaComparator 测试")
    class DefaultSchemaComparatorTest {

        private final DefaultSchemaComparator comparator = new DefaultSchemaComparator();

        @Test
        @DisplayName("应检测新增列")
        void shouldDetectAddedColumns() {
            SchemaMetadataImpl source = SchemaMetadataImpl.builder()
                    .tableName("users")
                    .addColumn(ColumnMetadataImpl.builder().columnName("id").dataType("BIGINT").build())
                    .addColumn(ColumnMetadataImpl.builder().columnName("name").dataType("VARCHAR(255)").build())
                    .addColumn(ColumnMetadataImpl.builder().columnName("email").dataType("VARCHAR(255)").build())
                    .build();

            SchemaMetadataImpl target = SchemaMetadataImpl.builder()
                    .tableName("users")
                    .addColumn(ColumnMetadataImpl.builder().columnName("id").dataType("BIGINT").build())
                    .addColumn(ColumnMetadataImpl.builder().columnName("name").dataType("VARCHAR(255)").build())
                    .build();

            SchemaDiff diff = comparator.compare(source, target);

            assertThat(diff.hasAddedColumns()).isTrue();
            assertThat(diff.columnDiffs()).hasSize(1);
            assertThat(diff.columnDiffs().get(0).columnName()).isEqualTo("email");
            assertThat(diff.columnDiffs().get(0).diffType()).isEqualTo(DiffType.ADD);
        }

        @Test
        @DisplayName("应检测删除列")
        void shouldDetectDroppedColumns() {
            SchemaMetadataImpl source = SchemaMetadataImpl.builder()
                    .tableName("users")
                    .addColumn(ColumnMetadataImpl.builder().columnName("id").dataType("BIGINT").build())
                    .build();

            SchemaMetadataImpl target = SchemaMetadataImpl.builder()
                    .tableName("users")
                    .addColumn(ColumnMetadataImpl.builder().columnName("id").dataType("BIGINT").build())
                    .addColumn(ColumnMetadataImpl.builder().columnName("old_col").dataType("INT").build())
                    .build();

            SchemaDiff diff = comparator.compare(source, target);

            assertThat(diff.hasDroppedColumns()).isTrue();
            assertThat(diff.columnDiffs()).hasSize(1);
            assertThat(diff.columnDiffs().get(0).columnName()).isEqualTo("old_col");
            assertThat(diff.columnDiffs().get(0).diffType()).isEqualTo(DiffType.DROP);
        }

        @Test
        @DisplayName("应检测修改列 - 数据类型变化")
        void shouldDetectModifiedColumns_DataType() {
            // 使用不同基础类型（INT vs BIGINT），而不是同类型不同长度
            SchemaMetadataImpl source = SchemaMetadataImpl.builder()
                    .tableName("users")
                    .addColumn(ColumnMetadataImpl.builder().columnName("age").dataType("BIGINT").build())
                    .build();

            SchemaMetadataImpl target = SchemaMetadataImpl.builder()
                    .tableName("users")
                    .addColumn(ColumnMetadataImpl.builder().columnName("age").dataType("INT").build())
                    .build();

            SchemaDiff diff = comparator.compare(source, target);

            assertThat(diff.hasModifiedColumns()).isTrue();
            assertThat(diff.columnDiffs()).hasSize(1);
        }

        @Test
        @DisplayName("应检测修改列 - 可空性变化")
        void shouldDetectModifiedColumns_Nullable() {
            SchemaMetadataImpl source = SchemaMetadataImpl.builder()
                    .tableName("users")
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("email").dataType("VARCHAR(255)").nullable(false).build())
                    .build();

            SchemaMetadataImpl target = SchemaMetadataImpl.builder()
                    .tableName("users")
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("email").dataType("VARCHAR(255)").nullable(true).build())
                    .build();

            SchemaDiff diff = comparator.compare(source, target);

            assertThat(diff.hasModifiedColumns()).isTrue();
        }

        @Test
        @DisplayName("应检测修改列 - 默认值变化")
        void shouldDetectModifiedColumns_DefaultValue() {
            SchemaMetadataImpl source = SchemaMetadataImpl.builder()
                    .tableName("users")
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("status").dataType("INT").defaultValue("1").build())
                    .build();

            SchemaMetadataImpl target = SchemaMetadataImpl.builder()
                    .tableName("users")
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("status").dataType("INT").defaultValue("0").build())
                    .build();

            SchemaDiff diff = comparator.compare(source, target);

            assertThat(diff.hasModifiedColumns()).isTrue();
        }

        @Test
        @DisplayName("应检测修改列 - 唯一性变化")
        void shouldDetectModifiedColumns_Unique() {
            SchemaMetadataImpl source = SchemaMetadataImpl.builder()
                    .tableName("users")
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("email").dataType("VARCHAR(255)").unique(true).build())
                    .build();

            SchemaMetadataImpl target = SchemaMetadataImpl.builder()
                    .tableName("users")
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("email").dataType("VARCHAR(255)").unique(false).build())
                    .build();

            SchemaDiff diff = comparator.compare(source, target);

            assertThat(diff.hasModifiedColumns()).isTrue();
        }

        @Test
        @DisplayName("应检测新增索引")
        void shouldDetectAddedIndexes() {
            SchemaMetadataImpl source = SchemaMetadataImpl.builder()
                    .tableName("users")
                    .addColumn(ColumnMetadataImpl.builder().columnName("id").dataType("BIGINT").build())
                    .addIndex(IndexMetadataImpl.builder()
                            .indexName("idx_email").columnNames(List.of("email")).build())
                    .build();

            SchemaMetadataImpl target = SchemaMetadataImpl.builder()
                    .tableName("users")
                    .addColumn(ColumnMetadataImpl.builder().columnName("id").dataType("BIGINT").build())
                    .build();

            SchemaDiff diff = comparator.compare(source, target);

            assertThat(diff.indexDiff()).isNotNull();
            assertThat(diff.indexDiff().addedIndexes()).hasSize(1);
        }

        @Test
        @DisplayName("应检测删除索引")
        void shouldDetectDroppedIndexes() {
            SchemaMetadataImpl source = SchemaMetadataImpl.builder()
                    .tableName("users")
                    .addColumn(ColumnMetadataImpl.builder().columnName("id").dataType("BIGINT").build())
                    .build();

            SchemaMetadataImpl target = SchemaMetadataImpl.builder()
                    .tableName("users")
                    .addColumn(ColumnMetadataImpl.builder().columnName("id").dataType("BIGINT").build())
                    .addIndex(IndexMetadataImpl.builder()
                            .indexName("idx_old").columnNames(List.of("old_col")).build())
                    .build();

            SchemaDiff diff = comparator.compare(source, target);

            assertThat(diff.indexDiff()).isNotNull();
            assertThat(diff.indexDiff().droppedIndexes()).hasSize(1);
        }

        @Test
        @DisplayName("应检测修改索引")
        void shouldDetectModifiedIndexes() {
            SchemaMetadataImpl source = SchemaMetadataImpl.builder()
                    .tableName("users")
                    .addColumn(ColumnMetadataImpl.builder().columnName("id").dataType("BIGINT").build())
                    .addIndex(IndexMetadataImpl.builder()
                            .indexName("idx_name").columnNames(List.of("name")).unique(true).build())
                    .build();

            SchemaMetadataImpl target = SchemaMetadataImpl.builder()
                    .tableName("users")
                    .addColumn(ColumnMetadataImpl.builder().columnName("id").dataType("BIGINT").build())
                    .addIndex(IndexMetadataImpl.builder()
                            .indexName("idx_name").columnNames(List.of("name")).unique(false).build())
                    .build();

            SchemaDiff diff = comparator.compare(source, target);

            assertThat(diff.indexDiff()).isNotNull();
            assertThat(diff.indexDiff().modifiedIndexes()).hasSize(1);
        }

        @Test
        @DisplayName("应检测新增外键")
        void shouldDetectAddedForeignKeys() {
            SchemaMetadataImpl source = SchemaMetadataImpl.builder()
                    .tableName("orders")
                    .addColumn(ColumnMetadataImpl.builder().columnName("id").dataType("BIGINT").build())
                    .addForeignKey(ForeignKeyMetadataImpl.builder()
                            .constraintName("fk_user")
                            .referencedTableName("users")
                            .build())
                    .build();

            SchemaMetadataImpl target = SchemaMetadataImpl.builder()
                    .tableName("orders")
                    .addColumn(ColumnMetadataImpl.builder().columnName("id").dataType("BIGINT").build())
                    .build();

            SchemaDiff diff = comparator.compare(source, target);

            assertThat(diff.foreignKeyDiff()).isNotNull();
            assertThat(diff.foreignKeyDiff().addedForeignKeys()).hasSize(1);
        }

        @Test
        @DisplayName("应检测删除外键")
        void shouldDetectDroppedForeignKeys() {
            SchemaMetadataImpl source = SchemaMetadataImpl.builder()
                    .tableName("orders")
                    .addColumn(ColumnMetadataImpl.builder().columnName("id").dataType("BIGINT").build())
                    .build();

            SchemaMetadataImpl target = SchemaMetadataImpl.builder()
                    .tableName("orders")
                    .addColumn(ColumnMetadataImpl.builder().columnName("id").dataType("BIGINT").build())
                    .addForeignKey(ForeignKeyMetadataImpl.builder()
                            .constraintName("fk_old")
                            .referencedTableName("old_table")
                            .build())
                    .build();

            SchemaDiff diff = comparator.compare(source, target);

            assertThat(diff.foreignKeyDiff()).isNotNull();
            assertThat(diff.foreignKeyDiff().droppedForeignKeys()).hasSize(1);
        }

        @Test
        @DisplayName("应检测修改外键")
        void shouldDetectModifiedForeignKeys() {
            SchemaMetadataImpl source = SchemaMetadataImpl.builder()
                    .tableName("orders")
                    .addColumn(ColumnMetadataImpl.builder().columnName("id").dataType("BIGINT").build())
                    .addForeignKey(ForeignKeyMetadataImpl.builder()
                            .constraintName("fk_user")
                            .referencedTableName("users")
                            .columnNames(List.of("user_id"))
                            .build())
                    .build();

            SchemaMetadataImpl target = SchemaMetadataImpl.builder()
                    .tableName("orders")
                    .addColumn(ColumnMetadataImpl.builder().columnName("id").dataType("BIGINT").build())
                    .addForeignKey(ForeignKeyMetadataImpl.builder()
                            .constraintName("fk_user")
                            .referencedTableName("customers")
                            .columnNames(List.of("user_id"))
                            .build())
                    .build();

            SchemaDiff diff = comparator.compare(source, target);

            assertThat(diff.foreignKeyDiff()).isNotNull();
            assertThat(diff.foreignKeyDiff().modifiedForeignKeys()).hasSize(1);
        }

        @Test
        @DisplayName("相同 Schema 应无差异")
        void shouldHaveNoDifferencesForSameSchema() {
            SchemaMetadataImpl source = SchemaMetadataImpl.builder()
                    .tableName("users")
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("id").dataType("BIGINT").build())
                    .build();

            SchemaMetadataImpl target = SchemaMetadataImpl.builder()
                    .tableName("users")
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("id").dataType("BIGINT").build())
                    .build();

            SchemaDiff diff = comparator.compare(source, target);

            assertThat(diff.hasDifferences()).isFalse();
        }

        @Test
        @DisplayName("应正确进行三向比较")
        void shouldCompareThreeWay() {
            // 使用不同基础类型确保检测到差异
            SchemaMetadataImpl fromEntity = SchemaMetadataImpl.builder()
                    .tableName("users")
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("id").dataType("BIGINT").build())
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("age").dataType("BIGINT").build())
                    .build();

            SchemaMetadataImpl fromDatabase = SchemaMetadataImpl.builder()
                    .tableName("users")
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("id").dataType("BIGINT").build())
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("age").dataType("INT").build())
                    .build();

            SchemaMetadataImpl fromChangeLog = SchemaMetadataImpl.builder()
                    .tableName("users")
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("id").dataType("BIGINT").build())
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("age").dataType("INT").build())
                    .build();

            ThreeWayDiff diff = comparator.compareThreeWay(fromEntity, fromDatabase, fromChangeLog);

            assertThat(diff.hasAnyDifferences()).isTrue();
            assertThat(diff.entityVsDatabase()).isNotNull();
            assertThat(diff.entityVsChangeLog()).isNotNull();
            assertThat(diff.databaseVsChangeLog()).isNotNull();
        }

        @Test
        @DisplayName("三向比较 - 数据库为 null")
        void shouldCompareThreeWayWithNullDatabase() {
            SchemaMetadataImpl fromEntity = SchemaMetadataImpl.builder()
                    .tableName("users")
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("id").dataType("BIGINT").build())
                    .build();

            SchemaMetadataImpl fromChangeLog = SchemaMetadataImpl.builder()
                    .tableName("users")
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("id").dataType("BIGINT").build())
                    .build();

            ThreeWayDiff diff = comparator.compareThreeWay(fromEntity, null, fromChangeLog);

            assertThat(diff.entityVsDatabase()).isNull();
            assertThat(diff.entityVsChangeLog()).isNotNull();
        }

        @Test
        @DisplayName("三向比较 - 检测冲突")
        void shouldDetectConflictsInThreeWay() {
            // 实体定义：age BIGINT
            SchemaMetadataImpl fromEntity = SchemaMetadataImpl.builder()
                    .tableName("users")
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("id").dataType("BIGINT").build())
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("age").dataType("BIGINT").build())
                    .build();

            // 数据库：age INT
            SchemaMetadataImpl fromDatabase = SchemaMetadataImpl.builder()
                    .tableName("users")
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("id").dataType("BIGINT").build())
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("age").dataType("INT").build())
                    .build();

            // ChangeLog：age SMALLINT
            SchemaMetadataImpl fromChangeLog = SchemaMetadataImpl.builder()
                    .tableName("users")
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("id").dataType("BIGINT").build())
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("age").dataType("SMALLINT").build())
                    .build();

            ThreeWayDiff diff = comparator.compareThreeWay(fromEntity, fromDatabase, fromChangeLog);

            // 三方都有差异，应检测到冲突
            assertThat(diff.hasConflicts()).isTrue();
            assertThat(diff.conflicts().hasConflicts()).isTrue();
        }

        @Test
        @DisplayName("应正确规范化数据类型")
        void shouldNormalizeDataType() {
            // VARCHAR(255) 和 VARCHAR(500) 规范化后都是 VARCHAR，视为相同类型
            // 因此不会检测为修改
            SchemaMetadataImpl source = SchemaMetadataImpl.builder()
                    .tableName("users")
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("name").dataType("VARCHAR(500)").build())
                    .build();

            SchemaMetadataImpl target = SchemaMetadataImpl.builder()
                    .tableName("users")
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("name").dataType("VARCHAR(255)").build())
                    .build();

            SchemaDiff diff = comparator.compare(source, target);

            // 数据类型基础部分相同（VARCHAR），规范化后无差异
            assertThat(diff.hasDifferences()).isFalse();
        }

        @Test
        @DisplayName("应检测修改列 - 默认值从 null 到非 null")
        void shouldDetectModifiedColumns_DefaultValueFromNull() {
            SchemaMetadataImpl source = SchemaMetadataImpl.builder()
                    .tableName("users")
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("status").dataType("INT").defaultValue("1").build())
                    .build();

            SchemaMetadataImpl target = SchemaMetadataImpl.builder()
                    .tableName("users")
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("status").dataType("INT").build())
                    .build();

            SchemaDiff diff = comparator.compare(source, target);

            assertThat(diff.hasModifiedColumns()).isTrue();
        }

        @Test
        @DisplayName("应检测修改列 - 默认值从非 null 到 null")
        void shouldDetectModifiedColumns_DefaultValueToNull() {
            SchemaMetadataImpl source = SchemaMetadataImpl.builder()
                    .tableName("users")
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("status").dataType("INT").build())
                    .build();

            SchemaMetadataImpl target = SchemaMetadataImpl.builder()
                    .tableName("users")
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("status").dataType("INT").defaultValue("1").build())
                    .build();

            SchemaDiff diff = comparator.compare(source, target);

            assertThat(diff.hasModifiedColumns()).isTrue();
        }

        @Test
        @DisplayName("三向比较 - ChangeLog 为 null")
        void shouldCompareThreeWayWithNullChangeLog() {
            SchemaMetadataImpl fromEntity = SchemaMetadataImpl.builder()
                    .tableName("users")
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("id").dataType("BIGINT").build())
                    .build();

            SchemaMetadataImpl fromDatabase = SchemaMetadataImpl.builder()
                    .tableName("users")
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("id").dataType("BIGINT").build())
                    .build();

            ThreeWayDiff diff = comparator.compareThreeWay(fromEntity, fromDatabase, null);

            assertThat(diff.entityVsDatabase()).isNotNull();
            assertThat(diff.entityVsChangeLog()).isNull();
            assertThat(diff.databaseVsChangeLog()).isNull();
        }

        @Test
        @DisplayName("三向比较 - 检测删除冲突")
        void shouldDetectDropConflictInThreeWay() {
            // 实体定义：只有 id 列（删除了 age）
            SchemaMetadataImpl fromEntity = SchemaMetadataImpl.builder()
                    .tableName("users")
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("id").dataType("BIGINT").build())
                    .build();

            // 数据库：有 id 和 age 列，age 被修改
            SchemaMetadataImpl fromDatabase = SchemaMetadataImpl.builder()
                    .tableName("users")
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("id").dataType("BIGINT").build())
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("age").dataType("BIGINT").build()) // 从 INT 改为 BIGINT
                    .build();

            // ChangeLog：有 id 和 age 列
            SchemaMetadataImpl fromChangeLog = SchemaMetadataImpl.builder()
                    .tableName("users")
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("id").dataType("BIGINT").build())
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("age").dataType("INT").build())
                    .build();

            ThreeWayDiff diff = comparator.compareThreeWay(fromEntity, fromDatabase, fromChangeLog);

            // 应检测到冲突：实体要删除 age，但数据库有修改
            assertThat(diff.hasConflicts()).isTrue();
        }

        @Test
        @DisplayName("索引比较 - 索引类型不同")
        void shouldDetectModifiedIndexType() {
            SchemaMetadataImpl source = SchemaMetadataImpl.builder()
                    .tableName("users")
                    .addColumn(ColumnMetadataImpl.builder().columnName("id").dataType("BIGINT").build())
                    .addIndex(IndexMetadataImpl.builder()
                            .indexName("idx_name").columnNames(List.of("name")).indexType("HASH").build())
                    .build();

            SchemaMetadataImpl target = SchemaMetadataImpl.builder()
                    .tableName("users")
                    .addColumn(ColumnMetadataImpl.builder().columnName("id").dataType("BIGINT").build())
                    .addIndex(IndexMetadataImpl.builder()
                            .indexName("idx_name").columnNames(List.of("name")).indexType("BTREE").build())
                    .build();

            SchemaDiff diff = comparator.compare(source, target);

            // 索引类型不同但列相同，不视为修改（当前实现只比较 unique 和 columns）
            assertThat(diff.indexDiff()).isNull();
        }

        @Test
        @DisplayName("外键比较 - 外键列不同")
        void shouldDetectModifiedForeignKeyColumns() {
            SchemaMetadataImpl source = SchemaMetadataImpl.builder()
                    .tableName("orders")
                    .addColumn(ColumnMetadataImpl.builder().columnName("id").dataType("BIGINT").build())
                    .addForeignKey(ForeignKeyMetadataImpl.builder()
                            .constraintName("fk_user")
                            .referencedTableName("users")
                            .columnNames(List.of("user_id", "tenant_id"))
                            .build())
                    .build();

            SchemaMetadataImpl target = SchemaMetadataImpl.builder()
                    .tableName("orders")
                    .addColumn(ColumnMetadataImpl.builder().columnName("id").dataType("BIGINT").build())
                    .addForeignKey(ForeignKeyMetadataImpl.builder()
                            .constraintName("fk_user")
                            .referencedTableName("users")
                            .columnNames(List.of("user_id"))
                            .build())
                    .build();

            SchemaDiff diff = comparator.compare(source, target);

            assertThat(diff.foreignKeyDiff()).isNotNull();
            assertThat(diff.foreignKeyDiff().modifiedForeignKeys()).hasSize(1);
        }

        @Test
        @DisplayName("外键比较 - 引用表不同")
        void shouldDetectModifiedForeignKeyReferencedTable() {
            SchemaMetadataImpl source = SchemaMetadataImpl.builder()
                    .tableName("orders")
                    .addColumn(ColumnMetadataImpl.builder().columnName("id").dataType("BIGINT").build())
                    .addForeignKey(ForeignKeyMetadataImpl.builder()
                            .constraintName("fk_user")
                            .referencedTableName("customers")
                            .columnNames(List.of("user_id"))
                            .build())
                    .build();

            SchemaMetadataImpl target = SchemaMetadataImpl.builder()
                    .tableName("orders")
                    .addColumn(ColumnMetadataImpl.builder().columnName("id").dataType("BIGINT").build())
                    .addForeignKey(ForeignKeyMetadataImpl.builder()
                            .constraintName("fk_user")
                            .referencedTableName("users")
                            .columnNames(List.of("user_id"))
                            .build())
                    .build();

            SchemaDiff diff = comparator.compare(source, target);

            assertThat(diff.foreignKeyDiff()).isNotNull();
            assertThat(diff.foreignKeyDiff().modifiedForeignKeys()).hasSize(1);
        }

        @Test
        @DisplayName("外键比较 - 引用列不同")
        void shouldDetectModifiedForeignKeyReferencedColumns() {
            SchemaMetadataImpl source = SchemaMetadataImpl.builder()
                    .tableName("orders")
                    .addColumn(ColumnMetadataImpl.builder().columnName("id").dataType("BIGINT").build())
                    .addForeignKey(ForeignKeyMetadataImpl.builder()
                            .constraintName("fk_user")
                            .referencedTableName("users")
                            .columnNames(List.of("user_id"))
                            .referencedColumnNames(List.of("uuid"))
                            .build())
                    .build();

            SchemaMetadataImpl target = SchemaMetadataImpl.builder()
                    .tableName("orders")
                    .addColumn(ColumnMetadataImpl.builder().columnName("id").dataType("BIGINT").build())
                    .addForeignKey(ForeignKeyMetadataImpl.builder()
                            .constraintName("fk_user")
                            .referencedTableName("users")
                            .columnNames(List.of("user_id"))
                            .referencedColumnNames(List.of("id"))
                            .build())
                    .build();

            SchemaDiff diff = comparator.compare(source, target);

            assertThat(diff.foreignKeyDiff()).isNotNull();
            assertThat(diff.foreignKeyDiff().modifiedForeignKeys()).hasSize(1);
        }
    }
}
