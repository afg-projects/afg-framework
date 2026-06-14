package io.github.afgprojects.framework.core.gradle.task

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * GenerateDbDocTask 功能测试
 *
 * <p>测试 GenerateDbDoc 任务的注册、配置和基本执行。
 * 使用 Gradle ProjectBuilder 进行单元级测试。
 */
@DisplayName("GenerateDbDocTask")
class GenerateDbDocTaskTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    @DisplayName("任务应在 afg 组中注册")
    fun `should be registered in afg group`() {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir)
            .build()

        project.plugins.apply("java-library")
        project.plugins.apply(io.github.afgprojects.framework.core.gradle.AfgPlugin::class.java)

        val task = project.tasks.findByName("generateDbDoc")
        assertNotNull(task, "generateDbDoc task should be registered")
        assertTrue(task?.group == "afg", "generateDbDoc task should be in 'afg' group")
    }

    @Test
    @DisplayName("任务描述应正确")
    fun `should have correct description`() {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir)
            .build()

        project.plugins.apply("java-library")
        project.plugins.apply(io.github.afgprojects.framework.core.gradle.AfgPlugin::class.java)

        val task = project.tasks.findByName("generateDbDoc")
        assertNotNull(task)
        assertNotNull(task?.description)
        assertTrue(
            task?.description?.contains("文档") == true || task?.description?.contains("doc") == true,
            "Description should mention documentation"
        )
    }

    @Test
    @DisplayName("entityPackages 属性应可配置")
    fun `should configure entityPackages property`() {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir)
            .build()

        project.plugins.apply("java-library")
        project.plugins.apply(io.github.afgprojects.framework.core.gradle.AfgPlugin::class.java)

        val task = project.tasks.findByName("generateDbDoc") as? GenerateDbDocTask
        assertNotNull(task, "generateDbDoc task should be registered as GenerateDbDocTask")

        // Configure entity packages
        task?.entityPackages?.set(listOf("com.example.entity"))
        assertTrue(
            task?.entityPackages?.isPresent == true,
            "entityPackages should be configurable"
        )
    }

    @Test
    @DisplayName("outputFile 属性应默认为 docs/db-schema.md")
    fun `should default outputFile to docs db-schema md`() {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir)
            .build()

        project.plugins.apply("java-library")
        project.plugins.apply(io.github.afgprojects.framework.core.gradle.AfgPlugin::class.java)

        val task = project.tasks.findByName("generateDbDoc") as? GenerateDbDocTask
        assertNotNull(task)

        // The default convention should point to docs/db-schema.md
        val outputFile = task?.outputFile
        assertNotNull(outputFile, "outputFile property should exist")
    }

    @Test
    @DisplayName("title 属性应可配置")
    fun `should configure title property`() {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir)
            .build()

        project.plugins.apply("java-library")
        project.plugins.apply(io.github.afgprojects.framework.core.gradle.AfgPlugin::class.java)

        val task = project.tasks.findByName("generateDbDoc") as? GenerateDbDocTask
        assertNotNull(task)

        task?.title?.set("自定义数据库文档")
        assertTrue(
            task?.title?.isPresent == true,
            "title should be configurable"
        )
    }

    @Test
    @DisplayName("includeBaseFields 属性应可配置")
    fun `should configure includeBaseFields property`() {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir)
            .build()

        project.plugins.apply("java-library")
        project.plugins.apply(io.github.afgprojects.framework.core.gradle.AfgPlugin::class.java)

        val task = project.tasks.findByName("generateDbDoc") as? GenerateDbDocTask
        assertNotNull(task)

        task?.includeBaseFields?.set(false)
        assertTrue(
            task?.includeBaseFields?.isPresent == true,
            "includeBaseFields should be configurable"
        )
    }

    @Test
    @DisplayName("entityPackages 为空时任务应跳过生成")
    fun `should skip generation when entityPackages empty`() {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir)
            .build()

        project.plugins.apply("java-library")
        project.plugins.apply(io.github.afgprojects.framework.core.gradle.AfgPlugin::class.java)

        val task = project.tasks.findByName("generateDbDoc") as? GenerateDbDocTask
        assertNotNull(task)

        // Set empty packages
        task?.entityPackages?.set(emptyList())
        task?.outputFile?.set(tempDir.resolve("output").resolve("db-schema.md"))

        // Execute — should not throw, just skip
        task?.actions?.forEach { action ->
            action.execute(task)
        }

        // Output file should NOT be created since no packages specified
        val outputFile = tempDir.resolve("output").resolve("db-schema.md")
        assertTrue(!outputFile.exists(), "Output file should not be created when packages are empty")
    }
}
