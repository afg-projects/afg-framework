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
 * EnumMetadataProcessor 集成测试。
 * <p>
 * 验证 APT 能正确生成枚举元数据类和索引文件。
 */
@DisplayName("EnumMetadataProcessor Integration Tests")
class EnumMetadataProcessorTest {

    private static List<File> classpathFiles;

    @BeforeAll
    static void setUpClasspath() {
        classpathFiles = getClasspathFiles();
    }

    @Nested
    @DisplayName("枚举元数据类生成")
    class MetadataGenerationTests {

        @Test
        @DisplayName("应该为 @AfgEnum 注解的枚举类生成元数据类")
        void shouldGenerateMetadataClassForAnnotatedEnum() {
            JavaFileObject enumSource = JavaFileObjects.forSourceString(
                "io.github.afgprojects.framework.apt.entity.test.SimpleTestEnum",
                """
                package io.github.afgprojects.framework.apt.entity.test;

                import io.github.afgprojects.framework.apt.entity.AfgEnum;

                @AfgEnum
                public enum SimpleTestEnum {
                    ACTIVE,
                    INACTIVE
                }
                """
            );

            var compilation = javac()
                .withProcessors(new EnumMetadataProcessor())
                .withClasspath(classpathFiles)
                .compile(enumSource);

            assertThat(compilation.errors()).isEmpty();
        }

        @Test
        @DisplayName("生成的元数据类应该实现 EnumMetadata 接口")
        void shouldImplementEnumMetadataInterface() throws IOException {
            String generatedSource = compileAndExtractGeneratedSource(
                "InterfaceTestEnum",
                """
                package io.github.afgprojects.framework.apt.entity.test;

                import io.github.afgprojects.framework.apt.entity.AfgEnum;

                @AfgEnum
                public enum InterfaceTestEnum {
                    ACTIVE,
                    INACTIVE
                }
                """
            );

            assertThat(generatedSource).contains("implements EnumMetadata<InterfaceTestEnum>");
        }

        @Test
        @DisplayName("应该生成所有必需的接口方法")
        void shouldGenerateAllRequiredMethods() throws IOException {
            String generatedSource = compileAndExtractGeneratedSource(
                "MethodsTestEnum",
                """
                package io.github.afgprojects.framework.apt.entity.test;

                import io.github.afgprojects.framework.apt.entity.AfgEnum;

                @AfgEnum
                public enum MethodsTestEnum {
                    ACTIVE,
                    INACTIVE
                }
                """
            );

            assertThat(generatedSource).contains("enumType()");
            assertThat(generatedSource).contains("valueField()");
            assertThat(generatedSource).contains("labelField()");
            assertThat(generatedSource).contains("i18nPrefix()");
            assertThat(generatedSource).contains("values()");
        }
    }

    @Nested
    @DisplayName("属性提取")
    class AttributeExtractionTests {

        @Test
        @DisplayName("应该使用默认 valueField 和 labelField")
        void shouldUseDefaultFieldNames() throws IOException {
            String generatedSource = compileAndExtractGeneratedSource(
                "DefaultFieldsTestEnum",
                """
                package io.github.afgprojects.framework.apt.entity.test;

                import io.github.afgprojects.framework.apt.entity.AfgEnum;

                @AfgEnum
                public enum DefaultFieldsTestEnum {
                    ACTIVE,
                    INACTIVE
                }
                """
            );

            assertThat(generatedSource).contains("return \"value\"");
            assertThat(generatedSource).contains("return \"label\"");
        }

        @Test
        @DisplayName("应该提取自定义 valueField 和 labelField")
        void shouldExtractCustomFieldNames() throws IOException {
            String generatedSource = compileAndExtractGeneratedSource(
                "CustomFieldsTestEnum",
                """
                package io.github.afgprojects.framework.apt.entity.test;

                import io.github.afgprojects.framework.apt.entity.AfgEnum;

                @AfgEnum(valueField = "code", labelField = "description")
                public enum CustomFieldsTestEnum {
                    ACTIVE,
                    INACTIVE
                }
                """
            );

            assertThat(generatedSource).contains("return \"code\"");
            assertThat(generatedSource).contains("return \"description\"");
        }

        @Test
        @DisplayName("应该提取 i18nPrefix")
        void shouldExtractI18nPrefix() throws IOException {
            String generatedSource = compileAndExtractGeneratedSource(
                "I18nTestEnum",
                """
                package io.github.afgprojects.framework.apt.entity.test;

                import io.github.afgprojects.framework.apt.entity.AfgEnum;

                @AfgEnum(i18nPrefix = "enum.user-status")
                public enum I18nTestEnum {
                    ACTIVE,
                    INACTIVE
                }
                """
            );

            assertThat(generatedSource).contains("return \"enum.user-status\"");
        }
    }

    @Nested
    @DisplayName("编译期校验")
    class ValidationTests {

        @Test
        @DisplayName("@AfgEnum 标注非枚举类应该编译失败")
        void shouldFailWhenAnnotatingNonEnumClass() {
            JavaFileObject nonEnumSource = JavaFileObjects.forSourceString(
                "io.github.afgprojects.framework.apt.entity.test.NonEnumClass",
                """
                package io.github.afgprojects.framework.apt.entity.test;

                import io.github.afgprojects.framework.apt.entity.AfgEnum;

                @AfgEnum
                public class NonEnumClass {
                }
                """
            );

            var compilation = javac()
                .withProcessors(new EnumMetadataProcessor())
                .withClasspath(classpathFiles)
                .compile(nonEnumSource);

            assertThat(compilation.errors()).isNotEmpty();
            assertThat(compilation.diagnostics().stream()
                .anyMatch(d -> d.getMessage(null).contains("@AfgEnum 只能标注枚举类")))
                .isTrue();
        }
    }

    @Nested
    @DisplayName("索引文件生成")
    class IndexGenerationTests {

        @Test
        @DisplayName("应该生成 META-INF/afg/enum-metadata.index")
        void shouldGenerateEnumMetadataIndex() throws IOException {
            JavaFileObject enumSource = JavaFileObjects.forSourceString(
                "io.github.afgprojects.framework.apt.entity.test.IndexTestEnum",
                """
                package io.github.afgprojects.framework.apt.entity.test;

                import io.github.afgprojects.framework.apt.entity.AfgEnum;

                @AfgEnum
                public enum IndexTestEnum {
                    ACTIVE,
                    INACTIVE
                }
                """
            );

            var compilation = javac()
                .withProcessors(new EnumMetadataProcessor())
                .withClasspath(classpathFiles)
                .compile(enumSource);

            assertThat(compilation.errors()).isEmpty();

            // 验证索引文件生成
            var indexFiles = compilation.generatedFiles()
                .stream()
                .filter(f -> f.getName().contains("enum-metadata.index"))
                .toList();

            assertThat(indexFiles).isNotEmpty();

            // 验证索引文件包含生成的类名
            String indexContent = indexFiles.get(0).getCharContent(false).toString();
            assertThat(indexContent).contains("io.github.afgprojects.framework.apt.entity.test.metadata.IndexTestEnumEnumMetadata");
        }
    }

    /**
     * 编译测试源码并提取生成的元数据类源码
     */
    private String compileAndExtractGeneratedSource(String className, String sourceCode) throws IOException {
        JavaFileObject enumSource = JavaFileObjects.forSourceString(
            "io.github.afgprojects.framework.apt.entity.test." + className,
            sourceCode
        );

        var compilation = javac()
            .withProcessors(new EnumMetadataProcessor())
            .withClasspath(classpathFiles)
            .compile(enumSource);

        assertThat(compilation.errors())
            .as("Compilation should succeed, but got errors: " + compilation.diagnostics())
            .isEmpty();

        // 查找生成的元数据类
        JavaFileObject generatedFile = compilation.generatedSourceFiles()
            .stream()
            .filter(f -> f.getName().contains("EnumMetadata"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No EnumMetadata class generated"));

        return generatedFile.getCharContent(false).toString();
    }

    /**
     * 获取当前 classpath 文件列表
     */
    private static List<File> getClasspathFiles() {
        List<File> files = new ArrayList<>();

        String classpath = System.getProperty("java.class.path");
        if (classpath != null) {
            for (String path : classpath.split(File.pathSeparator)) {
                File file = new File(path);
                if (file.exists() && !files.contains(file)) {
                    files.add(file);
                }
            }
        }

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader instanceof URLClassLoader urlClassLoader) {
            for (URL url : urlClassLoader.getURLs()) {
                File file = new File(url.getFile());
                if (file.exists() && !files.contains(file)) {
                    files.add(file);
                }
            }
        }

        return files;
    }
}
