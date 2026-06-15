package io.github.afgprojects.framework.core.gradle

import io.github.afgprojects.framework.core.gradle.extension.AfgExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import java.io.File

/**
 * AFG Framework Gradle 插件（轻量版）。
 *
 * 应用此插件时，自动配置：
 * 1. 依赖版本管理 — Spring Boot BOM + AFG Framework BOM
 * 2. 框架核心依赖 — core + apt-api + apt-impl + Lombok + configuration-processor + 测试
 * 3. 部署模式 — standalone 自动应用 Spring Boot 插件
 * 4. 编译选项 — UTF-8、-parameters、-Xlint:all
 * 5. 源码集 — APT 生成代码目录
 * 6. 代码质量 — JaCoCo + PMD（可选）
 * 7. 测试配置 — JUnit Platform
 */
class AfgPlugin : Plugin<Project> {

    companion object {
        const val DEFAULT_SPRING_BOOT_VERSION = "4.0.6"
        const val DEFAULT_FRAMEWORK_VERSION = "1.0.0-SNAPSHOT"
        const val FRAMEWORK_GROUP = "io.github.afg-projects"
    }

    override fun apply(project: Project) {
        // 创建扩展配置
        val extension = project.extensions.create("afg", AfgExtension::class.java)

        // 应用基础插件
        project.plugins.apply("java-library")

        // 配置框架特性（立即配置）
        configureCompileOptions(project)
        configureSourceSets(project)
        configureCodeQuality(project)
        configureTesting(project)

        // 配置依赖版本管理
        configureDependencyManagement(project, extension)

        // 项目评估后配置（需要 extension 的最终值）
        project.afterEvaluate {
            configureFrameworkDependencies(project, extension)
            configureDeploymentMode(project, extension)
        }
    }

    /**
     * 配置依赖版本管理 — Spring Boot BOM + AFG Framework BOM。
     */
    private fun configureDependencyManagement(project: Project, extension: AfgExtension) {
        val springBootVersion = extension.springBootVersion.getOrElse(DEFAULT_SPRING_BOOT_VERSION)
        val frameworkVersion = extension.frameworkVersion.getOrElse(DEFAULT_FRAMEWORK_VERSION)

        project.dependencies.apply {
            // Spring Boot BOM — enforcedPlatform 确保版本一致
            val springBootBom = enforcedPlatform("org.springframework.boot:spring-boot-dependencies:$springBootVersion")
            add("implementation", springBootBom)
            add("annotationProcessor", springBootBom)
            add("testAnnotationProcessor", springBootBom)

            // AFG Framework BOM — 统一管理所有框架模块版本
            add("implementation", platform("$FRAMEWORK_GROUP:afg-framework-bom:$frameworkVersion"))
        }
    }

    /**
     * 配置框架核心依赖。
     */
    private fun configureFrameworkDependencies(project: Project, extension: AfgExtension) {
        val frameworkVersion = extension.frameworkVersion.getOrElse(DEFAULT_FRAMEWORK_VERSION)

        project.dependencies.apply {
            // 框架核心
            add("implementation", "$FRAMEWORK_GROUP:afg-framework-core:$frameworkVersion")

            // APT 注解处理器（统一入口：模块索引、实体元数据等）
            add("annotationProcessor", "$FRAMEWORK_GROUP:afg-framework-apt-impl:$frameworkVersion")

            // APT 注解依赖（编译时可用）
            add("compileOnly", "$FRAMEWORK_GROUP:afg-framework-apt-api:$frameworkVersion")

            // Lombok（可选，默认启用，版本由 Spring Boot BOM 管理）
            if (extension.useLombok.getOrElse(true)) {
                add("compileOnly", "org.projectlombok:lombok")
                add("annotationProcessor", "org.projectlombok:lombok")
                add("testCompileOnly", "org.projectlombok:lombok")
                add("testAnnotationProcessor", "org.projectlombok:lombok")
            }

            // Spring Boot 配置元数据处理器（生成 META-INF/spring-configuration-metadata.json）
            add("annotationProcessor", "org.springframework.boot:spring-boot-configuration-processor")

            // 测试依赖（版本由 Spring Boot BOM 管理）
            add("testImplementation", "org.junit.jupiter:junit-jupiter")
            add("testImplementation", "org.assertj:assertj-core")
            add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
        }
    }

    /**
     * 配置部署模式。
     */
    private fun configureDeploymentMode(project: Project, extension: AfgExtension) {
        val standalone = extension.standalone.getOrElse(true)

        if (standalone) {
            // 独立部署：自动应用 Spring Boot 插件，生成可执行 bootJar
            if (!project.plugins.hasPlugin("org.springframework.boot")) {
                project.plugins.apply("org.springframework.boot")
            }
            try {
                project.tasks.findByName("bootJar")?.enabled = true
                (project.tasks.findByName("jar") as? Jar)?.enabled = true
            } catch (_: Exception) {
                // 忽略配置错误
            }
        } else {
            // 聚合部署：作为普通 jar 被主应用依赖
            try {
                project.tasks.findByName("bootJar")?.enabled = false
                val jar = project.tasks.findByName("jar") as? Jar
                jar?.enabled = true
                // 排除 db/changelog/changelog.xml，由主应用统一管理迁移
                jar?.exclude("db/changelog/changelog.xml")
            } catch (_: Exception) {
                // 忽略配置错误
            }
        }
    }

    /**
     * 配置编译选项。
     */
    private fun configureCompileOptions(project: Project) {
        project.tasks.withType(JavaCompile::class.java) {
            options.encoding = "UTF-8"
            options.compilerArgs.add("-parameters")  // 保留参数名，支持反射
            options.compilerArgs.add("-Xlint:all")    // 启用所有警告
        }
    }

    /**
     * 配置源码集（包含 APT 生成的源码目录）。
     */
    private fun configureSourceSets(project: Project) {
        val sourceSets = project.extensions.findByType(SourceSetContainer::class.java) ?: return
        val mainSourceSet = sourceSets.getByName("main")

        val generatedDir = project.layout.buildDirectory.dir("generated/sources/afg/java/main")
        mainSourceSet.java.srcDir(generatedDir)

        val generatedResources = project.layout.buildDirectory.dir("generated/resources/afg/main")
        mainSourceSet.resources.srcDir(generatedResources)
    }

    /**
     * 配置代码质量检查 — JaCoCo + PMD（可选）。
     */
    private fun configureCodeQuality(project: Project) {
        // JaCoCo
        project.plugins.apply("jacoco")
        project.extensions.findByType(org.gradle.testing.jacoco.plugins.JacocoPluginExtension::class.java)?.apply {
            toolVersion = "0.8.14"
        }

        project.tasks.named("jacocoTestReport", org.gradle.testing.jacoco.tasks.JacocoReport::class.java) {
            dependsOn("test")
            reports {
                xml.required.set(true)
                html.required.set(true)
            }
        }

        project.tasks.named("test") {
            finalizedBy("jacocoTestReport")
        }

        // PMD（可选）
        if (project.plugins.hasPlugin("pmd")) {
            project.extensions.findByType(org.gradle.api.plugins.quality.PmdExtension::class.java)?.apply {
                isConsoleOutput = true
                toolVersion = "7.23.0"
                rulesMinimumPriority.set(5)
                ruleSets = listOf()
                val ruleSetFile = File(project.rootProject.projectDir, "config/pmd/pmd-ruleset.xml")
                if (ruleSetFile.exists()) {
                    ruleSetFiles = project.files(ruleSetFile)
                }
            }

            project.tasks.named("pmdTest") {
                enabled = false
            }
        }
    }

    /**
     * 配置测试。
     */
    private fun configureTesting(project: Project) {
        project.tasks.withType(org.gradle.api.tasks.testing.Test::class.java) {
            useJUnitPlatform()
            maxParallelForks = 1          // 单进程，避免 Testcontainers 容器冲突
            failFast = false              // 失败不停止
            testLogging {
                events("passed", "skipped", "failed")
                showStandardStreams = true
            }
        }
    }
}
