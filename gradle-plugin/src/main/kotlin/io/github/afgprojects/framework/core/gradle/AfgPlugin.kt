package io.github.afgprojects.framework.core.gradle

import io.github.afgprojects.framework.core.gradle.extension.AfgExtension
import io.github.afgprojects.framework.core.gradle.extension.MigrationExtension
import io.github.afgprojects.framework.core.gradle.extension.ReverseEngineeringExtension
import io.github.afgprojects.framework.core.gradle.task.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import java.io.File

/**
 * AFG Framework Gradle 插件
 *
 * 应用此插件时，会自动配置：
 * 1. Spring Boot BOM 版本管理
 * 2. 框架核心依赖
 * 3. 部署模式配置（独立部署 / 聚合部署）
 * 4. 代码规范（JaCoCo）
 * 5. 编译选项（-parameters、UTF-8）
 * 6. 源码生成目录
 * 7. 测试配置
 */
class AfgPlugin : Plugin<Project> {

    companion object {
        const val DEFAULT_SPRING_BOOT_VERSION = "4.0.6"
        const val DEFAULT_FRAMEWORK_VERSION = "1.0.0-SNAPSHOT"
        const val FRAMEWORK_GROUP = "io.github.afg-projects"
    }

    override fun apply(project: Project) {
        // 创建嵌套扩展实例
        val migrationExt = project.objects.newInstance(MigrationExtension::class.java)
        val reverseExt = project.objects.newInstance(ReverseEngineeringExtension::class.java)

        // 创建扩展配置（传入嵌套扩展）
        val extension = project.extensions.create(
            "afg",
            AfgExtension::class.java,
            migrationExt,
            reverseExt
        )

        // 应用基础插件
        project.plugins.apply("java-library")

        // 配置框架特性（编译选项、源码集等立即配置）
        configureCompileOptions(project)
        configureSourceSets(project)
        configureCodeQuality(project)
        configureTesting(project)

        // 注册自定义任务
        registerTasks(project, extension)

        // 配置依赖版本管理（立即配置，不需要 afterEvaluate）
        configureDependencyManagement(project, extension)

        // 项目评估后配置
        project.afterEvaluate {
            configureFrameworkDependencies(project, extension)
            configureDeploymentMode(project, extension)
            configureAfterEvaluate(project, extension)
        }
    }

    /**
     * 配置依赖版本管理
     */
    private fun configureDependencyManagement(project: Project, extension: AfgExtension) {
        val springBootVersion = extension.springBootVersion.getOrElse(DEFAULT_SPRING_BOOT_VERSION)
        val frameworkVersion = extension.frameworkVersion

        // 配置 Spring Boot BOM
        project.dependencies.apply {
            // Spring Boot BOM - 使用 enforcedPlatform 确保版本一致
            val springBootBom = enforcedPlatform("org.springframework.boot:spring-boot-dependencies:$springBootVersion")
            add("implementation", springBootBom)
            add("annotationProcessor", springBootBom)
            add("testAnnotationProcessor", springBootBom)
        }

        // 配置框架依赖版本约束
        // 当声明 io.github.afg-projects:afg-framework-* 但不指定版本时，使用 frameworkVersion
        project.configurations.all {
            resolutionStrategy.eachDependency {
                if (requested.group == FRAMEWORK_GROUP &&
                    requested.name.startsWith("afg-framework-") &&
                    requested.version.isNullOrBlank()) {
                    useVersion(frameworkVersion.getOrElse(DEFAULT_FRAMEWORK_VERSION))
                }
            }
        }
    }

    /**
     * 配置部署模式
     */
    private fun configureDeploymentMode(project: Project, extension: AfgExtension) {
        val standalone = extension.standalone.getOrElse(true)

        if (standalone) {
            // 独立部署：自动应用 Spring Boot 插件，生成可执行 bootJar
            if (!project.plugins.hasPlugin("org.springframework.boot")) {
                project.plugins.apply("org.springframework.boot")
            }
            try {
                val bootJar = project.tasks.findByName("bootJar")
                bootJar?.enabled = true
                val jar = project.tasks.findByName("jar") as? Jar
                jar?.enabled = true
            } catch (e: Exception) {
                // 忽略配置错误
            }
        } else {
            // 聚合部署：作为普通 jar 被主应用依赖
            try {
                val bootJar = project.tasks.findByName("bootJar")
                bootJar?.enabled = false
                val jar = project.tasks.findByName("jar") as? Jar
                jar?.enabled = true

                // 排除 db/changelog/changelog.xml，由主应用统一管理迁移
                jar?.exclude("db/changelog/changelog.xml")
            } catch (e: Exception) {
                // 忽略配置错误
            }
        }
    }

    /**
     * 配置框架核心依赖
     */
    private fun configureFrameworkDependencies(project: Project, extension: AfgExtension) {
        project.dependencies.apply {
            // 添加框架核心依赖
            val frameworkVersion = extension.frameworkVersion.getOrElse(DEFAULT_FRAMEWORK_VERSION)

            // 核心依赖
            add("implementation", "io.github.afg-projects:afg-framework-core:$frameworkVersion")

            // 自动添加 APT 注解处理器（统一入口）
            // apt-impl 包含所有处理器：模块索引、实体元数据等
            add("annotationProcessor", "io.github.afg-projects:afg-framework-apt-impl:$frameworkVersion")

            // 自动添加 APT 注解依赖（编译时可用）
            add("compileOnly", "io.github.afg-projects:afg-framework-apt-api:$frameworkVersion")

            // 自动添加 Lombok（如果启用）- 版本由 Spring Boot BOM 管理
            if (extension.useLombok.getOrElse(true)) {
                add("compileOnly", "org.projectlombok:lombok")
                add("annotationProcessor", "org.projectlombok:lombok")
                add("testCompileOnly", "org.projectlombok:lombok")
                add("testAnnotationProcessor", "org.projectlombok:lombok")
            }

            // 自动添加 Spring Boot 配置元数据处理器
            // 生成 META-INF/spring-configuration-metadata.json，支持 IDE 配置提示
            add("annotationProcessor", "org.springframework.boot:spring-boot-configuration-processor")

            // 自动添加测试依赖 - 版本由 Spring Boot BOM 管理
            add("testImplementation", "org.junit.jupiter:junit-jupiter")
            add("testImplementation", "org.assertj:assertj-core")
            add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
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
    private fun configureSourceSets(project: Project) {
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
        // 注册框架配置任务
        project.tasks.register("afgInfo") {
            group = "afg"
            description = "显示 AFG Framework 配置信息"
            doLast {
                println("AFG Framework 配置信息:")
                println("  Spring Boot 版本: ${extension.springBootVersion.getOrElse(DEFAULT_SPRING_BOOT_VERSION)}")
                println("  框架版本: ${extension.frameworkVersion.getOrElse(DEFAULT_FRAMEWORK_VERSION)}")
                println("  独立部署: ${extension.standalone.getOrElse(true)}")
                println("  使用 Lombok: ${extension.useLombok.getOrElse(true)}")
                println("  启用代码生成: ${extension.enableCodegen.getOrElse(false)}")
                println("  基础包名: ${extension.basePackage.orNull ?: project.group}")
                println("  安全模式: ${extension.securityMode.orNull ?: "无"}")
                println("  数据库类型: ${extension.databaseType.getOrElse("H2")}")
            }
        }

        // 注册初始化配置任务
        project.tasks.register("afgInit", AfgInitTask::class.java) {
            group = "afg"
            description = "生成项目初始文件（Application.java, application.yml, UserDetailsServiceImpl.java 等）"
            basePackage.convention(extension.basePackage.getOrElse(project.group.toString().ifEmpty { "com.example" }))
            securityMode.convention(extension.securityMode)
            databaseType.convention(extension.databaseType.getOrElse("H2"))
            outputDir.convention(project.projectDir)
            overwrite.convention(false)
        }

        // 注册迁移生成任务（仅在配置了 entityPackages 时有效）
        project.tasks.register("generateMigration", GenerateMigrationTask::class.java) {
            group = "afg"
            description = "扫描实体类，生成 Liquibase 迁移脚本"
            entityPackages.convention(extension.migration.entityPackages)
            changesetAuthor.convention(extension.migration.author)
            checkDatabase.convention(extension.migration.checkDatabase)
            checkChangeLog.convention(extension.migration.checkChangeLog)
            changeLogFile.convention(extension.migration.changeLogFile)
        }

        // 注册实体生成任务
        project.tasks.register("generateEntity", GenerateEntityTask::class.java) {
            group = "afg"
            description = "从数据库逆向生成实体类"
        }

        // 注册数据库迁移任务
        project.tasks.register("generateEntityFromDb", GenerateEntityFromDbTask::class.java) {
            group = "afg"
            description = "从数据库表结构生成实体类"
        }

        // 注册数据库迁移执行任务
        project.tasks.register("dbMigrate", DbMigrateTask::class.java) {
            group = "afg"
            description = "执行 Liquibase 数据库迁移"
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
    }

}
