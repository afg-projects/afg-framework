plugins {
    `java-library`
}

dependencies {
    // Commons 模块（异常、结果模型）
    api(project(":commons"))

    // APT 注解 API（模块注解、实体注解）
    api(project(":apt-api"))

    // Jackson
    api(libs.bundles.jackson)

    // Micrometer Tracing & Observation
    api(libs.micrometer.tracing)
    api(libs.micrometer.observation)

    // Micrometer Tracing Bridge - Brave (Zipkin compatible)
    // 用户可以选择使用 Brave 或 OpenTelemetry，两者选其一
    compileOnly(libs.micrometer.tracing.bridge.brave)
    compileOnly(libs.zipkin.reporter.brave)

    // Micrometer Tracing Bridge - OpenTelemetry
    compileOnly(libs.micrometer.tracing.bridge.otel)
    compileOnly(libs.otel.exporter.zipkin)
    compileOnly(libs.otel.exporter.otlp)

    // Casbin
    api(libs.jcasbin)

    // JSpecify (空安全注解)
    api(libs.jspecify)

    // Spring Security
    compileOnly(libs.spring.boot.starter.security)

    // Validation
    compileOnly(libs.jakarta.validation.api)

    // Spring Boot
    compileOnly(libs.spring.boot.starter.web)
    compileOnly(libs.spring.boot.starter.validation)

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)


    // Logging
    compileOnly(libs.slf4j.api)

    // Micrometer (指标监控)
    compileOnly(libs.micrometer.core)

    // Spring Boot Actuator (健康检查)
    compileOnly(libs.spring.boot.starter.actuator)
    compileOnly(libs.spring.boot.health)

    // SpringDoc OpenAPI (AfgOpenApiAutoConfiguration - optional)
    compileOnly(libs.springdoc.openapi.starter.webmvc)

    // HikariCP (连接池健康检查)
    compileOnly(libs.spring.boot.starter.data.jdbc)

    // AOP (指标注解切面)
    api(libs.spring.boot.starter.aspectj)

    // Logback (结构化日志)
    api(libs.logback.classic)

    // Caffeine (本地缓存)
    api(libs.caffeine)

    // Spring Expression (SpEL for cache key)
    compileOnly(libs.spring.expression)

    // jsoup (HTML 清洗)
    api(libs.jsoup)

    // OWASP AntiSamy (XSS 防护)
    api(libs.antisamy)

    // MyBatis-Plus Dynamic Datasource (多数据源)
    compileOnly(libs.dynamic.datasource)

    // SnakeYAML (YAML 解析)
    compileOnly(libs.snakeyaml)

    // Spring Data Redis
    compileOnly(libs.spring.boot.starter.data.redis)

    // Test
    testImplementation(libs.bundles.testing)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.web)
    testImplementation(libs.spring.boot.starter.security)
    testImplementation(libs.jakarta.validation.api)
    testImplementation(libs.spring.boot.starter.validation)
    testImplementation(libs.spring.boot.starter.actuator)
    testImplementation(libs.spring.boot.health)
    testImplementation(libs.spring.aop)
    testImplementation(libs.aspectj.weaver)
    testImplementation(libs.spring.boot.test)
    testImplementation(libs.spring.test)

    // Lombok for test
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    // Caffeine for test
    testImplementation(libs.caffeine)

    // jsoup for test
    testImplementation(libs.jsoup)

    // OWASP AntiSamy for test
    testImplementation(libs.antisamy)

    // Testcontainers for integration tests
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)

    // Spring Data Redis for test
    testImplementation(libs.spring.boot.starter.data.redis)

    // Spring JDBC and H2 for DataSource health test
    testImplementation(libs.spring.boot.starter.data.jdbc)
    testImplementation(libs.h2)

    // Micrometer Tracing Brave for test
    testImplementation(libs.micrometer.tracing.bridge.brave)
    testImplementation(libs.zipkin.reporter.brave)

    // ArchUnit (架构测试)
    testImplementation(libs.archunit.junit5)

    // JMH (性能基准测试)
    testImplementation(libs.jmh.core)
    testAnnotationProcessor(libs.jmh.generator.annprocess)
}


