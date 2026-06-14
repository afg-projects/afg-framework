plugins {
    `java-library`
    jacoco
}

dependencies {
    api(project(":data-core"))

    // Liquibase Core（版本由 Spring Boot BOM 管理）
    api(libs.liquibase.core)

    // ClassGraph（类路径扫描）
    implementation(libs.classgraph)

    // Test（版本由 Spring Boot BOM 管理）
    testImplementation(libs.bundles.testing)
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-jdbc")
    // Testcontainers PostgreSQL（替代 H2，确保测试与生产环境一致）
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
    testRuntimeOnly("org.postgresql:postgresql:42.7.5")
}
