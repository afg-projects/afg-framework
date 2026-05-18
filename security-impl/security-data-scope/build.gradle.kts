plugins {
    `java-library`
}

dependencies {
    api(project(":security-core"))
    api(project(":data-core"))
    api(project(":data-impl:data-jdbc"))
    api(project(":apt-api"))

    api(libs.jspecify)
    api(libs.spring.boot.starter.web)

    implementation(libs.spring.jdbc)

    // APT 处理器
    annotationProcessor(project(":apt-impl"))
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    testImplementation(libs.bundles.testing)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.h2)

    implementation(libs.jakarta.persistence.api)
}
