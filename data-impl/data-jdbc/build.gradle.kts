plugins {
    `java-library`
}

dependencies {
    // 依赖 data-core 接口
    api(project(":data-core"))

    // 依赖 data-sql SQL 构建
    api(project(":data-impl:data-sql"))

    // 依赖 core 模块（用于缓存集成）
    api(project(":core"))

    // Spring JDBC (JdbcClient) - 版本与 Spring Boot 4.0.5 同步
    api(libs.spring.jdbc)

    // Micrometer for metrics
    api(libs.micrometer.core)

    // AspectJ for AOP
    api(libs.aspectj.weaver)

    // JSpecify 空安全注解
    api(libs.jspecify)

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // Test
    testImplementation(libs.bundles.testing)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.h2)
    testImplementation(libs.caffeine)
    // Redisson for cache tests
    testImplementation(libs.redisson.core)
    // For testing annotation detection in SimpleFieldMetadata
    testImplementation("jakarta.persistence:jakarta.persistence-api")
    testImplementation("org.springframework.data:spring-data-commons")
    testImplementation("com.baomidou:mybatis-plus-annotation:3.5.16")
    // Lombok for tests
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
    // Testcontainers for integration tests
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
    // JMH (性能基准测试)
    testImplementation(libs.jmh.core)
    testAnnotationProcessor(libs.jmh.generator.annprocess)
}


