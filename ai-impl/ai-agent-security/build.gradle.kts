plugins {
    `java-library`
}

dependencies {
    // 依赖 ai-core 接口
    api(project(":ai-core"))

    // 依赖 security-core（权限服务）
    api(project(":security-core"))

    // 依赖 ai-agent 实现（ToolContextProvider, ToolPermissionChecker 等）
    api(project(":ai-impl:ai-agent"))

    // Spring Boot AutoConfiguration
    api(libs.spring.boot.autoconfigure)

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
}
