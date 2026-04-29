plugins {
    `java-library`
}

dependencies {
    // 依赖 core 模块
    api(project(":core"))

    // Redisson (Redis 客户端，支持缓存、锁、任务调度)
    api(libs.redisson)
    api(libs.redisson.core)

    // Spring AOP (注解切面)
    compileOnly(libs.spring.aop)
    compileOnly(libs.aspectj.weaver)

    // Spring Expression (SpEL)
    compileOnly(libs.spring.expression)

    // Spring Boot Health (for HealthIndicator)
    compileOnly(libs.spring.boot.health)

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // Test
    testImplementation(libs.bundles.testing)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.health)

    // Testcontainers (集成测试)
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
}


