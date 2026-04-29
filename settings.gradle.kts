rootProject.name = "afg-framework"

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        // Maven Central (优先，确保获取最新版本)
        mavenCentral()
        // 阿里云镜像（优先）
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        // 腾讯云镜像（备用）
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public") }
        // 华为云镜像（备用）
        maven { url = uri("https://repo.huaweicloud.com/repository/maven/public") }
    }
}

// Gradle 插件
include("gradle-plugin")

// 核心模块
include("core")

// 数据访问模块
include("data-core")
include("data-impl:data-sql")       // SQL 解析和构建
include("data-impl:data-jdbc")      // JDBC 实现
include("data-impl:data-liquibase") // Liquibase 集成（数据库迁移、逆向工程）

// Spring Boot Starter
include("spring-boot-starter")

// 集成模块（中间件集成）
include("integration:afg-redis")        // Redis 集成（缓存、分布式锁、延迟队列、任务调度）
include("integration:afg-jdbc")         // JDBC 集成（审计日志数据库存储）
include("integration:afg-kafka")        // Kafka 集成（事件发布）
include("integration:afg-nacos")        // Nacos 集成（配置中心）
include("integration:afg-apollo")       // Apollo 集成（配置中心）
include("integration:afg-consul")       // Consul 集成（配置中心）
include("integration:afg-rabbitmq")     // RabbitMQ 集成（事件发布）
include("integration:afg-websocket")    // WebSocket 集成（实时通信）
include("integration:afg-storage")      // Storage 集成（文件存储抽象层）
