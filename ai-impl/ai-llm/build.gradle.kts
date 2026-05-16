plugins {
    `java-library`
}

dependencies {
    // 依赖 ai-core 接口
    api(project(":ai-core"))

    // Spring AI BOM
    implementation(platform("org.springframework.ai:spring-ai-bom:1.0.0-M6"))

    // Spring AI OpenAI
    api("org.springframework.ai:spring-ai-openai")

    // Spring AI Anthropic (可选)
    compileOnly("org.springframework.ai:spring-ai-anthropic")

    // Spring AI Ollama (可选)
    compileOnly("org.springframework.ai:spring-ai-ollama")

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
    testImplementation("org.springframework.ai:spring-ai-anthropic")
    testImplementation("org.springframework.ai:spring-ai-ollama")
}
