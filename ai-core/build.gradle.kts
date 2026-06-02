plugins {
    `java-library`
}

dependencies {
    // 依赖 core 模块
    api(project(":core"))
    api(project(":data-core"))

    // JSpecify 空安全注解
    api(libs.jspecify)

    // Reactor Core (响应式编程，用于流式响应)
    api(libs.reactor.core)

    // Spring Boot Starter (for ApplicationEvent)
    api(libs.spring.boot.starter)

    // Spring Web (for HttpHeaders)
    api(libs.spring.boot.starter.web)

    // Spring Boot Autoconfigure (for @ConfigurationProperties)
    api(libs.spring.boot.autoconfigure)

    // Spring Boot Actuator (for AiHealthEndpoint, compileOnly as it's optional)
    compileOnly(libs.spring.boot.starter.actuator)

    // Spring JDBC (for JdbcToolAuditLogger, compileOnly as it's optional)
    compileOnly(libs.spring.boot.starter.data.jdbc)

    // Spring Security Core (for CasbinToolPermissionChecker, compileOnly as it's optional)
    compileOnly(project(":security-core"))

    // Jakarta Validation (for PiiCheckRequest @NotBlank, compileOnly as it's optional)
    compileOnly(libs.jakarta.validation.api)

    // JPA (for RAG entities, compileOnly as it's optional)
    compileOnly(libs.jakarta.persistence.api)

    // APT API (for @AfEntity on RAG entities, compileOnly as it's optional)
    compileOnly(project(":apt-api"))

    // PDF 解析 (ETL)
    implementation("org.apache.pdfbox:pdfbox:3.0.1")

    // Markdown 解析 (ETL)
    implementation("org.commonmark:commonmark:0.22.0")
    implementation("org.commonmark:commonmark-ext-gfm-tables:0.22.0")

    // 编码检测 (ETL)
    implementation("com.github.albfernandez:juniversalchardet:2.4.0")

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    // Test
    testImplementation(libs.bundles.testing)
    testImplementation(libs.spring.boot.starter.test)
}
