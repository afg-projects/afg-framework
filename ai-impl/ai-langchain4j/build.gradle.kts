plugins {
    `java-library`
}

dependencies {
    // 依赖 ai-core
    api(project(":ai-core"))

    // LangChain4j 核心
    api(libs.langchain4j)

    // 可选的 LangChain4j 模型提供者
    compileOnly(libs.langchain4j.open.ai)
    compileOnly(libs.langchain4j.ollama)

    // Spring Boot
    api(libs.spring.boot.starter)
    api(libs.spring.boot.autoconfigure)
    api(libs.reactor.core)
    api(libs.jackson.databind)
    api(libs.jspecify)

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    // Test
    testImplementation(libs.bundles.testing)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation("io.projectreactor:reactor-test")
}
