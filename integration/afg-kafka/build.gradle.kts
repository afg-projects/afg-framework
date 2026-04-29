plugins {
    `java-library`
}

dependencies {
    // 依赖 core 模块
    api(project(":core"))

    // Spring Boot AutoConfigure
    compileOnly(libs.spring.boot.starter)

    // Kafka
    api(libs.spring.kafka)

    // Spring Boot Health (for HealthIndicator)
    compileOnly(libs.spring.boot.health)

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // Test
    testImplementation(libs.bundles.testing)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.health)
    // Testcontainers Kafka
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.kafka)
}


