plugins {
    `java-library`
}

dependencies {
    // ===== 核心模块（必须依赖）=====
    api(project(":core"))

    // ===== 数据访问模块（推荐依赖）=====
    api(project(":data-core"))
    api(project(":data-impl:data-sql"))
    api(project(":data-impl:data-jdbc"))

    // ===== 集成模块（可选依赖）=====
    // 用户按需引入，这些模块通过各自的 AutoConfiguration.imports 注册
    // Spring Boot 的 @ConditionalOnClass 机制会在类路径存在时自动激活配置

    // Redis 集成（缓存、分布式锁、延迟队列、任务调度）
    compileOnly(project(":integration:afg-redis"))

    // Kafka 集成（事件发布）
    compileOnly(project(":integration:afg-kafka"))

    // RabbitMQ 集成（事件发布）
    compileOnly(project(":integration:afg-rabbitmq"))

    // Nacos 集成（配置中心）
    compileOnly(project(":integration:afg-nacos"))

    // Apollo 集成（配置中心）
    compileOnly(project(":integration:afg-apollo"))

    // Consul 集成（配置中心、服务发现）
    compileOnly(project(":integration:afg-consul"))

    // JDBC 集成（审计日志数据库存储）
    compileOnly(project(":integration:afg-jdbc"))

    // SpringDoc OpenAPI (API 文档)
    api(libs.springdoc.openapi.starter.webmvc)

    // Spring Boot (用于 @AutoConfiguration 注解)
    compileOnly(libs.spring.boot.starter)
    compileOnly(libs.spring.boot.starter.web)

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // Test
    testImplementation(libs.bundles.testing)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.web)
}


