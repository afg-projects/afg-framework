plugins {
    `java-library`
}

dependencies {
    api(project(":security-core"))
    api(libs.spring.boot.starter.oauth2.authorization.server)
    api(libs.jspecify)
    api("com.nimbusds:nimbus-jose-jwt:9.37.3")
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
    testImplementation(libs.bundles.testing)
    testImplementation(libs.spring.boot.starter.test)
}
