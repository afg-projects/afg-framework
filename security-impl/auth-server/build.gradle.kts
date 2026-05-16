plugins {
    `java-library`
}

dependencies {
    api(project(":security-core"))
    api(project(":data-core"))
    api(project(":data-impl:data-jdbc"))
    api(project(":apt-api"))
    api(libs.spring.boot.starter.web)
    api(libs.spring.boot.starter.oauth2.authorization.server)
    api(libs.jspecify)

    // Nimbus JOSE JWT（JWT 处理）
    api(libs.nimbus.jose.jwt)

    // JJWT（JWT 处理 - 另一种实现）
    api(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)

    api(libs.caffeine)
    implementation(libs.spring.jdbc)
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
