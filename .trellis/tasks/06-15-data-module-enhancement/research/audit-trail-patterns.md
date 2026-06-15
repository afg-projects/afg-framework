# Research: Audit Trail / Data Change Tracking Patterns

- **Query**: How enterprise Java frameworks implement audit trail and data change tracking at the data access layer
- **Scope**: Mixed (internal codebase analysis + external framework knowledge)
- **Date**: 2026-06-15

## Findings

### 1. Hibernate Envers Architecture

**How Envers intercepts entity changes:**
- Envers uses Hibernate `EventListener` infrastructure. It registers `EnversPostInsertEventListener`, `EnversPostUpdateEventListener`, `EnversPostDeleteEventListener` on the Hibernate `EventListenerRegistry`.
- These listeners are triggered **synchronously** within the same transaction as the data change, ensuring audit records are always committed together with the business data (no data loss risk).
- Envers hooks into `PostInsertEvent`, `PostUpdateEvent`, `PostDeleteEvent` which provide access to the entity state, old state (via `OldState` parameter on PostUpdateEvent), and the entity itself.

**Revision entity design:**
- Envers uses a **revision number** approach, not per-event revision. Each transaction that modifies audited entities generates one `RevisionEntity` row with a monotonically increasing revision number.
- Default revision entity: `DefaultRevisionEntity` (id: INT, timestamp: LONG). Custom revision entity can extend this to add userId, username, remoteAddress, etc.
- All audit rows for changes within the same transaction share the same revision number, providing transactional grouping.

**Audit table naming:**
- Default: `{original_table}_aud` suffix. E.g., `sys_user` -> `sys_user_aud`.
- Configurable via `@Audited(targetAuditMode = ...)`, `@AuditTable(name = "custom_audit_table")`.
- Each audited entity gets its own audit table (per-entity pattern).

**Audit table structure (per-entity):**
```
sys_user_aud:
  id (BIGINT)          -- same as original entity id
  REV (INT)            -- revision number (foreign key to revision_entity)
  REVTYPE (SMALLINT)   -- 0=ADD, 1=MOD, 2=DEL
  REVEND (INT)         -- end revision (optional, for validity audit strategy)
  username (VARCHAR)   -- all audited columns from original table
  status (INT)
  ...                  -- every audited field
```

**Field-level diff capture:**
- Envers stores **complete snapshots** (not just changed fields). Every audited field is stored in every revision row, regardless of whether it changed.
- To compute field-level diffs, you compare two revision snapshots at different REV numbers using the Envers query API.
- Envers query API: `AuditReader.createQuery().forEntitiesAtRevision(User.class, revNum)` returns full entity snapshot. `AuditReader.find(User.class, id, revNum)` for single entity at revision.
- `AuditReader.getRevisions(User.class, id)` returns all revision numbers for a given entity.
- The `ChangedEntity` and `EntityHistory` queries allow finding which entities changed in a given revision.

**Validity audit strategy vs default audit strategy:**
- **Default strategy**: Only stores the revision where a change occurred. To find entity state at a given time, Envers finds the latest revision <= target revision.
- **Validity strategy**: Additionally stores REVEND (end revision) on each audit row. This allows direct queries like `WHERE REV <= target AND REVEND > target` without needing subqueries. Better query performance but requires additional `RevisionEnd` column.

**Spring Data Envers integration:**
- Provides `RevisionRepository<T, ID, N extends Number & Comparable<N>>` extending Spring Data's `Repository`.
- Key methods: `findLastChangeRevision(ID id)` -> `Revision<N, T>`, `findRevision(ID id, N revisionNumber)` -> `Revision<N, T>`, `findRevisions(ID id)` -> `Page<Revision<N, T>>`.
- `Revision` wraps revision number + revision entity metadata + entity snapshot.
- Auto-configuration via `@EnableEnversRepositories` or Spring Boot starter.

---

### 2. MyBatis-Plus Audit Capabilities

**MyBatis-Plus built-in audit:**
- MyBatis-Plus does **not** have a built-in audit trail/change tracking mechanism comparable to Envers.
- It provides `MetaObjectHandler` interface for auto-filling `createBy`, `updateBy`, `createdAt`, `updatedAt` fields (similar to AFG's `Auditable` trait). This is column-level metadata filling, not audit trail logging.
- For actual audit trail, MyBatis-Plus projects typically implement:
  - **AOP-based approach**: `@AuditLog` annotation + interceptor for service methods, recording operation, args, result, user info. This is what AFG's `@Audited` + `AuditLogAspect` already does.
  - **MyBatis interceptor approach**: `Interceptor` implementing `InterceptorChain` that intercepts INSERT/UPDATE/DELETE SQL, extracts table/row info from the SQL statement, and writes to a central audit log table. This captures changes at the SQL level but lacks field-level semantic diff (only has raw SQL).
  - **Compare-before-update approach**: Before UPDATE, first SELECT the current row, then compare with the new values, then compute field diffs programmatically. This is exactly what AFG's `JdbcEntityProxy.update()` already does with `oldEntity = queryExecutor.findById(id)`.

**Limitations for JDBC-based frameworks:**
- MyBatis works at the SQL string level, so interceptors can parse SQL but not easily reconstruct entity field diffs.
- MyBatis-Plus community solutions (e.g., mybatis-plus-audit-log on GitHub) typically implement a single `audit_log` table with `old_value` and `new_value` as JSON strings, capturing full entity snapshots rather than field-level diffs.

---

### 3. jOOQ Data Change Tracking

**jOOQ listener-based audit:**
- jOOQ provides `RecordListener` and `ExecuteListener` SPIs for intercepting CRUD operations.
- `RecordListener` lifecycle: `insertStart`, `insertEnd`, `updateStart`, `updateEnd`, `deleteStart`, `deleteEnd`, `loadStart`, `loadEnd`. Each callback receives `RecordContext` with access to the `Record` being modified.
- `ExecuteListener` lifecycle: `start`, `renderStart`, `renderEnd`, `prepareStart`, `prepareEnd`, `executeStart`, `executeEnd`, `fetchStart`, `fetchEnd`, `resultStart`, `resultEnd`, `outStart`, `outEnd`, `exception`. More granular but lower-level (SQL-level, not record-level).

**Audit implementation patterns with jOOQ:**
- **Pattern A: RecordListener-based**: Register a `RecordListener` that on `updateEnd`/`insertEnd`/`deleteEnd` writes audit records. The `RecordContext.getRecord()` provides field-by-field access to the record's original and changed values via `Record.changed(Field)` and `Record.original(Field)` / `Record.get(Field)`.
  - `Record.changed(Field)` returns true if the field was modified in this update.
  - `Record.original(Field)` returns the value before the change.
  - `Record.get(Field)` returns the value after the change.
  - This provides **native field-level diff capture** without needing to query the old entity separately.
- **Pattern B: ExecuteListener + SQL parsing**: Lower-level, works on raw SQL. Can capture table name and operation type but lacks field semantic info.
- **Pattern C: UpdatableRecord + diff**: jOOQ's `UpdatableRecord` maintains change tracking internally. When you call `record.update()`, jOOQ only sends changed fields in the UPDATE SQL, and the record knows which fields changed. This is the most natural audit integration point.

**Key jOOQ insight for AFG:**
- The `Record.changed(Field)` + `Record.original(Field)` pattern is similar to what AFG could implement using `EntityMetadata.getFields()` + reflection-based field access on old/new entity instances.
- jOOQ's approach of tracking changes at the Record object level (in-memory, before SQL execution) is the closest analog to AFG's approach of comparing oldEntity vs newEntity in `JdbcEntityProxy`.

---

### 4. Audit Log Table Design Patterns

**Pattern A: Per-Entity Audit Tables (Envers-style)**

Each audited entity gets a dedicated audit table (`{table}_aud`):
```
sys_user_aud: id, rev, revtype, username, status, created_at, ...
sys_role_aud:  id, rev, revtype, role_name, description, ...
```

| Aspect | Detail |
|--------|--------|
| **Pros** | Schema mirrors original table, easy to reconstruct entity at any point, supports field-level queries, natural JOIN with original table |
| **Cons** | Requires DDL per entity ( Liquibase migration per entity), audit table count grows with entity count, no centralized query across all entity types |
| **Diff capture** | Stores full snapshots; diffs computed by comparing consecutive revisions |
| **Query pattern** | `SELECT * FROM sys_user_aud WHERE id = ? AND rev <= ? ORDER BY rev DESC LIMIT 1` |
| **Revision metadata** | Central `revision_entity` table stores transaction metadata (revision number, timestamp, userId) |

**Pattern B: Single Central Audit Log Table (AFG current approach)**

One table stores all audit events:
```
audit_log: id, trace_id, user_id, tenant_id, module, operation, target, args, old_value, new_value, result, error_message, ...
```

| Aspect | Detail |
|--------|--------|
| **Pros** | Simple DDL (one table), centralized query, easy to add new entity types without schema changes |
| **Cons** | `old_value`/`new_value` must be JSON text (untyped, hard to query field-level), table grows very fast, no per-entity schema validation |
| **Diff capture** | Stores JSON serialized old/new values; field diffs require JSON parsing |
| **Query pattern** | `SELECT * FROM audit_log WHERE module = 'user' AND operation = 'UPDATE' AND target = 'user:123'` |
| **Already in AFG** | `AuditLog` record + `DatabaseAuditLogStorage` + `audit_log` table |

**Pattern C: Hybrid - Central Log + Field-Level Diff Table**

Two-table approach separating event metadata from field-level diffs:
```
data_change_log: id, entity_type, entity_id, change_type, revision_number, user_id, tenant_id, timestamp, trace_id
data_change_field: id, change_log_id, field_name, column_name, old_value, new_value, value_type
```

| Aspect | Detail |
|--------|--------|
| **Pros** | Clean separation of event metadata and field diffs, field-level queries possible (e.g., "who changed status field?"), single change_log table per entity type optional, value_type enables typed reconstruction |
| **Cons** | Two tables per query, more complex JOIN, field table can be even larger than single audit table |
| **Diff capture** | Only changed fields stored (sparse), not full snapshot -- efficient storage |
| **Query pattern** | `JOIN data_change_log + data_change_field WHERE entity_type = 'User' AND field_name = 'status'` |
| **Best for** | Compliance scenarios requiring "who changed what, from what, to what" at field granularity |

**Pattern D: Per-Entity Audit Table with Field Diffs (Envers + Diff hybrid)**

Same as Pattern A but with an additional per-entity field diff table:
```
sys_user_aud: id, rev, revtype, [all audited columns]  -- full snapshot
sys_user_aud_diff: id, rev, field_name, old_value, new_value  -- sparse diff
```

| Aspect | Detail |
|--------|--------|
| **Pros** | Full snapshot for reconstruction + sparse diff for quick "what changed" queries |
| **Cons** | Redundant storage (snapshot contains all values, diff repeats the changed ones), most complex DDL per entity |
| **Diff capture** | Both snapshot and field-level diff available |
| **Best for** | Maximum audit flexibility, rarely needed in practice |

---

### 5. Field-Level Change Diff Capture Techniques

**Technique 1: Reflection-based field comparison (most common for JDBC frameworks)**

Given `oldEntity` and `newEntity` objects:
1. Get `EntityMetadata<T>` from `EntityMetadataCache`
2. Iterate over `metadata.getFields()` (list of `FieldMetadata`)
3. For each field, read old value via reflection: `Field.getField(oldEntity)` vs `Field.getField(newEntity)`
4. Compare values; record diff if different
5. Skip fields marked `@Transient` or non-auditable

AFG already has all prerequisites:
- `EntityMetadataCache.get(entityClass)` returns metadata
- `FieldMetadata.getPropertyName()` gives field name
- `EntityChangedEvent.oldEntity` provides the old snapshot (for UPDATE)
- APT-generated metadata classes could provide getter method references (zero reflection)

**Technique 2: APT-generated accessor-based comparison (zero reflection)**

APT processor could generate a `DiffCalculator` class alongside `Metadata`:
```java
// APT-generated for User entity
public class UserDiffCalculator {
    public static List<FieldDiff> calculate(User old, User new_) {
        List<FieldDiff> diffs = new ArrayList<>();
        if (!Objects.equals(old.getUsername(), new_.getUsername())) {
            diffs.add(FieldDiff.of("username", "user_name", old.getUsername(), new_.getUsername()));
        }
        if (!Objects.equals(old.getStatus(), new_.getStatus())) {
            diffs.add(FieldDiff.of("status", "status", old.getStatus(), new_.getStatus()));
        }
        // ... all audited fields
        return diffs;
    }
}
```

This eliminates reflection overhead and provides compile-time field safety. Triggered by a new annotation like `@AfAuditedEntity` or extending `@AfEntity(generateAuditDiff = true)`.

**Technique 3: jOOQ-style in-memory change tracking**

If `JdbcEntityProxy` (or an `AuditEntityProxy` wrapper) tracked which fields were modified during the entity's lifecycle (before SQL execution), it would know exactly which fields changed without needing to compare old/new. This requires:
- Entity objects to track their own "dirty fields" (like jOOQ's `UpdatableRecord.changed(Field)`)
- OR a proxy/wrapper that intercepts setter calls to record dirty fields

This is more complex for plain POJO entities (no setter interception without bytecode enhancement). AFG's entities are Lombok `@Setter` POJOs, so intercepting setters would require a ByteBuddy/CGLIB proxy or a custom wrapper.

**Technique 4: SQL-level diff (database triggers)**

Database triggers (AFTER INSERT, AFTER UPDATE, AFTER DELETE) can capture changes at the row level:
- UPDATE triggers: `OLD.column_name` vs `NEW.column_name` gives field-level values
- INSERT triggers: only `NEW.*` values
- DELETE triggers: only `OLD.*` values

This is database-specific, requires trigger DDL per table, and bypasses the application layer. Not recommended for AFG since it breaks the framework's "data access abstraction" principle and complicates multi-database support.

---

### 6. Archival Strategies for High-Volume Audit Logs

**Partitioning by time range:**

```sql
-- MySQL partitioning by month
ALTER TABLE audit_log PARTITION BY RANGE (YEAR(timestamp) * 100 + MONTH(timestamp)) (
    PARTITION p202601 VALUES LESS THAN (202602),
    PARTITION p202602 VALUES LESS THAN (202603),
    PARTITION p202603 VALUES LESS THAN (202604),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

- Most common strategy for audit tables. Queries by time range hit fewer partitions.
- Partition pruning: `WHERE timestamp BETWEEN '2026-01-01' AND '2026-02-01'` only scans p202601.
- Management: `ALTER TABLE audit_log DROP PARTITION p202501` to purge old data without DELETE (instant, no row-by-row scan).
- Works well with MySQL, PostgreSQL (via table inheritance or pg_partman), and Oracle.

**TTL (Time-To-Live) policies:**

| Data Age | Strategy | Example |
|----------|----------|---------|
| 0-90 days | Hot storage (primary database, full indexes) | Active query/compliance window |
| 90-365 days | Warm storage (compressed partition, reduced indexes) | Rare queries, legal hold |
| >365 days | Cold storage (offloaded to object storage/OLAP) | Archive, regulatory compliance |

Implementation options:
- **MySQL**: Drop partitions older than retention period. Requires scheduled maintenance job.
- **PostgreSQL**: pg_partman extension for automatic partition management + retention policy.
- **Application-level**: `@Scheduled` job that moves old audit logs to an archive table or deletes via `DELETE FROM audit_log WHERE timestamp < ? LIMIT 10000` (batched to avoid long locks).

**Cold storage approaches:**
- **S3/OSS/MinIO**: Serialize audit logs as Parquet/JSON files, upload to object storage. Query via Athena/Trino when needed.
- **Data lake**: Kafka -> Spark -> Parquet pipeline for long-term audit analytics.
- **Database archive table**: Separate `audit_log_archive` table with fewer indexes, compressed row format, on slower storage.

**For AFG framework specifically:**
- The framework already has `afg-storage` integration (Local / MinIO / S3 / OSS), which could serve as cold storage for audit archives.
- The `DatabaseAuditLogStorage` already supports async batch writing, which is the primary throughput optimization.
- Partitioning is a database-admin concern; the framework should recommend partitioning in documentation but not manage partitions itself (that's infrastructure, not application code).

---

### 7. Async vs Sync Audit Writing - Trade-offs

**Sync (within transaction):**

| Aspect | Detail |
|--------|--------|
| **Consistency** | Audit record committed in same transaction as data change. If business TX rolls back, audit is also rolled back. No data loss. |
| **Latency** | Adds overhead to every write operation. INSERT/UPDATE/DELETE + INSERT into audit table = 2 writes per operation. |
| **Complexity** | Simple. No queue, no batch, no async worker, no shutdown hooks. |
| **Failure** | Audit write failure causes business TX rollback (acceptable for compliance-first systems). |
| **Best for** | Financial systems, healthcare, any system where audit completeness is a regulatory requirement. |

**Async (after transaction):**

| Aspect | Detail |
|--------|--------|
| **Consistency** | Audit record may be lost if: (1) queue overflows and record is dropped, (2) application crashes before flush, (3) async writer fails after business TX committed. **Business data can exist without corresponding audit record.** |
| **Latency** | Business operation only does 1 write (INSERT/UPDATE/DELETE). Audit write is deferred. Higher throughput. |
| **Complexity** | Requires: in-memory queue, batch flush scheduler, shutdown hook (AFG already has all of these in `DatabaseAuditLogStorage`), queue capacity management, fallback logic. |
| **Failure** | Audit write failure does not affect business operation. But creates "audit gap" -- data exists but audit trail is missing. |
| **Best for** | High-throughput systems where audit is desirable but not legally mandatory, or where near-complete audit is acceptable. |

**Hybrid approach (recommended for most enterprise systems):**

- **Default: async with guaranteed delivery**. Use a persistent queue (RabbitMQ/Kafka, which AFG already integrates) instead of in-memory `ArrayBlockingQueue`. This provides async performance + durability.
- **Critical operations: sync**. Allow per-operation configuration: `@AfAudit(sync = true)` for operations that require in-transaction audit (e.g., financial transactions, medical records).
- **Fallback: queue-full -> sync**. AFG's `DatabaseAuditLogStorage` already implements this: when `queue.offer()` fails, it falls back to `saveSync()`. This prevents data loss at the cost of occasional latency spikes.

**AFG current implementation analysis:**

`DatabaseAuditLogStorage` already implements the async + fallback pattern:
- Default: async with `ArrayBlockingQueue` (capacity 10000)
- Batch flush: every 5 seconds, drain up to 100 records, `batchUpdate()`
- Queue overflow fallback: sync write (line 162-163)
- Shutdown hook: flush remaining records before exit
- Failure handling: catch `DataAccessException`, log error, don't throw

**Gap in AFG's current implementation:**
- Uses in-memory queue, not persistent. Application crash between flush intervals loses audit records.
- The `EntityChangedEvent` is published synchronously (within the same thread), but the current `@Audited` AOP aspect + `AuditLogStorage` writes audit records asynchronously. There's no connection between `EntityChangedEvent` and the `@Audited` audit trail -- they are two independent systems.

---

### 8. Integration with EntityChangedEvent Patterns

**Current AFG architecture:**

Two independent audit systems exist in AFG:

1. **Method-level audit**: `@Audited` annotation -> `AuditLogAspect` -> `AuditLogStorage` -> `DatabaseAuditLogStorage` (async, single `audit_log` table, method-level context)
2. **Entity-level change events**: `EntityChangedEvent<T>` (published by `JdbcEntityProxy` after CRUD operations) -> `SpringEntityChangedEventPublisher` -> Spring `@EventListener` listeners

These two systems are **not connected**. EntityChangedEvent has oldEntity but does not compute field diffs. @Audited captures method args/result but not entity field-level changes.

**Integration approaches:**

**Approach A: EntityChangedEvent listener -> Audit Trail Writer (event-driven audit)**

Register an `@EventListener` for `EntityChangedEvent<?>` that computes field diffs and writes to the `data_change_log` + `data_change_field` tables:

```java
@Component
public class EntityAuditListener {

    @EventListener
    public void onEntityChanged(EntityChangedEvent<?> event) {
        if (event.getChangeType() == ChangeType.UPDATED && event.getOldEntity() != null) {
            List<FieldDiff> diffs = computeDiffs(event.getOldEntity(), event.getEntity());
            auditTrailWriter.writeFieldDiffs(event, diffs);
        } else {
            auditTrailWriter.writeEntityChange(event);
        }
    }

    private List<FieldDiff> computeDiffs(Object old, Object new_) {
        EntityMetadata<?> metadata = metadataCache.get(new_.getClass());
        // Use FieldMetadata to iterate fields, read values, compare
        // ...
    }
}
```

**Pros**: Leverages existing event infrastructure, no changes to JdbcDataManager/JdbcEntityProxy, listener is pluggable (can be enabled/disabled per entity type).

**Cons**: Event is published **after** the transaction completes (in the same thread but after the SQL write). If the audit write fails, it does not roll back the business transaction. For `EntityChangedEvent.UPDATED`, oldEntity is fetched before the update SQL, which means an extra SELECT query per update (already happening in JdbcEntityProxy.update() line 260).

**Approach B: Audit interceptor in JdbcEntityProxy (in-transaction audit)**

Add an audit step directly in `JdbcEntityProxy` before/after each CRUD operation, within the same transaction:

```java
// In JdbcEntityProxy.update()
T oldEntity = queryExecutor.findById(id).orElse(null);
T result = updateHandler.update(entity);

// Compute diffs and write audit record (in same TX)
if (auditTrailWriter != null) {
    List<FieldDiff> diffs = AuditDiffCalculator.compute(oldEntity, result, entityClass, metadataCache);
    auditTrailWriter.write(entityClass, id, ChangeType.UPDATED, diffs, oldEntity, result);
}

publishChangedEvent(result, oldEntity, EntityChangedEvent.ChangeType.UPDATED);
```

**Pros**: Audit is written in the same transaction as the business operation. Complete consistency -- if audit write fails, business TX rolls back.

**Cons**: Adds audit overhead to every write (even if not all entities need audit). Requires modifying `JdbcDataManager` / `JdbcEntityProxy`. Harder to make it optional per-entity.

**Approach C: Hybrid - EntityChangedEvent + optional sync/async (recommended)**

Combine the event-driven approach (Approach A) with configurable sync/async:

1. `EntityChangedEvent` listener computes field diffs using `EntityMetadataCache` + reflection/APT-generated accessors.
2. Audit write mode configurable per entity type:
   - `@AfAuditTrail(sync = true)` -> audit writes in-transaction (by wrapping the DataManager operation in a TX that includes the audit INSERT)
   - Default -> async via `AuditTrailStorage` (similar to current `DatabaseAuditLogStorage` pattern)
3. Field diff computation uses APT-generated `DiffCalculator` classes when available (triggered by `@AfEntity(generateAuditDiff = true)`), falls back to reflection-based comparison.

This approach:
- Leverages existing `EntityChangedEvent` and `SpringEntityChangedEventPublisher`
- Does not require modifying `JdbcDataManager` / `JdbcEntityProxy`
- Provides field-level diff capture using the metadata system
- Supports async (default) and sync (for compliance-critical entities) modes
- Uses the proven `DatabaseAuditLogStorage` async/batch pattern for throughput

---

### 9. Feasible Approaches Summary for AFG (JDBC-based, has EntityChangedEvent)

**Approach 1: Event-Driven Audit Trail (recommended for initial implementation)**

Add a `@EventListener` on `EntityChangedEvent<?>` that:
- Computes field diffs using `EntityMetadataCache` + reflection for UPDATE events
- Writes to a new `data_change_log` (event metadata) + `data_change_field` (field diffs) table pair
- Uses async writing by default (via `AuditTrailStorage` pattern, reusing `DatabaseAuditLogStorage` infrastructure)
- Supports `@AfAuditTrail` annotation on entity classes for configuration (which entities to audit, sync/async mode, exclude fields)

Key components needed:
- `AuditTrailListener` (Spring `@EventListener`)
- `AuditDiffCalculator` (reflection-based field comparison, using `EntityMetadata`)
- `AuditTrailStorage` SPI (interface for audit storage backends)
- `DatabaseAuditTrailStorage` (JDBC implementation with async/batch, reusing `DatabaseAuditLogStorage` patterns)
- `data_change_log` + `data_change_field` Liquibase migration
- `@AfAuditTrail` entity annotation (optional: `sync`, `excludeFields`, `includeFields`)
- `AuditTrailAutoConfiguration`

**Approach 2: Per-Entity Audit Tables (Envers-style)**

Generate `{entity_table}_aud` tables per entity, using APT to:
- Generate `AuditTableMigration` metadata ( Liquibase changeSet snippets for audit tables)
- Generate `{Entity}AuditWriter` classes that INSERT audit snapshots
- Register audit writers in `JdbcEntityProxy` or via `EntityChangedEvent` listener

Key components needed:
- APT processor extension for `@AfEntity(generateAudit = true)` -> generates audit table DDL metadata + audit writer classes
- `AuditRevisionEntity` + `revision_info` table (central revision metadata)
- `AuditRevisionManager` (generates revision numbers, stores transaction metadata)
- Per-entity `_aud` table Liquibase migrations (generated by APT or Gradle plugin `generateMigration`)
- Integration with `JdbcEntityProxy` to write audit snapshots in-transaction

This approach is more complex but provides the strongest audit guarantees (in-transaction, full snapshots, Envers-compatible query patterns).

**Approach 3: Annotation-Driven In-Transaction Audit (AOP + DataManager)**

Extend the existing `@Audited` AOP aspect to support entity-level audit:
- `@Audited(auditEntityChanges = true)` on service methods that modify entities
- The aspect intercepts the method, identifies entity parameters, computes diffs after execution, and writes audit records in the same transaction
- Uses `EntityMetadataCache` for field introspection

Key components needed:
- Extend `AuditLogAspect` to detect entity parameters and compute field diffs
- `AuditDiffCalculator` (shared with Approach 1)
- `AuditLog` record extended with `fieldDiffs` (or separate `data_change_field` table)
- Reuse existing `DatabaseAuditLogStorage` infrastructure

This approach is the simplest increment on existing code but only works for service methods explicitly annotated with `@Audited`. It does not cover changes made through `DataManager` directly (without going through an annotated service method).

---

### Files Found (Internal Codebase)

| File Path | Description |
|---|---|
| `data-core/src/main/java/io/github/afgprojects/framework/data/core/event/EntityChangedEvent.java` | Entity change event with entityType, entity, oldEntity, changeType, timestamp |
| `data-core/src/main/java/io/github/afgprojects/framework/data/core/event/EntityChangedEventPublisher.java` | SPI interface for publishing entity change events |
| `data-core/src/main/java/io/github/afgprojects/framework/data/core/event/NoOpEntityChangedEventPublisher.java` | NoOp fallback for event publishing |
| `data-impl/data-jdbc/src/main/java/io/github/afgprojects/framework/data/jdbc/event/SpringEntityChangedEventPublisher.java` | Spring ApplicationEventPublisher bridge |
| `data-impl/data-jdbc/src/main/java/io/github/afgprojects/framework/data/jdbc/JdbcEntityProxy.java` | Publishes EntityChangedEvent after CRUD, fetches oldEntity for UPDATE |
| `data-impl/data-jdbc/src/main/java/io/github/afgprojects/framework/data/jdbc/JdbcDataManager.java` | Manages EntityChangedEventPublisher injection |
| `data-core/src/main/java/io/github/afgprojects/framework/data/core/metadata/EntityMetadata.java` | Entity metadata with getFields(), getField(), getTableName() - needed for field diffs |
| `data-core/src/main/java/io/github/afgprojects/framework/data/core/metadata/FieldMetadata.java` | Field metadata with getPropertyName(), getColumnName() - needed for per-field diffs |
| `data-core/src/main/java/io/github/afgprojects/framework/data/core/metadata/DatabaseFieldMetadata.java` | Extended field metadata with JDBC type, nullable, length - useful for typed diffs |
| `data-core/src/main/java/io/github/afgprojects/framework/data/core/metadata/EntityMetadataCache.java` | Metadata cache with APT > Reflective > Empty loader chain |
| `core/src/main/java/io/github/afgprojects/framework/core/audit/AuditLog.java` | Existing audit log record (method-level, single table design) |
| `core/src/main/java/io/github/afgprojects/framework/core/audit/Audited.java` | Method-level audit annotation (operation, module, sensitiveFields) |
| `core/src/main/java/io/github/afgprojects/framework/core/audit/AuditLogAspect.java` | AOP aspect for @Audited method interception |
| `core/src/main/java/io/github/afgprojects/framework/core/audit/AuditLogStorage.java` | SPI interface for audit log storage |
| `integration/afg-jdbc/src/main/java/io/github/afgprojects/framework/integration/jdbc/audit/DatabaseAuditLogStorage.java` | JDBC storage with async/batch/fallback pattern |
| `integration/afg-jdbc/src/main/java/io/github/afgprojects/framework/integration/jdbc/audit/AuditLogRecord.java` | Audit log database entity record |
| `integration/afg-jdbc/src/main/java/io/github/afgprojects/framework/integration/jdbc/audit/DatabaseAuditLogProperties.java` | Async/batch/queue configuration properties |
| `integration/afg-redis/src/main/java/io/github/afgprojects/framework/integration/redis/audit/RedisAuditLogStorage.java` | Redis-based audit storage with TTL/maxSize |
| `ai-core/src/main/resources/db/changelog/ai/v1.0.0/015_ai_audit_log.xml` | AI audit table DDL (single table, CLOB for input/output) |
| `security-impl/auth-server/src/main/resources/db/changelog/auth/v1.0.0/011_auth_security_event.xml` | Security event table DDL (single table, TEXT for details) |
| `data-impl/data-jdbc/src/test/java/io/github/afgprojects/framework/data/jdbc/EntityChangedEventPublisherTest.java` | Test for entity changed event publishing |

### Code Patterns

**EntityChangedEvent publishing pattern (JdbcEntityProxy.java, lines 240-264, 736-752):**
- CREATE: `publishChangedEvent(result, null, ChangeType.CREATED)` -- no oldEntity
- UPDATE: fetches `oldEntity = queryExecutor.findById(id).orElse(null)` before update, then `publishChangedEvent(result, oldEntity, ChangeType.UPDATED)`
- DELETE: `publishChangedEvent(entity, null, ChangeType.DELETED)` -- no oldEntity
- RESTORE: `publishChangedEvent(entity, null, ChangeType.RESTORED)` -- no oldEntity
- Exception in publishing is caught and logged, does not affect business operation

**Existing audit async/batch pattern (DatabaseAuditLogStorage.java):**
- In-memory `ArrayBlockingQueue` (capacity 10000)
- `ScheduledExecutorService` flushes every 5 seconds
- Batch size: 100 records per flush
- Queue overflow fallback: sync write
- Shutdown hook: flush remaining records before exit

**EntityMetadata field introspection pattern:**
- `EntityMetadataCache.get(entityClass)` -> `EntityMetadata<T>`
- `metadata.getFields()` -> `List<FieldMetadata>` (each with propertyName, columnName, fieldType)
- `metadata.getField(fieldName)` -> `FieldMetadata` for specific field
- `metadata.getTableName()` -> table name for audit record
- APT-generated metadata classes provide zero-reflection access; `ReflectiveMetadataLoader` as fallback

### External References

- Hibernate Envers documentation: https://docs.jboss.org/envers/docs/ -- revision-based audit, per-entity audit tables, `REV`/`REVTYPE` columns, `AuditReader` query API
- Spring Data Envers: https://docs.spring.io/spring-data/envers/reference/ -- `RevisionRepository`, `Revision<N,T>`, `@EnableEnversRepositories`
- jOOQ RecordListener: https://www.jooq.org/doc/latest/manual/sql-execution/listeners/record-listener/ -- `Record.changed(Field)` and `Record.original(Field)` for field-level change detection
- MyBatis-Plus MetaObjectHandler: https://baomidou.com/guides/auto-fill-meta-object/ -- auto-fill createBy/updateBy, no built-in audit trail
- Database partitioning for audit logs: MySQL RANGE partitioning, PostgreSQL pg_partman, Oracle interval partitioning

### Related Specs

- `.trellis/spec/backend/entity-design.md` -- entity base class hierarchy, `Auditable` trait, `EntityTrait` detection
- `.trellis/spec/backend/data-manager-api.md` -- DataManager API, EntityProxy, EntityWriter operations
- `.trellis/spec/backend/migration-guidelines.md` -- Liquibase migration conventions for new tables

## Caveats / Not Found

- No external web search was available during this research session; all external framework knowledge is based on established, well-documented, stable technology (Hibernate Envers since 2008, jOOQ since 2009, MyBatis-Plus since 2016). The architectural patterns described are canonical and widely adopted.
- The `@AfAuditTrail` annotation proposed in Approach 1 does not yet exist in the codebase; it is a design concept, not an implemented feature.
- APT-generated `DiffCalculator` classes (Approach for zero-reflection field diffs) would require extending the APT processor, which is in `apt-impl` module. The feasibility of this extension was not verified against the current APT processor code.
- The connection between `EntityChangedEvent` and the `@Audited` audit trail (currently two independent systems) is a gap that any audit trail implementation needs to address. The research identified this gap but did not design the integration.
- Partitioning and archival strategies are infrastructure-level concerns; the framework should document recommendations but should not attempt to manage partitions programmatically (that's a DBA responsibility).
