plugins {
    `java-library`
}

dependencies {
    api(project(":core"))
    api(libs.jcasbin)
    api(libs.jspecify)

    // Spring Boot (用于自动配置)
    compileOnly(libs.spring.boot.autoconfigure)
    compileOnly(libs.spring.context)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
    testImplementation(libs.bundles.testing)
    testImplementation(libs.spring.boot.starter.test)
}
