plugins {
    `java-library`
}

group = "io.github.afg-projects"
version = "1.0.0-SNAPSHOT"

dependencies {
    api(project(":governance:proto"))
    api(project(":core"))
    api("io.grpc:grpc-netty-shaded:1.81.0")

    compileOnly(libs.spring.boot.starter)
    compileOnly(libs.spring.boot.autoconfigure)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:${libs.versions.spring.boot.get()}")

    testImplementation(libs.bundles.testing)
    testImplementation(libs.spring.boot.starter.test)
}

// 覆盖 artifact ID，避免与根项目默认命名冲突
// 默认: afg-framework-client → 改为: afg-framework-governance-client
configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
    coordinates(
        groupId = "io.github.afg-projects",
        artifactId = "afg-framework-governance-client",
        version = project.version.toString()
    )
}

tasks.named("compileJava") {
    dependsOn("processResources")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

// 排除 Protobuf 生成代码的 PMD 检查
tasks.named("pmdMain") {
    doFirst {
        val pmdTask = this as org.gradle.api.plugins.quality.Pmd
        pmdTask.setSource(pmdTask.source.filter { file ->
            !file.absolutePath.contains("generated")
        }.asFileTree)
    }
}
