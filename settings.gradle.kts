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

// 通用工具模块（最底层，无依赖）
include("commons")

// APT 注解和处理器（统一模块）
include("apt-api")
include("apt-impl")

// 核心模块
include("core")

// AI 模块
include("ai-core")
include("ai-impl:ai-llm")           // LLM 实现（OpenAI, Anthropic, Ollama）
include("ai-impl:ai-agent")         // Agent 实现（ToolRegistry, ReAct, Plan-Execute）
include("ai-impl:ai-rag")           // RAG 实现（VectorStore, Embedding, Retriever）
include("ai-impl:ai-resilience")    // 韧性实现（重试、熔断、降级）
include("ai-impl:ai-observability")  // 可观测性实现（指标、追踪、审计）
include("ai-impl:ai-security")      // 安全实现（API Key 管理、内容安全、PII 检测）
include("ai-impl:ai-persistence")   // 持久化实现（会话存储、消息历史）
include("ai-impl:ai-performance")   // 性能优化实现（缓存、速率限制）
include("ai-spring-boot-starter")   // Spring Boot 自动配置

// 数据访问模块
include("data-core")
include("data-impl:data-sql")       // SQL 解析和构建
include("data-impl:data-jdbc")      // JDBC 实现
include("data-impl:data-liquibase") // Liquibase 集成（数据库迁移、逆向工程）

// 安全模块
include("security-core")
include("security-impl:auth-server")       // OAuth2 授权服务器
include("security-impl:resource-server")   // 资源服务器
include("security-impl:security-casbin")   // Casbin 权限集成
include("security-impl:security-permission")  // 安全权限模块（RBAC + Casbin）
include("security-impl:security-data-scope")  // 数据权限模块

// Spring Boot Starter
include("spring-boot-starter")

// 集成模块（中间件集成）
include("integration:afg-redis")        // Redis 集成（缓存、分布式锁、延迟队列、任务调度）
include("integration:afg-jdbc")         // JDBC 集成（审计日志数据库存储）
include("integration:afg-rabbitmq")     // RabbitMQ 集成（事件发布）
include("integration:afg-websocket")    // WebSocket 集成（实时通信）
include("integration:afg-storage")      // Storage 集成（文件存储抽象层）
include("integration:afg-governance")   // Governance 集成（配置中心、服务注册）
