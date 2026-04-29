package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.dialect.Dialect;
import io.github.afgprojects.framework.data.core.dialect.H2Dialect;
import io.github.afgprojects.framework.data.jdbc.metadata.SimpleEntityMetadata;
import io.github.afgprojects.framework.data.jdbc.metadata.SimpleFieldMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * EntityQueryHelper 测试
 */
@DisplayName("EntityQueryHelper Tests")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EntityQueryHelperTest {

    @Mock
    private SimpleEntityMetadata<TestEntity> metadata;

    @Mock
    private SimpleFieldMetadata idField;

    @Mock
    private SimpleFieldMetadata nameField;

    @Mock
    private SimpleFieldMetadata statusField;

    private Dialect dialect;
    private EntityQueryHelper<TestEntity> helper;

    @BeforeEach
    void setUp() {
        dialect = new H2Dialect();

        lenient().when(idField.getColumnName()).thenReturn("id");
        lenient().when(idField.getPropertyName()).thenReturn("id");
        lenient().when(idField.isId()).thenReturn(true);
        lenient().when(idField.isGenerated()).thenReturn(true);
        doReturn(Long.class).when(idField).getFieldType();
        lenient().when(idField.getValue(any())).thenReturn(1L);

        lenient().when(nameField.getColumnName()).thenReturn("name");
        lenient().when(nameField.getPropertyName()).thenReturn("name");
        lenient().when(nameField.isId()).thenReturn(false);
        lenient().when(nameField.isGenerated()).thenReturn(false);
        doReturn(String.class).when(nameField).getFieldType();

        lenient().when(statusField.getColumnName()).thenReturn("status");
        lenient().when(statusField.getPropertyName()).thenReturn("status");
        lenient().when(statusField.isId()).thenReturn(false);
        lenient().when(statusField.isGenerated()).thenReturn(false);
        doReturn(Integer.class).when(statusField).getFieldType();

        lenient().when(metadata.getTableName()).thenReturn("test_entity");
        lenient().when(metadata.getFields()).thenReturn(List.of(idField, nameField, statusField));
        lenient().when(metadata.getIdField()).thenReturn(idField);

        helper = new EntityQueryHelper<>(TestEntity.class, dialect, metadata);
    }

    @Nested
    @DisplayName("SQL Building Tests")
    class SqlBuildingTests {

        @Test
        @DisplayName("buildInsertSql should generate correct INSERT SQL")
        void testBuildInsertSql() {
            String sql = helper.buildInsertSql();

            assertThat(sql).contains("INSERT INTO");
            assertThat(sql).contains("test_entity");
            assertThat(sql).contains("name, status");
            assertThat(sql).contains("VALUES (?, ?)");
        }

        @Test
        @DisplayName("buildUpdateSql for non-versioned entity")
        void testBuildUpdateSqlNonVersioned() {
            String sql = helper.buildUpdateSql(false);

            assertThat(sql).contains("UPDATE test_entity SET");
            assertThat(sql).contains("name = ?");
            assertThat(sql).contains("status = ?");
            assertThat(sql).contains("WHERE id = ?");
        }

        @Test
        @DisplayName("buildSelectSql without WHERE clause")
        void testBuildSelectSqlNoWhere() {
            String sql = helper.buildSelectSql(null);

            assertThat(sql).contains("SELECT id, name, status FROM test_entity");
            assertThat(sql).doesNotContain("WHERE");
        }

        @Test
        @DisplayName("buildSelectSql with WHERE clause")
        void testBuildSelectSqlWithWhere() {
            String sql = helper.buildSelectSql("status = 1");

            assertThat(sql).contains("SELECT id, name, status FROM test_entity");
            assertThat(sql).contains("WHERE status = 1");
        }

        @Test
        @DisplayName("buildDeleteSql without WHERE clause")
        void testBuildDeleteSqlNoWhere() {
            String sql = helper.buildDeleteSql(null);

            assertThat(sql).contains("DELETE FROM test_entity");
            assertThat(sql).doesNotContain("WHERE");
        }

        @Test
        @DisplayName("buildDeleteSql with WHERE clause")
        void testBuildDeleteSqlWithWhere() {
            String sql = helper.buildDeleteSql("id = 1");

            assertThat(sql).contains("DELETE FROM test_entity");
            assertThat(sql).contains("WHERE id = 1");
        }
    }

    @Nested
    @DisplayName("Field Access Tests")
    class FieldAccessTests {

        @Test
        @DisplayName("getIdField should return ID field metadata")
        void testGetIdField() {
            var result = helper.getIdField();

            assertThat(result).isNotNull();
            assertThat(result.getColumnName()).isEqualTo("id");
        }

        @Test
        @DisplayName("findFieldByName should find field by property name")
        void testFindFieldByName() {
            var result = helper.findFieldByName("name");

            assertThat(result).isNotNull();
            assertThat(result.getColumnName()).isEqualTo("name");
        }

        @Test
        @DisplayName("findFieldByName with non-existent field")
        void testFindFieldByNameNotFound() {
            var result = helper.findFieldByName("nonexistent");

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("Utility Method Tests")
    class UtilityMethodTests {

        @Test
        @DisplayName("columnNameToFieldName should convert snake_case to camelCase")
        void testColumnNameToFieldName() {
            assertThat(helper.columnNameToFieldName("user_name")).isEqualTo("userName");
            assertThat(helper.columnNameToFieldName("created_at")).isEqualTo("createdAt");
            assertThat(helper.columnNameToFieldName("id")).isEqualTo("id");
        }

        @Test
        @DisplayName("fieldNameToColumnName should convert camelCase to snake_case")
        void testFieldNameToColumnName() {
            assertThat(helper.fieldNameToColumnName("userName")).isEqualTo("user_name");
            assertThat(helper.fieldNameToColumnName("createdAt")).isEqualTo("created_at");
            assertThat(helper.fieldNameToColumnName("id")).isEqualTo("id");
        }

        @Test
        @DisplayName("getTableName should return table name from metadata")
        void testGetTableName() {
            assertThat(helper.getTableName()).isEqualTo("test_entity");
        }

        @Test
        @DisplayName("getEntityClass should return entity class")
        void testGetEntityClass() {
            assertThat(helper.getEntityClass()).isEqualTo(TestEntity.class);
        }

        @Test
        @DisplayName("getMetadata should return metadata")
        void testGetMetadata() {
            assertThat(helper.getMetadata()).isSameAs(metadata);
        }

        @Test
        @DisplayName("getDialect should return dialect")
        void testGetDialect() {
            assertThat(helper.getDialect()).isSameAs(dialect);
        }
    }

    /**
     * 测试实体类
     */
    static class TestEntity {
        private Long id;
        private String name;
        private Integer status;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }
    }
}
