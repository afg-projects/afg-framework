package io.github.afgprojects.framework.data.liquibase.generator;

import io.github.afgprojects.framework.data.core.schema.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ChangeLogGenerator 单元测试
 * <p>
 * 验证 Liquibase XML changelog 文件生成逻辑。
 */
class ChangeLogGeneratorTest {

    private final ChangeLogGenerator generator = new ChangeLogGenerator();

    @TempDir
    Path tempDir;

    // ========== generateCreateTable ==========

    @Nested
    @DisplayName("generateCreateTable")
    class GenerateCreateTable {

        @Test
        @DisplayName("should generate createTable changelog when valid schema provided")
        void shouldGenerateCreateTableChangelog_whenValidSchemaProvided() throws IOException {
            SchemaMetadata schema = createTestSchema();
            Path outputPath = tempDir.resolve("001_test_table.xml");

            generator.generateCreateTable(schema, "platform", "1.0.0", 1, outputPath);

            assertThat(Files.exists(outputPath)).isTrue();
            String content = Files.readString(outputPath);

            assertThat(content).contains("<databaseChangeLog");
            assertThat(content).contains("<changeSet");
            assertThat(content).contains("<createTable");
            assertThat(content).contains("tableName=\"test_table\"");
            assertThat(content).contains("author=\"afg\"");
            assertThat(content).contains("v1.0.0-001-test-table");
        }

        @Test
        @DisplayName("should generate column definitions when schema has columns")
        void shouldGenerateColumnDefinitions_whenSchemaHasColumns() throws IOException {
            SchemaMetadata schema = createTestSchema();
            Path outputPath = tempDir.resolve("001_test_table.xml");

            generator.generateCreateTable(schema, "platform", "1.0.0", 1, outputPath);

            String content = Files.readString(outputPath);

            assertThat(content).contains("name=\"id\"");
            assertThat(content).contains("name=\"name\"");
            assertThat(content).contains("name=\"status\"");
        }

        @Test
        @DisplayName("should generate primary key constraint when column is primary key")
        void shouldGeneratePrimaryKeyConstraint_whenColumnIsPrimaryKey() throws IOException {
            SchemaMetadata schema = createTestSchema();
            Path outputPath = tempDir.resolve("001_test_table.xml");

            generator.generateCreateTable(schema, "platform", "1.0.0", 1, outputPath);

            String content = Files.readString(outputPath);

            assertThat(content).contains("primaryKey=\"true\"");
        }

        @Test
        @DisplayName("should generate with remarks when remarks provided")
        void shouldGenerateWithRemarks_whenRemarksProvided() throws IOException {
            SchemaMetadata schema = createTestSchema();
            Path outputPath = tempDir.resolve("001_test_table.xml");

            generator.generateCreateTable(schema, "platform", "1.0.0", 1, "afg", outputPath, "用户表");

            String content = Files.readString(outputPath);

            assertThat(content).contains("remarks=\"用户表\"");
        }

        @Test
        @DisplayName("should include XML schema declaration when changelog generated")
        void shouldIncludeXmlSchemaDeclaration_whenChangelogGenerated() throws IOException {
            SchemaMetadata schema = createTestSchema();
            Path outputPath = tempDir.resolve("001_test_table.xml");

            generator.generateCreateTable(schema, "platform", "1.0.0", 1, outputPath);

            String content = Files.readString(outputPath);

            assertThat(content).contains("xsi:schemaLocation");
            assertThat(content).contains("</databaseChangeLog>");
        }

        @Test
        @DisplayName("should generate index changelog when schema has indexes")
        void shouldGenerateIndexChangelog_whenSchemaHasIndexes() throws IOException {
            IndexMetadata index = IndexMetadataImpl.builder()
                    .indexName("idx_test_name")
                    .columnNames(List.of("name"))
                    .unique(false)
                    .build();
            SchemaMetadata schema = SchemaMetadataImpl.builder()
                    .tableName("test_table")
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("id").dataType("BIGINT").nullable(false)
                            .primaryKey(true).autoIncrement(true).build())
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("name").dataType("VARCHAR(50)").nullable(false).build())
                    .addIndex(index)
                    .build();
            Path outputPath = tempDir.resolve("001_test_table.xml");

            generator.generateCreateTable(schema, "platform", "1.0.0", 1, outputPath);

            String content = Files.readString(outputPath);

            assertThat(content).contains("<createIndex");
            assertThat(content).contains("indexName=\"idx_test_name\"");
        }
    }

    // ========== generateFileName ==========

    @Nested
    @DisplayName("generateFileName")
    class GenerateFileName {

        @Test
        @DisplayName("should generate file name with sequence and table name")
        void shouldGenerateFileName_withSequenceAndTableName() {
            String fileName = generator.generateFileName(1, "sys_user");

            assertThat(fileName).isEqualTo("001_sys_user.xml");
        }

        @Test
        @DisplayName("should pad sequence number with zeros")
        void shouldPadSequenceNumber_withZeros() {
            String fileName = generator.generateFileName(15, "sys_role");

            assertThat(fileName).isEqualTo("015_sys_role.xml");
        }
    }

    // ========== generateOutputDirectory ==========

    @Nested
    @DisplayName("generateOutputDirectory")
    class GenerateOutputDirectory {

        @Test
        @DisplayName("should generate correct directory path")
        void shouldGenerateCorrectDirectoryPath() {
            Path baseDir = Path.of("src/main/resources/db");
            Path result = generator.generateOutputDirectory(baseDir, "platform", "1.0.0");

            assertThat(result).isEqualTo(
                    Path.of("src/main/resources/db/changelog/platform/v1.0.0"));
        }
    }

    // ========== Helper ==========

    private SchemaMetadata createTestSchema() {
        return SchemaMetadataImpl.builder()
                .tableName("test_table")
                .addColumn(ColumnMetadataImpl.builder()
                        .columnName("id").dataType("BIGINT").nullable(false)
                        .primaryKey(true).autoIncrement(true).build())
                .addColumn(ColumnMetadataImpl.builder()
                        .columnName("name").dataType("VARCHAR(50)").nullable(false).build())
                .addColumn(ColumnMetadataImpl.builder()
                        .columnName("status").dataType("INT").nullable(true).build())
                .primaryKey(PrimaryKeyMetadataImpl.builder()
                        .constraintName("pk_test_table")
                        .columnNames(List.of("id")).build())
                .build();
    }
}