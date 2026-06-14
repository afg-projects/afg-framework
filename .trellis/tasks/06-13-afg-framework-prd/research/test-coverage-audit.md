# Research: Test Coverage Audit

- **Query**: afg-framework test coverage audit - identify modules/features lacking tests or with insufficient testing
- **Scope**: internal
- **Date**: 2026-06-14

## Findings

### 1. Module Test Coverage Overview

Total: 215 test files across 24 modules. 13 modules have zero test files.

| Module | Source Files | Test Files | Ratio | Status |
|--------|-------------|------------|-------|--------|
| commons | 13 | 13 | 1:1 | GOOD |
| apt-impl | 14 | 4 | 3.5:1 | ADEQUATE |
| core | 583 | 47 | 12.4:1 | LOW |
| data-core | 176 | 32 | 5.5:1 | MODERATE |
| data-impl/data-sql | 17 | 13 | 1.3:1 | GOOD |
| data-impl/data-jdbc | 53 | 14 | 3.8:1 | MODERATE |
| data-impl/data-liquibase | 9 | 6 | 1.5:1 | GOOD |
| security-core | 93 | 47 | 2:1 | GOOD |
| security-impl/auth-server | 132 | 13 | 10.2:1 | LOW |
| **security-impl/resource-server** | **26** | **0** | -- | **NONE** |
| ai-core | 452 | 26 | 17.4:1 | VERY LOW |
| **ai-impl/ai-spring-ai** | **19** | **0** | -- | **NONE** |
| **ai-impl/ai-langchain4j** | **21** | **0** | -- | **NONE** |
| **integration/afg-redis** | **36** | **0** | -- | **NONE** |
| **integration/afg-jdbc** | **7** | **0** | -- | **NONE** |
| **integration/afg-rabbitmq** | **9** | **0** | -- | **NONE** |
| **integration/afg-websocket** | **21** | **0** | -- | **NONE** |
| **integration/afg-storage** | **25** | **0** | -- | **NONE** |
| **governance/proto** | 0 (proto) | **0** | -- | **NONE** |
| **governance/client** | **10** | **0** | -- | **NONE** |
| **governance/server** | **48** | **0** | -- | **NONE** |
| **apt-api** | **12** | **0** | -- | **NONE** |
| gradle-plugin | Kotlin | 1 | -- | LOW |

#### Key Observations

- **13 modules have ZERO test files**: resource-server, ai-spring-ai, ai-langchain4j, all 5 integration modules, all 3 governance modules, apt-api
- **Core (583 source / 47 test)**: The largest module with the worst ratio. Many sub-features only have unit tests for NoOp/default implementations
- **ai-core (452 source / 26 test)**: Second largest module, second worst ratio. 38 workflow node implementations with zero dedicated node tests
- **auth-server (132 source / 13 test)**: All 13 tests are unit tests (no @SpringBootTest, no Testcontainers)

### 2. New Feature Test Coverage Matrix

#### Core Module

| Feature | Source Files | Test Files | Has Unit Test | Has Integration Test | Status |
|---------|-------------|------------|---------------|---------------------|--------|
| AccessLogFilter | `core/.../web/logging/AccessLogFilter.java` | `AccessLogFilterTest.java` | YES (MockHttpServletRequest/Response) | NO | PARTIAL |
| Validation | `ValidationAutoConfiguration.java`, `PhoneValidator.java`, `ValidationInvocationInterceptor.java` | `ValidationAutoConfigurationTest.java` (props only) | PARTIAL (only props defaults) | NO | INSUFFICIENT |
| SSE | `LocalSseConnectionManager.java`, `NoOpSseConnectionManager.java`, `SseAutoConfiguration.java` | `LocalSseConnectionManagerTest.java` | YES | NO | PARTIAL |
| IdGenerator | `SnowflakeIdGenerator.java`, `UuidIdGenerator.java`, `NoOpIdGenerator.java`, `IdGeneratorAutoConfiguration.java` | `SnowflakeIdGeneratorTest.java`, `UuidIdGeneratorTest.java`, `NoOpIdGeneratorTest.java` | YES | NO | PARTIAL |
| Notification | `LogNotificationService.java`, `NoOpNotificationService.java`, `NotificationAutoConfiguration.java` | `LogNotificationServiceTest.java`, `NoOpNotificationServiceTest.java` | YES | NO | PARTIAL |
| Webhook | `LocalWebhookService.java`, `InMemoryWebhookRepository.java`, `NoOpWebhookService.java`, `WebhookAutoConfiguration.java` | `LocalWebhookServiceTest.java`, `InMemoryWebhookRepositoryTest.java` | YES (uses real HttpServer) | NO | PARTIAL |
| StateMachine | `LocalStateMachineFactory.java`, `DefaultStateMachineInstance.java`, `NoOpStateMachineFactory.java`, `StateMachineDefinition.java`, `StateMachineAutoConfiguration.java` | `LocalStateMachineFactoryTest.java`, `DefaultStateMachineInstanceTest.java`, `NoOpStateMachineFactoryTest.java`, `StateMachineDefinitionTest.java`, `InvalidTransitionExceptionTest.java` | YES | NO | PARTIAL |
| ImportExport | `CsvDataExporter.java`, `CsvDataImporter.java`, `NoOpDataExporter.java`, `NoOpDataImporter.java`, `ImportExportAutoConfiguration.java` | `CsvDataExporterTest.java`, `CsvDataImporterTest.java`, `ExportMetadataResolverTest.java` | YES | NO | PARTIAL |
| EnumManagement | `LocalEnumRegistry.java`, `NoOpEnumRegistry.java`, `EnumManagementEndpoint.java`, `EnumManagementAutoConfiguration.java` | `LocalEnumRegistryTest.java` | YES | NO | PARTIAL |

**Missing for core features**:
- `PhoneValidator` -- no test at all
- `ValidationInvocationInterceptor` -- no test at all
- `NoOpSseConnectionManager` -- no test at all
- `SseAutoConfiguration` -- no test at all
- AutoConfiguration conditional registration tests for most features

#### Data Module

| Feature | Source Files | Test Files | Has Unit Test | Has Integration Test | Status |
|---------|-------------|------------|---------------|---------------------|--------|
| TreeQuery | `data-core/.../query/TreeQuery.java`, `data-core/.../entity/TreeEntity.java`, `data-jdbc/.../query/JdbcTreeQuery.java` | `TreeEntityTest.java` (data-core only) | PARTIAL (entity only) | NO | INSUFFICIENT |
| EntityChangedEvent | `data-core/.../event/EntityChangedEvent.java`, `data-core/.../event/EntityChangedEventPublisher.java`, `data-core/.../event/NoOpEntityChangedEventPublisher.java`, `data-jdbc/.../event/SpringEntityChangedEventPublisher.java` | NONE | NO | NO | **NONE** |
| DataSource (routing) | `data-core/.../annotation/DataSource.java`, `data-jdbc/.../datasource/DataSourceAspect.java`, `data-jdbc/.../datasource/DataSourceContextHolder.java`, `core/.../datasource/lb/ReadDataSourceLoadBalancer.java` | NONE | NO | NO | **NONE** |
| IdGenerator integration | `data-jdbc/.../EntityInsertHandler.java` (uses IdGenerator) | NONE (IdGenerator integration in data-jdbc) | NO | NO | **NONE** |

#### Security Module

| Feature | Source Files | Test Files | Has Unit Test | Has Integration Test | Status |
|---------|-------------|------------|---------------|---------------------|--------|
| SocialLogin (4 strategies) | `WechatLoginStrategy.java`, `DingTalkLoginStrategy.java`, `FeishuLoginStrategy.java`, `WeComLoginStrategy.java`, `AbstractSocialLoginStrategy.java`, `SocialLoginAutoConfiguration.java` | NONE | NO | NO | **NONE** |
| TOTP/2FA | `TotpService.java`, `NoOpTotpService.java` (security-core), `DefaultTotpService.java`, `TwoFactorAuthenticationService.java`, `TotpController.java`, `TotpAutoConfiguration.java` (auth-server) | NONE | NO | NO | **NONE** |

#### AI Module

| Feature | Source Files | Test Files | Has Unit Test | Has Integration Test | Status |
|---------|-------------|------------|---------------|---------------------|--------|
| @Skill annotation | `SkillRegistrar.java`, `DefaultSkillRegistry.java`, `@Skill` annotation | `SkillRegistrarTest.java` | YES | NO | PARTIAL |
| @Tool annotation | `ToolRegistrar.java`, `DefaultToolRegistry.java`, `@Tool` annotation | `ToolRegistrarTest.java` | YES | NO | PARTIAL |
| 35+ workflow nodes | 38 node implementations across 7 categories (ai, transform, rag, output, checkpoint, human, input, logic, tool) | `AiWorkflowControllerTest.java` (CRUD only) | NO | PARTIAL (controller CRUD only) | **INSUFFICIENT** |

**Workflow nodes without dedicated tests (38 total)**:
- AI: `AiChatNode`, `AiEmbeddingNode`, `AiServiceNode`
- Transform: `MappingNode`, `JsonTransformNode`, `TextTransformNode`, `FilterNode`, `AggregateNode`
- RAG: `EmbeddingNode`, `ReRankNode`, `RetrievalNode`
- Output: `WebhookNode`, `NotificationNode`, `FileOutputNode`, `OutputNode`, `LogOutputNode`
- Checkpoint: `CheckpointNode`, `RecoveryNode`
- Human: `HumanApprovalNode`, `HumanChoiceNode`, `HumanInputNode`
- Input: `DatabaseQueryNode`, `FileInputNode`, `HttpRequestNode`, `InputNode`
- Logic: `ConditionNode`, `DelayNode`, `LoopNode`, `MergeNode`, `SubWorkflowNode`, `SwitchNode`
- Tool: `CodeExecuteNode`, `DatabaseWriteNode`, `HttpCallNode`, `McpToolNode`, `ToolNode`

#### Gradle Plugin

| Feature | Source Files | Test Files | Status |
|---------|-------------|------------|--------|
| generateDbDoc | `GenerateDbDocTask.kt` | `AfgPluginTest.kt` (only checks task registration) | INSUFFICIENT (no functional test) |

### 3. Integration Test Gap Analysis

#### Modules with @SpringBootTest

| Module | @SpringBootTest Count | Testcontainers Used | Description |
|--------|----------------------|---------------------|-------------|
| data-impl/data-jdbc | 4 | YES (PostgreSQL, MySQL) | Well-structured: BaseDataTest/BaseMysqlTest/BaseWebTest with real DB |
| data-impl/data-liquibase | 1 | NO (uses H2) | Uses H2 in-memory DB (violates framework guideline) |
| ai-core | 2 | YES (MySQL) | AbstractAiWebTest with real DB for controller tests |

#### Modules with ZERO Integration Tests

| Module | Source File Count | Key Classes Without Integration Tests |
|--------|------------------|---------------------------------------|
| **security-impl/auth-server** | 132 | All 13 tests are unit tests. No @SpringBootTest, no Testcontainers. Missing: LoginController integration, OAuth2 flow, Casbin RBAC with real DB, SocialLogin, TOTP/2FA |
| **security-impl/resource-server** | 26 | No tests at all. Missing: JWT validation, permission checking, tenant resolution, ResourceServerAutoConfiguration |
| **ai-impl/ai-spring-ai** | 19 | No tests at all. Missing: SpringAiChatClient, SpringAiEmbeddingClient, Advisor, ChatMemory integration |
| **ai-impl/ai-langchain4j** | 21 | No tests at all. Missing: Lc4jChatClient, Lc4jEmbeddingClient, ToolAdapter integration |
| **integration/afg-redis** | 36 | No tests at all. Missing: RedisCache, RedisDistributedLock, RedisRateLimitStorage, RedissonScheduler, RedisAuditLogStorage (has Testcontainers deps in build.gradle.kts) |
| **integration/afg-rabbitmq** | 9 | No tests at all. Missing: RabbitMQEventPublisher (has Testcontainers deps in build.gradle.kts) |
| **integration/afg-websocket** | 21 | No tests at all. Missing: WebSocketConfigurer, AuthChannelInterceptor, WebSocketSessionManager |
| **integration/afg-storage** | 25 | No tests at all. Missing: all storage implementations (Local/MinIO/S3/OSS) (has Testcontainers deps in build.gradle.kts) |
| **integration/afg-jdbc** | 7 | No tests at all. Missing: JdbcAuditLogStorage (has Testcontainers deps in build.gradle.kts) |
| **governance/server** | 48 | No tests at all. Missing: gRPC service, REST controllers, config/service logic, Liquibase migrations |
| **governance/client** | 10 | No tests at all. Missing: gRPC client, auto-configuration |
| **core** | 583 | All 47 tests are unit tests. No @SpringBootTest. Missing: AutoConfiguration registration tests, web filter integration tests, batch operation integration |

#### Notable: Integration modules have Testcontainers dependencies but no test files

The following modules have Testcontainers dependencies declared in `build.gradle.kts` but zero test files:
- `integration/afg-redis` (testcontainers + testcontainers.junit.jupiter + testcontainers)
- `integration/afg-rabbitmq` (testcontainers + testcontainers.junit.jupiter + testcontainers.rabbitmq)
- `integration/afg-storage` (testcontainers + testcontainers.junit.jupiter)
- `integration/afg-jdbc` (testcontainers + testcontainers.junit.jupiter)

### 4. Test Quality / Specification Compliance

#### Mockito Usage (Prohibited by Framework)

**Result: CLEAN** -- Zero instances of `@Mock`, `Mockito.mock()`, `@MockBean`, or `when(...).thenReturn(...)` found in any test file.

#### Test Naming Convention

| Pattern | Count | Compliance |
|---------|-------|-----------|
| `shouldXxx_whenYyy` | 714 | COMPLIANT (preferred) |
| `shouldXxx` (without `when`) | 2121 | ACCEPTABLE (shorter form) |
| `testXxx` | 1 | VIOLATION (only 1 instance, in APT test setup code, not a real test method) |

**Verdict**: Test naming is overwhelmingly compliant. The single `testXxx` instance is a helper method in `ServiceMetadataProcessorTest` (line 339), not an actual test method.

#### H2 Usage (Framework Prohibits H2 for Tests)

**1 violation found**: `data-impl/data-liquibase/LiquibaseAutoConfigurationTest.java` uses `jdbc:h2:mem:liquibase_test` with `org.h2.Driver`. This should be replaced with Testcontainers PostgreSQL per framework guidelines.

#### @Sql Usage (Framework Prohibits)

**Result: CLEAN** -- Zero instances of `@Sql` found in test files. Tests use DataManager for data preparation as required.

#### @Transactional Usage

| Module | @Transactional Count | Appropriate Use? |
|--------|---------------------|------------------|
| data-impl/data-jdbc | 10 | YES -- data operation tests, auto-rollback |
| ai-core | 1 | YES |

### 5. DataManager Key Path Integration Test Coverage

DataManager integration tests exist only in `data-impl/data-jdbc` and cover:

| Key Path | Test File | Test Methods | Status |
|----------|-----------|-------------|--------|
| **CRUD (save/find/delete)** | `JdbcDataManagerCrudTest` | 5 methods (save new, save existing, findById, findAll, deleteById) | COVERED |
| **Condition Query** | `JdbcDataManagerCrudTest` | 2 methods (single condition, multiple AND conditions) | PARTIAL -- only eq and AND; missing: like, in, between, isNull, gt/lt, or, notIn, jsonContains |
| **Pagination** | `JdbcDataManagerCrudTest` | 1 method (basic page request) | PARTIAL -- missing: multi-page, sort, empty result, boundary cases |
| **Soft Delete** | `JdbcDataManagerCrudTest` | 1 method (shouldSetDeletedFlag_whenDeleteSoftDeleteEntity) | PARTIAL -- missing: restoreById, includeDeleted, cascade soft delete |
| **Multi-Tenant Isolation** | `JdbcDataManagerTenantIsolationTest` | 8 methods (save/query/filter/scope) | GOOD |
| **Optimistic Lock** | `JdbcDataManagerOptimisticLockTest` | 4 methods (version increment, conflict, retry) | GOOD |
| **Aggregate** | `JdbcDataManagerAggregateTest` | 7 methods (count, groupBy, sum, avg, max/min, where, countDistinct) | GOOD |
| **Association** | `JdbcDataManagerAssociationTest` | 5 methods (ManyToOne, OneToMany, eager, error cases) | GOOD |
| **Projection** | `JdbcDataManagerProjectionTest` | 9 methods (record, condition, single, POJO, page, select, interface, count) | GOOD |
| **AutoConfiguration** | `DataManagerAutoConfigurationTest` | -- | COVERED |

**Missing DataManager integration tests**:
- `restoreById` / soft-delete recovery
- `includeDeleted()` query
- `saveAll` batch operations
- `existsById`
- `count` with condition
- `deleteByCondition`
- `updateAll` conditional updates
- `executeInTransaction`
- `withDataScope()` data permission
- `withAssociation` preloading
- Tree query operations (`JdbcTreeQuery`)
- EntityChangedEvent publishing
- IdGenerator integration with EntityInsertHandler
- DataSource routing (`@DataSource` annotation + `DataSourceAspect`)

## Caveats / Not Found

- Test file counts are based on `*Test.java` / `*Tests.java` naming. Any test files with non-standard names would be missed.
- Source file counts include `package-info.java` files, slightly inflating counts.
- The `gradle-plugin` module uses Kotlin (`.kt`) test files, counted separately.
- Integration test gap analysis is based on `@SpringBootTest` annotation search; some integration-style tests may use other Spring test annotations.
- Coverage ratio is a rough file-count metric, not a line-coverage percentage. Some test files contain many test methods (e.g., ConditionsTest with 20+ methods), while others may have only 1-2.
