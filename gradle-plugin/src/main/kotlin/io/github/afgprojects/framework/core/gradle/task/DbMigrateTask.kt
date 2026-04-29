package io.github.afgprojects.framework.core.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.sql.Connection
import java.sql.DriverManager

/**
 * 数据库迁移任务
 * <p>
 * 使用 Liquibase 执行数据库迁移
 */
@DisableCachingByDefault(because = "Modifies database state, not cacheable")
abstract class DbMigrateTask : DefaultTask() {

    @get:Input
    abstract val jdbcUrl: Property<String>

    @get:Input
    abstract val username: Property<String>

    @get:Input
    abstract val password: Property<String>

    @get:Input
    abstract val changeLogFile: Property<String>

    @get:Input
    @get:Optional
    abstract val targetVersion: Property<String>

    @get:Input
    abstract val contexts: Property<String>

    @get:Input
    abstract val labels: Property<String>

    init {
        changeLogFile.convention("src/main/resources/db/changelog.xml")
        targetVersion.convention("")  // 空表示迁移到最新版本
        contexts.convention("")
        labels.convention("")
    }

    @TaskAction
    fun migrate() {
        val url = jdbcUrl.get()
        val user = username.get()
        val pass = password.get()
        val changeLog = changeLogFile.get()
        val version = targetVersion.getOrElse("")
        val ctx = contexts.get()
        val lbl = labels.get()

        logger.lifecycle("Executing database migration")
        logger.lifecycle("JDBC URL: $url")
        logger.lifecycle("ChangeLog: $changeLog")
        logger.lifecycle("Target version: ${if (version.isEmpty()) "latest" else version}")
        logger.lifecycle("Contexts: $ctx")
        logger.lifecycle("Labels: $lbl")

        // 连接数据库
        val connection: Connection = DriverManager.getConnection(url, user, pass)

        try {
            // 使用反射加载 LiquibaseMigrationRunner
            val runnerClass = Class.forName("io.github.afgprojects.data.liquibase.runner.LiquibaseMigrationRunner")
            val runner = runnerClass.getConstructor().newInstance()

            // 执行迁移
            if (version.isEmpty()) {
                val migrateMethod = runnerClass.getMethod("migrate", Connection::class.java, String::class.java)
                migrateMethod.invoke(runner, connection, changeLog)
            } else {
                val migrateMethod = runnerClass.getMethod(
                    "migrate", Connection::class.java, String::class.java,
                    String::class.java, String::class.java, String::class.java
                )
                migrateMethod.invoke(runner, connection, changeLog, version, ctx, lbl)
            }

            logger.lifecycle("Database migration completed successfully")
        } catch (e: Exception) {
            logger.error("Database migration failed: ${e.message}")
            throw e
        } finally {
            connection.close()
        }
    }
}