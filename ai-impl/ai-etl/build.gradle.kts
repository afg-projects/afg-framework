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

    // Spring AI（可选，用于 SpringAiLlmExecutor）
    compileOnly(libs.spring.ai.openai)

    // Spring Batch（可选，用于批处理）
    compileOnly("org.springframework.batch:spring-batch-core:6.0.0")

    // PDF 解析
    implementation("org.apache.pdfbox:pdfbox:3.0.1")

    // Markdown 解析
    implementation("org.commonmark:commonmark:0.22.0")
    implementation("org.commonmark:commonmark-ext-gfm-tables:0.22.0")

    // 编码检测
    implementation("com.github.albfernandez:juniversalchardet:2.4.0")

    // Apache Commons Codec（用于 MD5）
    implementation("commons-codec:commons-codec:1.16.0")

    // Test
    testImplementation(libs.bundles.testing)
    testImplementation(libs.spring.boot.starter.test)
}