plugins {
    `java-library`
}

group = "io.github.afg-projects"
version = "1.0.0-SNAPSHOT"

dependencies {
    api(project(":governance:proto"))
    implementation(project(":core"))
    implementation(project(":data-impl:data-jdbc"))
    implementation(project(":data-impl:data-liquibase"))
    implementation(project(":security-impl:auth-server"))
    // spring-grpc 自动包含 grpc-netty，显式声明确保使用框架版本 1.81.0（高于 spring-grpc 默认的 1.77.1）
    implementation(libs.grpc.netty)
    implementation(libs.spring.grpc.server.spring.boot.starter)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.jackson.yaml)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:${libs.versions.spring.boot.get()}")
    annotationProcessor(project(":apt-impl"))
    compileOnly(project(":apt-api"))
    testImplementation(libs.bundles.testing)
    testImplementation(libs.spring.boot.starter.test)
}

// 覆盖 artifact ID，避免与根项目默认命名冲突
// 默认: afg-framework-server → 改为: afg-framework-governance-server
configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
    coordinates(
        groupId = "io.github.afg-projects",
        artifactId = "afg-framework-governance-server",
        version = project.version.toString()
    )
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.named("compileJava") {
    dependsOn("processResources")
}
