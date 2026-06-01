plugins {
    `java-library`
}

dependencies {
    // 依赖 ai-core 接口
    api(project(":ai-core"))

    // JSpecify 空安全注解
    api(libs.jspecify)

    // Micrometer for metrics
    api(libs.micrometer.core)

    // Spring Boot Actuator (for health endpoint)
    compileOnly(libs.spring.boot.starter.actuator)

    // Spring Boot AutoConfiguration
    api(libs.spring.boot.autoconfigure)

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    // Test
    testImplementation(libs.bundles.testing)
}