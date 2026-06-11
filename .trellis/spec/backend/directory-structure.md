# 模块目录结构

> **权威来源**：[`docs/framework-prd.md`](../../docs/framework-prd.md) §4.1 / §4.2 / §4.3 / §4.4 / §6.1

---

## 模块清单与目录结构

> 来源：PRD §4.1

框架由 **20 个 Gradle 子模块**组成，按职责分为 7 大类：

```
afg-framework/
├── 通用层
│   ├── commons/                    # 通用工具（最底层，零框架依赖）
│   ├── apt-api/                    # APT 注解定义（@AfEntity, @AfService 等）
│   └── apt-impl/                   # APT 处理器实现（编译时元数据生成）
│
├── 核心层
│   └── core/                       # 框架核心（31+ AutoConfiguration：缓存、锁、事件、调度、审计、国际化等）
│
├── 数据层
│   ├── data-core/                  # 数据访问抽象（DataManager 接口、实体基类、条件构建器）
│   └── data-impl/
│       ├── data-sql/               # SQL 解析与构建器（SqlQueryBuilder / SqlInsertBuilder 等）
│       ├── data-jdbc/              # JDBC 增强实现（JdbcDataManager + 审计日志存储）
│       └── data-liquibase/         # Liquibase 数据库迁移基础设施
│
├── 安全层
│   ├── security-core/              # 安全 SPI 接口（认证、授权、租户、权限等抽象）
│   └── security-impl/
│       ├── auth-server/            # 认证服务器（9 AutoConfiguration：OAuth2 + Casbin + 多租户 + 数据权限）
│       └── resource-server/        # 资源服务器（2 AutoConfiguration：JWT 验证 + 远程权限校验）
│
├── AI 层
│   ├── ai-core/                    # AI 核心（16 AutoConfiguration：Chat/Agent/Workflow/RAG/Tool/Skill）
│   └── ai-impl/
│       ├── ai-spring-ai/           # Spring AI 适配（7 AutoConfiguration）
│       └── ai-langchain4j/         # LangChain4J 适配（7 AutoConfiguration）
│
├── 集成层
│   └── afg-redis/                  # Redis/Redisson（缓存、锁、限流、调度、审计存储）
│
├── 治理层
│   └── governance/
│       ├── proto/                  # gRPC Proto 定义（注册中心、配置中心协议）
│       ├── client/                 # 客户端 SDK（gRPC Client + Spring Boot AutoConfiguration）
│       └── server/                 # 服务端模块（gRPC Service + REST API + Liquibase 迁移）
│
└── 工具层
    └── gradle-plugin/              # 自定义 Gradle 插件（项目脚手架、代码生成、迁移工具）
```

---

## 依赖关系图

> 来源：PRD §4.2

```
commons（零依赖）
  └→ data-core
  │    └→ data-sql（SQL 构建器实现）
  │    └→ data-jdbc ←── core（JdbcDataManager + 审计日志存储）
  │         └→ data-liquibase（数据库迁移）
  └→ core
  │    └→ ai-core → ai-spring-ai / ai-langchain4j（AI 适配）
  └→ security-core
       └→ auth-server ←── data-jdbc + core
       └→ resource-server ←── data-jdbc + core
governance/proto（零框架依赖）
  └→ governance/client ←── proto + core
       └→ governance/server ←── client + core + data-jdbc + auth-server + data-liquibase
afg-redis ←── core（Redis/Redisson 实现 core 定义的分布式 SPI）
```

**依赖规则**：

- 依赖方向：从底层到上层，**禁止反向依赖和循环依赖**
- `commons` 和 `governance/proto` 是零依赖模块，不依赖框架内任何其他模块
- 集成模块（afg-redis）依赖 core 定义的 SPI 接口，不依赖其他集成模块
- `core` 依赖 `commons`，但不依赖 `data-core`
- `data-jdbc` 同时依赖 `data-core` 和 `core`（汇聚点）
- `FullEntity` 不继承 `TenantEntity`，需要多租户 + 全功能时需手动添加 `tenantId` 字段

---

## Maven 坐标

> 来源：PRD §4.3

- **Group ID**：`io.github.afg-projects`
- **Artifact ID**：`afg-framework-{module-name}`
- **版本**：`1.0.0-SNAPSHOT`
- **发布目标**：Maven Central（通过 vanniktech.maven.publish 插件自动签名 + 发布）

| 分类 | 模块 | Artifact ID |
|------|------|------------|
| 通用层 | 通用工具 | `afg-framework-commons` |
| 通用层 | APT 注解 | `afg-framework-apt-api` |
| 通用层 | APT 实现 | `afg-framework-apt-impl` |
| 核心层 | 核心 | `afg-framework-core` |
| 数据层 | 数据抽象 | `afg-framework-data-core` |
| 数据层 | SQL 构建 | `afg-framework-data-sql` |
| 数据层 | JDBC 实现 | `afg-framework-data-jdbc` |
| 数据层 | 数据库迁移 | `afg-framework-data-liquibase` |
| 安全层 | 安全 SPI | `afg-framework-security-core` |
| 安全层 | 认证服务器 | `afg-framework-auth-server` |
| 安全层 | 资源服务器 | `afg-framework-resource-server` |
| AI 层 | AI 核心 | `afg-framework-ai-core` |
| AI 层 | Spring AI 适配 | `afg-framework-ai-spring-ai` |
| AI 层 | LangChain4J 适配 | `afg-framework-ai-langchain4j` |
| 集成层 | Redis 集成 | `afg-framework-afg-redis` |
| 治理层 | 治理 Proto | `afg-framework-governance-proto` |
| 治理层 | 治理客户端 | `afg-framework-governance-client` |
| 治理层 | 治理服务端 | `afg-framework-governance-server` |
| 工具层 | Gradle 插件 | `afg-framework-gradle-plugin` |

---

## 模块内包路径

每个模块遵循 `io.github.afgprojects.framework.{module}.{subpackage}` 的包路径约定：

### commons

```
io.github.afgprojects.framework.commons
├── model/           # Result<T>, PageData<T>
├── exception/       # AfgException, BusinessException, ErrorCode, CommonErrorCode, ErrorCategory
├── util/            # NamingUtils, DateUtils, CollectionUtils, StringUtils, IoUtils
└── assert/          # ArgumentAssert
```

### apt-api

```
io.github.afgprojects.framework.apt
├── entity/          # @AfEntity, @CommonFieldDefinition, @CommonFieldDefinitions
├── service/         # @AfService, @AfOperation, @AfParam, @AfResult
├── module/          # @AfgModuleAnnotation
├── enum_/           # @AfgEnum
└── crypto/          # @EncryptedField
```

### apt-impl

```
io.github.afgprojects.framework.apt.processor
├── EntityMetadataProcessor        # → {Entity}Metadata.java
├── ServiceMetadataProcessor       # → {Service}Metadata.java
├── AfgModuleAnnotationProcessor   # → META-INF/afg/modules/{module}
└── EnumMetadataProcessor          # → {Enum}Metadata.java
```

### core

```
io.github.afgprojects.framework.core
├── api.cache/           # AfgCache<V>, CacheManager
├── api.lock/            # DistributedLock
├── api.event/           # EventPublisher<T>, EventSubscriber<T>
├── api.scheduler/       # DistributedTaskScheduler, DelayQueue
├── api.ratelimit/       # RateLimitStorage
├── api.audit/           # AuditLogStorage
├── api.feature/         # FeatureFlagManager
├── api.notification/    # NotificationService, NotificationChannel
├── api.webhook/         # WebhookService, WebhookRepository
├── api.statemachine/    # StateMachine<T, S>, StateMachineFactory
├── api.id/              # IdGenerator
├── api.sse/             # SseConnectionManager
├── api.importexport/    # DataExporter, DataImporter, FormatHandler
├── api.encryption/      # FieldEncryptor
├── cache/               # DefaultCacheManager (Caffeine)
├── lock/                # 内存锁实现
├── event/               # LocalEventPublisher
├── scheduler/           # LocalTaskScheduler
├── module/              # @AfgModuleAnnotation 注册与发现
├── audit/               # 审计日志框架
├── ratelimit/           # @RateLimited
├── feature/             # @FeatureToggle
├── i18n/                # LocaleFilter
├── validation/          # 统一校验异常处理
├── importexport/        # Excel (EasyExcel) + CSV
├── statemachine/        # @StateMachine
├── enummgmt/            # @AfgEnum
├── notification/        # LogNotificationService
├── webhook/             # 内存注册 + HTTP 分发
├── duplicatesubmit/     # @DuplicateSubmit
├── apiversion/          # @ApiVersion
├── id/                  # SnowflakeIdGenerator
└── autoconfigure/       # 31+ AutoConfiguration 类
```

### data-core

```
io.github.afgprojects.framework.data.core
├── DataManager                  # 统一数据操作门面接口
├── EntityProxy<T>               # 实体操作代理
├── EntityQuery<T>               # 链式查询
├── ProjectedQuery<T, R>         # DTO 投影查询
├── condition/                   # Condition, Conditions.builder()
├── entity/                      # BaseEntity, TenantEntity, SoftDeleteEntity, VersionedEntity, FullEntity, TreeEntity
├── relation/                    # @ManyToOne, @OneToMany, @OneToOne, @ManyToMany
├── query/                       # BaseQuery, PageRequest, AggregateQuery
├── dialect/                     # DatabaseDialect 接口 + 10 种方言实现
├── type/                        # 16+ TypeHandler（BigDecimal, Instant, JSON, Enum 等）
└── autoconfigure/               # TenantContextAutoConfiguration, TransactionAutoConfiguration
```

### data-sql

```
io.github.afgprojects.framework.data.sql
├── SqlQueryBuilder              # SELECT 查询构建（CTE / JOIN / 子查询 / 窗口函数）
├── SqlInsertBuilder             # INSERT 构建（批量 / ON DUPLICATE KEY UPDATE）
├── SqlUpdateBuilder             # UPDATE 构建
├── SqlDeleteBuilder             # DELETE 构建
├── WindowFunctionBuilder        # 窗口函数构建
└── SqlRewriteContext            # SQL 重写上下文（租户隔离 / 数据权限注入）
```

### data-jdbc

```
io.github.afgprojects.framework.data.jdbc
├── JdbcDataManager              # DataManager 的 JDBC 实现
├── JdbcEntityProxy              # EntityProxy 的 JDBC 实现
├── EntityMetadataCache          # 实体元数据缓存
├── AptMetadataLoader            # APT 元数据加载（优先）
├── ReflectiveMetadataLoader     # 反射元数据加载（降级）
├── audit/                       # JdbcAuditLogStorage
└── autoconfigure/               # DataManagerAutoConfiguration, EntityCacheAutoConfiguration, SqlMetricsAutoConfiguration
```

### data-liquibase

```
io.github.afgprojects.framework.data.liquibase
├── SpringLiquibase              # Liquibase 集成
├── SchemaComparator             # 实体 vs 数据库 vs 基线三方差异对比
└── autoconfigure/               # LiquibaseAutoConfiguration
```

### security-core

```
io.github.afgprojects.framework.security.core
├── authentication/    # AfgAuthentication, AfgUserDetails, AfgUserDetailsService
├── authorization/     # AfgEnforcer, AfgSecurityContext
├── login/             # LoginService, TokenService, CaptchaService, LoginStrategyFactory
├── oauth2/            # OAuth2AuthorizationService, OAuth2ClientService, AuthorizationCodeStorage
├── permission/        # PermissionService, RbacService, AbacService, DataScopeService
├── tenant/            # TenantResolver, TenantResolverChain, HeaderTenantResolver, AfgTenantService
└── security/          # LoginFailureTracker, DeviceLimiter, PasswordValidator, IpRestrictionChecker
```

### security-impl/auth-server

```
io.github.afgprojects.framework.security.auth
├── autoconfigure/     # 9 个 AutoConfiguration
├── controller/        # LoginController, OAuth2Controller
├── login/             # 7 种 LoginStrategy（UsernamePassword / Mobile / Email / Wechat / DingTalk / Feishu / WeCom）
├── token/             # JWT Token 签发与验证
├── oauth2/            # OAuth2 授权服务器实现
├── casbin/            # jCasbin RBAC 策略引擎集成
├── permission/        # @RequirePermission, @RequireRole
├── tenant/            # 多租户（3 种隔离模式 + 解析器链 + 过滤器）
├── datascope/         # 数据权限（5 种 DataScopeType + 自动条件注入）
├── security/          # 登录锁定 + 设备限制 + 密码校验 + IP 限制
├── audit/             # 安全审计（登录日志 + 安全事件 + 告警）
└── entity/            # 安全相关实体
```

### security-impl/resource-server

```
io.github.afgprojects.framework.security.resource
├── autoconfigure/     # ResourceServerAutoConfiguration, DefaultSecurityAutoConfiguration
├── jwt/               # JWT Token 验证
├── permission/        # 远程权限校验
└── tenant/            # 租户解析（Token / Header）
```

### ai-core

```
io.github.afgprojects.framework.ai.core
├── api.agent/                  # Agent, AgentExecutor, AgentRequest, AgentResponse
├── api.chat/                   # AfgChatClient, AfgEmbeddingClient, ChatClientRegistry, EmbeddingClientRegistry
├── api.etl/                    # EtlPipeline, EtlPipelineBuilder, Source, Transformer
├── api.memory/                 # ConversationMemory
├── api.model/                  # ModelInfo, ModelRegistry, ModelType
├── api.multiagent/             # Coordinator, Orchestrator, AgentWorkflow
├── api.multiagent.communication/ # AgentMessage, CommunicationBus
├── api.multiagent.decomposition/ # TaskDecomposer, SubTask
├── api.multiagent.human/       # HumanInteraction, HumanDecision
├── api.multiagent.node/        # AgentNode, HumanNode, ParallelNode, RouterNode
├── api.observability/          # AuditLogger, MetricsCollector, Tracer
├── api.performance/            # Cache, RateLimiter
├── api.persistence/            # SessionStore, MessageHistoryStore
├── api.pipeline/               # ChatPipeline, ApplicationConfig, SearchMode
├── api.planning/               # ReActExecutor, PlanExecuteExecutor, ReflectionExecutor
├── api.rag/                    # VectorStore, EmbeddingService, KnowledgeBaseService
├── api.resilience/             # CircuitBreaker, RetryPolicy, ResilienceExecutor
├── api.security/               # ApiKeyManager, ContentSafetyChecker, PiiDetector
├── api.tool/                   # Tool, SecureTool, ToolRegistry
├── api.tool.remote/            # ToolDiscoveryClient, ToolEndpoint
├── api.workflow/               # DagEngine, WorkflowNode, ExecutionContext
├── api.workflow.checkpoint/    # CheckpointManager
├── api.workflow.definition/    # WorkflowDefinition, NodeDefinition, EdgeDefinition
├── api.workflow.dsl/           # DslConverter, DslValidator, VariableResolver
├── chat/                       # @AiChat AOP 实现
├── agent/                      # @AiAgent AOP 实现
├── model/                      # DefaultChatClientRegistry, InMemoryModelRegistry
├── tool/                       # ToolRegistry 默认实现
├── skill/                      # @Skill 路由
├── rag/                        # NoOpVectorStore
├── workflow/                   # 37 种内置节点 + DAG 引擎
├── pipeline/                   # ChatPipeline 默认实现
├── persistence/                # DefaultSessionStore
├── resilience/                 # DefaultCircuitBreaker, DefaultRetryPolicy
├── performance/                # DefaultCache (Caffeine)
├── security/                   # ContentSafetyChecker, PiiDetector
├── observability/              # AuditLogger, MetricsCollector
├── etl/                        # ETL 默认实现
├── entity/                     # AI 相关实体（model/application/agent/workflow/knowledge/chat/security/tool）
└── autoconfigure/              # 16 个 AutoConfiguration
```

### ai-impl/ai-spring-ai

```
io.github.afgprojects.framework.ai.springai
├── SpringAiChatClient           # AfgChatClient 的 Spring AI 实现
├── SpringAiEmbeddingClient      # AfgEmbeddingClient 的 Spring AI 实现
├── advisor/                     # Spring AI Advisor 适配
├── memory/                      # ChatMemory 适配
├── observation/                 # Observation 适配
├── rag/                         # Spring AI RAG 适配
└── autoconfigure/               # 7 个 AutoConfiguration
```

### ai-impl/ai-langchain4j

```
io.github.afgprojects.framework.ai.langchain4j
├── Lc4jChatClient               # AfgChatClient 的 LangChain4J 实现
├── Lc4jEmbeddingClient          # AfgEmbeddingClient 的 LangChain4J 实现
├── tool/                        # ToolAdapter
├── memory/                      # ChatMemory 适配
├── observation/                 # Observation 适配
├── advisor/                     # Advisor 适配
└── autoconfigure/               # 7 个 AutoConfiguration
```

### integration/afg-redis

```
io.github.afgprojects.framework.redis
├── RedisCacheManager            # CacheManager 的 Redis 实现
├── RedisDistributedLock         # DistributedLock 的 Redis 实现（4 种锁类型）
├── RedissonTaskScheduler        # DistributedTaskScheduler 的 Redis 实现
├── RedissonDelayQueue           # DelayQueue 的 Redis 实现
├── RedisAuditLogStorage         # AuditLogStorage 的 Redis 实现
├── RedisRateLimitStorage        # RateLimitStorage 的 Redis 实现
├── RedissonStorageClient        # FeatureFlagManager.DistributedStorageClient 的 Redis 实现
└── autoconfigure/               # RedisAutoConfiguration
```

### governance/proto

```
io.github.afgprojects.framework.governance.proto
└── (gRPC 生成的 Java 类)       # SubscribeConfig / ConfigAckStream / GetConfigs / PublishConfig / RegisterService / HeartbeatStream / DiscoverServices
```

### governance/client

```
io.github.afgprojects.framework.governance.client
├── GovernanceConfigClient       # 配置中心客户端
├── GovernanceRegistryClient     # 服务注册客户端
├── ConfigAutoRegistrar          # 启动时自动注册配置
├── GrpcChannelManager           # gRPC Channel 管理
├── SignatureInterceptor         # 签名拦截器
└── autoconfigure/               # GovernanceClientAutoConfiguration
```

### governance/server

```
io.github.afgprojects.framework.governance.server
├── grpc/                        # gRPC Service 实现
├── controller/                  # REST API（配置组/配置项/配置值/配置历史/服务注册/环境管理）
├── entity/                      # 治理相关实体
├── liquibase/                   # 数据库迁移脚本
└── autoconfigure/               # GovernanceServerAutoConfiguration
```

### gradle-plugin

```
io.github.afgprojects.framework.core.gradle
├── AfgFrameworkPlugin           # 插件入口
├── AfgFrameworkExtension        # afg {} 扩展配置
├── AfgInfoTask                  # afgInfo 任务
├── AfgInitTask                  # afgInit 任务
├── GenerateMigrationTask        # generateMigration 任务
├── GenerateEntityTask           # generateEntity 任务
├── GenerateEntityFromDbTask     # generateEntityFromDb 任务
└── DbMigrateTask                # dbMigrate 任务
```

---

## 模块选择指南

> 来源：PRD §4.4

| 场景 | 需要的模块 | 最小依赖 |
|------|-----------|---------|
| 基础 CRUD 应用 | core + data-jdbc | `afg-framework-core` + `afg-framework-data-jdbc` |
| 需要多租户 | 上述 + auth-server | + `afg-framework-auth-server` |
| 需要资源服务器 | core + resource-server | `afg-framework-core` + `afg-framework-resource-server` |
| 需要 AI 对话 | 上述 + ai-core + ai-langchain4j | + `afg-framework-ai-core` + `afg-framework-ai-langchain4j` |
| 需要 RAG 知识库 | 上述 + 向量数据库 | + AI 模块 + 向量数据库驱动 |
| 需要分布式缓存/锁 | 上述 + afg-redis | + `afg-framework-afg-redis` |
| 需要配置中心 | 上述 + governance-client | + `afg-framework-governance-client` |
| 完整单体应用 | 全部 | 所有模块 |

> Gradle 插件根据 `securityMode` 和 `basePackage` 自动添加核心依赖，开发者只需添加可选模块。

---

## 命名规范

> 来源：PRD §6.1

### 类命名

| 类型 | 规则 | 示例 |
|------|------|------|
| 类名 | UpperCamelCase | `UserService` / `OrderController` |
| 方法名 | lowerCamelCase，动词开头 | `findById` / `createOrder` / `isExpired` |
| 字段名 | lowerCamelCase | `userName` / `createdAt` |
| 常量名 | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` / `DEFAULT_PAGE_SIZE` |
| 包名 | 全小写，点分隔 | `io.github.afgprojects.framework.core.cache` |

### 数据库命名

| 类型 | 规则 | 示例 |
|------|------|------|
| 表名 | lower_snake_case | `sys_user` / `sec_role` |
| 列名 | lower_snake_case | `user_name` / `created_at` |
| 主键约束 | `pk_{表名}` | `pk_sys_user` |
| 唯一约束 | `uk_{表名}_{字段名}` | `uk_user_username` |
| 普通索引 | `idx_{表名}_{字段名}` | `idx_user_tenant` |
| 外键约束 | `fk_{表名}_{关联表}` | `fk_user_dept` |

### 框架特殊命名

| 类型 | 规则 | 示例 |
|------|------|------|
| AutoConfiguration | `{Feature}AutoConfiguration` | `CacheAutoConfiguration` |
| SPI 接口 | 名词或动词 | `DistributedLock` / `EventPublisher` |
| 默认实现 | `Default{Spi}` 或 `{Technology}{Spi}` | `DefaultCacheManager` / `RedisDistributedLock` |
| NoOp 实现 | `NoOp{Spi}` | `NoOpVectorStore` |
| 实体类 | 名词，无后缀 | `User` / `Order` / `SecRole` |
| Controller | `{Entity}Controller` | `UserController` |
| Service | `{Entity}Service` | `UserService` |
| 异常类 | `{Name}Exception` | `BusinessException` / `OptimisticLockException` |
| 错误码枚举 | `{Domain}ErrorCode` | `CommonErrorCode` / `AiErrorCode` |

### 字段命名约定

| Java 字段 | 数据库列 | 类型约定 |
|----------|---------|---------|
| `createdAt` | `created_at` | `Instant`（非 LocalDateTime） |
| `updatedAt` | `updated_at` | `Instant` |
| `deletedAt` | `deleted_at` | `Instant` |
| `deleted` | `deleted` | `Boolean`（默认 false） |
| `version` | `version` | `Integer`（默认 0） |
| `tenantId` | `tenant_id` | `String` |
| `createBy` | `create_by` | `Long`（用户 ID） |
| `updateBy` | `update_by` | `Long` |

### 配置属性命名

- 前缀：`afg.{module}.{feature}`
- 风格：kebab-case
- 层级：不超过 4 层

```yaml
afg:
  ai:
    rag:
      enabled: true              # 功能开关
      embedding-dimensions: 1536 # 行为参数
```

### Liquibase changeSet 命名

| 类型 | 格式 | 示例 |
|------|------|------|
| changeSet id | `v{版本}-{序号}-{表名}[-{操作}]` | `v1.0.0-001-sys-user` |
| 迁移文件 | `{序号}_{表名}.xml` | `001_sys_user.xml` |
| author | 统一 `afg` | — |

---

## 实体基类速查

| 需求 | 基类 | 字段 | 特征接口 |
|------|------|------|---------|
| 基础 | `BaseEntity` | id(Long), createdAt(Instant), updatedAt(Instant) | — |
| 多租户 | `TenantEntity` | + tenantId(String) | — |
| 软删除 | `SoftDeleteEntity` | + deleted(Boolean=false) | `SoftDeletable` |
| 软删除+时间戳 | `TimestampSoftDeleteEntity` | + deletedAt(Instant) | `TimestampSoftDeletable` |
| 乐观锁 | `VersionedEntity` | + version(Integer=0) | `Versioned` |
| 全功能 | `FullEntity` | + deleted + version + createBy + updateBy | `SoftDeletable` + `Versioned` + `Auditable` |
| 树形 | `TreeEntity<T>` | + parentId + level + path + sortOrder + children | `Treeable<T>` |

> **注意**：`FullEntity` 不继承 `TenantEntity`。需要多租户 + 全功能时，继承 `FullEntity` 并手动添加 `tenantId` 字段。
