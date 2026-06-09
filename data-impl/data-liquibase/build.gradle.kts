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
    testImplementation(libs.h2)
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-jdbc")
}


