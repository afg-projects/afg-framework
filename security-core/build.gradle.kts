plugins {
    `java-library`
}

dependencies {
    // 依赖 core 模块
    api(project(":core"))

    // 依赖 data-core 模块（用于 DataScope）
    api(project(":data-core"))

    // Spring Security（版本由 Spring Boot BOM 管理）
    api(libs.spring.boot.starter.security)

    // Spring Data Commons (for Page and Pageable)
    implementation("org.springframework.data:spring-data-commons")

    // Jakarta Servlet API (for TenantResolver - compile only, provided by web container，版本由 Spring Boot BOM 管理)
    compileOnly(libs.jakarta.servlet.api)

    // JSpecify 空安全注解（版本由 Spring Boot BOM 管理）
    api(libs.jspecify)

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    // Test
    testImplementation(libs.bundles.testing)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.jakarta.servlet.api)
}
