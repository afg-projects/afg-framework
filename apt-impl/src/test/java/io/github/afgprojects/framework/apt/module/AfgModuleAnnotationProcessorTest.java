package io.github.afgprojects.framework.apt.module;

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
 * AfgModuleAnnotationProcessor 集成测试。
 * <p>
 * 验证 APT 能正确生成模块索引文件和提取模块元数据。
 */
@DisplayName("AfgModuleAnnotationProcessor Integration Tests")
class AfgModuleAnnotationProcessorTest {

    private static List<File> classpathFiles;

    @BeforeAll
    static void setUpClasspath() {
        classpathFiles = getClasspathFiles();
    }

    @Nested
    @DisplayName("索引文件生成")
    class IndexGenerationTests {

        @Test
        @DisplayName("应该生成 META-INF/afg-modules.index 索引文件")
        void shouldGenerateModuleIndexFile() {
            JavaFileObject moduleSource = JavaFileObjects.forSourceString(
                "io.github.afgprojects.framework.apt.module.test.TestModuleConfig",
                """
                package io.github.afgprojects.framework.apt.module.test;

                import io.github.afgprojects.framework.apt.module.AfgModuleAnnotation;

                @AfgModuleAnnotation(id = "test", name = "测试模块")
                public class TestModuleConfig {
                }
                """
            );

            var compilation = javac()
                .withProcessors(new AfgModuleAnnotationProcessor())
                .withClasspath(classpathFiles)
                .compile(moduleSource);

            assertThat(compilation.errors()).isEmpty();

            // 验证索引文件生成
            var indexFiles = compilation.generatedFiles()
                .stream()
                .filter(f -> f.getName().contains("afg-modules.index"))
                .toList();

            assertThat(indexFiles).isNotEmpty();
        }

        @Test
        @DisplayName("索引文件应该包含模块配置类全限定名")
        void shouldIncludeModuleConfigClassNameInIndex() throws IOException {
            JavaFileObject moduleSource = JavaFileObjects.forSourceString(
                "io.github.afgprojects.framework.apt.module.test.IndexContentModuleConfig",
                """
                package io.github.afgprojects.framework.apt.module.test;

                import io.github.afgprojects.framework.apt.module.AfgModuleAnnotation;

                @AfgModuleAnnotation(id = "indexContent", name = "索引内容测试模块")
                public class IndexContentModuleConfig {
                }
                """
            );

            var compilation = javac()
                .withProcessors(new AfgModuleAnnotationProcessor())
                .withClasspath(classpathFiles)
                .compile(moduleSource);

            assertThat(compilation.errors()).isEmpty();

            var indexFiles = compilation.generatedFiles()
                .stream()
                .filter(f -> f.getName().contains("afg-modules.index"))
                .toList();

            assertThat(indexFiles).isNotEmpty();

            String indexContent = indexFiles.get(0).getCharContent(false).toString();
            assertThat(indexContent).contains("io.github.afgprojects.framework.apt.module.test.IndexContentModuleConfig");
        }
    }

    @Nested
    @DisplayName("basePackage 提取")
    class BasePackageExtractionTests {

        @Test
        @DisplayName("应该使用注解类所在的包名作为默认 basePackage")
        void shouldUseClassPackageAsDefaultBasePackage() throws IOException {
            JavaFileObject moduleSource = JavaFileObjects.forSourceString(
                "io.github.afgprojects.framework.apt.module.test.DefaultPkgModuleConfig",
                """
                package io.github.afgprojects.framework.apt.module.test;

                import io.github.afgprojects.framework.apt.module.AfgModuleAnnotation;

                @AfgModuleAnnotation(id = "defaultPkg")
                public class DefaultPkgModuleConfig {
                }
                """
            );

            var compilation = javac()
                .withProcessors(new AfgModuleAnnotationProcessor())
                .withClasspath(classpathFiles)
                .compile(moduleSource);

            assertThat(compilation.errors()).isEmpty();

            var indexFiles = compilation.generatedFiles()
                .stream()
                .filter(f -> f.getName().contains("afg-modules.index"))
                .toList();

            String indexContent = indexFiles.get(0).getCharContent(false).toString();
            assertThat(indexContent).contains("io.github.afgprojects.framework.apt.module.test");
        }

        @Test
        @DisplayName("应该使用显式指定的 basePackage")
        void shouldUseExplicitBasePackage() throws IOException {
            JavaFileObject moduleSource = JavaFileObjects.forSourceString(
                "io.github.afgprojects.framework.apt.module.test.ExplicitPkgModuleConfig",
                """
                package io.github.afgprojects.framework.apt.module.test;

                import io.github.afgprojects.framework.apt.module.AfgModuleAnnotation;

                @AfgModuleAnnotation(id = "explicitPkg", basePackage = "com.example.auth")
                public class ExplicitPkgModuleConfig {
                }
                """
            );

            var compilation = javac()
                .withProcessors(new AfgModuleAnnotationProcessor())
                .withClasspath(classpathFiles)
                .compile(moduleSource);

            assertThat(compilation.errors()).isEmpty();

            var indexFiles = compilation.generatedFiles()
                .stream()
                .filter(f -> f.getName().contains("afg-modules.index"))
                .toList();

            String indexContent = indexFiles.get(0).getCharContent(false).toString();
            assertThat(indexContent).contains("com.example.auth");
        }
    }

    @Nested
    @DisplayName("contextPath 默认值")
    class ContextPathTests {

        @Test
        @DisplayName("应该使用 /{moduleId}-api 作为默认 contextPath")
        void shouldUseModuleIdApiAsDefaultContextPath() throws IOException {
            JavaFileObject moduleSource = JavaFileObjects.forSourceString(
                "io.github.afgprojects.framework.apt.module.test.DefaultContextPathModuleConfig",
                """
                package io.github.afgprojects.framework.apt.module.test;

                import io.github.afgprojects.framework.apt.module.AfgModuleAnnotation;

                @AfgModuleAnnotation(id = "auth")
                public class DefaultContextPathModuleConfig {
                }
                """
            );

            var compilation = javac()
                .withProcessors(new AfgModuleAnnotationProcessor())
                .withClasspath(classpathFiles)
                .compile(moduleSource);

            assertThat(compilation.errors()).isEmpty();

            var indexFiles = compilation.generatedFiles()
                .stream()
                .filter(f -> f.getName().contains("afg-modules.index"))
                .toList();

            String indexContent = indexFiles.get(0).getCharContent(false).toString();
            assertThat(indexContent).contains("/auth-api");
        }

        @Test
        @DisplayName("应该使用显式指定的 contextPath")
        void shouldUseExplicitContextPath() throws IOException {
            JavaFileObject moduleSource = JavaFileObjects.forSourceString(
                "io.github.afgprojects.framework.apt.module.test.ExplicitContextPathModuleConfig",
                """
                package io.github.afgprojects.framework.apt.module.test;

                import io.github.afgprojects.framework.apt.module.AfgModuleAnnotation;

                @AfgModuleAnnotation(id = "auth", contextPath = "/custom-api")
                public class ExplicitContextPathModuleConfig {
                }
                """
            );

            var compilation = javac()
                .withProcessors(new AfgModuleAnnotationProcessor())
                .withClasspath(classpathFiles)
                .compile(moduleSource);

            assertThat(compilation.errors()).isEmpty();

            var indexFiles = compilation.generatedFiles()
                .stream()
                .filter(f -> f.getName().contains("afg-modules.index"))
                .toList();

            String indexContent = indexFiles.get(0).getCharContent(false).toString();
            assertThat(indexContent).contains("/custom-api");
        }
    }

    @Nested
    @DisplayName("完整属性提取")
    class FullAttributeExtractionTests {

        @Test
        @DisplayName("应该正确提取所有模块属性")
        void shouldExtractAllModuleAttributes() throws IOException {
            JavaFileObject moduleSource = JavaFileObjects.forSourceString(
                "io.github.afgprojects.framework.apt.module.test.FullAttrModuleConfig",
                """
                package io.github.afgprojects.framework.apt.module.test;

                import io.github.afgprojects.framework.apt.module.AfgModuleAnnotation;

                @AfgModuleAnnotation(
                    id = "full",
                    name = "完整属性测试模块",
                    contextPath = "/full-api",
                    basePackage = "com.example.full",
                    dependencies = {"core", "data"},
                    version = "2.0.0",
                    description = "完整属性测试",
                    configFile = "module-full.yml"
                )
                public class FullAttrModuleConfig {
                }
                """
            );

            var compilation = javac()
                .withProcessors(new AfgModuleAnnotationProcessor())
                .withClasspath(classpathFiles)
                .compile(moduleSource);

            assertThat(compilation.errors()).isEmpty();

            var indexFiles = compilation.generatedFiles()
                .stream()
                .filter(f -> f.getName().contains("afg-modules.index"))
                .toList();

            String indexContent = indexFiles.get(0).getCharContent(false).toString();
            assertThat(indexContent).contains("full");
            assertThat(indexContent).contains("module-full.yml");
            assertThat(indexContent).contains("/full-api");
            assertThat(indexContent).contains("com.example.full");
            assertThat(indexContent).contains("io.github.afgprojects.framework.apt.module.test.FullAttrModuleConfig");
        }

        @Test
        @DisplayName("应该使用默认 configFile 当未指定时")
        void shouldUseDefaultConfigFileWhenNotSpecified() throws IOException {
            JavaFileObject moduleSource = JavaFileObjects.forSourceString(
                "io.github.afgprojects.framework.apt.module.test.DefaultConfigModuleConfig",
                """
                package io.github.afgprojects.framework.apt.module.test;

                import io.github.afgprojects.framework.apt.module.AfgModuleAnnotation;

                @AfgModuleAnnotation(id = "myModule")
                public class DefaultConfigModuleConfig {
                }
                """
            );

            var compilation = javac()
                .withProcessors(new AfgModuleAnnotationProcessor())
                .withClasspath(classpathFiles)
                .compile(moduleSource);

            assertThat(compilation.errors()).isEmpty();

            var indexFiles = compilation.generatedFiles()
                .stream()
                .filter(f -> f.getName().contains("afg-modules.index"))
                .toList();

            String indexContent = indexFiles.get(0).getCharContent(false).toString();
            assertThat(indexContent).contains("module-myModule.yml");
        }
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
