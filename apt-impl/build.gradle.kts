plugins {
    `java-library`
}

group = property("projectGroup").toString()
version = property("projectVersion").toString()

dependencies {
    // 依赖 commons 模块（通用工具）
    implementation(project(":commons"))

    // 依赖 apt-api
    implementation(project(":apt-api"))

    // Jakarta Persistence API（用于解析 @Table、@Column 等注解，版本由 Spring Boot BOM 管理）
    implementation(libs.jakarta.persistence.api)

    // Jackson（用于解析 JSON 配置文件，版本由 Spring Boot BOM 管理）
    implementation(libs.jackson.databind)

    // JavaPoet（用于生成 Java 源代码）
    implementation(libs.javapoet)

    // AutoService（自动生成 META-INF/services）
    implementation(libs.auto.service)
    annotationProcessor(libs.auto.service)

    // Test dependencies
    testImplementation(libs.bundles.testing)
    testImplementation(libs.compile.testing)

    // APT 测试需要 data-core 模块的类
    testImplementation(project(":data-core"))

    // APT 测试需要 core 模块的 ServiceMetadata 等接口
    testImplementation(project(":core"))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
