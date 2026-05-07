plugins {
    `java-library`
}

dependencies {
    api(project(":security-core"))
    api(libs.spring.boot.starter.oauth2.resource.server)
    api(libs.jspecify)

    // Jakarta Servlet API
    compileOnly("jakarta.servlet:jakarta.servlet-api")

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
