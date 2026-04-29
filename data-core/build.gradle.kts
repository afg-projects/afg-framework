plugins {
    `java-library`
}

dependencies {
    // 依赖core模块
    api(project(":core"))

    // JSpecify 空安全注解
    api(libs.jspecify)

    // Spring Boot Starter (for ApplicationEvent)
    api(libs.spring.boot.starter)

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    // Test
    testImplementation(libs.bundles.testing)
    testImplementation(libs.spring.boot.starter.test)
}


