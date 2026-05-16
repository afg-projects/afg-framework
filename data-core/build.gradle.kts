plugins {
    `java-library`
}

dependencies {
    // 依赖 commons 模块（通用工具）
    api(project(":commons"))

    // 依赖 core 模块
    api(project(":core"))

    // JSpecify 空安全注解
    api(libs.jspecify)

    // Jakarta Persistence API (用于 @Table/@Column 注解支持)
    api("jakarta.persistence:jakarta.persistence-api")

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


