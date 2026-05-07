plugins {
    `java-library`
}

dependencies {
    // 依赖 core 模块
    api(project(":core"))

    // Spring Security
    api(libs.spring.boot.starter.security)

    // Jakarta Servlet API (for TenantResolver - compile only, provided by web container)
    compileOnly("jakarta.servlet:jakarta.servlet-api")

    // JSpecify 空安全注解
    api(libs.jspecify)

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    // Test
    testImplementation(libs.bundles.testing)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation("jakarta.servlet:jakarta.servlet-api")
}
