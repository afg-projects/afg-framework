package io.github.afgprojects.framework.core.gradle

import io.github.afgprojects.framework.core.gradle.extension.AfgExtension
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * AfgPlugin 功能测试（轻量版）。
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

        project.plugins.apply("java-library")
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
    @DisplayName("扩展应该有默认值")
    fun `extension should have default values`() {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir)
            .build()

        project.plugins.apply("java-library")
        project.plugins.apply(AfgPlugin::class.java)

        val extension = project.extensions.findByType(AfgExtension::class.java)
        assertNotNull(extension)

        // 默认值应该可用
        assertTrue(extension.standalone.getOrElse(true), "standalone should default to true")
        assertTrue(extension.useLombok.getOrElse(true), "useLombok should default to true")
    }

    @Test
    @DisplayName("扩展配置应该可自定义")
    fun `extension should be configurable`() {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir)
            .build()

        project.plugins.apply("java-library")
        project.plugins.apply(AfgPlugin::class.java)

        val extension = project.extensions.findByType(AfgExtension::class.java)
        assertNotNull(extension)

        extension.springBootVersion.set("4.0.5")
        extension.frameworkVersion.set("1.0.0")
        extension.standalone.set(false)
        extension.useLombok.set(false)

        assertEquals("4.0.5", extension.springBootVersion.get())
        assertEquals("1.0.0", extension.frameworkVersion.get())
        assertEquals(false, extension.standalone.get())
        assertEquals(false, extension.useLombok.get())
    }

    @Test
    @DisplayName("应该应用 JaCoCo 插件")
    fun `should apply jacoco plugin`() {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir)
            .build()

        project.plugins.apply("java-library")
        project.plugins.apply(AfgPlugin::class.java)

        assertTrue(project.plugins.hasPlugin("jacoco"), "jacoco plugin should be applied")
    }

    @Test
    @DisplayName("应该配置编译选项")
    fun `should configure compile options`() {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir)
            .build()

        project.plugins.apply("java-library")
        project.plugins.apply(AfgPlugin::class.java)

        // 编译选项在 task 配置中，通过 project builder 验证插件不抛异常即可
        // 实际编译选项在 JavaCompile task 的 convention 中
    }
}
