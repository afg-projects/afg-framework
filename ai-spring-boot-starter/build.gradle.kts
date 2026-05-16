plugins {
    `java-library`
}

dependencies {
    // 依赖 ai-core 接口
    api(project(":ai-core"))

    // 依赖 ai-llm 实现（可选）
    compileOnly(project(":ai-impl:ai-llm"))

    // 依赖 ai-agent 实现（可选）
    compileOnly(project(":ai-impl:ai-agent"))

    // Spring Boot Autoconfigure
    api(libs.spring.boot.autoconfigure)

    // Spring Boot Starter
    api(libs.spring.boot.starter)

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
}
