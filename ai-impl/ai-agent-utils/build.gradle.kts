plugins {
    `java-library`
}

dependencies {
    api(project(":ai-core"))
    api(project(":ai-impl:ai-agent"))

    // Spring
    api(libs.spring.context)
    api(libs.spring.beans)

    // Jackson for YAML parsing
    api(libs.jackson.databind)
    api(libs.jackson.yaml)

    // JSpecify
    api(libs.jspecify)

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    // Test
    testImplementation(libs.bundles.testing)
    testImplementation(project(":ai-impl:ai-llm"))
    testImplementation(libs.spring.ai.ollama)
}
