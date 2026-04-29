plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `jvm-test-suite`
    `maven-publish`
}

gradlePlugin {
    plugins {
        create("afgPlugin") {
            id = "io.github.afgprojects.plugin"
            implementationClass = "io.github.afgprojects.gradle.AfgPlugin"
            displayName = "AFG Platform Gradle Plugin"
            description = "Gradle plugin for AFG low-code platform"
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

    // 类扫描
    implementation("io.github.classgraph:classgraph:4.8.165")

    // Liquibase
    implementation("org.liquibase:liquibase-core:4.26.0")

    // Test dependencies
    testImplementation(kotlin("test"))
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.12.2")
}