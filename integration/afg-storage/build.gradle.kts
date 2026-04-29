plugins {
    `java-library`
}

dependencies {
    // 依赖 core 模块
    api(project(":core"))

    // Spring Boot Autoconfigure
    compileOnly(libs.spring.boot.autoconfigure)

    // AWS S3 SDK (用于 MinIO 和 S3)
    compileOnly(libs.aws.s3)

    // 阿里云 OSS SDK
    compileOnly(libs.aliyun.oss)

    // Spring Boot Health (for HealthIndicator)
    compileOnly(libs.spring.boot.health)

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // Test
    testImplementation(libs.bundles.testing)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.health)
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
}


