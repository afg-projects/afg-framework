plugins { `java-library` }

dependencies {
    // 依赖 ai-core 接口
    api(project(":ai-core"))
    // 依赖 apt-api（@AfEntity 注解）
    compileOnly(project(":apt-api"))

    // Spring Boot AutoConfiguration
    api(libs.spring.boot.autoconfigure)

    // JSpecify 空安全注解
    api(libs.jspecify)

    // Jakarta Persistence（@Table, @Column 等）
    compileOnly(libs.jakarta.persistence.api)

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    // Test
    testImplementation(libs.bundles.testing)
}
