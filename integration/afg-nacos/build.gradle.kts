plugins {
    `java-library`
}

dependencies {
    // 依赖 core 模块
    api(project(":core"))

    // 依赖 ai-core（工具发现接口）
    compileOnly(project(":ai-core"))

    // Spring Boot Autoconfigure
    compileOnly(libs.spring.boot.starter)
    compileOnly(libs.spring.boot.autoconfigure)

    // Nacos
    api(libs.nacos.client)

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // Test
    testImplementation(libs.bundles.testing)
    testImplementation(libs.spring.boot.starter.test)
}


