plugins {
    `java-library`
}

dependencies {
    // 依赖 ai-core 接口
    api(project(":ai-core"))

    // LangChain4j 核心
    api(libs.langchain4j)

    // LangChain4j LLM 提供者（可选，用户根据需要引入）
    // 注意：langchain4j-open-ai 是唯一有 1.0.0 正式版的提供者模块
    // Anthropic 和 Ollama 模块目前只有 beta 版本，用户需自行添加依赖
    compileOnly(libs.langchain4j.open.ai)

    // JSpecify 空安全注解
    api(libs.jspecify)

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    // Test
    testImplementation(libs.bundles.testing)
}