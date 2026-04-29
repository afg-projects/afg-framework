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
            id = "io.github.afg-projects.plugin"
            implementationClass = "io.github.afgprojects.framework.core.gradle.AfgPlugin"
            displayName = "AFG Framework Gradle Plugin"
            description = "Gradle plugin for AFG Framework - provides code generation, database migration, and reverse engineering capabilities"
            tags.set(listOf("afg", "framework", "codegen", "liquibase", "migration"))
        }
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
    implementation("com.github.ajalt.clikt:clikt:5.0.3")

    // 类扫描
    implementation("io.github.classgraph:classgraph:4.8.165")

    // Liquibase
    implementation("org.liquibase:liquibase-core:4.26.0")

    // SnakeYAML (YAML 解析)
    implementation("org.yaml:snakeyaml:2.4")

    // Test dependencies
    testImplementation(kotlin("test"))
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
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
