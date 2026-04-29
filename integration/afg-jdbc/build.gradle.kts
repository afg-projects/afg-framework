plugins {
    `java-library`
}

dependencies {
    // 依赖 core 模块
    api(project(":core"))

    // Spring JDBC (JdbcClient)
    api(libs.spring.jdbc)

    // Spring Boot Autoconfigure
    compileOnly(libs.spring.boot.starter)

    // Spring AOP (异步支持)
    compileOnly(libs.spring.aop)

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // Test
    testImplementation(libs.bundles.testing)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.h2)
    // Lombok for tests
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
    // Testcontainers for integration tests
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
}


