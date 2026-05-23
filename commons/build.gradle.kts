plugins {
    `java-library`
}

dependencies {
    // JSpecify 空安全注解
    api(libs.jspecify)

    // Jackson（用于 Result 的 JSON 注解）
    api(libs.bundles.jackson)

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // Test
    testImplementation(libs.bundles.testing)
}
