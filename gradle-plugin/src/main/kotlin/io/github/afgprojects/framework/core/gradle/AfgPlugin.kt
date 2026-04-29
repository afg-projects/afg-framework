package io.github.afgprojects.framework.core.gradle

import io.github.afgprojects.framework.core.gradle.extension.AfgExtension
import io.github.afgprojects.framework.core.gradle.task.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import java.io.File

/**
 * AFG Framework Gradle 插件
 *
 * 应用此插件时，会自动配置：
 * 1. 框架核心依赖
 * 2. 代码规范（PMD、JaCoCo）
 * 3. 编译选项（-parameters、UTF-8）
 * 4. 源码生成目录
 * 5. 测试配置
 */
class AfgPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        // 创建扩展配置
        val extension = project.extensions.create("afg", AfgExtension::class.java)

        // 应用基础插件
        project.plugins.apply("java-library")

        // 配置框架特性
        configureFrameworkDependencies(project, extension)
        configureCompileOptions(project)
        configureSourceSets(project, extension)
        configureCodeQuality(project)
        configureTesting(project)

        // 注册自定义任务
        registerTasks(project, extension)

        // 项目评估后配置
        project.afterEvaluate {
            configureAfterEvaluate(project, extension)
        }
    }

    /**
     * 配置框架核心依赖
     */
    private fun configureFrameworkDependencies(project: Project, extension: AfgExtension) {
        project.dependencies.apply {
            // 添加框架核心依赖
            val frameworkVersion = extension.frameworkVersion.getOrElse("1.0.0")

            // 根据模块类型添加不同依赖
            val moduleType = extension.moduleType.getOrElse("data")

            add("implementation", "io.github.afg-projects:afg-framework-core:$frameworkVersion")

            when (moduleType) {
                "data" -> {
                    add("implementation", "io.github.afg-projects:afg-framework-data-jdbc:$frameworkVersion")
                    add("implementation", "io.github.afg-projects:afg-framework-data-liquibase:$frameworkVersion")
                }
                "auth" -> {
                    add("implementation", "io.github.afg-projects:afg-framework-auth:$frameworkVersion")
                }
                "storage" -> {
                    add("implementation", "io.github.afg-projects:afg-framework-storage:$frameworkVersion")
                }
                "job" -> {
                    add("implementation", "io.github.afg-projects:afg-framework-job:$frameworkVersion")
                }
                "registry" -> {
                    add("implementation", "io.github.afg-projects:afg-framework-registry:$frameworkVersion")
                }
                "starter" -> {
                    // Starter 模块只依赖核心，其他依赖由使用者引入
                }
            }

            // 自动添加 Lombok（如果启用）
            if (extension.useLombok.getOrElse(true)) {
                add("compileOnly", "org.projectlombok:lombok:1.18.36")
                add("annotationProcessor", "org.projectlombok:lombok:1.18.36")
                add("testCompileOnly", "org.projectlombok:lombok:1.18.36")
                add("testAnnotationProcessor", "org.projectlombok:lombok:1.18.36")
            }

            // 自动添加 JSR-305 空安全注解（如果启用）
            if (extension.useJsr305.getOrElse(true)) {
                add("compileOnly", "com.google.code.findbugs:jsr305:3.0.2")
            }

            // 自动添加 Validation API（如果启用）
            if (extension.useValidation.getOrElse(true)) {
                add("implementation", "jakarta.validation:jakarta.validation-api:3.1.0")
            }

            // 自动添加测试依赖
            add("testImplementation", "org.junit.jupiter:junit-jupiter:5.12.2")
            add("testImplementation", "org.assertj:assertj-core:3.27.3")
            add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher:1.12.2")
        }
    }

    /**
     * 配置编译选项
     */
    private fun configureCompileOptions(project: Project) {
        project.tasks.withType(JavaCompile::class.java) {
            options.encoding = "UTF-8"
            // 保留参数名，支持反射获取参数名
            options.compilerArgs.add("-parameters")
            // 启用所有警告
            options.compilerArgs.add("-Xlint:all")
        }
    }

    /**
     * 配置源码集（包含生成的源码目录）
     */
    private fun configureSourceSets(project: Project, extension: AfgExtension) {
        val sourceSets = project.extensions.findByType(SourceSetContainer::class.java)
        if (sourceSets != null) {
            val mainSourceSet = sourceSets.getByName("main")

            // 添加框架生成的源码目录
            val generatedDir = project.layout.buildDirectory.dir("generated/sources/afg/java/main")
            mainSourceSet.java.srcDir(generatedDir)

            // 添加资源生成目录
            val generatedResources = project.layout.buildDirectory.dir("generated/resources/afg/main")
            mainSourceSet.resources.srcDir(generatedResources)
        }
    }

    /**
     * 配置代码质量检查
     */
    private fun configureCodeQuality(project: Project) {
        // 配置 JaCoCo
        project.plugins.apply("jacoco")
        project.extensions.findByType(org.gradle.testing.jacoco.plugins.JacocoPluginExtension::class.java)?.apply {
            toolVersion = "0.8.14"
        }

        // 配置 JaCoCo 报告
        project.tasks.named("jacocoTestReport", org.gradle.testing.jacoco.tasks.JacocoReport::class.java) {
            dependsOn("test")
            reports {
                xml.required.set(true)
                html.required.set(true)
            }
        }

        // 测试后生成覆盖率报告
        project.tasks.named("test") {
            finalizedBy("jacocoTestReport")
        }

        // 配置 PMD（可选）
        if (project.plugins.hasPlugin("pmd")) {
            project.extensions.findByType(org.gradle.api.plugins.quality.PmdExtension::class.java)?.apply {
                isConsoleOutput = true
                toolVersion = "7.23.0"
                rulesMinimumPriority.set(5)
                ruleSets = listOf()
                // 使用框架提供的 PMD 规则集
                val ruleSetFile = File(project.rootProject.projectDir, "config/pmd/pmd-ruleset.xml")
                if (ruleSetFile.exists()) {
                    ruleSetFiles = project.files(ruleSetFile)
                }
            }

            // 禁用测试代码的 PMD 检查
            project.tasks.named("pmdTest") {
                enabled = false
            }
        }
    }

    /**
     * 配置测试
     */
    private fun configureTesting(project: Project) {
        project.tasks.withType(org.gradle.api.tasks.testing.Test::class.java) {
            useJUnitPlatform()
            // 单进程运行，避免 Testcontainers 容器冲突
            maxParallelForks = 1
            // 测试失败时不停止，继续运行其他测试
            failFast = false
            // 显示标准输出和错误输出
            testLogging {
                events("passed", "skipped", "failed")
                showStandardStreams = true
            }
        }
    }

    /**
     * 注册自定义任务
     */
    private fun registerTasks(project: Project, extension: AfgExtension) {
        project.tasks.register("generateEntity", GenerateEntityTask::class.java)
        project.tasks.register("generateMigration", GenerateMigrationTask::class.java)
        project.tasks.register("generateEntityFromDb", GenerateEntityFromDbTask::class.java)
        project.tasks.register("dbMigrate", DbMigrateTask::class.java)
        project.tasks.register("apiDoc", ApiDocTask::class.java)

        // 注册框架配置任务
        project.tasks.register("afgInfo") {
            group = "afg"
            description = "显示 AFG Framework 配置信息"
            doLast {
                println("AFG Framework 配置信息:")
                println("  模块类型: ${extension.moduleType.getOrElse("data")}")
                println("  框架版本: ${extension.frameworkVersion.getOrElse("1.0.0")}")
                println("  使用 Lombok: ${extension.useLombok.getOrElse(true)}")
                println("  使用 JSR-305: ${extension.useJsr305.getOrElse(true)}")
                println("  使用 Validation: ${extension.useValidation.getOrElse(true)}")
                println("  启用代码生成: ${extension.enableCodegen.getOrElse(false)}")
            }
        }

        // 注册初始化配置任务
        project.tasks.register("afgInitConfig") {
            group = "afg"
            description = "生成框架配置文件模板"
            doLast {
                generateConfigTemplate(project, extension)
            }
        }
    }

    /**
     * 项目评估后配置
     */
    private fun configureAfterEvaluate(project: Project, extension: AfgExtension) {
        // 如果启用了代码生成，配置生成任务依赖
        if (extension.enableCodegen.getOrElse(false)) {
            project.tasks.named("compileJava") {
                dependsOn("generateEntity")
            }
        }

        // 配置迁移任务
        val migration = extension.migration
        if (migration.entityPackages.isPresent) {
            project.tasks.named("generateMigration", GenerateMigrationTask::class.java) {
                entityPackages.set(migration.entityPackages)
                changesetAuthor.set(migration.author)
                checkDatabase.set(migration.checkDatabase)
                checkChangeLog.set(migration.checkChangeLog)
                changeLogFile.set(migration.changeLogFile)
            }
        }
    }

    /**
     * 生成配置文件模板
     */
    private fun generateConfigTemplate(project: Project, extension: AfgExtension) {
        val resourcesDir = project.layout.projectDirectory.dir("src/main/resources").asFile
        if (!resourcesDir.exists()) {
            resourcesDir.mkdirs()
        }

        // 生成 application.yml 模板
        val applicationYml = File(resourcesDir, "application.yml")
        if (!applicationYml.exists()) {
            applicationYml.writeText("""
# AFG Framework Application Configuration
spring:
  application:
    name: ${project.name}

  # 数据源配置（根据实际环境修改）
  datasource:
    driver-class-name: org.h2.Driver
    url: jdbc:h2:mem:testdb
    username: sa
    password:

  # Liquibase 配置
  liquibase:
    enabled: true
    change-log: classpath:db/changelog.xml

# AFG Framework 配置
afg:
  data:
    # 数据权限配置
    data-scope:
      enabled: true
    # 租户隔离配置
    tenant:
      enabled: false
    # 软删除配置
    soft-delete:
      enabled: true
      field-name: deleted

# 日志配置
logging:
  level:
    root: INFO
    io.github.afgprojects: DEBUG

            """.trimIndent())
            println("生成配置文件: ${applicationYml.absolutePath}")
        }

        // 生成数据库迁移目录
        val dbDir = File(resourcesDir, "db/changelog")
        if (!dbDir.exists()) {
            dbDir.mkdirs()
        }

        val changelogXml = File(dbDir.parentFile, "changelog.xml")
        if (!changelogXml.exists()) {
            changelogXml.writeText("""
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.26.xsd">

    <!-- 包含迁移文件 -->
    <include file="changelog/init.xml" relativeToChangelogFile="true"/>

</databaseChangeLog>

            """.trimIndent())
            println("生成迁移配置: ${changelogXml.absolutePath}")
        }

        val initXml = File(dbDir, "init.xml")
        if (!initXml.exists()) {
            initXml.writeText("""
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.26.xsd">

    <!-- 初始化迁移 -->

</databaseChangeLog>

            """.trimIndent())
            println("生成初始迁移: ${initXml.absolutePath}")
        }

        println("\n配置文件模板生成完成！请根据实际环境修改配置。")
    }
}