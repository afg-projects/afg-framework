plugins {
    `java-platform`
    id("com.vanniktech.maven.publish")
}

group = property("projectGroup").toString()
version = property("projectVersion").toString()

dependencies {
    constraints {
        // ============================================================
        // Layer 0: Foundation (零依赖模块)
        // ============================================================
        api(project(":commons"))
        api(project(":apt-api"))

        // ============================================================
        // Layer 1: Core
        // ============================================================
        api(project(":core"))

        // ============================================================
        // Layer 2: Core Extensions
        // ============================================================
        api(project(":data-core"))
        api(project(":security-core"))

        // ============================================================
        // Layer 3: Implementations
        // ============================================================
        // Data implementations
        api(project(":data-impl:data-sql"))
        api(project(":data-impl:data-jdbc"))
        api(project(":data-impl:data-liquibase"))

        // Security implementations
        api(project(":security-impl:auth-server"))
        api(project(":security-impl:resource-server"))

        // APT implementation
        api(project(":apt-impl"))

        // ============================================================
        // Layer 4: AI
        // ============================================================
        api(project(":ai-core"))
        api(project(":ai-impl:ai-langchain4j"))
        api(project(":ai-impl:ai-spring-ai"))

        // ============================================================
        // Layer 5: Integrations (中间件集成)
        // ============================================================
        api(project(":integration:afg-redis"))
        api(project(":integration:afg-jdbc"))
        api(project(":integration:afg-rabbitmq"))
        api(project(":integration:afg-websocket"))
        api(project(":integration:afg-storage"))

        // ============================================================
        // Layer 6: Governance
        // ============================================================
        api(project(":governance:proto"))
        api(project(":governance:client"))
        api(project(":governance:server"))
    }
}

// Maven Central 发布配置
configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    coordinates(
        groupId = "io.github.afg-projects",
        artifactId = "afg-framework-bom",
        version = project.version.toString()
    )

    pom {
        name.set("AFG Framework BOM")
        description.set("AFG Framework — Bill of Materials (BOM). Import this to align all AFG module versions.")
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
