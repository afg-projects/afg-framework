# Research: data-module-gaps

- **Query**: Analyze data module (data-core + data-sql + data-jdbc + data-liquibase) actual gaps vs PRD
- **Scope**: internal
- **Date**: 2026-06-13

## Findings

### 1. TreeQuery — Tree-shaped query support

**Status: PARTIALLY IMPLEMENTED (entity + path calculation exist; dedicated TreeQuery API does NOT exist)**

| File Path | Description |
|---|---|
| `data-core/src/main/java/.../data/core/entity/Treeable.java` | Tree structure trait interface (parentId, level, path, sortOrder, children) |
| `data-core/src/main/java/.../data/core/entity/TreeEntity.java` | Tree entity base class extending BaseEntity implements Treeable |
| `data-impl/data-jdbc/src/main/java/.../data/jdbc/TreePathCalculator.java` | Static utility: calculates level and path from parent on insert/update |
| `data-core/src/main/java/.../data/core/metadata/EntityTrait.java` | EntityTrait.TREEABLE flag for metadata detection |
| `data-core/src/test/java/.../data/core/entity/TreeEntityTest.java` | Unit tests for TreeEntity |

**What exists:**
- `Treeable<T>` interface with full contract (parentId, level, path, sortOrder, children)
- `TreeEntity<T>` abstract base class providing default field implementations
- `TreePathCalculator` automatically computes level/path on insert and update (called by `EntityInsertHandler` and `EntityUpdateHandler`)
- `EntityTrait.TREEABLE` metadata trait detection

**What is MISSING:**
- No `TreeQuery` API on DataManager/EntityProxy (e.g., `findChildren(parentId)`, `findDescendants(id)`, `findAncestors(id)`, `findTree()` that returns a nested structure)
- No dedicated tree query builder that leverages the `path` field for efficient subtree queries (`WHERE path LIKE '/1/5/%'`)
- No `moveNode(id, newParentId)` operation that recalculates paths for the moved node and all descendants
- Tree queries must currently be done manually via `Conditions.builder().like("path", "/1/5/%").build()` and assembling children in application code

**Assessment:** The data model and automatic path calculation are solid. A dedicated TreeQuery API would significantly improve developer experience for tree operations.

---

### 2. IdGenerator SPI — Data-layer ID generation

**Status: EXISTS IN CORE MODULE, NOT IN DATA MODULE (no conflict)**

| File Path | Description |
|---|---|
| `core/src/main/java/.../core/api/id/IdGenerator.java` | SPI interface: nextId(), nextIdAsString(), getType() |
| `core/src/main/java/.../core/api/id/IdGeneratorType.java` | Enum: SNOWFLAKE, UUID, SEGMENT |
| `core/src/main/java/.../core/api/id/SnowflakeIdGenerator.java` | Snowflake implementation |
| `core/src/main/java/.../core/api/id/UuidIdGenerator.java` | UUID implementation |
| `core/src/main/java/.../core/api/id/NoOpIdGenerator.java` | NoOp fallback |
| `core/src/main/java/.../core/autoconfigure/IdGeneratorAutoConfiguration.java` | AutoConfiguration with @ConditionalOnMissingBean |

**Analysis:**
- `IdGenerator` SPI lives exclusively in `core` module (`io.github.afgprojects.framework.core.api.id`)
- Data modules (`data-core`, `data-jdbc`) have **zero** references to `IdGenerator`
- JdbcDataManager uses database auto-increment for ID generation (via `Statement.RETURN_GENERATED_KEYS`)
- No SEGMENT (database-based) implementation exists (noted as "暂不支持" in IdGeneratorAutoConfiguration line 90)
- No conflict between core and data module IdGenerator

**What is MISSING:**
- No integration between `IdGenerator` (core) and `JdbcDataManager` (data-jdbc) — DataManager does not use IdGenerator to pre-generate IDs before insert
- No SEGMENT/step-based ID generator (database sequence table approach)
- If PRD envisions DataManager using IdGenerator for non-auto-increment primary keys, this integration is absent

---

### 3. EncryptedTypeHandler — Field encryption

**Status: FULLY IMPLEMENTED (SPI + annotation + auto encrypt/decrypt + NoOp fallback)**

| File Path | Description |
|---|---|
| `data-core/src/main/java/.../data/core/entity/FieldEncryptor.java` | SPI interface: encrypt(plaintext, algorithm, keyRef), decrypt(ciphertext, algorithm, keyRef) |
| `data-core/src/main/java/.../data/core/entity/NoOpFieldEncryptor.java` | NoOp fallback (returns plaintext unchanged) |
| `apt-api/src/main/java/.../apt/entity/EncryptedField.java` | Annotation: @EncryptedField(algorithm="AES", keyRef="") |
| `data-impl/data-jdbc/src/main/java/.../data/jdbc/EntityInsertHandler.java` | Auto-encrypts @EncryptedField on INSERT (line ~372-387) |
| `data-impl/data-jdbc/src/main/java/.../data/jdbc/EntityUpdateHandler.java` | Auto-encrypts @EncryptedField on UPDATE (line ~188-203) |
| `data-impl/data-jdbc/src/main/java/.../data/jdbc/EntityMapper.java` | Auto-decrypts on SELECT (afterLoad, line ~141-174) |
| `data-impl/data-jdbc/src/main/java/.../data/jdbc/autoconfigure/DataManagerAutoConfiguration.java` | Injects FieldEncryptor into JdbcDataManager (line ~94-119) |
| `apt-impl/src/main/java/.../apt/entity/EntityMetadataProcessor.java` | APT validates @EncryptedField only on String fields |

**What exists:**
- Complete SPI with encrypt/decrypt contract
- APT compile-time validation (only String fields allowed)
- Auto-encrypt on INSERT and UPDATE
- Auto-decrypt on SELECT (before afterLoad callback)
- NoOp fallback via @ConditionalOnMissingBean
- Configurable algorithm and keyRef per field

**What is MISSING:**
- No built-in AES implementation of FieldEncryptor (only NoOp exists in framework; business must provide a real implementation)
- No `FieldEncryptionAutoConfiguration` that provides a default AES-based FieldEncryptor with key management from config properties
- The `afg.data.field-encryption.keys.*` config referenced in @EncryptedField Javadoc has no corresponding Properties class or AutoConfiguration

---

### 4. EntityChangedEvent — Entity change event publishing

**Status: NOT IMPLEMENTED**

No `EntityChangedEvent`, `EntityChangeEvent`, or any entity-level event publishing mechanism exists in data-core or data-jdbc.

**What exists (related):**
- `LifecycleCallbacks` interface in data-core provides `beforeCreate()`, `beforeUpdate()`, `afterLoad()`, `beforeDelete()` — but these are synchronous in-entity callbacks, not Spring ApplicationEvent publishing
- Core module has `EventPublisher` SPI and `DomainEventPublisher` — but data module does not integrate with them
- No automatic event publishing after save/update/delete operations

**What is MISSING:**
- No `EntityChangedEvent` class (with entity type, operation type, old/new values, changed fields)
- No automatic event publishing from EntityInsertHandler/EntityUpdateHandler/EntityDeleteHandler
- No integration with core's `EventPublisher` or `DomainEventPublisher`
- No `@EntityListener` or observer mechanism for cross-cutting reactions to entity changes

---

### 5. PessimisticLock — Pessimistic lock support

**Status: FULLY IMPLEMENTED**

| File Path | Description |
|---|---|
| `data-core/src/main/java/.../data/core/query/BaseQuery.java` | `withPessimisticLock()` method on BaseQuery interface (line ~125-138) |
| `data-core/src/main/java/.../data/core/EntityQuery.java` | Inherits withPessimisticLock() with full Javadoc (line ~201-214) |
| `data-impl/data-jdbc/src/main/java/.../data/jdbc/JdbcEntityQuery.java` | Implementation: sets pessimisticLock=true (line ~312-314), appends FOR UPDATE syntax (line ~530) |
| `data-impl/data-jdbc/src/main/java/.../data/jdbc/JdbcProjectedQuery.java` | Delegates to entityQuery.withPessimisticLock() (line ~159-161) |
| `data-core/src/main/java/.../data/core/dialect/Dialect.java` | `getForUpdateSyntax()` method (line ~117) |
| `data-core/src/main/java/.../data/core/dialect/AbstractDialect.java` | Default: "FOR UPDATE" (line ~148) |
| `data-core/src/main/java/.../data/core/dialect/SQLServerDialect.java` | SQL Server override (line ~86) |

**What exists:**
- `withPessimisticLock()` on BaseQuery/EntityQuery chainable API
- JdbcEntityQuery appends dialect-specific FOR UPDATE syntax
- Dialect SPI supports database-specific FOR UPDATE variants

---

### 6. TenantDataSourceResolver — Multi-tenant datasource routing

**Status: NOT IMPLEMENTED**

No `TenantDataSourceResolver`, `TenantRoutingDataSource`, or any tenant-to-datasource mapping exists.

**What exists (related):**
- Multi-tenant is implemented via **tenant column filtering** (same database, tenant_id column), not separate databases
- `TenantContextHolder` + `TenantScope` manage tenant ID in thread-local
- JdbcEntityQuery/JdbcEntityQueryExecutor automatically append `AND tenant_id = :tenantId` WHERE clause
- EntityInsertHandler auto-fills tenantId on insert
- `MultiDataSourceAutoConfiguration` in core supports multiple named datasources (master/slave) via MyBatis-Plus Dynamic Datasource, but this is for read-write separation, NOT tenant-based routing

**What is MISSING:**
- No `TenantDataSourceResolver` interface (tenant ID -> DataSource mapping)
- No `TenantRoutingDataSource` that routes to different databases per tenant
- No integration between tenant resolution and datasource selection
- Current multi-tenant is schema-isolation (shared database, tenant column) only; database-isolation (separate database per tenant) is not supported

---

### 7. @DataSource — Multi-datasource switching annotation

**Status: PARTIALLY IMPLEMENTED (programmatic API exists; declarative annotation does NOT exist in framework)**

| File Path | Description |
|---|---|
| `data-core/src/main/java/.../data/core/EntityQuery.java` | `withDataSource(String name)` method (line ~322) |
| `data-core/src/main/java/.../data/core/EntityProxy.java` | `withDataSource(String name)` convenience method (line ~120-126) |
| `data-impl/data-jdbc/src/main/java/.../data/jdbc/JdbcEntityQuery.java` | Sets dataSourceName field (line ~247-249) |
| `core/src/main/java/.../core/autoconfigure/MultiDataSourceAutoConfiguration.java` | Multi-datasource config based on MyBatis-Plus DynamicRoutingDataSource |

**What exists:**
- `withDataSource(name)` on EntityQuery/EntityProxy for programmatic datasource switching
- `MultiDataSourceAutoConfiguration` in core provides DynamicRoutingDataSource setup
- MyBatis-Plus `@DS` annotation is available if `dynamic-datasource-spring-boot-starter` is on classpath

**What is MISSING:**
- No framework-native `@DataSource` annotation (relies on third-party `@DS` from MyBatis-Plus)
- `JdbcEntityQuery.dataSourceName` field is set but **never consumed** — the actual query execution does not use this field to switch datasource. The `withDataSource()` API is a no-op at runtime.
- No AOP/interceptor that reads `@DataSource` and switches datasource context

---

### Additional Checks

#### DataManager API completeness

**Status: COMPREHENSIVE**

The DataManager interface provides:
- Entity operations: `entity(Class)` -> EntityProxy, `getEntityMetadata(Class)`
- SQL builders: `query()`, `update()`, `insert()`, `delete()`
- Transaction: `executeInTransaction()`, `executeInReadOnly()`
- Tenant: `tenantScope()`, `getTenantContextHolder()`
- Raw SQL: `executeUpdate()`, `queryForList()`, `queryForObject()`, `queryForOptional()`, `queryForCount()`
- Shortcuts: `findById`, `findOneByField`, `findAllByField`, `existsByField`, `countByField`, `save`, `saveAll`, `insertAll`, `deleteById`, `deleteAllById`, `findAllById`, `existsById`, `update`, `updateAll`, `restoreById`, `findAll`, `findOne`, `findList`, `findListWithDataScope`, `count`, `countByCondition`, `existsByCondition`, `deleteByCondition`

#### EntityProxy chainable API

**Status: COMPREHENSIVE**

EntityProxy extends EntityReader + EntityWriter, plus convenience methods:
- `withDataScope()`, `withDataScopes()`, `withTenant()`, `withDataSource()`, `withReadOnly()`, `includeDeleted()`, `withAssociation()`, `withAssociations()`, `clearAssociations()`
- `fetch()`, `fetchAll()` for lazy loading
- `findAll(Condition)`, `findAll(Condition, PageRequest)`, `count(Condition)`, `exists(Condition)`, `findOne(Condition)`, `findFirst(Condition)`

#### Conditions builder — IfPresent dynamic conditions

**Status: FULLY IMPLEMENTED**

Both `ConditionBuilder` (string-based) and `TypedConditionBuilder` (lambda-based) provide:
- `eqIfPresent`, `neIfPresent`, `likeIfPresent`, `inIfPresent`, `notInIfPresent`, `betweenIfPresent`, `gtIfPresent`, `geIfPresent`, `ltIfPresent`, `leIfPresent`
- Static methods on `Conditions` class also available
- Null/empty values are silently skipped (no condition added)

#### Soft delete implementation

**Status: FULLY IMPLEMENTED**

- `SoftDeletable` / `TimestampSoftDeletable` interfaces
- `SoftDeleteEntity` / `TimestampSoftDeleteEntity` base classes
- `EntitySoftDeleteHandler` automatically appends `deleted = false` / `deletedAt IS NULL` filter
- `includeDeleted()` bypasses soft delete filter
- `restoreById()` / `restoreAllById()` for recovery
- Delete on soft-delete entities performs logical delete (sets deleted=true / deletedAt=now)

#### Optimistic lock implementation

**Status: FULLY IMPLEMENTED**

- `Versioned` interface with `getVersion()`, `setVersion()`, `incrementVersion()`
- `VersionedEntity` base class
- `EntityUpdateHandler` checks `affectedRows == 0` for versioned entities and throws `OptimisticLockException`
- SQL UPDATE includes `WHERE version = ?` and `SET version = version + 1`

#### Audit (createBy/updateBy) implementation

**Status: FULLY IMPLEMENTED**

- `Auditable` marker interface
- `FullEntity` base class with createBy/updateBy fields
- `AuditableContext` SPI interface with `getCurrentUserId()`
- `NoOpAuditableContext` fallback
- `EntityInsertHandler.autoFillAuditable()` fills createBy/updateBy on insert
- `EntityUpdateHandler` fills updateBy on update

#### Multi-tenant implementation

**Status: FULLY IMPLEMENTED (column-based isolation only)**

- `TenantEntity` base class with tenantId field
- `TenantContextHolder` thread-local tenant context
- `TenantScope` try-with-resources scope management
- Automatic tenant filter on all queries (`AND tenant_id = :tenantId`)
- Auto-fill tenantId on insert
- `withTenant(tenantId)` for explicit tenant override
- **Limitation:** Database-level isolation (separate database per tenant) is NOT supported

#### Data scope (data permission) implementation

**Status: FULLY IMPLEMENTED**

- `DataScope` / `DataScopeType` (ALL, SELF, DEPT, DEPT_AND_CHILD, CUSTOM)
- `DataScopeSqlBuilder` in data-sql module
- `DataScopeContextProvider` SPI
- `withDataScope()` on EntityQuery/EntityProxy
- Automatic dept field detection

## Summary: Gap Table

| PRD Feature | Status | Priority | Notes |
|---|---|---|---|
| TreeQuery | PARTIAL | Medium | Entity+path calc exist; no dedicated tree query API (findChildren, findDescendants, moveNode) |
| IdGenerator SPI (data layer) | NOT IN DATA | Low | Exists in core; not integrated with DataManager (DataManager uses DB auto-increment) |
| EncryptedTypeHandler | IMPLEMENTED | — | SPI+annotation+auto encrypt/decrypt complete; missing built-in AES impl |
| EntityChangedEvent | NOT IMPLEMENTED | Medium | No entity change event publishing; LifecycleCallbacks exist but are in-entity only |
| PessimisticLock | IMPLEMENTED | — | withPessimisticLock() + dialect FOR UPDATE fully working |
| TenantDataSourceResolver | NOT IMPLEMENTED | Medium | Column-based tenant isolation works; database-level tenant isolation not supported |
| @DataSource | PARTIAL | High | withDataSource() API exists but is a **runtime no-op** (dataSourceName field is set but never consumed); no native @DataSource annotation |

## Caveats / Not Found

- The `withDataSource(name)` API on EntityQuery/EntityProxy appears to be a **dead API** — `JdbcEntityQuery.dataSourceName` is stored but never used in query execution. This is a functional gap, not just a missing feature.
- No built-in AES `FieldEncryptor` implementation exists; business applications must provide their own. The `afg.data.field-encryption.keys.*` configuration referenced in `@EncryptedField` Javadoc has no corresponding Properties class.
- The SEGMENT IdGenerator type is documented as "暂不支持" (not yet supported) in `IdGeneratorAutoConfiguration`.
