plugins {
    `java-library`
}

dependencies {
    // 依赖 ai-core 接口
    api(project(":ai-core"))

    // JSpecify 空安全注解
    api(libs.jspecify)

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    // Spring AI
    implementation(libs.spring.ai.openai)
    implementation("org.springframework.ai:spring-ai-pgvector-store:1.1.6")

    // 文档加载器依赖
    implementation("org.apache.pdfbox:pdfbox:3.0.1")
    implementation("org.commonmark:commonmark:0.22.0")
    implementation("org.commonmark:commonmark-ext-gfm-tables:0.22.0")
    implementation("com.github.albfernandez:juniversalchardet:2.4.0")

    // Test
    testImplementation(libs.bundles.testing)
    testImplementation(libs.spring.boot.starter.test)
}
