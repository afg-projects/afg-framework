import org.gradle.api.tasks.SourceSetContainer
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    id("org.springframework.boot") version "4.0.5" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("com.github.ben-manes.versions") version "0.54.0"
    id("org.owasp.dependencycheck") version "12.1.0"
    id("com.vanniktech.maven.publish") version "0.36.0" apply false
    id("org.jetbrains.kotlin.jvm") version "2.3.0" apply false
    pmd
}

allprojects {
    group = property("projectGroup").toString()
    version = property("projectVersion").toString()
}

subprojects {
    // gradle-plugin 子项目使用 kotlin-dsl，跳过其他插件应用
    if (name != "gradle-plugin") {
        apply(plugin = "java-library")
        apply(plugin = "io.spring.dependency-management")
        apply(plugin = "pmd")
        apply(plugin = "jacoco")
        apply(plugin = "com.vanniktech.maven.publish")

        // 配置 Spring Boot BOM 版本管理
        configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
            imports {
                mavenBom("org.springframework.boot:spring-boot-dependencies:4.0.5")
            }
        }

        configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_25
            targetCompatibility = JavaVersion.VERSION_25
            // 注意：不调用 withJavadocJar() 和 withSourcesJar()
            // 因为 vanniktech.maven.publish 插件会自动处理这些
        }

        // 配置 Javadoc 跳过严格检查
        tasks.withType<Javadoc> {
            options {
                this as StandardJavadocDocletOptions
                addStringOption("Xdoclint:none", "-quiet")
                addBooleanOption("html5", true)
            }
            isFailOnError = false
        }

        // 修复 maven publish 插件的任务依赖问题
        tasks.configureEach {
            if (name == "generateMetadataFileForMavenPublication") {
                dependsOn(tasks.matching { it.name.endsWith("JavadocJar") || it.name.endsWith("SourcesJar") })
            }
        }

        tasks.withType<JavaCompile> {
            options.encoding = "UTF-8"
            options.compilerArgs.add("-parameters")
        }

        tasks.withType<Test> {
            useJUnitPlatform()
            // 确保测试在单个进程中运行，避免 Testcontainers 容器冲突
            maxParallelForks = 1
            finalizedBy("jacocoTestReport")
        }

        configurations.all {
            resolutionStrategy {
                failOnNonReproducibleResolution()
            }
        }

        // PMD 配置
        pmd {
            isConsoleOutput = true
            toolVersion = "7.23.0"
            rulesMinimumPriority.set(5)
            ruleSets = listOf()
            ruleSetFiles = files("${rootProject.projectDir}/config/pmd/pmd-ruleset.xml")
        }

        // 禁用测试代码的 PMD 检查
        tasks.named("pmdTest") {
            enabled = false
        }

        // JaCoCo 配置
        configure<JacocoPluginExtension> {
            toolVersion = "0.8.14"
        }

        tasks.named<JacocoReport>("jacocoTestReport") {
            dependsOn("test")
            reports {
                xml.required.set(true)
                html.required.set(true)
            }
        }

        // Maven Central 发布配置
        configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
            publishToMavenCentral(automaticRelease = true)
            signAllPublications()

            coordinates(
                groupId = "io.github.afg-projects",
                artifactId = "afg-framework-${project.name}",
                version = project.version.toString()
            )

            pom {
                name.set("AFG Framework - ${project.name}")
                description.set("AFG Framework - Enterprise Java Development Framework")
                url.set("https://github.com/afg-projects/afg-framework")
                inceptionYear.set("2024")

                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                        distribution.set("repo")
                    }
                }

                developers {
                    developer {
                        id.set("afgprojects")
                        name.set("AFG Projects Team")
                    }
                }

                scm {
                    url.set("https://github.com/afg-projects/afg-framework")
                    connection.set("scm:git:git://github.com/afg-projects/afg-framework.git")
                    developerConnection.set("scm:git:ssh://github.com/afg-projects/afg-framework.git")
                }
            }
        }
    }
}

// 依赖版本检查配置
tasks.named<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask>("dependencyUpdates") {
    // 输出格式
    outputFormatter = "text,json"
    outputDir = "${layout.buildDirectory.get().asFile}/reports/dependency-updates"

    // 拒绝不稳定版本（alpha, beta, milestone, rc 等）
    rejectVersionIf {
        isNonStable(candidate.version)
    }
}

/**
 * 判断版本是否为不稳定版本
 */
fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

// OWASP Dependency Check 配置
dependencyCheck {
    // CVSS 评分阈值配置
    // failBuildOnCVSS: 7.0 表示高危及以上漏洞（CVSS >= 7.0）将导致构建失败
    failBuildOnCVSS = 7.0f

    // 输出格式：HTML（人类可读）、JSON（机器解析）、JUNIT（CI/CD 集成）
    formats = listOf("HTML", "JSON", "JUNIT")

    // 抑制文件路径（用于处理误报）
    suppressionFile = "${rootProject.projectDir}/config/dependency-check/suppressions.xml"

    // NVD API 配置（使用 NIST 官方 API Key 可提高速率限制）
    // 环境变量 NVD_API_KEY 可在 CI/CD 中设置
    nvd {
        apiKey = System.getenv("NVD_API_KEY") ?: ""
        // API 请求延迟（毫秒），避免触发速率限制
        delay = 3500
    }

    // 分析配置
    analyzers {
        // 禁用实验性分析器以提高性能
        assemblyEnabled = false
        nodeEnabled = false
        nodeAuditEnabled = false
        // 禁用 OSS Index 分析器（避免网络超时问题，主要使用 NVD 数据库）
        ossIndexEnabled = false
    }

    // 跳过测试依赖扫描（可选，提高扫描速度）
    skipTestGroups = true

    // 输出目录
    outputDirectory = "${layout.buildDirectory.get().asFile}/reports/dependency-check"
}

// ==================== JMH 基准测试配置 ====================

/**
 * JMH 基准测试任务注册
 *
 * 使用方式：
 * ./gradlew jmh -Pbenchmark=<benchmark-class-simple-name>
 * ./gradlew jmh -Pbenchmark=SqlQueryBuilderBenchmark
 */
tasks.register("jmh", JavaExec::class) {
    group = "benchmark"
    description = "Run JMH benchmarks. Use -Pbenchmark=<name> to specify benchmark class."

    val benchmarkName = project.findProperty("benchmark") as String? ?: ".*Benchmark"
    val benchmarkModule = project.findProperty("module") as String? ?: ""

    // 根据模块参数选择要运行的子项目
    val benchmarkProjects = if (benchmarkModule.isNotEmpty()) {
        listOf(project(benchmarkModule))
    } else {
        // 默认运行所有包含基准测试的模块
        listOf(
            project(":core"),
            project(":data-impl:data-sql"),
            project(":data-impl:data-jdbc")
        )
    }

    dependsOn(benchmarkProjects.map { proj -> proj.tasks.named("testClasses") })

    inputs.files(benchmarkProjects.flatMap { proj ->
        proj.extensions.getByType(SourceSetContainer::class.java).getByName("test").runtimeClasspath
    })

    doFirst {
        classpath = files(benchmarkProjects.flatMap { proj ->
            val sourceSets = proj.extensions.getByType(SourceSetContainer::class.java)
            listOf(sourceSets.getByName("test").runtimeClasspath)
        })
    }

    mainClass.set("org.openjdk.jmh.Main")

    jvmArgs("-Xmx1G")
    setArgs(listOf(benchmarkName))
}

/**
 * 运行特定模块的基准测试
 *
 * 示例：
 * ./gradlew jmhSqlBuilder
 */
tasks.register("jmhSqlBuilder", JavaExec::class) {
    group = "benchmark"
    description = "Run SQL Builder benchmarks"

    val dataSql = project(":data-impl:data-sql")
    dependsOn(dataSql.tasks.named("testClasses"))

    inputs.files(dataSql.extensions.getByType(SourceSetContainer::class.java).getByName("test").runtimeClasspath)

    doFirst {
        val sourceSets = dataSql.extensions.getByType(SourceSetContainer::class.java)
        classpath = sourceSets.getByName("test").runtimeClasspath
    }

    mainClass.set("org.openjdk.jmh.Main")

    jvmArgs("-Xmx1G")
    setArgs(listOf("SqlQueryBuilderBenchmark|ConditionToSqlConverterBenchmark"))
}

/**
 * 运行缓存基准测试
 */
tasks.register("jmhCache", JavaExec::class) {
    group = "benchmark"
    description = "Run Cache benchmarks"

    val coreProject = project(":core")
    dependsOn(coreProject.tasks.named("testClasses"))

    inputs.files(coreProject.extensions.getByType(SourceSetContainer::class.java).getByName("test").runtimeClasspath)

    doFirst {
        val sourceSets = coreProject.extensions.getByType(SourceSetContainer::class.java)
        classpath = sourceSets.getByName("test").runtimeClasspath
    }

    mainClass.set("org.openjdk.jmh.Main")

    jvmArgs("-Xmx1G")
    // JMH 使用 regex pattern 作为参数
    setArgs(listOf("CacheManagerBenchmark"))
}

/**
 * 运行 DataManager 基准测试
 */
tasks.register("jmhDataManager", JavaExec::class) {
    group = "benchmark"
    description = "Run DataManager benchmarks"

    val dataJdbc = project(":data-impl:data-jdbc")
    dependsOn(dataJdbc.tasks.named("testClasses"))

    inputs.files(dataJdbc.extensions.getByType(SourceSetContainer::class.java).getByName("test").runtimeClasspath)

    doFirst {
        val sourceSets = dataJdbc.extensions.getByType(SourceSetContainer::class.java)
        classpath = sourceSets.getByName("test").runtimeClasspath
    }

    mainClass.set("org.openjdk.jmh.Main")

    jvmArgs("-Xmx1G")
    setArgs(listOf("DataManagerBenchmark|FieldAccessorBenchmark"))
}

/**
 * 运行所有基准测试
 */
tasks.register("jmhAll", JavaExec::class) {
    group = "benchmark"
    description = "Run all benchmarks"

    val allProjects = listOf(
        project(":core"),
        project(":data-impl:data-sql"),
        project(":data-impl:data-jdbc")
    )

    dependsOn(allProjects.map { proj -> proj.tasks.named("testClasses") })

    inputs.files(allProjects.flatMap { proj ->
        proj.extensions.getByType(SourceSetContainer::class.java).getByName("test").runtimeClasspath
    })

    doFirst {
        classpath = files(allProjects.flatMap { proj ->
            val sourceSets = proj.extensions.getByType(SourceSetContainer::class.java)
            listOf(sourceSets.getByName("test").runtimeClasspath)
        })
    }

    mainClass.set("org.openjdk.jmh.Main")

    jvmArgs("-Xmx2G")
    setArgs(listOf(".*Benchmark"))
}
