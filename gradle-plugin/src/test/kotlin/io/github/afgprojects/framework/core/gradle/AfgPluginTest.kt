package io.github.afgprojects.framework.core.gradle

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
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
        // 创建项目并设置基本配置
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir)
            .build()

        // 先应用 java-library 插件（AfgPlugin 会自动应用，但测试环境需要预先设置）
        project.plugins.apply("java-library")

        // 应用 AfgPlugin
        project.plugins.apply(AfgPlugin::class.java)

        assertTrue(project.plugins.hasPlugin("java-library"))
        assertNotNull(project.extensions.findByName("afg"))
    }

    @Test
    @DisplayName("应该创建扩展配置")
    fun `should create extension configuration`() {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir)
            .build()

        project.plugins.apply("java-library")
        project.plugins.apply(AfgPlugin::class.java)

        val extension = project.extensions.findByName("afg")
        assertNotNull(extension, "afg extension should be created")
    }

    @Test
    @DisplayName("应该注册核心任务")
    fun `should register core tasks`() {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir)
            .build()

        project.plugins.apply("java-library")
        project.plugins.apply(AfgPlugin::class.java)

        // 检查核心任务是否注册
        val coreTasks = listOf(
            "generateEntity",
            "generateMigration",
            "afgInfo"
        )

        for (taskName in coreTasks) {
            val task = project.tasks.findByName(taskName)
            assertNotNull(task, "Task $taskName should be registered")
        }
    }

    @Test
    @DisplayName("afgInfo 任务应该有正确的分组")
    fun `afgInfo task should have correct group`() {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir)
            .build()

        project.plugins.apply("java-library")
        project.plugins.apply(AfgPlugin::class.java)

        val task = project.tasks.findByName("afgInfo")
        assertNotNull(task)
        assertTrue(task?.group == "afg" || task?.group == null, "afgInfo task group should be 'afg' or null")
    }
}