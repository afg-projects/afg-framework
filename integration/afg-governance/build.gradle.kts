plugins {
    `java-library`
    id("com.google.protobuf") version "0.9.4"
}

group = "io.github.afg-projects"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    api("io.grpc:grpc-netty-shaded:1.62.0")
    api("io.grpc:grpc-protobuf:1.62.0")
    api("io.grpc:grpc-stub:1.62.0")
    api("com.google.protobuf:protobuf-java:3.25.2")
    api("javax.annotation:javax.annotation-api:1.3.2")

    compileOnly(libs.spring.boot.starter)
    compileOnly(libs.spring.boot.autoconfigure)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(libs.bundles.testing)
    testImplementation(libs.spring.boot.starter.test)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.2"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.62.0"
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
