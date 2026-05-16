plugins {
    `java-library`
}

dependencies {
    api(project(":security-core"))
    api(project(":data-core"))
    api(project(":data-impl:data-jdbc"))
    api(libs.spring.boot.starter.oauth2.authorization.server)
    api(libs.jspecify)
    api("com.nimbusds:nimbus-jose-jwt:9.37.3")
    api(libs.caffeine)
    implementation(libs.spring.jdbc)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
    testImplementation(libs.bundles.testing)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.h2)
    // Jakarta Persistence API for @Table annotation
    implementation("jakarta.persistence:jakarta.persistence-api:3.2.0")
}
