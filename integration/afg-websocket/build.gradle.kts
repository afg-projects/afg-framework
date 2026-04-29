plugins {
    `java-library`
}

dependencies {
    // 依赖 core 模块
    api(project(":core"))

    // Spring WebSocket (包含 spring-messaging)
    api(libs.spring.boot.starter.websocket)

    // Spring Security (用于 WebSocket 认证)
    compileOnly(libs.spring.boot.starter.security)

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // Test
    testImplementation(libs.bundles.testing)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.websocket)
    testImplementation(libs.spring.boot.starter.security)
}


