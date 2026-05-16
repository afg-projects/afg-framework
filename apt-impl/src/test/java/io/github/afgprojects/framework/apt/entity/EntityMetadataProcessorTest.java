package io.github.afgprojects.framework.apt.entity;

import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import static com.google.testing.compile.Compiler.javac;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * EntityMetadataProcessor 集成测试
 * <p>
 * 验证 APT 能正确生成元数据类，并且生成的类能正确实现 DatabaseEntityMetadata 接口。
 */
@DisplayName("EntityMetadataProcessor Integration Tests")
class EntityMetadataProcessorTest {

    private static List<File> classpathFiles;

    @BeforeAll
    static void setUpClasspath() {
        // 获取当前类加载器的 classpath
        classpathFiles = getClasspathFiles();
    }

    @Nested
    @DisplayName("元数据类生成")
    class MetadataGenerationTests {

        @Test
        @DisplayName("应该为 @AfEntity 注解的类生成元数据类")
        void shouldGenerateMetadataClassForAnnotatedEntity() {
            // 准备测试实体源码
            JavaFileObject entitySource = JavaFileObjects.forSourceString(
                "io.github.afgprojects.framework.apt.entity.test.TestEntity",
                """
                package io.github.afgprojects.framework.apt.entity.test;

                import io.github.afgprojects.framework.apt.entity.AfEntity;
                import jakarta.persistence.Table;

                @AfEntity
                @Table(name = "sys_user")
                public class TestEntity {
                    private Long id;
                    private String name;
                }
                """
            );

            // 编译并验证生成了元数据类
            var compilation = javac()
                .withProcessors(new EntityMetadataProcessor())
                .withClasspath(classpathFiles)
                .compile(entitySource);

            // 验证编译成功（无错误）
            assertThat(compilation.errors()).isEmpty();
        }

        @Test
        @DisplayName("应该从 @Table 注解提取表名")
        void shouldExtractTableNameFromAnnotation() throws IOException {
            String generatedSource = compileAndExtractGeneratedSource(
                "TableTestEntity",
                """
                package io.github.afgprojects.framework.apt.entity.test;

                import io.github.afgprojects.framework.apt.entity.AfEntity;
                import jakarta.persistence.Table;

                @AfEntity
                @Table(name = "sys_user")
                public class TableTestEntity {
                    private Long id;
                }
                """
            );

            assertThat(generatedSource).contains("TABLE_NAME = \"sys_user\"");
        }

        @Test
        @DisplayName("应该从 @Column 注解提取列名")
        void shouldExtractColumnNameFromAnnotation() throws IOException {
            String generatedSource = compileAndExtractGeneratedSource(
                "ColumnTestEntity",
                """
                package io.github.afgprojects.framework.apt.entity.test;

                import io.github.afgprojects.framework.apt.entity.AfEntity;
                import jakarta.persistence.Column;

                @AfEntity
                public class ColumnTestEntity {
                    private Long id;

                    @Column(name = "is_deleted")
                    private Boolean deleted;
                }
                """
            );

            assertThat(generatedSource).contains("return \"is_deleted\"");
        }

        @Test
        @DisplayName("应该识别 @Id 注解标记的主键字段")
        void shouldIdentifyIdField() throws IOException {
            String generatedSource = compileAndExtractGeneratedSource(
                "IdTestEntity",
                """
                package io.github.afgprojects.framework.apt.entity.test;

                import io.github.afgprojects.framework.apt.entity.AfEntity;
                import jakarta.persistence.Id;

                @AfEntity
                public class IdTestEntity {
                    @Id
                    private Long userId;

                    private String name;
                }
                """
            );

            assertThat(generatedSource).contains("return true").contains("userId");
        }

        @Test
        @DisplayName("应该将 camelCase 属性名转换为 snake_case 列名")
        void shouldConvertCamelCaseToSnakeCase() throws IOException {
            String generatedSource = compileAndExtractGeneratedSource(
                "CamelCaseTestEntity",
                """
                package io.github.afgprojects.framework.apt.entity.test;

                import io.github.afgprojects.framework.apt.entity.AfEntity;

                @AfEntity
                public class CamelCaseTestEntity {
                    private Long id;
                    private String userName;
                    private Integer userAge;
                }
                """
            );

            assertThat(generatedSource).contains("user_name");
            assertThat(generatedSource).contains("user_age");
        }
    }

    @Nested
    @DisplayName("生成的元数据类结构")
    class GeneratedClassStructureTests {

        @Test
        @DisplayName("应该实现 DatabaseEntityMetadata 接口")
        void shouldImplementDatabaseEntityMetadata() throws IOException {
            String generatedSource = compileAndExtractGeneratedSource(
                "InterfaceTestEntity",
                """
                package io.github.afgprojects.framework.apt.entity.test;

                import io.github.afgprojects.framework.apt.entity.AfEntity;

                @AfEntity
                public class InterfaceTestEntity {
                    private Long id;
                }
                """
            );

            assertThat(generatedSource).contains("implements DatabaseEntityMetadata<InterfaceTestEntity>");
        }

        @Test
        @DisplayName("应该生成所有必需的接口方法")
        void shouldGenerateAllRequiredMethods() throws IOException {
            String generatedSource = compileAndExtractGeneratedSource(
                "MethodsTestEntity",
                """
                package io.github.afgprojects.framework.apt.entity.test;

                import io.github.afgprojects.framework.apt.entity.AfEntity;

                @AfEntity
                public class MethodsTestEntity {
                    private Long id;
                    private String name;
                }
                """
            );

            // EntityMetadata 接口方法
            assertThat(generatedSource).contains("getEntityClass()");
            assertThat(generatedSource).contains("getTableName()");
            assertThat(generatedSource).contains("getIdField()");
            assertThat(generatedSource).contains("getFields()");
            assertThat(generatedSource).contains("getField(String propertyName)");

            // 特性标记方法
            assertThat(generatedSource).contains("isSoftDeletable()");
            assertThat(generatedSource).contains("isTenantAware()");
            assertThat(generatedSource).contains("isAuditable()");
            assertThat(generatedSource).contains("isVersioned()");

            // ColumnNameAware 接口方法
            assertThat(generatedSource).contains("getColumnName(String propertyName)");
        }

        @Test
        @DisplayName("应该为每个字段生成 FieldMetadata 内部类")
        void shouldGenerateFieldMetadataInnerClasses() throws IOException {
            String generatedSource = compileAndExtractGeneratedSource(
                "FieldMetadataTestEntity",
                """
                package io.github.afgprojects.framework.apt.entity.test;

                import io.github.afgprojects.framework.apt.entity.AfEntity;

                @AfEntity
                public class FieldMetadataTestEntity {
                    private Long id;
                    private String firstName;
                    private String lastName;
                }
                """
            );

            assertThat(generatedSource).contains("class IdFieldMetadata implements DatabaseFieldMetadata");
            assertThat(generatedSource).contains("class FirstNameFieldMetadata implements DatabaseFieldMetadata");
            assertThat(generatedSource).contains("class LastNameFieldMetadata implements DatabaseFieldMetadata");
        }
    }

    @Nested
    @DisplayName("Boolean 字段处理")
    class BooleanFieldTests {

        @Test
        @DisplayName("应该正确处理 Boolean 类型字段的 is_ 前缀")
        void shouldHandleBooleanIsPrefix() throws IOException {
            String generatedSource = compileAndExtractGeneratedSource(
                "BooleanTestEntity",
                """
                package io.github.afgprojects.framework.apt.entity.test;

                import io.github.afgprojects.framework.apt.entity.AfEntity;
                import jakarta.persistence.Column;

                @AfEntity
                public class BooleanTestEntity {
                    private Long id;

                    @Column(name = "is_deleted")
                    private Boolean deleted;

                    @Column(name = "is_active")
                    private Boolean active;
                }
                """
            );

            assertThat(generatedSource).contains("is_deleted");
            assertThat(generatedSource).contains("is_active");
        }

        @Test
        @DisplayName("Boolean 字段无 @Column 注解时应转换为 snake_case")
        void shouldConvertBooleanFieldWithoutAnnotation() throws IOException {
            String generatedSource = compileAndExtractGeneratedSource(
                "BooleanNoAnnotationTestEntity",
                """
                package io.github.afgprojects.framework.apt.entity.test;

                import io.github.afgprojects.framework.apt.entity.AfEntity;

                @AfEntity
                public class BooleanNoAnnotationTestEntity {
                    private Long id;
                    private Boolean isEnabled;
                }
                """
            );

            // isEnabled -> is_enabled (snake_case)
            assertThat(generatedSource).contains("is_enabled");
        }
    }

    @Nested
    @DisplayName("边界场景")
    class EdgeCaseTests {

        @Test
        @DisplayName("应该处理无 @Table 注解的实体")
        void shouldHandleEntityWithoutTableAnnotation() throws IOException {
            String generatedSource = compileAndExtractGeneratedSource(
                "NoTableTestEntity",
                """
                package io.github.afgprojects.framework.apt.entity.test;

                import io.github.afgprojects.framework.apt.entity.AfEntity;

                @AfEntity
                public class NoTableTestEntity {
                    private Long id;
                }
                """
            );

            // 类名转 snake_case: NoTableTestEntity -> no_table_test_entity
            assertThat(generatedSource).contains("TABLE_NAME = \"no_table_test_entity\"");
        }

        @Test
        @DisplayName("应该处理空实体（无字段）")
        void shouldHandleEmptyEntity() throws IOException {
            String generatedSource = compileAndExtractGeneratedSource(
                "EmptyTestEntity",
                """
                package io.github.afgprojects.framework.apt.entity.test;

                import io.github.afgprojects.framework.apt.entity.AfEntity;

                @AfEntity
                public class EmptyTestEntity {
                }
                """
            );

            assertThat(generatedSource).contains("FIELDS = List.of(");
            assertThat(generatedSource).contains("getIdField()");
            // 空实体的主键字段通过流式查找返回 null
            assertThat(generatedSource).contains(".orElse(null)");
        }

        @Test
        @DisplayName("应该跳过静态字段")
        void shouldSkipStaticFields() throws IOException {
            String generatedSource = compileAndExtractGeneratedSource(
                "StaticFieldTestEntity",
                """
                package io.github.afgprojects.framework.apt.entity.test;

                import io.github.afgprojects.framework.apt.entity.AfEntity;

                @AfEntity
                public class StaticFieldTestEntity {
                    private static final String CONSTANT = "constant";
                    private Long id;
                    private String name;
                }
                """
            );

            // 只应该有 id 和 name 两个字段
            assertThat(generatedSource).contains("class IdFieldMetadata");
            assertThat(generatedSource).contains("class NameFieldMetadata");
            assertThat(generatedSource).doesNotContain("CONSTANT");
        }
    }

    /**
     * 编译测试源码并提取生成的元数据类源码
     *
     * @param className 类名
     * @param sourceCode 测试实体源码
     * @return 生成的元数据类源码
     */
    private String compileAndExtractGeneratedSource(String className, String sourceCode) throws IOException {
        JavaFileObject entitySource = JavaFileObjects.forSourceString(
            "io.github.afgprojects.framework.apt.entity.test." + className,
            sourceCode
        );

        // 编译并获取生成的文件
        var compilation = javac()
            .withProcessors(new EntityMetadataProcessor())
            .withClasspath(classpathFiles)
            .compile(entitySource);

        // 验证编译成功
        assertThat(compilation.errors())
            .as("Compilation should succeed, but got errors: " + compilation.diagnostics())
            .isEmpty();

        // 查找生成的元数据类
        JavaFileObject generatedFile = compilation.generatedSourceFiles()
            .stream()
            .filter(f -> f.getName().contains("Metadata"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No metadata class generated"));

        return generatedFile.getCharContent(false).toString();
    }

    /**
     * 获取当前 classpath 文件列表
     */
    private static List<File> getClasspathFiles() {
        List<File> files = new ArrayList<>();

        // 从当前类加载器获取 classpath
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader instanceof URLClassLoader urlClassLoader) {
            for (URL url : urlClassLoader.getURLs()) {
                File file = new File(url.getFile());
                if (file.exists()) {
                    files.add(file);
                }
            }
        }

        // 添加 java.class.path 中的条目
        String classpath = System.getProperty("java.class.path");
        if (classpath != null) {
            for (String path : classpath.split(File.pathSeparator)) {
                File file = new File(path);
                if (file.exists() && !files.contains(file)) {
                    files.add(file);
                }
            }
        }

        return files;
    }
}