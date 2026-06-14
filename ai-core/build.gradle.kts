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

    // AspectJ (for AOP aspects: AiChatAspect, ContentSafetyAspect, AiAuditedAspect, ToolExecutionAspect)
    implementation(libs.aspectj.weaver)

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

    // APT 实现 (for annotation processing to generate module index and entity metadata)
    annotationProcessor(project(":apt-impl"))

    // PDF 解析 (ETL)
    implementation(libs.pdfbox)

    // Markdown 解析 (ETL)
    implementation(libs.commonmark)
    implementation(libs.commonmark.ext.gfm.tables)

    // 编码检测 (ETL)
    implementation(libs.juniversalchardet)

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    // Test
    testImplementation(libs.bundles.testing)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.webflux)  // SSE/WebClient testing
    testImplementation(libs.spring.boot.restclient)  // RestClient.Builder for web tests
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.mysql)
    testImplementation(libs.mysql.connector)
    testImplementation(project(":data-impl:data-liquibase"))
    testImplementation(project(":data-impl:data-jdbc"))
}
