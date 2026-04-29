package io.github.afgprojects.framework.core.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * 实体代码生成任务
 */
@DisableCachingByDefault(because = "Generates source code, output location dependent")
abstract class GenerateEntityTask : DefaultTask() {

    @get:Input
    abstract val entityName: Property<String>

    @get:Input
    abstract val tableName: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    init {
        outputDir.convention(project.layout.buildDirectory.dir("generated/sources/afg/java/main"))
    }

    @TaskAction
    fun generate() {
        val entity = entityName.get()
        val table = tableName.get()
        val output = outputDir.get().asFile

        logger.lifecycle("Generating entity: $entity from table: $table")
        logger.lifecycle("Output directory: ${output.absolutePath}")

        // 生成实体类
        val entityFile = output.resolve("io/github/afgprojects/generated/${entity}Entity.java")
        entityFile.parentFile.mkdirs()

        val content = buildEntityClass(entity, table)
        entityFile.writeText(content)

        logger.lifecycle("Entity generated: ${entityFile.absolutePath}")
    }

    private fun buildEntityClass(entity: String, table: String): String {
        return """
package io.github.afgprojects.generated;

import io.github.afgprojects.model.entity.BaseEntity;

/**
 * 自动生成的实体类: $entity
 * 对应表: $table
 */
public class ${entity}Entity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    // TODO: 根据元数据生成字段

    public ${entity}Entity() {
    }
}
        """.trimIndent()
    }
}
