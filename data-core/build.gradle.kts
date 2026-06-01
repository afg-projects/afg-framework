plugins {
    `java-library`
}

dependencies {
    // 依赖 commons 模块（通用工具）
    api(project(":commons"))

    // 依赖 core 模块
    api(project(":core"))

    // JSpecify 空安全注解（版本由 Spring Boot BOM 管理）
    api(libs.jspecify)

    // Jakarta Persistence API (用于 @Table/@Column 注解支持，版本由 Spring Boot BOM 管理)
    api(libs.jakarta.persistence.api)

    // Spring Boot Starter (for ApplicationEvent，版本由 Spring Boot BOM 管理)
    api(libs.spring.boot.starter)

    // Spring TX (for TransactionAdapter/PlatformTransactionManager)
    api(libs.spring.tx)

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    // Test
    testImplementation(libs.bundles.testing)
    testImplementation(libs.spring.boot.starter.test)
}


