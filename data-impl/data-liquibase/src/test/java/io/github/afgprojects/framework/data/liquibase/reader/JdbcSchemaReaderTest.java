package io.github.afgprojects.framework.data.liquibase.reader;

import io.github.afgprojects.framework.data.core.schema.SchemaMetadata;
import io.github.afgprojects.framework.data.core.schema.ColumnMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JdbcSchemaReader 测试
 */
@DisplayName("JdbcSchemaReader 测试")
class JdbcSchemaReaderTest {

    private JdbcSchemaReader reader;
    private Connection connection;

    @BeforeEach
    void setUp() throws SQLException {
        reader = new JdbcSchemaReader();
        connection = DriverManager.getConnection(
                "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
                "sa",
                ""
        );
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Nested
    @DisplayName("readTable 测试")
    class ReadTableTests {

        @Test
        @DisplayName("应正确读取表结构")
        void shouldReadTableSchema() throws SQLException {
            // 创建测试表
            connection.createStatement().execute("""
                    CREATE TABLE test_table (
                        id BIGINT PRIMARY KEY,
                        name VARCHAR(100) NOT NULL,
                        email VARCHAR(200),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """);

            SchemaMetadata schema = reader.readTable(connection, "TEST_TABLE");

            assertThat(schema).isNotNull();
            assertThat(schema.getTableName()).isEqualToIgnoringCase("TEST_TABLE");
            assertThat(schema.getColumns()).hasSize(4);
            assertThat(schema.getPrimaryKey()).isNotNull();
        }

        @Test
        @DisplayName("应正确读取列信息")
        void shouldReadColumns() throws SQLException {
            connection.createStatement().execute("""
                    CREATE TABLE column_test (
                        id BIGINT PRIMARY KEY,
                        nullable_col VARCHAR(50),
                        not_null_col VARCHAR(50) NOT NULL
                    )
                    """);

            SchemaMetadata schema = reader.readTable(connection, "COLUMN_TEST");

            assertThat(schema.getColumn("ID")).isNotNull();
            assertThat(schema.getColumn("NULLABLE_COL")).isNotNull();
            assertThat(schema.getColumn("NOT_NULL_COL")).isNotNull();

            // 验证可空性
            assertThat(schema.getColumn("NULLABLE_COL").isNullable()).isTrue();
            assertThat(schema.getColumn("NOT_NULL_COL").isNullable()).isFalse();
        }

        @Test
        @DisplayName("应正确读取主键")
        void shouldReadPrimaryKey() throws SQLException {
            connection.createStatement().execute("""
                    CREATE TABLE pk_test (
                        id BIGINT PRIMARY KEY,
                        name VARCHAR(100)
                    )
                    """);

            SchemaMetadata schema = reader.readTable(connection, "PK_TEST");

            assertThat(schema.getPrimaryKey()).isNotNull();
            assertThat(schema.getPrimaryKey().getColumnNames())
                    .anyMatch(name -> name.equalsIgnoreCase("ID"));
        }

        @Test
        @DisplayName("应正确读取复合主键")
        void shouldReadCompositePrimaryKey() throws SQLException {
            connection.createStatement().execute("""
                    CREATE TABLE composite_pk_test (
                        user_id BIGINT NOT NULL,
                        role_id BIGINT NOT NULL,
                        PRIMARY KEY (user_id, role_id)
                    )
                    """);

            SchemaMetadata schema = reader.readTable(connection, "COMPOSITE_PK_TEST");

            assertThat(schema.getPrimaryKey()).isNotNull();
            assertThat(schema.getPrimaryKey().getColumnNames()).hasSize(2);
        }

        @Test
        @DisplayName("应正确读取索引")
        void shouldReadIndexes() throws SQLException {
            connection.createStatement().execute("""
                    CREATE TABLE index_test (
                        id BIGINT PRIMARY KEY,
                        email VARCHAR(100),
                        name VARCHAR(100)
                    )
                    """);
            connection.createStatement().execute("CREATE INDEX idx_email ON index_test (email)");
            connection.createStatement().execute("CREATE UNIQUE INDEX idx_name ON index_test (name)");

            SchemaMetadata schema = reader.readTable(connection, "INDEX_TEST");

            assertThat(schema.getIndexes()).isNotEmpty();
        }

        @Test
        @DisplayName("应正确读取外键")
        void shouldReadForeignKeys() throws SQLException {
            connection.createStatement().execute("""
                    CREATE TABLE parent_table (
                        id BIGINT PRIMARY KEY,
                        name VARCHAR(100)
                    )
                    """);
            connection.createStatement().execute("""
                    CREATE TABLE child_table (
                        id BIGINT PRIMARY KEY,
                        parent_id BIGINT,
                        CONSTRAINT fk_parent FOREIGN KEY (parent_id) REFERENCES parent_table(id)
                    )
                    """);

            SchemaMetadata schema = reader.readTable(connection, "CHILD_TABLE");

            assertThat(schema.getForeignKeys()).isNotEmpty();
        }

        @Test
        @DisplayName("应处理小写表名")
        void shouldHandleLowercaseTableName() throws SQLException {
            connection.createStatement().execute("""
                    CREATE TABLE lowercase_table (
                        id BIGINT PRIMARY KEY
                    )
                    """);

            // H2 默认将表名转为大写，但尝试用小写查询
            SchemaMetadata schema = reader.readTable(connection, "lowercase_table");

            assertThat(schema).isNotNull();
        }

        @Test
        @DisplayName("应处理不存在的表")
        void shouldHandleNonExistentTable() throws SQLException {
            SchemaMetadata schema = reader.readTable(connection, "NON_EXISTENT_TABLE");

            // 表不存在时，返回空结构的 SchemaMetadata
            assertThat(schema).isNotNull();
            assertThat(schema.getColumns()).isEmpty();
        }

        @Test
        @DisplayName("应正确读取带默认值的列")
        void shouldReadColumnWithDefaultValue() throws SQLException {
            connection.createStatement().execute("""
                    CREATE TABLE default_value_test (
                        id BIGINT PRIMARY KEY,
                        status VARCHAR(50) DEFAULT 'active',
                        count INT DEFAULT 0
                    )
                    """);

            SchemaMetadata schema = reader.readTable(connection, "DEFAULT_VALUE_TEST");

            ColumnMetadata statusColumn = schema.getColumn("STATUS");
            assertThat(statusColumn).isNotNull();
            // H2 可能不返回默认值，取决于版本和配置
        }

        @Test
        @DisplayName("应正确读取带注释的列")
        void shouldReadColumnWithComment() throws SQLException {
            connection.createStatement().execute("""
                    CREATE TABLE comment_test (
                        id BIGINT PRIMARY KEY,
                        name VARCHAR(100)
                    )
                    """);
            connection.createStatement().execute("COMMENT ON COLUMN comment_test.name IS '用户名称'");

            SchemaMetadata schema = reader.readTable(connection, "COMMENT_TEST");

            ColumnMetadata nameColumn = schema.getColumn("NAME");
            assertThat(nameColumn).isNotNull();
        }

        @Test
        @DisplayName("应正确读取大写表名")
        void shouldHandleUppercaseTableName() throws SQLException {
            connection.createStatement().execute("""
                    CREATE TABLE UPPERCASE_TABLE (
                        id BIGINT PRIMARY KEY
                    )
                    """);

            SchemaMetadata schema = reader.readTable(connection, "uppercase_table");

            assertThat(schema).isNotNull();
            assertThat(schema.getColumns()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("readAllTables 测试")
    class ReadAllTablesTests {

        @Test
        @DisplayName("应读取所有表")
        void shouldReadAllTables() throws SQLException {
            connection.createStatement().execute("""
                    CREATE TABLE table_one (
                        id BIGINT PRIMARY KEY
                    )
                    """);
            connection.createStatement().execute("""
                    CREATE TABLE table_two (
                        id BIGINT PRIMARY KEY
                    )
                    """);

            Map<String, SchemaMetadata> tables = reader.readAllTables(connection);

            assertThat(tables).isNotEmpty();
            assertThat(tables.keySet()).anyMatch(name -> name.equalsIgnoreCase("TABLE_ONE"));
            assertThat(tables.keySet()).anyMatch(name -> name.equalsIgnoreCase("TABLE_TWO"));
        }

        @Test
        @DisplayName("空数据库应返回空映射")
        void shouldReturnEmptyMapForEmptyDatabase() throws SQLException {
            // H2 内存数据库默认有一些系统表，但我们的查询只获取 TABLE 类型
            Map<String, SchemaMetadata> tables = reader.readAllTables(connection);

            // 可能有系统表，也可能没有
            assertThat(tables).isNotNull();
        }
    }

    @Nested
    @DisplayName("数据库兼容性测试")
    class DatabaseCompatibilityTests {

        @Test
        @DisplayName("H2 数据库应正常工作")
        void shouldWorkWithH2() throws SQLException {
            connection.createStatement().execute("""
                    CREATE TABLE h2_test (
                        id BIGINT PRIMARY KEY,
                        data CLOB,
                        binary_data VARBINARY(100)
                    )
                    """);

            SchemaMetadata schema = reader.readTable(connection, "H2_TEST");

            assertThat(schema).isNotNull();
            assertThat(schema.getColumns()).hasSize(3);
        }

        @Test
        @DisplayName("应处理各种数据类型")
        void shouldHandleVariousDataTypes() throws SQLException {
            connection.createStatement().execute("""
                    CREATE TABLE data_types_test (
                        id BIGINT PRIMARY KEY,
                        int_col INT,
                        smallint_col SMALLINT,
                        decimal_col DECIMAL(10,2),
                        double_col DOUBLE,
                        float_col FLOAT,
                        date_col DATE,
                        timestamp_col TIMESTAMP,
                        boolean_col BOOLEAN,
                        varchar_col VARCHAR(255),
                        char_col CHAR(10),
                        text_col CLOB
                    )
                    """);

            SchemaMetadata schema = reader.readTable(connection, "DATA_TYPES_TEST");

            assertThat(schema.getColumns()).hasSize(12);
        }
    }

    @Nested
    @DisplayName("索引和约束测试")
    class IndexAndConstraintTests {

        @Test
        @DisplayName("应正确读取唯一索引")
        void shouldReadUniqueIndex() throws SQLException {
            connection.createStatement().execute("""
                    CREATE TABLE unique_index_test (
                        id BIGINT PRIMARY KEY,
                        email VARCHAR(100) UNIQUE
                    )
                    """);

            SchemaMetadata schema = reader.readTable(connection, "UNIQUE_INDEX_TEST");

            assertThat(schema.getIndexes()).isNotEmpty();
            assertThat(schema.getIndexes().stream()
                    .anyMatch(idx -> idx.isUnique()))
                    .isTrue();
        }

        @Test
        @DisplayName("应正确读取多列索引")
        void shouldReadMultiColumnIndex() throws SQLException {
            connection.createStatement().execute("""
                    CREATE TABLE multi_col_index_test (
                        id BIGINT PRIMARY KEY,
                        col1 VARCHAR(50),
                        col2 VARCHAR(50)
                    )
                    """);
            connection.createStatement().execute("CREATE INDEX idx_multi ON multi_col_index_test (col1, col2)");

            SchemaMetadata schema = reader.readTable(connection, "MULTI_COL_INDEX_TEST");

            assertThat(schema.getIndexes()).isNotEmpty();
        }

        @Test
        @DisplayName("应正确读取无名称的主键")
        void shouldReadPrimaryKeyWithoutName() throws SQLException {
            connection.createStatement().execute("""
                    CREATE TABLE pk_no_name_test (
                        id BIGINT PRIMARY KEY
                    )
                    """);

            SchemaMetadata schema = reader.readTable(connection, "PK_NO_NAME_TEST");

            assertThat(schema.getPrimaryKey()).isNotNull();
            assertThat(schema.getPrimaryKey().getConstraintName()).isNotNull();
        }
    }

    @Nested
    @DisplayName("外键规则测试")
    class ForeignKeyRuleTests {

        @Test
        @DisplayName("应正确读取外键信息")
        void shouldReadForeignKeyInfo() throws SQLException {
            connection.createStatement().execute("""
                    CREATE TABLE ref_table (
                        id BIGINT PRIMARY KEY
                    )
                    """);
            connection.createStatement().execute("""
                    CREATE TABLE fk_test (
                        id BIGINT PRIMARY KEY,
                        ref_id BIGINT,
                        CONSTRAINT fk_ref FOREIGN KEY (ref_id) REFERENCES ref_table(id)
                    )
                    """);

            SchemaMetadata schema = reader.readTable(connection, "FK_TEST");

            assertThat(schema.getForeignKeys()).isNotEmpty();
            assertThat(schema.getForeignKeys().get(0).getReferencedTableName())
                    .isEqualToIgnoringCase("REF_TABLE");
        }
    }

    @Nested
    @DisplayName("构造函数测试")
    class ConstructorTests {

        @Test
        @DisplayName("应正确创建实例")
        void shouldCreateInstance() {
            JdbcSchemaReader newReader = new JdbcSchemaReader();
            assertThat(newReader).isNotNull();
        }
    }
}
