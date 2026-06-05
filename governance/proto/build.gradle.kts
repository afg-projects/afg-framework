plugins {
    `java-library`
    id("com.google.protobuf") version "0.9.4"
}

group = "io.github.afg-projects"
version = "1.0.0-SNAPSHOT"

// 覆盖 artifact ID，避免与根项目默认命名冲突
// 默认: afg-framework-proto → 改为: afg-framework-governance-proto
configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
    coordinates(
        groupId = "io.github.afg-projects",
        artifactId = "afg-framework-governance-proto",
        version = project.version.toString()
    )
}

dependencies {
    api("io.grpc:grpc-protobuf:1.81.0")
    api("io.grpc:grpc-stub:1.81.0")
    api("com.google.protobuf:protobuf-java:4.34.1")
    api("javax.annotation:javax.annotation-api:1.3.2")

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.2"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.62.2"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                create("grpc")
            }
        }
    }
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
