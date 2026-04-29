package io.github.afgprojects.framework.data.liquibase.generator;

import io.github.afgprojects.framework.data.core.schema.*;
import org.junit.jupiter.api.BeforeEach;
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
 * ChangeLogGenerator 测试
 */
@DisplayName("ChangeLogGenerator 测试")
class ChangeLogGeneratorTest {

    private ChangeLogGenerator generator;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        generator = new ChangeLogGenerator();
    }

    @Nested
    @DisplayName("generateCreateTable 测试")
    class GenerateCreateTableTests {

        @Test
        @DisplayName("应生成有效的 Liquibase XML 文件")
        void shouldGenerateValidXml() throws IOException {
            SchemaMetadata schema = createTestSchema();
            Path outputPath = tempDir.resolve("changelog.xml");

            generator.generateCreateTable(schema, "test-author", outputPath);

            assertThat(outputPath).exists();
            String content = Files.readString(outputPath);
            assertThat(content).contains("<?xml version=\"1.0\"");
            assertThat(content).contains("<databaseChangeLog");
            assertThat(content).contains("</databaseChangeLog>");
        }

        @Test
        @DisplayName("应包含 createTable 变更")
        void shouldIncludeCreateTableChange() throws IOException {
            SchemaMetadata schema = createTestSchema();
            Path outputPath = tempDir.resolve("changelog.xml");

            generator.generateCreateTable(schema, "test-author", outputPath);

            String content = Files.readString(outputPath);
            assertThat(content).contains("<createTable tableName=\"test_table\"");
            assertThat(content).contains("<column name=\"id\"");
            assertThat(content).contains("<column name=\"name\"");
        }

        @Test
        @DisplayName("应包含主键约束")
        void shouldIncludePrimaryKeyConstraint() throws IOException {
            SchemaMetadata schema = createTestSchema();
            Path outputPath = tempDir.resolve("changelog.xml");

            generator.generateCreateTable(schema, "test-author", outputPath);

            String content = Files.readString(outputPath);
            assertThat(content).contains("primaryKey=\"true\"");
        }

        @Test
        @DisplayName("应包含作者信息")
        void shouldIncludeAuthor() throws IOException {
            SchemaMetadata schema = createTestSchema();
            Path outputPath = tempDir.resolve("changelog.xml");

            generator.generateCreateTable(schema, "test-author", outputPath);

            String content = Files.readString(outputPath);
            assertThat(content).contains("author=\"test-author\"");
        }
    }

    @Nested
    @DisplayName("generateIncremental 测试")
    class GenerateIncrementalTests {

        @Test
        @DisplayName("应生成新增列的变更")
        void shouldGenerateAddColumnChange() throws IOException {
            ColumnMetadata newColumn = ColumnMetadataImpl.builder()
                    .columnName("new_column")
                    .dataType("VARCHAR(100)")
                    .nullable(true)
                    .build();

            SchemaDiff diff = new SchemaDiff(
                    "test_table",
                    true,
                    List.of(new ColumnDiff("new_column", DiffType.ADD, newColumn, null, List.of())),
                    null,
                    null
            );

            Path outputPath = tempDir.resolve("incremental.xml");

            generator.generateIncremental(diff, "test-author", outputPath);

            String content = Files.readString(outputPath);
            assertThat(content).contains("<addColumn");
            assertThat(content).contains("tableName=\"test_table\"");
        }

        @Test
        @DisplayName("应生成删除列的变更")
        void shouldGenerateDropColumnChange() throws IOException {
            SchemaDiff diff = new SchemaDiff(
                    "test_table",
                    true,
                    List.of(new ColumnDiff("old_column", DiffType.DROP, null, null, List.of())),
                    null,
                    null
            );

            Path outputPath = tempDir.resolve("incremental.xml");

            generator.generateIncremental(diff, "test-author", outputPath);

            String content = Files.readString(outputPath);
            assertThat(content).contains("<dropColumn");
            assertThat(content).contains("columnName=\"old_column\"");
        }

        @Test
        @DisplayName("应生成修改列的变更")
        void shouldGenerateModifyColumnChange() throws IOException {
            ColumnMetadata modifiedColumn = ColumnMetadataImpl.builder()
                    .columnName("name")
                    .dataType("VARCHAR(200)")
                    .nullable(false)
                    .build();

            SchemaDiff diff = new SchemaDiff(
                    "test_table",
                    true,
                    List.of(new ColumnDiff("name", DiffType.MODIFY, modifiedColumn, null, List.of("dataType changed"))),
                    null,
                    null
            );

            Path outputPath = tempDir.resolve("incremental.xml");

            generator.generateIncremental(diff, "test-author", outputPath);

            String content = Files.readString(outputPath);
            assertThat(content).contains("<modifyDataType");
            assertThat(content).contains("newDataType=\"VARCHAR(200)\"");
        }
    }

    @Nested
    @DisplayName("generateColumnElement 分支测试")
    class GenerateColumnElementBranchTests {

        @Test
        @DisplayName("应包含默认值")
        void shouldIncludeDefaultValue() throws IOException {
            ColumnMetadata columnWithDefault = ColumnMetadataImpl.builder()
                    .columnName("status")
                    .dataType("VARCHAR(20)")
                    .nullable(true)
                    .defaultValue("ACTIVE")
                    .build();

            SchemaMetadata schema = SchemaMetadataImpl.builder()
                    .tableName("test_table")
                    .addColumn(columnWithDefault)
                    .build();

            Path outputPath = tempDir.resolve("changelog.xml");
            generator.generateCreateTable(schema, "test-author", outputPath);

            String content = Files.readString(outputPath);
            assertThat(content).contains("<defaultValue>ACTIVE</defaultValue>");
        }

        @Test
        @DisplayName("应包含唯一约束")
        void shouldIncludeUniqueConstraint() throws IOException {
            ColumnMetadata uniqueColumn = ColumnMetadataImpl.builder()
                    .columnName("email")
                    .dataType("VARCHAR(100)")
                    .nullable(true)
                    .unique(true)
                    .build();

            SchemaMetadata schema = SchemaMetadataImpl.builder()
                    .tableName("test_table")
                    .addColumn(uniqueColumn)
                    .build();

            Path outputPath = tempDir.resolve("changelog.xml");
            generator.generateCreateTable(schema, "test-author", outputPath);

            String content = Files.readString(outputPath);
            assertThat(content).contains("unique=\"true\"");
        }

        @Test
        @DisplayName("应包含非空约束但不包含主键和唯一约束")
        void shouldIncludeNullableConstraintOnly() throws IOException {
            ColumnMetadata nonNullColumn = ColumnMetadataImpl.builder()
                    .columnName("name")
                    .dataType("VARCHAR(100)")
                    .nullable(false)
                    .primaryKey(false)
                    .unique(false)
                    .build();

            SchemaMetadata schema = SchemaMetadataImpl.builder()
                    .tableName("test_table")
                    .addColumn(nonNullColumn)
                    .build();

            Path outputPath = tempDir.resolve("changelog.xml");
            generator.generateCreateTable(schema, "test-author", outputPath);

            String content = Files.readString(outputPath);
            assertThat(content).contains("nullable=\"false\"");
            assertThat(content).doesNotContain("primaryKey=\"true\"");
            assertThat(content).doesNotContain("unique=\"true\"");
        }

        @Test
        @DisplayName("应同时包含主键和非空约束")
        void shouldIncludePrimaryKeyAndNullableConstraints() throws IOException {
            ColumnMetadata pkColumn = ColumnMetadataImpl.builder()
                    .columnName("id")
                    .dataType("BIGINT")
                    .nullable(false)
                    .primaryKey(true)
                    .build();

            SchemaMetadata schema = SchemaMetadataImpl.builder()
                    .tableName("test_table")
                    .addColumn(pkColumn)
                    .build();

            Path outputPath = tempDir.resolve("changelog.xml");
            generator.generateCreateTable(schema, "test-author", outputPath);

            String content = Files.readString(outputPath);
            assertThat(content).contains("primaryKey=\"true\"");
            assertThat(content).contains("nullable=\"false\"");
        }

        @Test
        @DisplayName("主键列可空时应包含主键约束")
        void shouldIncludePrimaryKeyWhenNullableIsTrue() throws IOException {
            // 测试 hasConstraints 短路求值：nullable=true 但 primaryKey=true
            ColumnMetadata pkColumn = ColumnMetadataImpl.builder()
                    .columnName("id")
                    .dataType("BIGINT")
                    .nullable(true)
                    .primaryKey(true)
                    .build();

            SchemaMetadata schema = SchemaMetadataImpl.builder()
                    .tableName("test_table")
                    .addColumn(pkColumn)
                    .build();

            Path outputPath = tempDir.resolve("changelog.xml");
            generator.generateCreateTable(schema, "test-author", outputPath);

            String content = Files.readString(outputPath);
            assertThat(content).contains("primaryKey=\"true\"");
            // nullable=true 不应生成 nullable 属性
            assertThat(content).doesNotContain("nullable=");
        }

        @Test
        @DisplayName("应同时包含约束和默认值")
        void shouldIncludeConstraintsAndDefaultValue() throws IOException {
            ColumnMetadata column = ColumnMetadataImpl.builder()
                    .columnName("code")
                    .dataType("VARCHAR(50)")
                    .nullable(false)
                    .unique(true)
                    .defaultValue("DEFAULT_CODE")
                    .build();

            SchemaMetadata schema = SchemaMetadataImpl.builder()
                    .tableName("test_table")
                    .addColumn(column)
                    .build();

            Path outputPath = tempDir.resolve("changelog.xml");
            generator.generateCreateTable(schema, "test-author", outputPath);

            String content = Files.readString(outputPath);
            assertThat(content).contains("nullable=\"false\"");
            assertThat(content).contains("unique=\"true\"");
            assertThat(content).contains("<defaultValue>DEFAULT_CODE</defaultValue>");
        }

        @Test
        @DisplayName("可空且无约束的列应生成自闭合标签")
        void shouldGenerateSelfClosingTagForNullableColumnWithoutConstraints() throws IOException {
            ColumnMetadata simpleColumn = ColumnMetadataImpl.builder()
                    .columnName("description")
                    .dataType("VARCHAR(500)")
                    .nullable(true)
                    .primaryKey(false)
                    .unique(false)
                    .build();

            SchemaMetadata schema = SchemaMetadataImpl.builder()
                    .tableName("test_table")
                    .addColumn(simpleColumn)
                    .build();

            Path outputPath = tempDir.resolve("changelog.xml");
            generator.generateCreateTable(schema, "test-author", outputPath);

            String content = Files.readString(outputPath);
            assertThat(content).contains("<column name=\"description\" type=\"VARCHAR(500)\"/>");
            assertThat(content).doesNotContain("<constraints");
            assertThat(content).doesNotContain("<defaultValue>");
        }
    }

    @Nested
    @DisplayName("escapeXml 特殊字符测试")
    class EscapeXmlTests {

        @Test
        @DisplayName("应转义默认值中的 & 符号")
        void shouldEscapeAmpersand() throws IOException {
            ColumnMetadata column = ColumnMetadataImpl.builder()
                    .columnName("url")
                    .dataType("VARCHAR(500)")
                    .nullable(true)
                    .defaultValue("http://example.com?a=1&b=2")
                    .build();

            SchemaMetadata schema = SchemaMetadataImpl.builder()
                    .tableName("test_table")
                    .addColumn(column)
                    .build();

            Path outputPath = tempDir.resolve("changelog.xml");
            generator.generateCreateTable(schema, "test-author", outputPath);

            String content = Files.readString(outputPath);
            assertThat(content).contains("<defaultValue>http://example.com?a=1&amp;b=2</defaultValue>");
        }

        @Test
        @DisplayName("应转义默认值中的小于号")
        void shouldEscapeLessThan() throws IOException {
            ColumnMetadata column = ColumnMetadataImpl.builder()
                    .columnName("condition")
                    .dataType("VARCHAR(100)")
                    .nullable(true)
                    .defaultValue("age < 18")
                    .build();

            SchemaMetadata schema = SchemaMetadataImpl.builder()
                    .tableName("test_table")
                    .addColumn(column)
                    .build();

            Path outputPath = tempDir.resolve("changelog.xml");
            generator.generateCreateTable(schema, "test-author", outputPath);

            String content = Files.readString(outputPath);
            assertThat(content).contains("<defaultValue>age &lt; 18</defaultValue>");
        }

        @Test
        @DisplayName("应转义默认值中的大于号")
        void shouldEscapeGreaterThan() throws IOException {
            ColumnMetadata column = ColumnMetadataImpl.builder()
                    .columnName("condition")
                    .dataType("VARCHAR(100)")
                    .nullable(true)
                    .defaultValue("age > 65")
                    .build();

            SchemaMetadata schema = SchemaMetadataImpl.builder()
                    .tableName("test_table")
                    .addColumn(column)
                    .build();

            Path outputPath = tempDir.resolve("changelog.xml");
            generator.generateCreateTable(schema, "test-author", outputPath);

            String content = Files.readString(outputPath);
            assertThat(content).contains("<defaultValue>age &gt; 65</defaultValue>");
        }

        @Test
        @DisplayName("应转义默认值中的双引号")
        void shouldEscapeDoubleQuote() throws IOException {
            ColumnMetadata column = ColumnMetadataImpl.builder()
                    .columnName("message")
                    .dataType("VARCHAR(200)")
                    .nullable(true)
                    .defaultValue("Say \"Hello\"")
                    .build();

            SchemaMetadata schema = SchemaMetadataImpl.builder()
                    .tableName("test_table")
                    .addColumn(column)
                    .build();

            Path outputPath = tempDir.resolve("changelog.xml");
            generator.generateCreateTable(schema, "test-author", outputPath);

            String content = Files.readString(outputPath);
            assertThat(content).contains("<defaultValue>Say &quot;Hello&quot;</defaultValue>");
        }

        @Test
        @DisplayName("应转义默认值中的单引号")
        void shouldEscapeSingleQuote() throws IOException {
            ColumnMetadata column = ColumnMetadataImpl.builder()
                    .columnName("message")
                    .dataType("VARCHAR(200)")
                    .nullable(true)
                    .defaultValue("It's a test")
                    .build();

            SchemaMetadata schema = SchemaMetadataImpl.builder()
                    .tableName("test_table")
                    .addColumn(column)
                    .build();

            Path outputPath = tempDir.resolve("changelog.xml");
            generator.generateCreateTable(schema, "test-author", outputPath);

            String content = Files.readString(outputPath);
            assertThat(content).contains("<defaultValue>It&apos;s a test</defaultValue>");
        }

        @Test
        @DisplayName("应转义默认值中的多种特殊字符")
        void shouldEscapeMultipleSpecialCharacters() throws IOException {
            ColumnMetadata column = ColumnMetadataImpl.builder()
                    .columnName("content")
                    .dataType("VARCHAR(500)")
                    .nullable(true)
                    .defaultValue("<script>alert(\"test\");</script>")
                    .build();

            SchemaMetadata schema = SchemaMetadataImpl.builder()
                    .tableName("test_table")
                    .addColumn(column)
                    .build();

            Path outputPath = tempDir.resolve("changelog.xml");
            generator.generateCreateTable(schema, "test-author", outputPath);

            String content = Files.readString(outputPath);
            assertThat(content).contains("<defaultValue>&lt;script&gt;alert(&quot;test&quot;);&lt;/script&gt;</defaultValue>");
        }
    }

    @Nested
    @DisplayName("generateIncremental ADD 列分支测试")
    class GenerateIncrementalAddColumnTests {

        @Test
        @DisplayName("新增列应包含默认值")
        void shouldIncludeDefaultValueForAddColumn() throws IOException {
            ColumnMetadata newColumn = ColumnMetadataImpl.builder()
                    .columnName("status")
                    .dataType("VARCHAR(20)")
                    .nullable(true)
                    .defaultValue("PENDING")
                    .build();

            SchemaDiff diff = new SchemaDiff(
                    "test_table",
                    true,
                    List.of(new ColumnDiff("status", DiffType.ADD, newColumn, null, List.of())),
                    null,
                    null
            );

            Path outputPath = tempDir.resolve("incremental.xml");
            generator.generateIncremental(diff, "test-author", outputPath);

            String content = Files.readString(outputPath);
            assertThat(content).contains("<addColumn");
            assertThat(content).contains("<defaultValue>PENDING</defaultValue>");
        }

        @Test
        @DisplayName("新增列应包含唯一约束")
        void shouldIncludeUniqueConstraintForAddColumn() throws IOException {
            ColumnMetadata newColumn = ColumnMetadataImpl.builder()
                    .columnName("code")
                    .dataType("VARCHAR(50)")
                    .nullable(true)
                    .unique(true)
                    .build();

            SchemaDiff diff = new SchemaDiff(
                    "test_table",
                    true,
                    List.of(new ColumnDiff("code", DiffType.ADD, newColumn, null, List.of())),
                    null,
                    null
            );

            Path outputPath = tempDir.resolve("incremental.xml");
            generator.generateIncremental(diff, "test-author", outputPath);

            String content = Files.readString(outputPath);
            assertThat(content).contains("unique=\"true\"");
        }

        @Test
        @DisplayName("ADD 类型但 sourceColumn 为 null 时不应生成变更")
        void shouldNotGenerateChangeForNullSourceColumn() throws IOException {
            SchemaDiff diff = new SchemaDiff(
                    "test_table",
                    true,
                    List.of(new ColumnDiff("null_column", DiffType.ADD, null, null, List.of())),
                    null,
                    null
            );

            Path outputPath = tempDir.resolve("incremental.xml");
            generator.generateIncremental(diff, "test-author", outputPath);

            String content = Files.readString(outputPath);
            assertThat(content).doesNotContain("<addColumn");
            assertThat(content).doesNotContain("null_column");
        }
    }

    @Nested
    @DisplayName("generateIncremental MODIFY 列分支测试")
    class GenerateIncrementalModifyColumnTests {

        @Test
        @DisplayName("MODIFY 类型但 sourceColumn 为 null 时不应生成变更")
        void shouldNotGenerateChangeForNullSourceColumn() throws IOException {
            SchemaDiff diff = new SchemaDiff(
                    "test_table",
                    true,
                    List.of(new ColumnDiff("null_column", DiffType.MODIFY, null, null, List.of())),
                    null,
                    null
            );

            Path outputPath = tempDir.resolve("incremental.xml");
            generator.generateIncremental(diff, "test-author", outputPath);

            String content = Files.readString(outputPath);
            assertThat(content).doesNotContain("<modifyDataType");
            assertThat(content).doesNotContain("null_column");
        }
    }

    private SchemaMetadata createTestSchema() {
        return SchemaMetadataImpl.builder()
                .tableName("test_table")
                .addColumn(ColumnMetadataImpl.builder()
                        .columnName("id")
                        .dataType("BIGINT")
                        .nullable(false)
                        .primaryKey(true)
                        .autoIncrement(true)
                        .build())
                .addColumn(ColumnMetadataImpl.builder()
                        .columnName("name")
                        .dataType("VARCHAR(100)")
                        .nullable(false)
                        .build())
                .addColumn(ColumnMetadataImpl.builder()
                        .columnName("email")
                        .dataType("VARCHAR(200)")
                        .nullable(true)
                        .build())
                .primaryKey(PrimaryKeyMetadataImpl.builder()
                        .constraintName("pk_test_table")
                        .columnNames(List.of("id"))
                        .build())
                .build();
    }
}
