package io.github.afgprojects.framework.core.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import kotlin.collections.iterator

/**
 * 从数据库逆向生成实体任务
 * <p>
 * 连接数据库，读取表结构，生成 Entity Java 代码
 */
@DisableCachingByDefault(because = "Output depends on database state")
abstract class GenerateEntityFromDbTask : DefaultTask() {

    @get:Input
    abstract val jdbcUrl: Property<String>

    @get:Input
    abstract val username: Property<String>

    @get:Input
    abstract val password: Property<String>

    @get:Input
    abstract val tables: ListProperty<String>

    @get:Input
    abstract val basePackage: Property<String>

    @get:Input
    abstract val useLombok: Property<Boolean>

    @get:Input
    abstract val useJsr305: Property<Boolean>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    init {
        tables.convention(listOf())
        basePackage.convention("io.github.afgprojects.entity")
        useLombok.convention(true)
        useJsr305.convention(true)
        outputDir.convention(project.layout.projectDirectory.dir("src/main/java"))
    }

    @TaskAction
    fun generate() {
        val url = jdbcUrl.get()
        val user = username.get()
        val pass = password.get()
        val tableList = tables.get()
        val pkg = basePackage.get()

        logger.lifecycle("Generating entities from database: $url")
        logger.lifecycle("Tables: ${if (tableList.isEmpty()) "all" else tableList}")
        logger.lifecycle("Package: $pkg")
        logger.lifecycle("Output directory: ${outputDir.get()}")

        // 连接数据库
        val connection: Connection = DriverManager.getConnection(url, user, pass)

        try {
            val outputPath = outputDir.get().asFile.toPath()

            // 使用反射加载类，避免直接依赖
            val jdbcReaderClass = Class.forName("io.github.afgprojects.data.liquibase.reader.JdbcSchemaReader")
            val entityGeneratorClass = Class.forName("io.github.afgprojects.data.liquibase.generator.EntityCodeGenerator")
            val schemaMetadataClass = Class.forName("io.github.afgprojects.data.core.schema.SchemaMetadata")

            // 创建实例
            val jdbcReader = jdbcReaderClass.getConstructor().newInstance()
            val entityGenerator = entityGeneratorClass.getConstructor(Boolean::class.java, Boolean::class.java)
                .newInstance(useLombok.get(), useJsr305.get())

            if (tableList.isEmpty()) {
                // 读取所有表
                logger.lifecycle("Reading all tables from database...")
                val readAllMethod = jdbcReaderClass.getMethod("readAllTables", Connection::class.java)
                @Suppress("UNCHECKED_CAST")
                val schemas = readAllMethod.invoke(jdbcReader, connection) as Map<String, Any>
                logger.lifecycle("Found ${schemas.size} tables")

                for ((tableName, schema) in schemas) {
                    try {
                        val generateMethod = entityGeneratorClass.getMethod(
                            "generate", schemaMetadataClass, String::class.java, Path::class.java
                        )
                        generateMethod.invoke(entityGenerator, schema, pkg, outputPath)
                        logger.lifecycle("Generated entity for table: $tableName")
                    } catch (e: Exception) {
                        logger.error("Failed to generate entity for table $tableName: ${e.message}")
                    }
                }
            } else {
                // 读取指定表
                val readTableMethod = jdbcReaderClass.getMethod("readTable", Connection::class.java, String::class.java)
                val generateMethod = entityGeneratorClass.getMethod(
                    "generate", schemaMetadataClass, String::class.java, Path::class.java
                )

                for (tableName in tableList) {
                    try {
                        logger.lifecycle("Reading table: $tableName")
                        val schema = readTableMethod.invoke(jdbcReader, connection, tableName)
                        generateMethod.invoke(entityGenerator, schema, pkg, outputPath)
                        logger.lifecycle("Generated entity for table: $tableName")
                    } catch (e: Exception) {
                        logger.error("Failed to generate entity for table $tableName: ${e.message}")
                    }
                }
            }

            logger.lifecycle("Entity generation completed successfully")
        } finally {
            connection.close()
        }
    }
}