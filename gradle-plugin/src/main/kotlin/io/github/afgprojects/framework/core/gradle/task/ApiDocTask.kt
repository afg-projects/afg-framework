package io.github.afgprojects.framework.core.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * API 文档生成任务
 */
@DisableCachingByDefault(because = "Generates documentation from source code analysis")
abstract class ApiDocTask : DefaultTask() {

    @get:Input
    abstract val apiPath: Property<String>

    @get:Input
    abstract val outputFormat: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    init {
        apiPath.convention("src/main/java")
        outputFormat.convention("openapi")
        outputDir.convention(project.layout.buildDirectory.dir("docs/api"))
    }

    @TaskAction
    fun generate() {
        val path = apiPath.get()
        val format = outputFormat.get()
        val output = outputDir.get().asFile

        logger.lifecycle("Generating API documentation from: $path")
        logger.lifecycle("Output format: $format")
        logger.lifecycle("Output directory: ${output.absolutePath}")

        // TODO: 实现 API 文档生成逻辑
        // 1. 扫描 Controller 类
        // 2. 解析注解
        // 3. 生成 OpenAPI 文档

        // 创建输出目录
        output.mkdirs()

        // 生成示例文档
        val docFile = output.resolve("openapi.yaml")
        docFile.writeText(buildOpenApiDoc())

        logger.lifecycle("API documentation generated: ${docFile.absolutePath}")
    }

    private fun buildOpenApiDoc(): String {
        return """
openapi: 3.0.0
info:
  title: AFG Platform API
  version: 1.0.0
  description: AFG 低代码平台 API 文档

paths:
  # TODO: 根据实际接口生成

components:
  schemas:
    # TODO: 根据实体生成
        """.trimIndent()
    }
}
