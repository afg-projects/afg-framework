plugins {
    `java-library`
}

dependencies {
    // 依赖 ai-core 接口
    api(project(":ai-core"))
    implementation(project(":commons"))
    implementation(project(":data-core"))

    // Spring Boot AutoConfiguration
    api(libs.spring.boot.autoconfigure)

    // LangChain4j
    implementation(libs.langchain4j)

    // Spring Boot Starter
    implementation(libs.spring.boot.starter)

    // Jackson
    implementation(libs.jackson.databind)

    // Reactor
    implementation(libs.reactor.core)

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
}
