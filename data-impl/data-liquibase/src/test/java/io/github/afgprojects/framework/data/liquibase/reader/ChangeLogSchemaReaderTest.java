package io.github.afgprojects.framework.data.liquibase.reader;

import io.github.afgprojects.framework.data.core.schema.SchemaMetadata;
import io.github.afgprojects.framework.data.core.schema.ColumnMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ChangeLogSchemaReader 测试
 */
@DisplayName("ChangeLogSchemaReader 测试")
class ChangeLogSchemaReaderTest {

    private ChangeLogSchemaReader reader;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        reader = new ChangeLogSchemaReader();
    }

    @Nested
    @DisplayName("read 测试")
    class ReadTests {

        @Test
        @DisplayName("应解析 createTable 变更")
        void shouldParseCreateTableChange() throws Exception {
            String changelogXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <databaseChangeLog
                        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                            http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.26.xsd">
                        <changeSet id="1" author="test">
                            <createTable tableName="test_table">
                                <column name="id" type="BIGINT">
                                    <constraints primaryKey="true" nullable="false"/>
                                </column>
                                <column name="name" type="VARCHAR(100)">
                                    <constraints nullable="false"/>
                                </column>
                            </createTable>
                        </changeSet>
                    </databaseChangeLog>
                    """;

            Path changelogPath = tempDir.resolve("changelog.xml");
            Files.writeString(changelogPath, changelogXml);

            Map<String, SchemaMetadata> schemas = reader.read(changelogPath);

            assertThat(schemas).containsKey("test_table");
            SchemaMetadata schema = schemas.get("test_table");
            assertThat(schema.getColumns()).hasSize(2);
        }

        @Test
        @DisplayName("应正确解析列属性")
        void shouldParseColumnAttributes() throws Exception {
            String changelogXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <databaseChangeLog
                        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                            http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.26.xsd">
                        <changeSet id="1" author="test">
                            <createTable tableName="users">
                                <column name="id" type="BIGINT">
                                    <constraints primaryKey="true" nullable="false"/>
                                </column>
                                <column name="email" type="VARCHAR(255)">
                                    <constraints unique="true" nullable="false"/>
                                </column>
                                <column name="status" type="VARCHAR(50)"/>
                            </createTable>
                        </changeSet>
                    </databaseChangeLog>
                    """;

            Path changelogPath = tempDir.resolve("changelog.xml");
            Files.writeString(changelogPath, changelogXml);

            Map<String, SchemaMetadata> schemas = reader.read(changelogPath);

            SchemaMetadata schema = schemas.get("users");
            assertThat(schema).isNotNull();

            ColumnMetadata idColumn = schema.getColumn("id");
            assertThat(idColumn).isNotNull();
            assertThat(idColumn.isPrimaryKey()).isTrue();
            assertThat(idColumn.isNullable()).isFalse();

            ColumnMetadata emailColumn = schema.getColumn("email");
            assertThat(emailColumn).isNotNull();
            assertThat(emailColumn.isUnique()).isTrue();

            ColumnMetadata statusColumn = schema.getColumn("status");
            assertThat(statusColumn).isNotNull();
            assertThat(statusColumn.isNullable()).isTrue();
        }

        @Test
        @DisplayName("应解析多个表")
        void shouldParseMultipleTables() throws Exception {
            String changelogXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <databaseChangeLog
                        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                            http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.26.xsd">
                        <changeSet id="1" author="test">
                            <createTable tableName="users">
                                <column name="id" type="BIGINT">
                                    <constraints primaryKey="true"/>
                                </column>
                            </createTable>
                        </changeSet>
                        <changeSet id="2" author="test">
                            <createTable tableName="roles">
                                <column name="id" type="BIGINT">
                                    <constraints primaryKey="true"/>
                                </column>
                            </createTable>
                        </changeSet>
                    </databaseChangeLog>
                    """;

            Path changelogPath = tempDir.resolve("changelog.xml");
            Files.writeString(changelogPath, changelogXml);

            Map<String, SchemaMetadata> schemas = reader.read(changelogPath);

            assertThat(schemas).hasSize(2);
            assertThat(schemas).containsKeys("users", "roles");
        }

        @Test
        @DisplayName("应解析 addColumn 变更")
        void shouldParseAddColumnChange() throws Exception {
            String changelogXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <databaseChangeLog
                        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                            http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.26.xsd">
                        <changeSet id="1" author="test">
                            <createTable tableName="users">
                                <column name="id" type="BIGINT">
                                    <constraints primaryKey="true"/>
                                </column>
                            </createTable>
                        </changeSet>
                        <changeSet id="2" author="test">
                            <addColumn tableName="users">
                                <column name="email" type="VARCHAR(255)">
                                    <constraints unique="true"/>
                                </column>
                            </addColumn>
                        </changeSet>
                    </databaseChangeLog>
                    """;

            Path changelogPath = tempDir.resolve("changelog.xml");
            Files.writeString(changelogPath, changelogXml);

            Map<String, SchemaMetadata> schemas = reader.read(changelogPath);

            SchemaMetadata schema = schemas.get("users");
            assertThat(schema).isNotNull();
            assertThat(schema.getColumns()).hasSize(2);
            assertThat(schema.getColumn("email")).isNotNull();
            assertThat(schema.getColumn("email").isUnique()).isTrue();
        }

        @Test
        @DisplayName("应处理 addColumn 到不存在的表")
        void shouldHandleAddColumnToNonExistingTable() throws Exception {
            String changelogXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <databaseChangeLog
                        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                            http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.26.xsd">
                        <changeSet id="1" author="test">
                            <addColumn tableName="existing_table">
                                <column name="new_col" type="VARCHAR(100)"/>
                            </addColumn>
                        </changeSet>
                    </databaseChangeLog>
                    """;

            Path changelogPath = tempDir.resolve("changelog.xml");
            Files.writeString(changelogPath, changelogXml);

            Map<String, SchemaMetadata> schemas = reader.read(changelogPath);

            assertThat(schemas).containsKey("existing_table");
            assertThat(schemas.get("existing_table").getColumns()).hasSize(1);
        }

        @Test
        @DisplayName("应解析默认值")
        void shouldParseDefaultValue() throws Exception {
            String changelogXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <databaseChangeLog
                        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                            http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.26.xsd">
                        <changeSet id="1" author="test">
                            <createTable tableName="users">
                                <column name="id" type="BIGINT">
                                    <constraints primaryKey="true"/>
                                </column>
                                <column name="status" type="VARCHAR(50)" defaultValue="active"/>
                            </createTable>
                        </changeSet>
                    </databaseChangeLog>
                    """;

            Path changelogPath = tempDir.resolve("changelog.xml");
            Files.writeString(changelogPath, changelogXml);

            Map<String, SchemaMetadata> schemas = reader.read(changelogPath);

            ColumnMetadata statusColumn = schemas.get("users").getColumn("status");
            assertThat(statusColumn).isNotNull();
            assertThat(statusColumn.getDefaultValue()).isEqualTo("active");
        }

        @Test
        @DisplayName("应处理没有类型的列（使用默认类型）")
        void shouldHandleColumnWithoutType() throws Exception {
            String changelogXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <databaseChangeLog
                        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                            http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.26.xsd">
                        <changeSet id="1" author="test">
                            <createTable tableName="users">
                                <column name="id" type="BIGINT">
                                    <constraints primaryKey="true"/>
                                </column>
                                <column name="description"/>
                            </createTable>
                        </changeSet>
                    </databaseChangeLog>
                    """;

            Path changelogPath = tempDir.resolve("changelog.xml");
            Files.writeString(changelogPath, changelogXml);

            Map<String, SchemaMetadata> schemas = reader.read(changelogPath);

            ColumnMetadata descColumn = schemas.get("users").getColumn("description");
            assertThat(descColumn).isNotNull();
            assertThat(descColumn.getDataType()).isEqualTo("VARCHAR(255)");
        }

        @Test
        @DisplayName("应处理空的 ChangeLog")
        void shouldHandleEmptyChangeLog() throws Exception {
            String changelogXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <databaseChangeLog
                        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                            http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.26.xsd">
                    </databaseChangeLog>
                    """;

            Path changelogPath = tempDir.resolve("changelog.xml");
            Files.writeString(changelogPath, changelogXml);

            Map<String, SchemaMetadata> schemas = reader.read(changelogPath);

            assertThat(schemas).isEmpty();
        }

        @Test
        @DisplayName("应处理不存在的文件")
        void shouldHandleNonExistentFile() {
            assertThatThrownBy(() -> reader.read("/nonexistent/changelog.xml"))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("read(String) 应正确解析路径")
        void shouldReadFromStringPath() throws Exception {
            String changelogXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <databaseChangeLog
                        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                            http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.26.xsd">
                        <changeSet id="1" author="test">
                            <createTable tableName="string_path_table">
                                <column name="id" type="BIGINT">
                                    <constraints primaryKey="true"/>
                                </column>
                            </createTable>
                        </changeSet>
                    </databaseChangeLog>
                    """;

            Path changelogPath = tempDir.resolve("changelog-string.xml");
            Files.writeString(changelogPath, changelogXml);

            Map<String, SchemaMetadata> schemas = reader.read(changelogPath.toString());

            assertThat(schemas).containsKey("string_path_table");
        }

        @Test
        @DisplayName("应处理多个 addColumn 到同一表")
        void shouldHandleMultipleAddColumns() throws Exception {
            String changelogXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <databaseChangeLog
                        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                            http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.26.xsd">
                        <changeSet id="1" author="test">
                            <createTable tableName="multi_add_table">
                                <column name="id" type="BIGINT">
                                    <constraints primaryKey="true"/>
                                </column>
                            </createTable>
                        </changeSet>
                        <changeSet id="2" author="test">
                            <addColumn tableName="multi_add_table">
                                <column name="col1" type="VARCHAR(50)"/>
                            </addColumn>
                        </changeSet>
                        <changeSet id="3" author="test">
                            <addColumn tableName="multi_add_table">
                                <column name="col2" type="VARCHAR(50)"/>
                            </addColumn>
                        </changeSet>
                    </databaseChangeLog>
                    """;

            Path changelogPath = tempDir.resolve("changelog.xml");
            Files.writeString(changelogPath, changelogXml);

            Map<String, SchemaMetadata> schemas = reader.read(changelogPath);

            SchemaMetadata schema = schemas.get("multi_add_table");
            assertThat(schema.getColumns()).hasSize(3);
            assertThat(schema.getColumn("col1")).isNotNull();
            assertThat(schema.getColumn("col2")).isNotNull();
        }

        @Test
        @DisplayName("应忽略不支持的 Change 类型")
        void shouldIgnoreUnsupportedChangeTypes() throws Exception {
            String changelogXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <databaseChangeLog
                        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                            http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.26.xsd">
                        <changeSet id="1" author="test">
                            <createTable tableName="test_table">
                                <column name="id" type="BIGINT">
                                    <constraints primaryKey="true"/>
                                </column>
                            </createTable>
                        </changeSet>
                        <changeSet id="2" author="test">
                            <dropTable tableName="other_table"/>
                        </changeSet>
                    </databaseChangeLog>
                    """;

            Path changelogPath = tempDir.resolve("changelog.xml");
            Files.writeString(changelogPath, changelogXml);

            Map<String, SchemaMetadata> schemas = reader.read(changelogPath);

            assertThat(schemas).containsKey("test_table");
            assertThat(schemas).hasSize(1);
        }
    }
}
