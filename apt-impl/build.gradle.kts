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

    // Jakarta Persistence API（用于解析 @Table、@Column 等注解）
    implementation("jakarta.persistence:jakarta.persistence-api:3.2.0")

    // Jackson（用于解析 JSON 配置文件）
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")

    // AutoService（自动生成 META-INF/services）
    implementation("com.google.auto.service:auto-service:1.1.1")
    annotationProcessor("com.google.auto.service:auto-service:1.1.1")

    // Test dependencies
    testImplementation(libs.bundles.testing)
    testImplementation(libs.compile.testing)

    // APT 测试需要 data-core 模块的类
    testImplementation(project(":data-core"))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
