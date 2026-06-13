# Research: PRD-Code Alignment Analysis

- **Query**: Compare docs/framework-prd.md with actual codebase implementation across all modules
- **Scope**: Internal (full codebase search)
- **Date**: 2026-06-13

## Findings

### Summary Counts

- **Fully Aligned**: ~65 items
- **PRD Described but Code Missing**: ~30 items (grouped by module below)
- **Code Implemented but PRD Not Described**: ~12 items
- **Description Inconsistent**: ~8 items

### Core AutoConfiguration Count Discrepancy

- PRD claims 31+ AutoConfigurations for core module
- Actual count: 31 AutoConfiguration files found
- PRD lists 42 named AutoConfigurations in Appendix A
- 11 of those 42 have NO code implementation (see Missing list below)

### CommonErrorCode Count

- PRD claims 94 standard error codes
- Actual: 61 enum values in CommonErrorCode.java

### AI Workflow Nodes

- PRD claims 37 built-in node types
- Actual: only 5 node classes found (WorkflowNode, AgentNode, AiServiceNode, HumanNode, ParallelNode, RouterNode + their Default implementations)
- 32 of 37 PRD-listed node types are missing from code

---

## Per-Module Analysis

### commons

#### Aligned
- Result<T>, PageData<T> - fully implemented
- AfgException, BusinessException, ErrorCode, ErrorCategory - fully implemented
- NamingUtils (toSnakeCase, toCamelCase, capitalize, uncapitalize) - fully implemented
- ArgumentAssert (notNull, notEmpty, isTrue, state) - fully implemented
- DateUtils, CollectionUtils, StringUtils, IoUtils - all implemented

#### Missing
- None - commons module is fully aligned with PRD

#### Inconsistent
- PRD says 94 CommonErrorCode values; actual code has 61

### apt-api + apt-impl

#### Aligned
- @AfEntity, @CommonFieldDefinition, @CommonFieldDefinitions - all present
- @AfService, @AfOperation, @AfParam, @AfResult - all present
- @AfgModuleAnnotation - present
- @AfgEnum, @EncryptedField - present
- EntityMetadataProcessor, ServiceMetadataProcessor, AfgModuleAnnotationProcessor, EnumMetadataProcessor - all present
- Compile-time validations (ERROR/WARNING messages) - present in processors

#### Missing
- None - APT modules are fully aligned

### core

#### Aligned
- 31 AutoConfiguration files exist (core set)
- @ConditionalOnFeature, @ConditionalOnPropertyNotEmpty, @ConditionalOnTenant - all present
- GlobalExceptionHandler - present
- Results (enhanced Result helper) - present
- JacksonUtils, JacksonMapper - present
- @Cached, @CacheEvict, @CachePut - present
- @Lock, DistributedLock, LockType - present
- @EventHandler, DomainEvent, DomainEventPublisher - present
- @ScheduledTask, @DistributedTask - present
- DynamicTaskManager - present
- @FeatureToggle - present
- @Audited - present
- @SignatureRequired - present
- BeanInvocationEngine, DefaultBeanInvocationEngine - present
- @Phone, PhoneValidator - present
- MdcFilter, SensitiveFieldProcessor - present
- LocaleFilter, LocaleAutoConfiguration - present
- MultiDataSourceAutoConfiguration - present
- CacheAutoConfiguration, LockAutoConfiguration, EventAutoConfiguration, SchedulerAutoConfiguration - present
- EncryptionAutoConfiguration (config encryption ENC()) - present
- CloudNativeAutoConfiguration, KubernetesProbeAutoConfiguration - present
- VirtualThreadAutoConfiguration - present

#### Missing (PRD described, code absent)
- **AccessLogAutoConfiguration** - PRD lists it, no code
- **ValidationAutoConfiguration** - PRD lists it, no code (validation exists but no dedicated AutoConfig)
- **SseAutoConfiguration** - PRD lists it, no code
- **SseConnectionManager** SPI - PRD lists it, no code
- **StateMachineAutoConfiguration** - PRD lists it, no code
- **@StateMachine / @Transition** annotations - PRD describes them, no code
- **StateMachine<T,S> / StateMachineFactory** SPI - PRD lists, no code
- **EnumManagementAutoConfiguration** - PRD lists it, no code
- **ImportExportAutoConfiguration** - PRD lists it, no code
- **@ExcelSheet / @ExcelColumn / @CsvSheet** annotations - PRD describes, no code
- **DataExporter / DataImporter / FormatHandler** SPI - PRD lists, no code
- **NotificationAutoConfiguration** - PRD lists it, no code
- **NotificationService / NotificationChannel** SPI - PRD lists, no code
- **WebhookAutoConfiguration** - PRD lists it, no code
- **WebhookService / WebhookRepository** SPI - PRD lists, no code
- **DuplicateSubmitAutoConfiguration** - PRD lists it, no code
- **@DuplicateSubmit** annotation - PRD describes, no code
- **ApiVersionAutoConfiguration** - PRD lists it, no code
- **@ApiVersion** annotation exists as model only, no AutoConfig
- **IdGeneratorAutoConfiguration** - PRD lists it, no code
- **IdGenerator SPI (Snowflake/Segment/UUID)** - PRD describes, no code
- **@RateLimited** annotation (core level) - not found; only @AiRateLimited exists in ai-core
- **AccessLogFilter** - PRD describes auto request logging, no code found
- **MdcTaskDecorator** - PRD mentions it for async MDC propagation, not found (MdcFilter exists but not TaskDecorator)

#### Code Has but PRD Doesn't Describe
- **FileStorage / FileStorageFactory** SPI in core/api/storage - PRD describes storage in afg-storage module but not as a core SPI
- **ServiceRegistry / ServiceDiscovery** SPI in core/api/registry - PRD doesn't mention these in core SPI list
- **ConfigEncryptor / AesConfigEncryptor** - PRD mentions EncryptionAutoConfiguration but doesn't detail the config encryption (ENC() prefix) SPI

#### Inconsistent
- PRD lists 42 core AutoConfigurations in Appendix A but only 31 exist in code
- PRD says core has `api.cache`, `api.lock`, `api.audit`, `api.feature` etc. as subpackages; actual code has these at `core/cache`, `core/lock`, `core/audit`, `core/feature` (not under `api/` subpackage)
- PRD says `@Cached` is the cache annotation; code has `@Cached` (aligned), but PRD also references `@Cacheable` in examples which is Spring native, not framework annotation

### data-core + data-sql + data-jdbc + data-liquibase

#### Aligned
- DataManager interface and JdbcDataManager implementation - present
- EntityProxy with query(), save(), insert(), update(), deleteById(), restoreById() - present
- Conditions builder with all operators (eq, ne, like, likeStartsWith, likeEndsWith, notLike, in, notIn, isNull, isNotNull, gt, ge, lt, le, between, notBetween, jsonContains, jsonPath) - present
- Dynamic conditions (eqIfPresent, likeIfPresent, geIfPresent, inIfPresent, betweenIfPresent) - present
- allOf / anyOf combination - present
- BaseEntity, TenantEntity, SoftDeleteEntity, TimestampSoftDeleteEntity, VersionedEntity, FullEntity - all present
- TreeEntity, Treeable - present
- SoftDeletable, TimestampSoftDeletable, Versioned, Auditable interfaces - present
- @ManyToOne, @OneToMany, @OneToOne, @ManyToMany - present
- PageRequest, PageData - present
- AggregateQuery - present
- ProjectedQuery, JdbcProjectedQuery - present
- 10 database dialects (MySQL, PostgreSQL, Oracle, SQLServer, H2, OceanBase, OpenGauss, GaussDB, Dm, Kingbase) - all present
- 16 TypeHandlers - present (BigDecimal, Blob, BooleanNumber, DateTime, Enum, Instant, Json, LocalDate, LocalTime, Number, OffsetDateTime, String, UUID, YearMonth, Year, ZonedDateTime)
- SchemaComparator - present
- LifecycleCallbacks - present
- OptimisticLockException - present
- TenantContextAutoConfiguration, TransactionAutoConfiguration - present
- DataManagerAutoConfiguration, EntityCacheAutoConfiguration, SqlMetricsAutoConfiguration - present
- LiquibaseAutoConfiguration - present
- AptMetadataLoader, ReflectiveMetadataLoader, EntityMetadataCache, MetadataProvider SPI - present

#### Missing
- **TreeQuery** - PRD describes TreeQuery for recursive/path/subtree queries, no code
- **IdGenerator SPI** (Snowflake/Segment/UUID strategies) - PRD describes, no code
- **@GeneratedValue(generator=SNOWFLAKE)** - PRD describes, no code
- **EncryptedTypeHandler** - PRD lists 16+ TypeHandlers including EncryptedTypeHandler, not found
- **EntityChangedEvent<T>** - PRD says save/update/delete auto-publishes entity change events, no code
- **PessimisticLock / withPessimisticLock()** - PRD describes `SELECT ... FOR UPDATE`, no code
- **TenantDataSourceResolver** - PRD describes for independent database mode, no code
- **DataSourceContextHolder / @DataSource** annotation - PRD describes, no code (MultiDataSourceAutoConfiguration exists but no annotation)

#### Inconsistent
- PRD says 94 CommonErrorCode; actual is 61
- PRD lists data-jdbc as having 4 AutoConfigurations but only 3 found (DataManagerAutoConfiguration, EntityCacheAutoConfiguration, SqlMetricsAutoConfiguration)

### security-core + auth-server + resource-server

#### Aligned
- 9 auth-server AutoConfigurations - all present and match PRD exactly
- 2 resource-server AutoConfigurations - present
- AfgUserDetailsService, AfgUserDetails, AfgAuthentication - present
- AfgEnforcer, AfgSecurityContext - present
- LoginService, TokenService, CaptchaService, LoginStrategyFactory - present
- OAuth2AuthorizationService, OAuth2ClientService, AuthorizationCodeStorage - present
- PermissionService, RbacService, AbacService, DataScopeService - present
- TenantResolver, TenantResolverChain, HeaderTenantResolver - present
- LoginFailureTracker, DeviceLimiter, PasswordValidator, IpRestrictionChecker - present
- UsernamePasswordLoginStrategy, MobileCaptchaLoginStrategy, EmailCaptchaLoginStrategy - present
- @RequirePermission, @RequireRole - present
- DataScopeType (ALL, SELF, DEPT, DEPT_AND_CHILD, CUSTOM) - present
- All NoOp implementations for security SPI - present

#### Missing
- **WechatLoginStrategy** - PRD describes, no code
- **DingTalkLoginStrategy** - PRD describes, no code
- **FeishuLoginStrategy** - PRD describes, no code
- **WeComLoginStrategy** - PRD describes, no code
- **TOTP / 2FA** - PRD describes TOTP verification, QR code generation, no code
- **Password reset endpoints** - PRD describes password reset flow, not verified in code
- **TenantDataSourceResolver** - for independent database mode, no code

#### Inconsistent
- PRD says social login strategies are implemented; actual code only has UsernamePassword, MobileCaptcha, EmailCaptcha (3 of 7)

### ai-core + ai-spring-ai + ai-langchain4j

#### Aligned
- 16 ai-core AutoConfigurations - all present matching PRD
- @AiChat, @AiAgent, @ModelRoute, @Workflow - all present
- @AiResilient, @AiRateLimited, @ContentSafety, @AiAudited, @ToolExecution - all present
- Skill system (SkillRegistry, SkillDispatcher, SkillExecutor, SkillResult, SkillContext) - present
- ETL pipeline (EtlPipeline, EtlPipelineBuilder, EtlResult, EtlContext) - present
- 7 LangChain4J AutoConfigurations - present and match PRD
- All AI SPI interfaces with NoOp defaults - present
- AiCoreAutoConfiguration through AiEntityAutoConfiguration - all present

#### Missing
- **@Skill annotation** - PRD shows `@Skill(name, description, intentKeywords)` but no annotation interface found
- **@Tool / @ToolParam annotations** - PRD shows `@Tool(name, description)` and `@ToolParam`, only @ToolExecution found (audit annotation, not the tool definition annotation)
- **37 workflow node types** - PRD claims 37 built-in nodes; actual code has only ~5 base node classes (WorkflowNode, AgentNode, AiServiceNode, HumanNode, ParallelNode, RouterNode). All 32 specific node types listed in PRD Appendix D are missing:
  - INPUT: InputNode, FileInputNode, HttpRequestNode, DatabaseQueryNode
  - AI: AiChatNode, AiEmbeddingNode
  - LOGIC: ConditionNode, LoopNode, SwitchNode, MergeNode, DelayNode, SubWorkflowNode
  - TOOL: ToolNode, HttpCallNode, DatabaseWriteNode, CodeExecuteNode, McpToolNode
  - OUTPUT: OutputNode, FileOutputNode, NotificationNode, WebhookNode, LogOutputNode
  - HUMAN: HumanApprovalNode, HumanInputNode, HumanChoiceNode
  - TRANSFORM: JsonTransformNode, TextTransformNode, MappingNode, FilterNode, AggregateNode
  - RAG: RetrievalNode, EmbeddingNode, ReRankNode
  - CHECKPOINT: CheckpointNode, RecoveryNode

#### Inconsistent
- PRD says ai-spring-ai has 7 AutoConfigurations including SpringAiEmbeddingAutoConfiguration and SpringAiToolAutoConfiguration; actual has 6 with SpringAiRagAutoConfiguration (not in PRD) and missing SpringAiEmbeddingAutoConfiguration and SpringAiToolAutoConfiguration
- PRD says 37 workflow nodes; actual has ~5 base node types

### integration modules

#### Aligned
- afg-redis: RedisAutoConfiguration - present
- afg-jdbc: JdbcAutoConfiguration with DatabaseAuditLogStorage - present
- afg-rabbitmq: RabbitMQEventAutoConfiguration, RabbitMQEventPublisher - present
- afg-websocket: WebSocketAutoConfiguration with auth interceptor - present
- afg-storage: StorageAutoConfiguration with Local/MinIO/S3/OSS - present

#### Missing
- **Kafka event integration** - PRD mentions `afg.core.event.type: KAFKA` and Kafka as alternative; only a Kafka properties class exists in core, no dedicated afg-kafka integration module
- PRD does not describe afg-jdbc, afg-rabbitmq, afg-websocket modules in section 5.7 (only describes afg-redis in detail)

#### Code Has but PRD Doesn't Describe
- **afg-jdbc** module - exists in code but PRD section 5.7 only covers afg-redis
- **afg-rabbitmq** module - exists in code but PRD section 5.7 only covers afg-redis
- **afg-websocket** module - exists in code but PRD section 5.7 only covers afg-redis
- **afg-storage** module - exists in code but PRD section 5.7 only covers afg-redis

#### Inconsistent
- PRD section 4.1 module list only shows `afg-redis` under integration layer, but code has 5 integration modules (afg-redis, afg-jdbc, afg-rabbitmq, afg-websocket, afg-storage)
- PRD section 5.7 title says "Integration -- afg-redis" but doesn't have dedicated sections for other integration modules

### governance

#### Aligned
- proto, client, server sub-modules - all present
- GovernanceClientAutoConfiguration, GovernanceServerAutoConfiguration - present
- gRPC proto definition - present

#### Missing
- None specifically - governance is marked as Alpha in PRD maturity matrix

### gradle-plugin

#### Aligned
- afgInfo, afgInit, generateMigration, generateEntity, generateEntityFromDb, dbMigrate - all present
- AfgExtension with all properties (springBootVersion, frameworkVersion, standalone, useLombok, enableCodegen, basePackage, securityMode, databaseType) - present

#### Missing
- **generateDbDoc** task - PRD describes it, no code
- **generateDocker** task - PRD describes it, no code (though afgInit generates Dockerfile)

---

## Caveats / Not Found

- Could not verify runtime behavior of all features (only file existence checked)
- Some features may be implemented under different class names than PRD specifies
- The PRD itself notes maturity levels (Alpha/Beta/GA) which explain some missing implementations
- Kafka integration exists only as a properties class in core, not as a full integration module
