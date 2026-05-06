plugins {
    `java-library`
}

dependencies {
    // JSR-305 空安全注解
    api("org.jspecify:jspecify:1.0.0")

    // Spring Context (用于 @Configuration, @ComponentScan)
    compileOnly("org.springframework:spring-context:7.0.6")
}

// 确保先编译此模块
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
