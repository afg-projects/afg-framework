plugins {
    `java-library`
}

group = property("projectGroup").toString()
version = property("projectVersion").toString()

dependencies {
    // 依赖 apt-api
    implementation(project(":apt-api"))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
