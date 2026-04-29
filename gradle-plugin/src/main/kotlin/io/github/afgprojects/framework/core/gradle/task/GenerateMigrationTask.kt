package io.github.afgprojects.framework.core.gradle.task

import io.github.classgraph.ClassGraph
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager

/**
 * 生成迁移脚本任务
 * <p>
 * 扫描实体类，比对数据库和 ChangeLog，生成 Liquibase XML 变更日志
 */
@DisableCachingByDefault(because = "Output depends on database state")
abstract class GenerateMigrationTask : DefaultTask() {

    @get:Input
    abstract val entityPackages: ListProperty<String>

    @get:Input
    abstract val changesetAuthor: Property<String>

    @get:Input
    abstract val checkDatabase: Property<Boolean>

    @get:Input
    abstract val checkChangeLog: Property<Boolean>

    @get:Input
    abstract val dryRun: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val jdbcUrl: Property<String>

    @get:Input
    @get:Optional
    abstract val jdbcUsername: Property<String>

    @get:Input
    @get:Optional
    abstract val jdbcPassword: Property<String>

    @get:Input
    @get:Optional
    abstract val dialectType: Property<String>

    @get:Input
    abstract val changeLogFile: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    init {
        entityPackages.convention(listOf())
        changesetAuthor.convention("auto-generated")
        checkDatabase.convention(false)
        checkChangeLog.convention(false)
        dryRun.convention(false)
        dialectType.convention("h2")
        changeLogFile.convention("src/main/resources/db/changelog.xml")
        outputFile.convention(project.layout.projectDirectory.file(changeLogFile.get()))
    }

    @TaskAction
    fun generate() {
        val packages = entityPackages.get()
        val author = changesetAuthor.get()

        logger.lifecycle("Generating migration from entity packages: $packages")
        logger.lifecycle("Author: $author")
        logger.lifecycle("Check database: ${checkDatabase.get()}")
        logger.lifecycle("Check changeLog: ${checkChangeLog.get()}")
        logger.lifecycle("Dry run: ${dryRun.get()}")

        if (packages.isEmpty()) {
            logger.warn("No entity packages specified, skipping migration generation")
            return
        }

        // 1. 扫描实体类
        val entityClasses = scanEntityClasses(packages)
        logger.lifecycle("Found ${entityClasses.size} entity classes")

        if (entityClasses.isEmpty()) {
            logger.warn("No entity classes found in packages: $packages")
            return
        }

        // 2. 使用 MigrationService 处理
        val migrationServiceClass = Class.forName("io.github.afgprojects.data.liquibase.MigrationService")
        val dialectClass = Class.forName("io.github.afgprojects.data.core.dialect.Dialect")

        // 创建 Dialect
        val dialect = createDialect(dialectType.get())

        // 创建 MigrationService
        val migrationServiceConstructor = migrationServiceClass.getConstructor(dialectClass)
        val migrationService = migrationServiceConstructor.newInstance(dialect)

        // 3. 连接数据库（如果需要）
        var connection: Connection? = null
        if (checkDatabase.get() && jdbcUrl.isPresent) {
            connection = DriverManager.getConnection(
                jdbcUrl.get(),
                jdbcUsername.getOrElse(""),
                jdbcPassword.getOrElse("")
            )
        }

        try {
            val outputPath: Path = outputFile.get().asFile.toPath()
            val changeLogPath: String? = if (checkChangeLog.get()) changeLogFile.get() else null

            // 4. 处理每个实体类
            for (entityClass in entityClasses) {
                processEntityWithService(
                    migrationService, migrationServiceClass,
                    entityClass, connection, changeLogPath, author, outputPath
                )
            }

            logger.lifecycle("Migration generation completed")
        } finally {
            connection?.close()
        }
    }

    private fun processEntityWithService(
        migrationService: Any,
        migrationServiceClass: Class<*>,
        entityClass: Class<*>,
        connection: Connection?,
        changeLogPath: String?,
        author: String,
        outputPath: Path
    ) {
        val tableName = inferTableName(entityClass)
        logger.lifecycle("Processing entity: ${entityClass.simpleName} -> table: $tableName")

        try {
            // 调用 generateMigrationWithComparison 方法
            val entityMetadataClass = Class.forName("io.github.afgprojects.data.core.metadata.EntityMetadata")
            val simpleEntityMetadataClass = Class.forName("io.github.afgprojects.data.jdbc.metadata.SimpleEntityMetadata")

            // 创建 EntityMetadata
            val entityMetadataConstructor = simpleEntityMetadataClass.getConstructor(Class::class.java)
            val entityMetadata = entityMetadataConstructor.newInstance(entityClass)

            // 调用 generateMigrationWithComparison
            val generateMethod = migrationServiceClass.getMethod(
                "generateMigrationWithComparison",
                entityMetadataClass,
                Connection::class.java,
                String::class.java,
                String::class.java,
                Path::class.java
            )

            val diff = generateMethod.invoke(
                migrationService,
                entityMetadata,
                connection,
                changeLogPath,
                author,
                outputPath
            )

            // 检查是否有冲突
            val hasConflictsMethod = diff.javaClass.getMethod("hasConflicts")
            val hasConflicts = hasConflictsMethod.invoke(diff) as Boolean

            if (hasConflicts) {
                logger.error("Conflicts detected for table $tableName, skipping generation")
            }

        } catch (e: Exception) {
            logger.error("Failed to process entity ${entityClass.name}: ${e.message}")
            if (logger.isDebugEnabled) {
                e.printStackTrace()
            }
        }
    }

    private fun scanEntityClasses(packages: List<String>): List<Class<*>> {
        val result = mutableListOf<Class<*>>()
        val baseEntityClassName = "io.github.afgprojects.data.core.entity.BaseEntity"

        for (pkg in packages) {
            logger.info("Scanning package: $pkg")

            ClassGraph()
                .enableClassInfo()
                .acceptPackages(pkg)
                .scan().use { scanResult ->
                    scanResult.allClasses.forEach { classInfo ->
                        // 查找继承 BaseEntity 的类
                        if (classInfo.extendsSuperclass(baseEntityClassName)) {
                            try {
                                val clazz = Class.forName(classInfo.name)
                                result.add(clazz)
                                logger.info("Found entity class: ${classInfo.name}")
                            } catch (e: ClassNotFoundException) {
                                logger.warn("Could not load class: ${classInfo.name}")
                            }
                        }
                    }
                }
        }

        return result
    }

    private fun inferTableName(entityClass: Class<*>): String {
        val className = entityClass.simpleName
        val tableName = StringBuilder()
        for (i in className.indices) {
            val c = className[i]
            if (i > 0 && c.isUpperCase()) {
                tableName.append('_')
            }
            tableName.append(c.lowercaseChar())
        }
        return tableName.toString()
    }

    private fun createDialect(type: String): Any {
        val dialectClassName = when (type.lowercase()) {
            "mysql" -> "io.github.afgprojects.data.core.dialect.MySQLDialect"
            "postgresql", "postgres" -> "io.github.afgprojects.data.core.dialect.PostgreSQLDialect"
            "h2" -> "io.github.afgprojects.data.core.dialect.H2Dialect"
            else -> "io.github.afgprojects.data.core.dialect.H2Dialect"
        }
        val dialectClass = Class.forName(dialectClassName)
        return dialectClass.getConstructor().newInstance()
    }
}