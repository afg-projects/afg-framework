plugins {
    `java-library`
}

dependencies {
    // 依赖 core 模块
    api(project(":core"))
    api(project(":data-core"))

    // JSpecify 空安全注解
    api(libs.jspecify)

    // Reactor Core (响应式编程，用于流式响应)
    api(libs.reactor.core)

    // Spring Boot Starter (for ApplicationEvent)
    api(libs.spring.boot.starter)

    // Spring Web (for HttpHeaders)
    api(libs.spring.boot.starter.web)

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    // Test
    testImplementation(libs.bundles.testing)
    testImplementation(libs.spring.boot.starter.test)
}
