plugins {
    `java-library`
}

dependencies {
    // 依赖 ai-core
    api(project(":ai-core"))

    // Spring AI 依赖
    api(libs.spring.ai.client.chat)
    api(libs.spring.ai.model)

    // 可选的 Spring AI 模型提供者
    compileOnly(libs.spring.ai.openai)
    compileOnly(libs.spring.ai.anthropic)
    compileOnly(libs.spring.ai.ollama)

    // Spring Boot
    api(libs.spring.boot.starter)
    api(libs.spring.boot.autoconfigure)
    api(libs.reactor.core)
    api(libs.jackson.databind)
    api(libs.jspecify)

    // Micrometer（可观测性适配）
    api(libs.micrometer.observation)
    api(libs.micrometer.core)

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    // Test
    testImplementation(libs.bundles.testing)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.ai.ollama)
    testImplementation("io.projectreactor:reactor-test")
}
