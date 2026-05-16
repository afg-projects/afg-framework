plugins {
    `java-library`
}

dependencies {
    api(project(":security-core"))
    api("org.casbin:jcasbin:1.55.0")
    api(libs.jspecify)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
    testImplementation(libs.bundles.testing)
    testImplementation(libs.spring.boot.starter.test)
}
