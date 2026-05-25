plugins {
    `java-library`
}

dependencies {
    // 依赖 ai-core 接口
    api(project(":ai-core"))
    api(project(":data-core"))

    // 依赖 ai-chat 实现（AfgChatClient、AiChatResponse、AiMessage）
    api(project(":ai-impl:ai-chat"))

    // 依赖 security-core（权限服务）
    api(project(":security-core"))

    // Spring JDBC（JdbcClient）
    implementation(libs.spring.jdbc)

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
