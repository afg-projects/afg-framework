plugins {
    `java-library`
}

dependencies {
    // 依赖 module-api
    implementation(project(":module-api"))

    // Test
    testImplementation(libs.bundles.testing)
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
