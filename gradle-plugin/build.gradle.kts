plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `jvm-test-suite`
    id("com.vanniktech.maven.publish")
    id("org.jetbrains.kotlin.jvm")
}

group = property("projectGroup").toString()
version = property("gradlePluginVersion").toString()

gradlePlugin {
    website.set("https://github.com/afg-projects/afg-framework")
    vcsUrl.set("https://github.com/afg-projects/afg-framework")

    plugins {
        create("afgPlugin") {
            id = "io.github.afg-projects.framework-plugin"
            implementationClass = "io.github.afgprojects.framework.core.gradle.AfgPlugin"
            displayName = "AFG Framework Gradle Plugin"
            description = "Gradle plugin for AFG Framework — auto-configures dependencies, compilation, testing, and code quality"
            tags.set(listOf("afg", "framework", "spring-boot", "codegen"))
        }
    }

    // 声明插件依赖 — standalone 模式自动应用 Spring Boot 插件
    dependencies {
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

    // Test dependencies
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testImplementation("org.assertj:assertj-core:3.27.3")
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
        description.set("Gradle plugin for AFG Framework — auto-configures dependencies, compilation, testing, and code quality")
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
