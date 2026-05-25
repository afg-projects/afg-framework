plugins {
    `java-library`
}

dependencies {
    api(project(":ai-core"))
    api(libs.spring.ai.client.chat)

    compileOnly(libs.spring.ai.openai)
    compileOnly(libs.spring.ai.anthropic)
    compileOnly(libs.spring.ai.ollama)

    api(libs.reactor.core)
    api(libs.jackson.databind)
    api(libs.jspecify)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    // Test dependencies
    testImplementation(libs.bundles.testing)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.ai.ollama)
    testImplementation("io.projectreactor:reactor-test")
}