package io.github.afgprojects.framework.core.gradle.service

import io.github.afgprojects.framework.core.gradle.service.impl.DefaultProjectInitService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * ProjectInitService 测试
 */
@DisplayName("ProjectInitService 测试")
class ProjectInitServiceTest {

    @TempDir
    lateinit var tempDir: Path

    private val service = DefaultProjectInitService()

    @Test
    @DisplayName("应该成功创建项目")
    fun `should create project successfully`() {
        val options = ProjectInitOptions(
            projectType = ProjectType.DATA,
            basePackage = "com.example.test",
            javaVersion = 21,
            databaseType = DatabaseType.H2
        )

        val result = service.init("test-project", tempDir, options)

        assertTrue(result.success)
        assertTrue(result.generatedFiles.isNotEmpty())
    }

    @Test
    @DisplayName("应该生成 build.gradle.kts")
    fun `should generate build gradle kts`() {
        val options = ProjectInitOptions(
            projectType = ProjectType.DATA,
            basePackage = "com.example.test",
            buildTool = BuildTool.GRADLE_KOTLIN
        )

        val result = service.init("test-project", tempDir, options)

        assertTrue(result.success)

        val buildFile = tempDir.resolve("test-project/build.gradle.kts")
        assertTrue(buildFile.exists())

        val content = buildFile.readText()
        assertTrue(content.contains("plugins"))
        assertTrue(content.contains("com.example.test"))
    }

    @Test
    @DisplayName("应该生成 Application 主类")
    fun `should generate application class`() {
        val options = ProjectInitOptions(
            projectType = ProjectType.DATA,
            basePackage = "com.example.test"
        )

        val result = service.init("test-project", tempDir, options)

        assertTrue(result.success)

        val appFile = tempDir.resolve("test-project/src/main/java/com/example/test/Application.java")
        assertTrue(appFile.exists())

        val content = appFile.readText()
        assertTrue(content.contains("@SpringBootApplication"))
        assertTrue(content.contains("package com.example.test"))
    }

    @Test
    @DisplayName("应该生成 application.yml 配置")
    fun `should generate application yml`() {
        val options = ProjectInitOptions(
            projectType = ProjectType.DATA,
            basePackage = "com.example.test",
            databaseType = DatabaseType.MYSQL
        )

        val result = service.init("test-project", tempDir, options)

        assertTrue(result.success)

        val configFile = tempDir.resolve("test-project/src/main/resources/application.yml")
        assertTrue(configFile.exists())

        val content = configFile.readText()
        assertTrue(content.contains("spring:"))
        assertTrue(content.contains("datasource:"))
    }

    @Test
    @DisplayName("应该根据数据库类型配置正确的驱动")
    fun `should configure correct driver for database type`() {
        val options = ProjectInitOptions(
            projectType = ProjectType.DATA,
            databaseType = DatabaseType.MYSQL
        )

        val result = service.init("test-project", tempDir, options)

        assertTrue(result.success)

        val buildFile = tempDir.resolve("test-project/build.gradle.kts")
        val content = buildFile.readText()
        assertTrue(content.contains("mysql-connector-j"))
    }

    @Test
    @DisplayName("应该处理已存在的目录")
    fun `should handle existing directory`() {
        // 先创建一个项目
        val options = ProjectInitOptions()
        service.init("test-project", tempDir, options)

        // 再次尝试创建同名项目
        val result = service.init("test-project", tempDir, options)

        assertTrue(!result.success)
        assertTrue(result.message.contains("已存在"))
    }
}