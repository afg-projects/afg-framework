plugins {
    `java-library`
    jacoco
}

dependencies {
    api(project(":data-core"))
    api("org.liquibase:liquibase-core:4.26.0")
    implementation("io.github.classgraph:classgraph:4.8.165")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("com.h2database:h2:2.3.232")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}


