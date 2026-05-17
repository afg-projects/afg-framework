plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `jvm-test-suite`
    application
    id("com.vanniktech.maven.publish")
    id("org.jetbrains.kotlin.jvm")
}

group = property("projectGroup").toString()
version = property("projectVersion").toString()

application {
    mainClass.set("io.github.afgprojects.framework.core.gradle.cli.AfgCliKt")
}

gradlePlugin {
    website.set("https://github.com/afg-projects/afg-framework")
    vcsUrl.set("https://github.com/afg-projects/afg-framework")

    plugins {
        create("afgPlugin") {
            id = "io.github.afg-projects.framework-plugin"
            implementationClass = "io.github.afgprojects.framework.core.gradle.AfgPlugin"
            displayName = "AFG Framework Gradle Plugin"
            description = "Gradle plugin for AFG Framework - provides code generation, database migration, and reverse engineering capabilities"
            tags.set(listOf("afg", "framework", "codegen", "liquibase", "migration"))
        }
    }

    // 声明插件依赖
    dependencies {
        // 独立部署模式时自动应用 Spring Boot 插件
        implementation("org.springframework.boot:org.springframework.boot.gradle.plugin:${rootProject.extra["springBootVersion"]}")
    }
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
        }
    }
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(kotlin("stdlib"))

    // CLI 框架
    implementation(libs.clikt)

    // 类扫描
    implementation(libs.classgraph)

    // Liquibase（gradle-plugin 不使用 Spring Boot BOM，需要显式版本）
    implementation("org.liquibase:liquibase-core:5.0.2")

    // SnakeYAML (YAML 解析，gradle-plugin 不使用 Spring Boot BOM，需要显式版本)
    implementation("org.yaml:snakeyaml:2.4")

    // Test dependencies（gradle-plugin 不使用 Spring Boot BOM，需要显式版本）
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("org.mockito:mockito-core:5.17.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.17.0")
    testImplementation("org.awaitility:awaitility:4.3.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.12.2")
}

// Maven Central 发布配置
configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    coordinates(
        groupId = "io.github.afg-projects",
        artifactId = "afg-framework-gradle-plugin",
        version = project.version.toString()
    )

    pom {
        name.set("AFG Framework Gradle Plugin")
        description.set("Gradle plugin for AFG Framework - provides code generation, database migration, and reverse engineering capabilities")
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
