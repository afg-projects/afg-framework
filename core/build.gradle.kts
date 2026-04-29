plugins {
    `java-library`
}

dependencies {
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

    // HikariCP (连接池健康检查)
    compileOnly(libs.spring.boot.starter.data.jdbc)

    // AOP (指标注解切面)
    compileOnly(libs.spring.aop)
    compileOnly(libs.aspectj.weaver)

    // Logback (结构化日志)
    compileOnly(libs.logback.classic)

    // Redisson (分布式存储，功能开关)
    compileOnly(libs.redisson)
    compileOnly(libs.redisson.core)

    // Caffeine (本地缓存)
    compileOnly(libs.caffeine)

    // Spring Expression (SpEL for cache key)
    compileOnly(libs.spring.expression)

    // jsoup (HTML 清洗)
    compileOnly(libs.jsoup)

    // OWASP AntiSamy (XSS 防护)
    compileOnly(libs.antisamy)

    // Nacos Config (配置中心)
    compileOnly(libs.nacos.client)

    // Apollo Config (配置中心)
    compileOnly(libs.apollo.client)

    // Consul Client (配置中心/服务发现)
    compileOnly(libs.consul.client)

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
    testImplementation(libs.spring.boot.starter.actuator)
    testImplementation(libs.spring.boot.health)
    testImplementation(libs.spring.aop)
    testImplementation(libs.aspectj.weaver)
    testImplementation(libs.spring.boot.test)
    testImplementation(libs.spring.test)

    // Redisson for test
    testImplementation(libs.redisson)
    testImplementation(libs.redisson.core)

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


