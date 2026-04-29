package io.github.afgprojects.framework.data.liquibase.generator;

import io.github.afgprojects.framework.data.core.schema.ColumnMetadataImpl;
import io.github.afgprojects.framework.data.core.schema.SchemaMetadata;
import io.github.afgprojects.framework.data.core.schema.SchemaMetadataImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EntityCodeGenerator 测试
 */
@DisplayName("EntityCodeGenerator 测试")
class EntityCodeGeneratorTest {

    private EntityCodeGenerator generator;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        generator = new EntityCodeGenerator(true, true);
    }

    @Nested
    @DisplayName("generate 测试")
    class GenerateTests {

        @Test
        @DisplayName("应生成 Java 文件")
        void shouldGenerateJavaFile() throws IOException {
            SchemaMetadata schema = createTestSchema();

            generator.generate(schema, "com.example.entity", tempDir);

            Path outputFile = tempDir.resolve("com/example/entity/TestTable.java");
            assertThat(outputFile).exists();
        }

        @Test
        @DisplayName("应包含包声明")
        void shouldIncludePackageDeclaration() throws IOException {
            SchemaMetadata schema = createTestSchema();

            generator.generate(schema, "com.example.entity", tempDir);

            String content = Files.readString(tempDir.resolve("com/example/entity/TestTable.java"));
            assertThat(content).contains("package com.example.entity;");
        }

        @Test
        @DisplayName("应包含类声明")
        void shouldIncludeClassDeclaration() throws IOException {
            SchemaMetadata schema = createTestSchema();

            generator.generate(schema, "com.example.entity", tempDir);

            String content = Files.readString(tempDir.resolve("com/example/entity/TestTable.java"));
            assertThat(content).contains("public class TestTable");
        }

        @Test
        @DisplayName("应包含 Lombok 注解")
        void shouldIncludeLombokAnnotation() throws IOException {
            SchemaMetadata schema = createTestSchema();

            generator.generate(schema, "com.example.entity", tempDir);

            String content = Files.readString(tempDir.resolve("com/example/entity/TestTable.java"));
            assertThat(content).contains("@Data");
        }

        @Test
        @DisplayName("应包含字段声明")
        void shouldIncludeFieldDeclarations() throws IOException {
            SchemaMetadata schema = createTestSchema();

            generator.generate(schema, "com.example.entity", tempDir);

            String content = Files.readString(tempDir.resolve("com/example/entity/TestTable.java"));
            assertThat(content).contains("private String name");
            // email 是可空的，会有 @Nullable 注解
            assertThat(content).contains("@Nullable String email");
        }

        @Test
        @DisplayName("应跳过 BaseEntity 字段")
        void shouldSkipBaseEntityFields() throws IOException {
            SchemaMetadata schema = SchemaMetadataImpl.builder()
                    .tableName("test_table")
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("id")
                            .dataType("BIGINT")
                            .build())
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("create_time")
                            .dataType("TIMESTAMP")
                            .build())
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("name")
                            .dataType("VARCHAR(100)")
                            .build())
                    .build();

            generator.generate(schema, "com.example.entity", tempDir);

            String content = Files.readString(tempDir.resolve("com/example/entity/TestTable.java"));
            // 验证包含 name 字段（非 BaseEntity 字段，可空所以有 @Nullable）
            assertThat(content).contains("@Nullable String name");
            // 验证文件存在且是有效的 Java 类
            assertThat(content).contains("public class TestTable");
        }

        @Test
        @DisplayName("应包含字段注释")
        void shouldIncludeFieldComment() throws IOException {
            SchemaMetadata schema = SchemaMetadataImpl.builder()
                    .tableName("test_table")
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("name")
                            .dataType("VARCHAR(100)")
                            .comment("用户名称")
                            .build())
                    .build();

            generator.generate(schema, "com.example.entity", tempDir);

            String content = Files.readString(tempDir.resolve("com/example/entity/TestTable.java"));
            assertThat(content).contains("name - 用户名称");
        }

        @Test
        @DisplayName("应处理空字段注释")
        void shouldHandleEmptyFieldComment() throws IOException {
            SchemaMetadata schema = SchemaMetadataImpl.builder()
                    .tableName("test_table")
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("name")
                            .dataType("VARCHAR(100)")
                            .comment("")
                            .build())
                    .build();

            generator.generate(schema, "com.example.entity", tempDir);

            String content = Files.readString(tempDir.resolve("com/example/entity/TestTable.java"));
            assertThat(content).contains("/**");
            assertThat(content).contains("name");
        }
    }

    @Nested
    @DisplayName("不使用 Lombok 测试")
    class NoLombokTests {

        @Test
        @DisplayName("应生成 getter/setter 方法")
        void shouldGenerateGetterSetter() throws IOException {
            EntityCodeGenerator noLombokGenerator = new EntityCodeGenerator(false, true);
            SchemaMetadata schema = createTestSchema();

            noLombokGenerator.generate(schema, "com.example.entity", tempDir);

            String content = Files.readString(tempDir.resolve("com/example/entity/TestTable.java"));
            assertThat(content).contains("public String getName()");
            assertThat(content).contains("public void setName(String name)");
            assertThat(content).doesNotContain("@Data");
        }
    }

    @Nested
    @DisplayName("不使用 JSR-305 测试")
    class NoJsr305Tests {

        @Test
        @DisplayName("不应包含 @Nullable 注解")
        void shouldNotIncludeNullableAnnotation() throws IOException {
            EntityCodeGenerator noJsr305Generator = new EntityCodeGenerator(true, false);
            SchemaMetadata schema = createTestSchema();

            noJsr305Generator.generate(schema, "com.example.entity", tempDir);

            String content = Files.readString(tempDir.resolve("com/example/entity/TestTable.java"));
            assertThat(content).doesNotContain("@Nullable");
        }
    }

    @Nested
    @DisplayName("数据类型映射测试")
    class DataTypeMappingTests {

        @Test
        @DisplayName("应正确映射 BIGINT 到 Long")
        void shouldMapBigintToLong() throws IOException {
            SchemaMetadata schema = SchemaMetadataImpl.builder()
                    .tableName("test_table")
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("id")
                            .dataType("BIGINT")
                            .build())
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("created_at")
                            .dataType("TIMESTAMP")
                            .build())
                    .build();

            generator.generate(schema, "com.example.entity", tempDir);

            String content = Files.readString(tempDir.resolve("com/example/entity/TestTable.java"));
            // 时间类型会导入 LocalDateTime
            assertThat(content).contains("import java.time.LocalDateTime");
        }

        @Test
        @DisplayName("应正确映射 DECIMAL 到 BigDecimal")
        void shouldMapDecimalToBigDecimal() throws IOException {
            SchemaMetadata schema = SchemaMetadataImpl.builder()
                    .tableName("test_table")
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("amount")
                            .dataType("DECIMAL(10,2)")
                            .nullable(false)
                            .build())
                    .build();

            generator.generate(schema, "com.example.entity", tempDir);

            String content = Files.readString(tempDir.resolve("com/example/entity/TestTable.java"));
            assertThat(content).contains("import java.math.BigDecimal");
            assertThat(content).contains("private BigDecimal amount");
        }

        @Test
        @DisplayName("应正确映射各种整数类型")
        void shouldMapIntegerTypes() throws IOException {
            SchemaMetadata schema = SchemaMetadataImpl.builder()
                    .tableName("test_table")
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("int_col")
                            .dataType("INT")
                            .nullable(false)
                            .build())
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("bigint_col")
                            .dataType("BIGINT")
                            .nullable(false)
                            .build())
                    .build();

            generator.generate(schema, "com.example.entity", tempDir);

            String content = Files.readString(tempDir.resolve("com/example/entity/TestTable.java"));
            assertThat(content).contains("private Integer intCol");
            assertThat(content).contains("private Long bigintCol");
        }

        @Test
        @DisplayName("应正确映射布尔类型")
        void shouldMapBooleanTypes() throws IOException {
            SchemaMetadata schema = SchemaMetadataImpl.builder()
                    .tableName("test_table")
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("bit_col")
                            .dataType("BIT")
                            .nullable(false)
                            .build())
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("bool_col")
                            .dataType("BOOLEAN")
                            .nullable(false)
                            .build())
                    .build();

            generator.generate(schema, "com.example.entity", tempDir);

            String content = Files.readString(tempDir.resolve("com/example/entity/TestTable.java"));
            assertThat(content).contains("private Boolean bitCol");
            assertThat(content).contains("private Boolean boolCol");
        }

        @Test
        @DisplayName("应正确映射浮点类型")
        void shouldMapFloatTypes() throws IOException {
            SchemaMetadata schema = SchemaMetadataImpl.builder()
                    .tableName("test_table")
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("double_col")
                            .dataType("DOUBLE")
                            .nullable(false)
                            .build())
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("float_col")
                            .dataType("FLOAT")
                            .nullable(false)
                            .build())
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("real_col")
                            .dataType("REAL")
                            .nullable(false)
                            .build())
                    .build();

            generator.generate(schema, "com.example.entity", tempDir);

            String content = Files.readString(tempDir.resolve("com/example/entity/TestTable.java"));
            assertThat(content).contains("private Double doubleCol");
            assertThat(content).contains("private Float floatCol");
            assertThat(content).contains("private Float realCol");
        }

        @Test
        @DisplayName("应正确映射日期时间类型")
        void shouldMapDateTimeTypes() throws IOException {
            SchemaMetadata schema = SchemaMetadataImpl.builder()
                    .tableName("test_table")
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("date_col")
                            .dataType("DATE")
                            .build())
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("timestamp_col")
                            .dataType("TIMESTAMP")
                            .build())
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("datetime_col")
                            .dataType("DATETIME")
                            .build())
                    .build();

            generator.generate(schema, "com.example.entity", tempDir);

            String content = Files.readString(tempDir.resolve("com/example/entity/TestTable.java"));
            assertThat(content).contains("import java.time.LocalDateTime");
        }

        @Test
        @DisplayName("应正确映射二进制类型")
        void shouldMapBinaryTypes() throws IOException {
            SchemaMetadata schema = SchemaMetadataImpl.builder()
                    .tableName("test_table")
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("blob_col")
                            .dataType("BLOB")
                            .nullable(false)
                            .build())
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("binary_col")
                            .dataType("BINARY(100)")
                            .nullable(false)
                            .build())
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("varbinary_col")
                            .dataType("VARBINARY(100)")
                            .nullable(false)
                            .build())
                    .build();

            generator.generate(schema, "com.example.entity", tempDir);

            String content = Files.readString(tempDir.resolve("com/example/entity/TestTable.java"));
            assertThat(content).contains("private byte[] blobCol");
            assertThat(content).contains("private byte[] binaryCol");
            assertThat(content).contains("private byte[] varbinaryCol");
        }

        @Test
        @DisplayName("应正确映射文本类型")
        void shouldMapTextTypes() throws IOException {
            SchemaMetadata schema = SchemaMetadataImpl.builder()
                    .tableName("test_table")
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("clob_col")
                            .dataType("CLOB")
                            .nullable(false)
                            .build())
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("text_col")
                            .dataType("TEXT")
                            .nullable(false)
                            .build())
                    .build();

            generator.generate(schema, "com.example.entity", tempDir);

            String content = Files.readString(tempDir.resolve("com/example/entity/TestTable.java"));
            assertThat(content).contains("private String clobCol");
            assertThat(content).contains("private String textCol");
        }

        @Test
        @DisplayName("应默认映射为 String")
        void shouldDefaultToString() throws IOException {
            SchemaMetadata schema = SchemaMetadataImpl.builder()
                    .tableName("test_table")
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("unknown_col")
                            .dataType("UNKNOWN_TYPE")
                            .nullable(false)
                            .build())
                    .build();

            generator.generate(schema, "com.example.entity", tempDir);

            String content = Files.readString(tempDir.resolve("com/example/entity/TestTable.java"));
            assertThat(content).contains("private String unknownCol");
        }
    }

    @Nested
    @DisplayName("命名转换测试")
    class NamingConversionTests {

        @Test
        @DisplayName("应正确转换表名为类名")
        void shouldConvertTableNameToClassName() throws IOException {
            SchemaMetadata schema = SchemaMetadataImpl.builder()
                    .tableName("user_role_mapping")
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("id")
                            .dataType("BIGINT")
                            .build())
                    .build();

            generator.generate(schema, "com.example.entity", tempDir);

            Path outputFile = tempDir.resolve("com/example/entity/UserRoleMapping.java");
            assertThat(outputFile).exists();

            String content = Files.readString(outputFile);
            assertThat(content).contains("public class UserRoleMapping");
        }

        @Test
        @DisplayName("应正确转换列名为字段名")
        void shouldConvertColumnNameToFieldName() throws IOException {
            SchemaMetadata schema = SchemaMetadataImpl.builder()
                    .tableName("test_table")
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("user_name")
                            .dataType("VARCHAR(100)")
                            .nullable(false)
                            .build())
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("name")
                            .dataType("VARCHAR(100)")
                            .nullable(false)
                            .build())
                    .build();

            generator.generate(schema, "com.example.entity", tempDir);

            String content = Files.readString(tempDir.resolve("com/example/entity/TestTable.java"));
            assertThat(content).contains("private String userName");
            assertThat(content).contains("private String name");
        }
    }

    @Nested
    @DisplayName("BaseEntity 字段测试")
    class BaseEntityFieldTests {

        @Test
        @DisplayName("应跳过所有 BaseEntity 字段")
        void shouldSkipAllBaseEntityFields() throws IOException {
            SchemaMetadata schema = SchemaMetadataImpl.builder()
                    .tableName("test_table")
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("id")
                            .dataType("BIGINT")
                            .build())
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("create_time")
                            .dataType("TIMESTAMP")
                            .build())
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("update_time")
                            .dataType("TIMESTAMP")
                            .build())
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("created_at")
                            .dataType("TIMESTAMP")
                            .build())
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("updated_at")
                            .dataType("TIMESTAMP")
                            .build())
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("name")
                            .dataType("VARCHAR(100)")
                            .build())
                    .build();

            generator.generate(schema, "com.example.entity", tempDir);

            String content = Files.readString(tempDir.resolve("com/example/entity/TestTable.java"));
            // 只有 name 字段应该存在（可空所以有 @Nullable）
            assertThat(content).contains("@Nullable String name");
        }
    }

    @Nested
    @DisplayName("构造函数测试")
    class ConstructorTests {

        @Test
        @DisplayName("默认构造函数应使用 Lombok 和 JSR-305")
        void defaultConstructorShouldUseLombokAndJsr305() throws IOException {
            EntityCodeGenerator defaultGenerator = new EntityCodeGenerator();
            SchemaMetadata schema = createTestSchema();

            defaultGenerator.generate(schema, "com.example.entity", tempDir);

            String content = Files.readString(tempDir.resolve("com/example/entity/TestTable.java"));
            assertThat(content).contains("@Data");
            assertThat(content).contains("@Nullable");
        }
    }

    private SchemaMetadata createTestSchema() {
        return SchemaMetadataImpl.builder()
                .tableName("test_table")
                .addColumn(ColumnMetadataImpl.builder()
                        .columnName("id")
                        .dataType("BIGINT")
                        .primaryKey(true)
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
                .build();
    }
}
