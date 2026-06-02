plugins {
    `java-library`
}

dependencies {
    api(project(":ai-core"))
    api(project(":ai-impl:ai-agent"))

    // Data module for PersistentSkillRegistry
    api(project(":data-core"))

    // Spring Boot AutoConfiguration
    api(libs.spring.boot.autoconfigure)

    // Spring
    api(libs.spring.context)
    api(libs.spring.beans)

    // Jackson for YAML parsing
    api(libs.jackson.databind)
    api(libs.jackson.yaml)

    // JSpecify
    api(libs.jspecify)

    // APT for entity metadata generation
    compileOnly(project(":apt-api"))
    annotationProcessor(project(":apt-impl"))

    // JPA annotations for entity
    compileOnly(libs.jakarta.persistence.api)

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    // Test
    api(project(":ai-impl:ai-chat"))
    testImplementation(libs.bundles.testing)
    testImplementation(project(":ai-impl:ai-chat"))
    testImplementation(libs.spring.ai.ollama)
}
