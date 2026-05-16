package io.github.afgprojects.framework.core.gradle.task

import io.github.afgprojects.framework.core.gradle.service.*
import org.gradle.api.DefaultTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import java.nio.file.Path
import kotlin.io.path.*

/**
 * 脚手架任务 - 快速生成 CRUD 代码
 * <p>
 * 根据实体名称和字段定义，自动生成：
 * - Entity 实体类
 * - Controller REST 控制器
 * - Liquibase 迁移脚本
 * - 单元测试
 * <p>
 * 示例：
 * <pre>
 * ./gradlew scaffold \
 *   --entity=User \
 *   --fields="username:String(50),email:String(100),status:Integer"
 * </pre>
 */
@DisableCachingByDefault(because = "Generates new code")
abstract class ScaffoldTask : DefaultTask() {

    @get:Input
    abstract val entityName: Property<String>

    @get:Input
    abstract val fields: ListProperty<String>

    @get:Input
    @get:Optional
    abstract val basePackage: Property<String>

    @get:Input
    @get:Optional
    abstract val tableName: Property<String>

    @get:Input
    @get:Optional
    abstract val apiPath: Property<String>

    @get:Input
    @get:Optional
    abstract val generateService: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val generateTests: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val useLombok: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val useSwagger: Property<Boolean>

    @get:OutputDirectory
    abstract val outputDirectory: org.gradle.api.file.DirectoryProperty

    init {
        entityName.convention("Entity")
        fields.convention(emptyList())
        basePackage.convention("com.example")
        generateService.convention(false)
        generateTests.convention(true)
        useLombok.convention(true)
        useSwagger.convention(true)
        outputDirectory.convention(project.layout.projectDirectory)
    }

    @TaskAction
    fun scaffold() {
        val name = entityName.get()
        val fieldList = fields.get()
        val pkg = basePackage.get()

        if (fieldList.isEmpty()) {
            logger.warn("未指定字段，使用默认字段: id, name")
        }

        logger.lifecycle("========================================")
        logger.lifecycle("生成脚手架代码: $name")
        logger.lifecycle("包名: $pkg")
        logger.lifecycle("字段: ${fieldList.joinToString(", ")}")
        logger.lifecycle("========================================")

        // 解析字段定义
        val fieldDefinitions = if (fieldList.isEmpty()) {
            listOf(
                FieldDefinition("name", FieldType.STRING, length = 100)
            )
        } else {
            fieldList.map { FieldDefinition.parse(it) }
        }

        // 构建选项
        val options = ScaffoldOptions(
            basePackage = pkg,
            generateEntity = true,
            generateController = true,
            generateMigration = true,
            generateService = generateService.get(),
            generateTests = generateTests.get(),
            useLombok = useLombok.get(),
            useValidation = true,
            useSwagger = useSwagger.get(),
            tableName = tableName.orNull,
            apiPath = apiPath.orNull
        )

        // 执行生成
        val service = DefaultScaffoldService()
        val result = service.generate(name, fieldDefinitions, outputDir(), options)

        if (result.success) {
            logger.lifecycle("========================================")
            logger.lifecycle("✅ ${result.message}")
            logger.lifecycle("")
            logger.lifecycle("生成的文件:")
            result.entityFile?.let { logger.lifecycle("  - Entity:    $it") }
            result.controllerFile?.let { logger.lifecycle("  - Controller: $it") }
            result.serviceFile?.let { logger.lifecycle("  - Service:   $it") }
            result.migrationFile?.let { logger.lifecycle("  - Migration: $it") }
            result.testFiles.forEach { logger.lifecycle("  - Test:      $it") }
            logger.lifecycle("========================================")
        } else {
            logger.error("❌ 生成失败: ${result.message}")
            throw org.gradle.api.GradleException(result.message)
        }
    }

    private fun outputDir(): Path = outputDirectory.asFile.get().toPath()
}
