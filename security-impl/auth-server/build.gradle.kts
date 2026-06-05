plugins {
    `java-library`
}

dependencies {
    // 基础依赖
    api(project(":security-core"))
    api(project(":data-core"))
    api(project(":apt-api"))
    api(project(":data-impl:data-jdbc"))

    api(libs.spring.boot.starter.web)
    api(libs.jspecify)

    // Spring Security Web（用于 SecurityFilterChain 配置）
    api(libs.spring.security.web)
    api(libs.spring.security.config)
    api(libs.spring.boot.starter.security)

    // Nimbus JOSE JWT（JWT 处理）
    api(libs.nimbus.jose.jwt)

    // JJWT（JWT 处理 - 另一种实现）
    api(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)

    api(libs.caffeine)
    implementation(libs.spring.jdbc)

    // Casbin（权限管理）
    api(libs.jcasbin)

    // APT 处理器（编译时生成元数据）
    annotationProcessor(project(":apt-impl"))
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
    testImplementation(libs.bundles.testing)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.h2)

    // Jakarta Persistence API for @Table annotation（版本由 Spring Boot BOM 管理）
    implementation(libs.jakarta.persistence.api)
}
