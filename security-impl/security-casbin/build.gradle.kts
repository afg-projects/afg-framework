plugins {
    `java-library`
}

dependencies {
    api(project(":security-core"))

    // Casbin（权限管理）
    api(libs.jcasbin)

    api(libs.jspecify)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
    testImplementation(libs.bundles.testing)
    testImplementation(libs.spring.boot.starter.test)
}
