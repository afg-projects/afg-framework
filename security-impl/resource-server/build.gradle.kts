plugins {
    `java-library`
}

dependencies {
    api(project(":security-core"))
    api(libs.spring.boot.starter.oauth2.resource.server)
    api(libs.jspecify)

    // Jakarta Servlet API（版本由 Spring Boot BOM 管理）
    compileOnly(libs.jakarta.servlet.api)

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
