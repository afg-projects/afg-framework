plugins {
    `java-library`
}

dependencies {
    // 依赖 ai-core 接口
    api(project(":ai-core"))

    // Spring AI OpenAI
    api(libs.spring.ai.openai)

    // Spring AI Chat (可选)
    compileOnly(libs.spring.ai.client.chat)

    // Spring AI Anthropic (可选)
    compileOnly(libs.spring.ai.anthropic)

    // Spring AI Ollama (可选)
    compileOnly(libs.spring.ai.ollama)

    // WebClient for HTTP calls
    api(libs.spring.webflux)

    // Jackson for JSON
    api(libs.jackson.databind)

    // JSpecify 空安全注解
    api(libs.jspecify)

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    // Test
    testImplementation(libs.bundles.testing)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.ai.anthropic)
    testImplementation(libs.spring.ai.ollama)
    testImplementation(project(":ai-impl:ai-agent"))
}
