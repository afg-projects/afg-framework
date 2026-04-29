package io.github.afgprojects.framework.core.gradle

import io.github.afgprojects.framework.core.gradle.extension.AfgExtension
import io.github.afgprojects.framework.core.gradle.task.ApiDocTask
import io.github.afgprojects.framework.core.gradle.task.DbMigrateTask
import io.github.afgprojects.framework.core.gradle.task.GenerateEntityFromDbTask
import io.github.afgprojects.framework.core.gradle.task.GenerateEntityTask
import io.github.afgprojects.framework.core.gradle.task.GenerateMigrationTask
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * AFG 平台 Gradle 插件
 */
class AfgPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        // 创建扩展配置
        project.extensions.create("afg", AfgExtension::class.java)

        // 应用基础插件
        project.plugins.apply("java-library")
        project.plugins.apply("maven-publish")

        // 注册自定义任务
        project.tasks.register("generateEntity", GenerateEntityTask::class.java)
        project.tasks.register("generateMigration", GenerateMigrationTask::class.java)
        project.tasks.register("generateEntityFromDb", GenerateEntityFromDbTask::class.java)
        project.tasks.register("dbMigrate", DbMigrateTask::class.java)
        project.tasks.register("apiDoc", ApiDocTask::class.java)
    }
}
