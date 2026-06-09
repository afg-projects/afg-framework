package io.github.afgprojects.framework.data.liquibase.generator;

import io.github.afgprojects.framework.data.core.schema.ColumnMetadataImpl;
import io.github.afgprojects.framework.data.core.schema.SchemaMetadata;
import io.github.afgprojects.framework.data.core.schema.SchemaMetadataImpl;
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
 * EntityCodeGenerator 单元测试
 * <p>
 * 验证从 SchemaMetadata 生成 Entity Java 代码的逻辑。
 */
class EntityCodeGeneratorTest {

    @TempDir
    Path tempDir;

    // ========== 代码生成 ==========

    @Nested
    @DisplayName("代码生成")
    class CodeGeneration {

        @Test
        @DisplayName("should generate Java file when valid schema provided")
        void shouldGenerateJavaFile_whenValidSchemaProvided() throws IOException {
            EntityCodeGenerator generator = new EntityCodeGenerator();
            SchemaMetadata schema = createTestSchema();

            generator.generate(schema, "com.example.entity", tempDir);

            Path generatedFile = tempDir.resolve("com/example/entity/SysUser.java");
            assertThat(Files.exists(generatedFile)).isTrue();
        }

        @Test
        @DisplayName("should generate class with correct name when schema has table name")
        void shouldGenerateClassWithCorrectName_whenSchemaHasTableName() throws IOException {
            EntityCodeGenerator generator = new EntityCodeGenerator();
            SchemaMetadata schema = createTestSchema();

            generator.generate(schema, "com.example.entity", tempDir);

            Path generatedFile = tempDir.resolve("com/example/entity/SysUser.java");
            String content = Files.readString(generatedFile);

            assertThat(content).contains("public class SysUser extends BaseEntity");
        }

        @Test
        @DisplayName("should generate package declaration when package provided")
        void shouldGeneratePackageDeclaration_whenPackageProvided() throws IOException {
            EntityCodeGenerator generator = new EntityCodeGenerator();
            SchemaMetadata schema = createTestSchema();

            generator.generate(schema, "com.example.entity", tempDir);

            Path generatedFile = tempDir.resolve("com/example/entity/SysUser.java");
            String content = Files.readString(generatedFile);

            assertThat(content).contains("package com.example.entity;");
        }

        @Test
        @DisplayName("should skip base entity fields when generating code")
        void shouldSkipBaseEntityFields_whenGeneratingCode() throws IOException {
            EntityCodeGenerator generator = new EntityCodeGenerator();
            SchemaMetadata schema = SchemaMetadataImpl.builder()
                    .tableName("sys_user")
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("id").dataType("BIGINT").nullable(false).build())
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("created_at").dataType("TIMESTAMP").nullable(true).build())
                    .addColumn(ColumnMetadataImpl.builder()
                            .columnName("name").dataType("VARCHAR(50)").nullable(false).build())
                    .build();

            generator.generate(schema, "com.example.entity", tempDir);

            Path generatedFile = tempDir.resolve("com/example/entity/SysUser.java");
            String content = Files.readString(generatedFile);

            // id and created_at should be skipped (BaseEntity fields)
            // only name field should appear
            assertThat(content).contains("private String name;");
            // Should not generate field declarations for id/created_at
            assertThat(content).doesNotContain("private Long id;");
        }
    }

    // ========== Lombok 配置 ==========

    @Nested
    @DisplayName("Lombok 配置")
    class LombokConfig {

        @Test
        @DisplayName("should include @Data annotation when lombok enabled")
        void shouldIncludeDataAnnotation_whenLombokEnabled() throws IOException {
            EntityCodeGenerator generator = new EntityCodeGenerator(true, true);
            SchemaMetadata schema = createTestSchema();

            generator.generate(schema, "com.example.entity", tempDir);

            String content = Files.readString(tempDir.resolve("com/example/entity/SysUser.java"));

            assertThat(content).contains("@Data");
        }

        @Test
        @DisplayName("should not include @Data annotation when lombok disabled")
        void shouldNotIncludeDataAnnotation_whenLombokDisabled() throws IOException {
            EntityCodeGenerator generator = new EntityCodeGenerator(false, false);
            SchemaMetadata schema = createTestSchema();

            generator.generate(schema, "com.example.entity", tempDir);

            String content = Files.readString(tempDir.resolve("com/example/entity/SysUser.java"));

            assertThat(content).doesNotContain("@Data");
            assertThat(content).contains("public String getUsername()");
            assertThat(content).contains("public void setUsername(");
        }
    }

    // ========== Helper ==========

    private SchemaMetadata createTestSchema() {
        return SchemaMetadataImpl.builder()
                .tableName("sys_user")
                .addColumn(ColumnMetadataImpl.builder()
                        .columnName("username").dataType("VARCHAR(50)").nullable(false).build())
                .addColumn(ColumnMetadataImpl.builder()
                        .columnName("status").dataType("INT").nullable(true).build())
                .build();
    }
}