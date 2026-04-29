package io.github.afgprojects.framework.core.gradle

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * AfgPlugin 功能测试
 */
@DisplayName("AfgPlugin 测试")
class AfgPluginTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    @DisplayName("插件应该正确应用")
    fun `plugin should be applied correctly`() {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir)
            .build()

        project.plugins.apply("io.github.afg-projects.plugin")

        assertTrue(project.plugins.hasPlugin("io.github.afg-projects.plugin"))
        assertNotNull(project.extensions.findByName("afg"))
    }

    @Test
    @DisplayName("应该创建默认配置")
    fun `should create default configuration`() {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir)
            .build()

        project.plugins.apply("io.github.afg-projects.plugin")

        val extension = project.extensions.findByName("afg")
        assertNotNull(extension)
    }

    @Test
    @DisplayName("应该注册所有任务")
    fun `should register all tasks`() {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir)
            .build()

        project.plugins.apply("io.github.afg-projects.plugin")

        val expectedTasks = listOf(
            "generateEntity",
            "generateMigration",
            "generateEntityFromDb",
            "dbMigrate",
            "apiDoc",
            "afgInfo",
            "afgInitConfig"
        )

        for (taskName in expectedTasks) {
            assertNotNull(project.tasks.findByName(taskName), "Task $taskName should be registered")
        }
    }

    @Test
    @DisplayName("afgInfo 任务应该显示配置信息")
    fun `afgInfo task should display configuration`() {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir)
            .build()

        project.plugins.apply("io.github.afg-projects.plugin")

        val task = project.tasks.getByName("afgInfo")
        assertNotNull(task)
        assertEquals("afg", task.group)
    }
}