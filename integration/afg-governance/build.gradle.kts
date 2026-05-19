plugins {
    `java-library`
    id("com.google.protobuf") version "0.9.4"
}

group = "io.github.afg-projects"
version = "1.0.0-SNAPSHOT"

dependencies {
    api(project(":core"))
    api("io.grpc:grpc-netty-shaded:1.81.0")
    api("io.grpc:grpc-protobuf:1.81.0")
    api("io.grpc:grpc-stub:1.81.0")
    api("com.google.protobuf:protobuf-java:4.34.1")
    api("javax.annotation:javax.annotation-api:1.3.2")

    compileOnly(libs.spring.boot.starter)
    compileOnly(libs.spring.boot.autoconfigure)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // 配置元数据处理器 - 使用 annotationProcessor 确保 APT 生效
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:${libs.versions.spring.boot.get()}")

    testImplementation(libs.bundles.testing)
    testImplementation(libs.spring.boot.starter.test)
}

// 确保 annotationProcessor 在编译前运行
tasks.named("compileJava") {
    dependsOn("processResources")
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
