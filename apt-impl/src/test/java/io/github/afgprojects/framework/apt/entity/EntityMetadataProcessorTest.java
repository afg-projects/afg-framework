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

    @Nested
    @DisplayName("通用字段元数据")
    class CommonFieldMetadataTests {

        @Test
        @DisplayName("应该使用 CommonFieldMetadata.CREATED_AT 替代内部类")
        void shouldUseCommonFieldMetadataForCreatedAt() throws IOException {
            String generatedSource = compileAndExtractGeneratedSource(
                "CommonFieldCreatedAtTestEntity",
                """
                package io.github.afgprojects.framework.apt.entity.test;

                import io.github.afgprojects.framework.apt.entity.AfEntity;
                import java.time.LocalDateTime;

                @AfEntity
                public class CommonFieldCreatedAtTestEntity {
                    private Long id;
                    private LocalDateTime createdAt;
                    private String name;
                }
                """
            );

            // 应该使用 CommonFieldMetadata.CREATED_AT
            assertThat(generatedSource).contains("CommonFieldMetadata.CREATED_AT");
            // 不应该生成 CreatedAtFieldMetadata 内部类
            assertThat(generatedSource).doesNotContain("class CreatedAtFieldMetadata");
        }

        @Test
        @DisplayName("应该使用 CommonFieldMetadata.UPDATED_AT 替代内部类")
        void shouldUseCommonFieldMetadataForUpdatedAt() throws IOException {
            String generatedSource = compileAndExtractGeneratedSource(
                "CommonFieldUpdatedAtTestEntity",
                """
                package io.github.afgprojects.framework.apt.entity.test;

                import io.github.afgprojects.framework.apt.entity.AfEntity;
                import java.time.LocalDateTime;

                @AfEntity
                public class CommonFieldUpdatedAtTestEntity {
                    private Long id;
                    private LocalDateTime updatedAt;
                }
                """
            );

            assertThat(generatedSource).contains("CommonFieldMetadata.UPDATED_AT");
            assertThat(generatedSource).doesNotContain("class UpdatedAtFieldMetadata");
        }

        @Test
        @DisplayName("应该使用 CommonFieldMetadata.DELETED 替代内部类")
        void shouldUseCommonFieldMetadataForDeleted() throws IOException {
            String generatedSource = compileAndExtractGeneratedSource(
                "CommonFieldDeletedTestEntity",
                """
                package io.github.afgprojects.framework.apt.entity.test;

                import io.github.afgprojects.framework.apt.entity.AfEntity;

                @AfEntity
                public class CommonFieldDeletedTestEntity {
                    private Long id;
                    private Boolean deleted;
                }
                """
            );

            assertThat(generatedSource).contains("CommonFieldMetadata.DELETED");
            assertThat(generatedSource).doesNotContain("class DeletedFieldMetadata");
        }

        @Test
        @DisplayName("应该使用 CommonFieldMetadata.TENANT_ID 替代内部类")
        void shouldUseCommonFieldMetadataForTenantId() throws IOException {
            String generatedSource = compileAndExtractGeneratedSource(
                "CommonFieldTenantIdTestEntity",
                """
                package io.github.afgprojects.framework.apt.entity.test;

                import io.github.afgprojects.framework.apt.entity.AfEntity;

                @AfEntity
                public class CommonFieldTenantIdTestEntity {
                    private Long id;
                    private String tenantId;
                }
                """
            );

            assertThat(generatedSource).contains("CommonFieldMetadata.TENANT_ID");
            assertThat(generatedSource).doesNotContain("class TenantIdFieldMetadata");
        }

        @Test
        @DisplayName("应该使用 CommonFieldMetadata.VERSION_LONG 替代内部类")
        void shouldUseCommonFieldMetadataForVersionLong() throws IOException {
            String generatedSource = compileAndExtractGeneratedSource(
                "CommonFieldVersionLongTestEntity",
                """
                package io.github.afgprojects.framework.apt.entity.test;

                import io.github.afgprojects.framework.apt.entity.AfEntity;

                @AfEntity
                public class CommonFieldVersionLongTestEntity {
                    private Long id;
                    private Long version;
                }
                """
            );

            assertThat(generatedSource).contains("CommonFieldMetadata.VERSION_LONG");
            assertThat(generatedSource).doesNotContain("class VersionFieldMetadata");
        }

        @Test
        @DisplayName("应该使用 CommonFieldMetadata.VERSION_INTEGER 替代内部类")
        void shouldUseCommonFieldMetadataForVersionInteger() throws IOException {
            String generatedSource = compileAndExtractGeneratedSource(
                "CommonFieldVersionIntegerTestEntity",
                """
                package io.github.afgprojects.framework.apt.entity.test;

                import io.github.afgprojects.framework.apt.entity.AfEntity;

                @AfEntity
                public class CommonFieldVersionIntegerTestEntity {
                    private Long id;
                    private Integer version;
                }
                """
            );

            assertThat(generatedSource).contains("CommonFieldMetadata.VERSION_INTEGER");
            assertThat(generatedSource).doesNotContain("class VersionFieldMetadata");
        }

        @Test
        @DisplayName("自定义列名时不应该使用通用字段元数据")
        void shouldNotUseCommonFieldMetadataForCustomColumnName() throws IOException {
            String generatedSource = compileAndExtractGeneratedSource(
                "CustomColumnNameTestEntity",
                """
                package io.github.afgprojects.framework.apt.entity.test;

                import io.github.afgprojects.framework.apt.entity.AfEntity;
                import jakarta.persistence.Column;
                import java.time.LocalDateTime;

                @AfEntity
                public class CustomColumnNameTestEntity {
                    private Long id;

                    @Column(name = "create_time")
                    private LocalDateTime createdAt;
                }
                """
            );

            // 自定义列名，应该生成内部类
            assertThat(generatedSource).contains("class CreatedAtFieldMetadata");
            assertThat(generatedSource).contains("create_time");
            // 不应该使用 CommonFieldMetadata
            assertThat(generatedSource).doesNotContain("CommonFieldMetadata.CREATED_AT");
        }

        @Test
        @DisplayName("类型不匹配时不应该使用通用字段元数据")
        void shouldNotUseCommonFieldMetadataForMismatchedType() throws IOException {
            String generatedSource = compileAndExtractGeneratedSource(
                "MismatchedTypeTestEntity",
                """
                package io.github.afgprojects.framework.apt.entity.test;

                import io.github.afgprojects.framework.apt.entity.AfEntity;

                @AfEntity
                public class MismatchedTypeTestEntity {
                    private Long id;
                    // createdAt 应该是 LocalDateTime，但这里是 String
                    private String createdAt;
                }
                """
            );

            // 类型不匹配，应该生成内部类
            assertThat(generatedSource).contains("class CreatedAtFieldMetadata");
            assertThat(generatedSource).doesNotContain("CommonFieldMetadata.CREATED_AT");
        }

        @Test
        @DisplayName("多个通用字段应该全部使用 CommonFieldMetadata")
        void shouldUseCommonFieldMetadataForMultipleFields() throws IOException {
            String generatedSource = compileAndExtractGeneratedSource(
                "MultipleCommonFieldsTestEntity",
                """
                package io.github.afgprojects.framework.apt.entity.test;

                import io.github.afgprojects.framework.apt.entity.AfEntity;
                import java.time.LocalDateTime;

                @AfEntity
                public class MultipleCommonFieldsTestEntity {
                    private Long id;
                    private String tenantId;
                    private LocalDateTime createdAt;
                    private LocalDateTime updatedAt;
                    private Boolean deleted;
                    private Long version;
                    private String name;
                }
                """
            );

            // 所有通用字段都应该使用 CommonFieldMetadata
            assertThat(generatedSource).contains("CommonFieldMetadata.TENANT_ID");
            assertThat(generatedSource).contains("CommonFieldMetadata.CREATED_AT");
            assertThat(generatedSource).contains("CommonFieldMetadata.UPDATED_AT");
            assertThat(generatedSource).contains("CommonFieldMetadata.DELETED");
            assertThat(generatedSource).contains("CommonFieldMetadata.VERSION_LONG");

            // 只应该生成 name 字段的内部类
            assertThat(generatedSource).contains("class NameFieldMetadata");
            // 不应该生成通用字段的内部类
            assertThat(generatedSource).doesNotContain("class TenantIdFieldMetadata");
            assertThat(generatedSource).doesNotContain("class CreatedAtFieldMetadata");
            assertThat(generatedSource).doesNotContain("class UpdatedAtFieldMetadata");
            assertThat(generatedSource).doesNotContain("class DeletedFieldMetadata");
            assertThat(generatedSource).doesNotContain("class VersionFieldMetadata");
        }
    }

    @Nested
    @DisplayName("配置文件注册")
    class ConfigFileRegistrationTests {

        @Test
        @DisplayName("应该从配置文件加载通用字段")
        void shouldLoadCommonFieldsFromConfig() throws IOException {
            String generatedSource = compileAndExtractGeneratedSource(
                "ConfigFieldTestEntity",
                """
                package io.github.afgprojects.framework.apt.entity.test;

                import io.github.afgprojects.framework.apt.entity.AfEntity;

                @AfEntity
                public class ConfigFieldTestEntity {
                    private Long id;
                    private String orgId;  // 从配置文件注册
                }
                """
            );

            // 如果配置文件存在且包含 orgId，应该使用 CommonFieldMetadata.ORG_ID
            // 否则生成内部类
            assertThat(generatedSource).contains("orgId");
        }
    }

    @Nested
    @DisplayName("注解注册")
    class AnnotationRegistrationTests {

        @Test
        @DisplayName("应该从字段上的注解注册通用字段")
        void shouldRegisterFromFieldAnnotation() throws IOException {
            String generatedSource = compileAndExtractGeneratedSource(
                "AnnotationFieldTestEntity",
                """
                package io.github.afgprojects.framework.apt.entity.test;

                import io.github.afgprojects.framework.apt.entity.AfEntity;
                import io.github.afgprojects.framework.apt.entity.CommonFieldDefinition;

                @AfEntity
                public class AnnotationFieldTestEntity {
                    private Long id;

                    @CommonFieldDefinition(name = "BIZ_TYPE", propertyName = "bizType", fieldType = String.class)
                    private String bizType;
                }
                """
            );

            // 注解定义的字段应该被识别，生成对应的字段元数据内部类
            // 因为 CommonFieldMetadata 类中没有 BIZ_TYPE 常量
            assertThat(generatedSource).contains("BizTypeFieldMetadata");
            assertThat(generatedSource).contains("bizType");
        }

        @Test
        @DisplayName("应该从类上的注解注册多个通用字段")
        void shouldRegisterFromClassAnnotation() throws IOException {
            String generatedSource = compileAndExtractGeneratedSource(
                "AnnotationClassTestEntity",
                """
                package io.github.afgprojects.framework.apt.entity.test;

                import io.github.afgprojects.framework.apt.entity.AfEntity;
                import io.github.afgprojects.framework.apt.entity.CommonFieldDefinition;
                import io.github.afgprojects.framework.apt.entity.CommonFieldDefinitions;

                @AfEntity
                @CommonFieldDefinitions({
                    @CommonFieldDefinition(name = "REGION_CODE", propertyName = "regionCode", fieldType = String.class),
                    @CommonFieldDefinition(name = "BIZ_LINE", propertyName = "bizLine", fieldType = String.class)
                })
                public class AnnotationClassTestEntity {
                    private Long id;
                    private String regionCode;
                    private String bizLine;
                }
                """
            );

            // 注解定义的字段应该被识别，生成对应的字段元数据内部类
            assertThat(generatedSource).contains("RegionCodeFieldMetadata");
            assertThat(generatedSource).contains("BizLineFieldMetadata");
        }
    }

    @Nested
    @DisplayName("优先级规则")
    class PriorityTests {

        @Test
        @DisplayName("框架内置字段不可被注解覆盖")
        void frameworkFieldsCannotBeOverridden() throws IOException {
            String generatedSource = compileAndExtractGeneratedSource(
                "OverrideFrameworkTestEntity",
                """
                package io.github.afgprojects.framework.apt.entity.test;

                import io.github.afgprojects.framework.apt.entity.AfEntity;
                import io.github.afgprojects.framework.apt.entity.CommonFieldDefinition;
                import java.time.LocalDateTime;

                @AfEntity
                public class OverrideFrameworkTestEntity {
                    private Long id;

                    // 尝试覆盖框架内置的 createdAt
                    @CommonFieldDefinition(name = "CREATED_AT", propertyName = "createdAt", fieldType = LocalDateTime.class)
                    private LocalDateTime createdAt;
                }
                """
            );

            // 应该使用框架内置的 CREATED_AT，而不是注解定义的
            assertThat(generatedSource).contains("CommonFieldMetadata.CREATED_AT");
            // 不应该生成 CreatedAtFieldMetadata 内部类
            assertThat(generatedSource).doesNotContain("class CreatedAtFieldMetadata");
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