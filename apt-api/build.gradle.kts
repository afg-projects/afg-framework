plugins {
    `java-library`
}

group = property("projectGroup").toString()
version = property("projectVersion").toString()

dependencies {
    // JSR-305 空安全注解（版本由 Spring Boot BOM 管理）
    api(libs.jspecify)

    // Spring Context (用于 @Configuration, @ComponentScan，版本由 Spring Boot BOM 管理)
    compileOnly(libs.spring.context)

    // Jakarta Persistence API（可选依赖，用于 @Table、@Column 等注解，版本由 Spring Boot BOM 管理）
    compileOnly(libs.jakarta.persistence.api)
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}