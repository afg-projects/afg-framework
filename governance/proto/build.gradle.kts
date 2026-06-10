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
    api(libs.grpc.protobuf)
    api(libs.grpc.stub)
    api(libs.protobuf.java)
    api(libs.javax.annotation.api)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.8"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.81.0"
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
