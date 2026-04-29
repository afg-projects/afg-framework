plugins {
    `java-library`
}

dependencies {
    // 依赖 data-core
    api(project(":data-core"))

    // JSqlParser - 轻量级 SQL 解析器
    api(libs.jsqlparser)

    // JSpecify 空安全注解
    api(libs.jspecify)

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // Test
    testImplementation(libs.bundles.testing)

    // JMH (性能基准测试)
    testImplementation(libs.jmh.core)
    testAnnotationProcessor(libs.jmh.generator.annprocess)
}


