plugins {
    `java-library`
}

group = property("projectGroup").toString()
version = property("projectVersion").toString()

dependencies {
    // JSR-305 空安全注解
    api("org.jspecify:jspecify:1.0.0")

    // Spring Context (用于 @Configuration, @ComponentScan)
    compileOnly("org.springframework:spring-context:${rootProject.extra["springFrameworkVersion"]}")

    // Jakarta Persistence API（可选依赖，用于 @Table、@Column 等注解）
    compileOnly("jakarta.persistence:jakarta.persistence-api:3.2.0")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}