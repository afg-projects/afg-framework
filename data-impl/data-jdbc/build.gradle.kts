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

    // Spring JDBC (JdbcClient) - 版本由 Spring Boot BOM 管理
    api(libs.spring.jdbc)

    // Spring Boot Starter Data JDBC (for DataSourceAutoConfiguration)
    compileOnly(libs.spring.boot.starter.data.jdbc)

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
    testImplementation(libs.spring.boot.starter.web)
    testImplementation(libs.h2)
    testImplementation(libs.caffeine)
    // Redisson for cache tests
    testImplementation(libs.redisson.core)
    // For testing annotation detection in SimpleFieldMetadata（版本由 Spring Boot BOM 管理）
    testImplementation(libs.jakarta.persistence.api)
    testImplementation(libs.spring.data.commons)
    // Lombok for tests
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
        // Liquibase for test schema migration
    testImplementation(project(":data-impl:data-liquibase"))
    testImplementation(libs.liquibase.core)
    testImplementation("org.springframework.boot:spring-boot-starter-liquibase")
    // PostgreSQL driver for Testcontainers
    testRuntimeOnly("org.postgresql:postgresql:42.7.5")
    // Testcontainers for integration tests
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.mysql)
    // JMH (性能基准测试)
    testImplementation(libs.jmh.core)
    testAnnotationProcessor(libs.jmh.generator.annprocess)
}


