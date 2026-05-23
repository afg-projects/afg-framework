plugins {
    `java-library`
}

dependencies {
    // 依赖 ai-core 接口
    api(project(":ai-core"))

    // 依赖 ai-impl 实现（可选）
    compileOnly(project(":ai-impl:ai-llm"))
    compileOnly(project(":ai-impl:ai-agent"))
    compileOnly(project(":ai-impl:ai-agent-utils"))
    compileOnly(project(":ai-impl:ai-resilience"))
    compileOnly(project(":ai-impl:ai-observability"))
    compileOnly(project(":ai-impl:ai-security"))
    compileOnly(project(":ai-impl:ai-persistence"))
    compileOnly(project(":ai-impl:ai-performance"))
    compileOnly(project(":ai-impl:ai-rag"))
    compileOnly(project(":ai-impl:ai-etl"))

    // Spring AI VectorStore (可选，用于 RAG)
    compileOnly("org.springframework.ai:spring-ai-vector-store:1.1.6")

    // Spring Boot Autoconfigure
    api(libs.spring.boot.autoconfigure)

    // Spring Boot Starter
    api(libs.spring.boot.starter)

    // Spring Boot JDBC（审计日志存储）
    compileOnly(libs.spring.boot.starter.data.jdbc)

    // Spring Boot Actuator（健康检查）
    compileOnly(libs.spring.boot.starter.actuator)

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
    testImplementation(project(":ai-impl:ai-llm"))
    testImplementation(project(":ai-impl:ai-agent"))
    testImplementation(project(":ai-impl:ai-agent-utils"))
    testImplementation(project(":ai-impl:ai-resilience"))
    testImplementation(project(":ai-impl:ai-observability"))
    testImplementation(project(":ai-impl:ai-security"))
    testImplementation(project(":ai-impl:ai-persistence"))
    testImplementation(project(":ai-impl:ai-performance"))
    testImplementation(project(":ai-impl:ai-rag"))
    testImplementation(project(":ai-impl:ai-etl"))
}
