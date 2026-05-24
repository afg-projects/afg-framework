package io.github.afgprojects.framework.apt.service;

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
 * ServiceMetadataProcessor integration tests.
 * <p>
 * Verifies that the APT processor correctly generates ServiceMetadata implementation classes
 * from @AfService-annotated classes with @AfOperation methods.
 */
@DisplayName("ServiceMetadataProcessor Integration Tests")
class ServiceMetadataProcessorTest {

    private static List<File> classpathFiles;

    @BeforeAll
    static void setUpClasspath() {
        classpathFiles = getClasspathFiles();
    }

    @Nested
    @DisplayName("ServiceMetadata class generation")
    class MetadataGenerationTests {

        @Test
        @DisplayName("Should generate ServiceMetadata class for a simple @AfService with @AfOperation methods")
        void shouldGenerateMetadataClassForSimpleService() throws IOException {
            String generatedSource = compileAndExtractGeneratedSource(
                "SimpleTestService",
                """
                package io.github.afgprojects.framework.apt.service.test;

                import io.github.afgprojects.framework.apt.api.AfService;
                import io.github.afgprojects.framework.apt.api.AfOperation;
                import io.github.afgprojects.framework.apt.api.AfParam;

                @AfService(name = "userService", description = "User management service")
                public class SimpleTestService {

                    @AfOperation(name = "createUser", description = "Create a new user")
                    public String createUser(@AfParam(name = "username", description = "The username") String username) {
                        return "Created: " + username;
                    }

                    @AfOperation(name = "deleteUser", description = "Delete a user")
                    public void deleteUser(@AfParam(name = "userId", description = "The user ID") Long userId) {
                    }
                }
                """
            );

            assertThat(generatedSource).contains("implements ServiceMetadata<SimpleTestService>");
            assertThat(generatedSource).contains("SimpleTestServiceServiceMetadata");
            assertThat(generatedSource).contains("serviceName()");
            assertThat(generatedSource).contains("description()");
            assertThat(generatedSource).contains("serviceType()");
            assertThat(generatedSource).contains("operations()");
        }

        @Test
        @DisplayName("Should default service name to decapitalized class name when not specified")
        void shouldDefaultServiceNameToDecapitalizedClassName() throws IOException {
            String generatedSource = compileAndExtractGeneratedSource(
                "DefaultNameService",
                """
                package io.github.afgprojects.framework.apt.service.test;

                import io.github.afgprojects.framework.apt.api.AfService;
                import io.github.afgprojects.framework.apt.api.AfOperation;

                @AfService
                public class DefaultNameService {
                    @AfOperation
                    public void doSomething() {}
                }
                """
            );

            assertThat(generatedSource).contains("defaultNameService");
        }

        @Test
        @DisplayName("Should extract all @AfOperation method metadata")
        void shouldExtractOperationMetadata() throws IOException {
            String generatedSource = compileAndExtractGeneratedSource(
                "OperationTestService",
                """
                package io.github.afgprojects.framework.apt.service.test;

                import io.github.afgprojects.framework.apt.api.AfService;
                import io.github.afgprojects.framework.apt.api.AfOperation;

                @AfService
                public class OperationTestService {

                    @AfOperation(name = "findUser", description = "Find a user by ID", permission = "user:read", audit = true, tenantScope = true, dataScope = true)
                    public String findUser(Long id) {
                        return "user";
                    }
                }
                """
            );

            assertThat(generatedSource).contains("findUser");
            assertThat(generatedSource).contains("user:read");
            assertThat(generatedSource).contains("SimpleOperationMetadata");
            assertThat(generatedSource).contains("MethodKey");
        }

        @Test
        @DisplayName("Should extract @AfParam metadata from method parameters")
        void shouldExtractParamMetadata() throws IOException {
            String generatedSource = compileAndExtractGeneratedSource(
                "ParamTestService",
                """
                package io.github.afgprojects.framework.apt.service.test;

                import io.github.afgprojects.framework.apt.api.AfService;
                import io.github.afgprojects.framework.apt.api.AfOperation;
                import io.github.afgprojects.framework.apt.api.AfParam;

                @AfService
                public class ParamTestService {

                    @AfOperation
                    public String greet(@AfParam(name = "name", description = "The name to greet", required = true) String name,
                                        @AfParam(name = "times", description = "Number of greetings", defaultValue = "1") int times) {
                        return "Hello";
                    }
                }
                """
            );

            assertThat(generatedSource).contains("SimpleParameterMetadata");
            assertThat(generatedSource).contains("name");
            assertThat(generatedSource).contains("The name to greet");
            assertThat(generatedSource).contains("times");
            assertThat(generatedSource).contains("1");
        }

        @Test
        @DisplayName("Should extract @AfResult metadata from method")
        void shouldExtractResultMetadata() throws IOException {
            String generatedSource = compileAndExtractGeneratedSource(
                "ResultTestService",
                """
                package io.github.afgprojects.framework.apt.service.test;

                import io.github.afgprojects.framework.apt.api.AfService;
                import io.github.afgprojects.framework.apt.api.AfOperation;
                import io.github.afgprojects.framework.apt.api.AfResult;

                @AfService
                public class ResultTestService {

                    @AfOperation(name = "listUsers")
                    @AfResult(description = "List of users", paged = true)
                    public String listUsers() {
                        return "users";
                    }
                }
                """
            );

            assertThat(generatedSource).contains("List of users");
            // paged=true is the last argument in SimpleOperationMetadata constructor
            // The generated code has the boolean value true as the last constructor arg
            assertThat(generatedSource).contains("SimpleOperationMetadata");
            assertThat(generatedSource).contains("MethodKey");
        }

        @Test
        @DisplayName("Should handle @AfService with category and tags")
        void shouldHandleCategoryAndTags() throws IOException {
            String generatedSource = compileAndExtractGeneratedSource(
                "CategoryTestService",
                """
                package io.github.afgprojects.framework.apt.service.test;

                import io.github.afgprojects.framework.apt.api.AfService;
                import io.github.afgprojects.framework.apt.api.AfOperation;

                @AfService(name = "roleService", description = "Role management", category = "system", tags = {"auth", "rbac"})
                public class CategoryTestService {
                    @AfOperation
                    public void listRoles() {}
                }
                """
            );

            assertThat(generatedSource).contains("roleService");
            assertThat(generatedSource).contains("system");
            assertThat(generatedSource).contains("auth");
            assertThat(generatedSource).contains("rbac");
        }

        @Test
        @DisplayName("Should handle @AfOperation with requiredRoles")
        void shouldHandleRequiredRoles() throws IOException {
            String generatedSource = compileAndExtractGeneratedSource(
                "RolesTestService",
                """
                package io.github.afgprojects.framework.apt.service.test;

                import io.github.afgprojects.framework.apt.api.AfService;
                import io.github.afgprojects.framework.apt.api.AfOperation;

                @AfService
                public class RolesTestService {
                    @AfOperation(name = "adminOp", requiredRoles = {"ADMIN", "SUPERADMIN"})
                    public void adminOp() {}
                }
                """
            );

            assertThat(generatedSource).contains("ADMIN");
            assertThat(generatedSource).contains("SUPERADMIN");
        }

        @Test
        @DisplayName("Should handle @AfOperation with async and deprecated flags")
        void shouldHandleAsyncAndDeprecated() throws IOException {
            String generatedSource = compileAndExtractGeneratedSource(
                "AsyncTestService",
                """
                package io.github.afgprojects.framework.apt.service.test;

                import io.github.afgprojects.framework.apt.api.AfService;
                import io.github.afgprojects.framework.apt.api.AfOperation;

                @AfService
                public class AsyncTestService {
                    @AfOperation(name = "asyncOp", async = true, deprecated = true)
                    public void asyncOp() {}
                }
                """
            );

            assertThat(generatedSource).contains("true");
        }

        @Test
        @DisplayName("Should handle @AfParam with enumValues")
        void shouldHandleEnumValues() throws IOException {
            String generatedSource = compileAndExtractGeneratedSource(
                "EnumTestService",
                """
                package io.github.afgprojects.framework.apt.service.test;

                import io.github.afgprojects.framework.apt.api.AfService;
                import io.github.afgprojects.framework.apt.api.AfOperation;
                import io.github.afgprojects.framework.apt.api.AfParam;

                @AfService
                public class EnumTestService {
                    @AfOperation
                    public void setStatus(@AfParam(name = "status", enumValues = {"ACTIVE", "INACTIVE", "DELETED"}) String status) {}
                }
                """
            );

            assertThat(generatedSource).contains("ACTIVE");
            assertThat(generatedSource).contains("INACTIVE");
            assertThat(generatedSource).contains("DELETED");
        }
    }

    @Nested
    @DisplayName("Compile-time validation")
    class ValidationTests {

        @Test
        @DisplayName("Should fail compilation on duplicate @AfOperation names")
        void shouldFailOnDuplicateOperationNames() {
            JavaFileObject serviceSource = JavaFileObjects.forSourceString(
                "io.github.afgprojects.framework.apt.service.test.DuplicateOpService",
                """
                package io.github.afgprojects.framework.apt.service.test;

                import io.github.afgprojects.framework.apt.api.AfService;
                import io.github.afgprojects.framework.apt.api.AfOperation;

                @AfService
                public class DuplicateOpService {
                    @AfOperation(name = "doWork")
                    public void doWork1() {}

                    @AfOperation(name = "doWork")
                    public void doWork2() {}
                }
                """
            );

            var compilation = javac()
                .withProcessors(new ServiceMetadataProcessor())
                .withClasspath(classpathFiles)
                .compile(serviceSource);

            assertThat(compilation.errors()).isNotEmpty();
            assertThat(compilation.diagnostics().stream()
                .anyMatch(d -> d.getMessage(null).contains("Duplicate @AfOperation name 'doWork'")))
                .isTrue();
        }
    }

    @Nested
    @DisplayName("Index file generation")
    class IndexGenerationTests {

        @Test
        @DisplayName("Should generate META-INF/afg/service-metadata.index")
        void shouldGenerateServiceMetadataIndex() throws IOException {
            JavaFileObject serviceSource = JavaFileObjects.forSourceString(
                "io.github.afgprojects.framework.apt.service.test.IndexTestService",
                """
                package io.github.afgprojects.framework.apt.service.test;

                import io.github.afgprojects.framework.apt.api.AfService;
                import io.github.afgprojects.framework.apt.api.AfOperation;

                @AfService(name = "indexTestService")
                public class IndexTestService {
                    @AfOperation
                    public void testOp() {}
                }
                """
            );

            var compilation = javac()
                .withProcessors(new ServiceMetadataProcessor())
                .withClasspath(classpathFiles)
                .compile(serviceSource);

            assertThat(compilation.errors()).isEmpty();

            // Verify the index file was generated
            var indexFiles = compilation.generatedFiles()
                .stream()
                .filter(f -> f.getName().contains("service-metadata.index"))
                .toList();

            assertThat(indexFiles).isNotEmpty();

            // Verify the index file contains the generated class name
            String indexContent = indexFiles.get(0).getCharContent(false).toString();
            assertThat(indexContent).contains("io.github.afgprojects.framework.apt.service.test.metadata.IndexTestServiceServiceMetadata");
        }
    }

    /**
     * Compile test source and extract the generated metadata class source code.
     */
    private String compileAndExtractGeneratedSource(String className, String sourceCode) throws IOException {
        JavaFileObject serviceSource = JavaFileObjects.forSourceString(
            "io.github.afgprojects.framework.apt.service.test." + className,
            sourceCode
        );

        var compilation = javac()
            .withProcessors(new ServiceMetadataProcessor())
            .withClasspath(classpathFiles)
            .compile(serviceSource);

        assertThat(compilation.errors())
            .as("Compilation should succeed, but got errors: " + compilation.diagnostics())
            .isEmpty();

        // Find the generated metadata class
        JavaFileObject generatedFile = compilation.generatedSourceFiles()
            .stream()
            .filter(f -> f.getName().contains("ServiceMetadata"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No ServiceMetadata class generated"));

        return generatedFile.getCharContent(false).toString();
    }

    /**
     * Get current classpath file list for compile-testing.
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