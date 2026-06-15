# Research: Field-Level / Column-Level Access Control at the Data Access Layer

- **Query**: How enterprise Java frameworks implement field-level / column-level access control; feasible approaches for a JDBC-based framework with existing Casbin RBAC
- **Scope**: Mixed (internal codebase analysis + external framework patterns)
- **Date**: 2026-06-15

## Findings

### 1. Definitions and Scope

Field-level access control (also called column-level security) is distinct from both row-level security (DataScope) and data masking (desensitization):

| Concern | Question | Example |
|---------|----------|---------|
| **Row-level security** | Which ROWS can this user see? | User in dept A can only see dept A records |
| **Field-level access control** | Which COLUMNS can this user read/write? | HR can see salary, engineering cannot |
| **Data masking** | How should a visible value be displayed? | Phone 138\*\*\*\*5678 for non-admin users |

Field-level ACL has two sub-concerns:
- **Read restriction**: Hide or nullify columns the user is not authorized to see
- **Write restriction**: Prevent the user from updating columns they are not authorized to modify

---

### 2. Internal Codebase: Existing Infrastructure and Integration Points

#### 2.1 Casbin RBAC Model (auth-server)

The current Casbin model uses RBAC with domains:

```
[request_definition]
r = sub, dom, obj, act

[policy_definition]
p = sub, dom, obj, act

[role_definition]
g = _, _, _

[matchers]
m = g(r.sub, r.dom, p.sub) && r.dom == p.dom && keyMatch2(r.obj, p.obj) && r.act == p.act
```

**Key observation:** The current model uses `(sub, dom, obj, act)` where `obj` = resource (e.g., `sys_user`) and `act` = action (e.g., `read`, `write`). For field-level ACL, the `obj` dimension can be extended to include field identifiers (e.g., `sys_user.salary`) or a new `field` dimension can be added.

**Files:**

| File Path | Description |
|---|---|
| `security-impl/auth-server/.../properties/casbin/CasbinConfig.java:54-71` | Default RBAC-domain model text |
| `security-impl/auth-server/.../casbin/enforcer/CasbinAfgEnforcer.java` | Enforcer implementation with `(sub, domain, resource, action)` |
| `security-impl/auth-server/.../casbin/model/CasbinRule.java` | Policy rule entity: ptype + v0(sub) + v1(dom) + v2(obj) + v3(act) |
| `security-impl/auth-server/.../permission/service/CasbinRbacService.java` | RBAC service with `hasPermission(userId, permission, tenantId)` where permission is `resource:action` |

#### 2.2 AfgEnforcer Interface (core)

```java
@FunctionalInterface
public interface AfgEnforcer {
    boolean enforce(String subject, String resource, String action);
}
```

This is the core authorization check. For field-level ACL, the `resource` parameter would encode the entity+field combination (e.g., `sys_user:salary`) and `action` would be `read` or `write`.

**File:** `core/.../web/security/AfgEnforcer.java`

#### 2.3 DataScope (Existing Row-Level Security)

The existing `DataScope` annotation and `DataScopeSqlBuilder` provide row-level filtering by appending WHERE conditions to SQL queries. This is the closest existing pattern for SQL-level access control.

**Key integration points:**

| File Path | Description |
|---|---|
| `core/.../security/datascope/DataScope.java` | `@DataScope` annotation with table, column, scopeType |
| `core/.../security/datascope/DataScopeType.java` | ALL, SELF, DEPT, DEPT_AND_CHILD, CUSTOM |
| `data-impl/data-sql/.../scope/DataScopeSqlBuilder.java` | Converts DataScope to SQL WHERE conditions |
| `data-impl/data-sql/.../scope/DataScopeProcessor.java` | Resolves placeholders like `#{currentUserId}` |
| `data-impl/data-jdbc/.../JdbcEntityQuery.java:202-236` | `withDataScope()` integration on EntityQuery |
| `data-impl/data-jdbc/.../JdbcEntityQuery.java:507-511` | WHERE clause construction with DataScope |

**DataScope flow:** `EntityQuery.withDataScope()` -> `DataScopeSqlBuilder.buildSql()` -> appends parameterized WHERE conditions to SELECT/UPDATE/DELETE SQL.

#### 2.4 EntityMapper (Read-Path Interception Point)

`EntityMapper.map()` converts `ResultSet` to entity objects. It iterates over all `FieldMetadata` from `EntityMetadata.getFields()` and sets each field from the ResultSet column.

**Key observation:** This is the primary interception point for read-path column-level access control. After mapping (and after decryption), restricted fields could be nullified or set to a sentinel value.

**File:** `data-impl/data-jdbc/.../EntityMapper.java:111-153`

#### 2.5 SqlBuilder (Write-Path Interception Point)

`SqlBuilder.buildUpdateSql()` generates `UPDATE table SET col1=?, col2=?, ... WHERE id=?` for ALL non-id, non-generated fields. There is no column exclusion mechanism.

**Key observation:** To support write-path column restrictions, the UPDATE SQL builder must be modified to exclude fields the user is not authorized to update, or the update must be rejected if it contains unauthorized field modifications.

**File:** `data-impl/data-jdbc/.../SqlBuilder.java:128-150`

#### 2.6 EntityUpdateHandler (Write-Path Execution)

`EntityUpdateHandler.update()` calls `queryHelper.buildUpdateSql()` and `queryHelper.extractUpdateParams()` to build and execute the UPDATE statement. No field-level permission check exists.

**File:** `data-impl/data-jdbc/.../EntityUpdateHandler.java:91-125`

#### 2.7 JdbcEntityQuery select/exclude (Partial Column Selection)

The query builder already supports `select()` and `exclude()` methods that control which columns appear in the SELECT clause:

```java
dataManager.entity(User.class)
    .query()
    .select(User::getId, User::getName)  // only these columns
    .exclude("salary")                    // all columns except salary
    .list();
```

**Implementation:** `JdbcEntityQuery` has `selectedFields` and `excludedFields` that are resolved in `resolveFields()` at SQL build time. The `buildSelectSql()` method uses these to construct `SELECT col1, col2 FROM ...` instead of `SELECT *`.

**File:** `data-impl/data-jdbc/.../JdbcEntityQuery.java:64-72, 146-196, 604-640`

#### 2.8 EntityTrait and EntityMetadata (Extensibility)

The `EntityTrait` enum already supports custom traits via the open-closed principle. Adding `FIELD_ACCESS_CONTROLLED` would require no changes to existing code.

The `FieldMetadata` interface provides `getPropertyName()`, `getColumnName()`, `getFieldType()`. For field-level ACL, it would need to be extended (or a companion annotation/metadata added) to carry access control declarations.

**Files:**

| File Path | Description |
|---|---|
| `data-core/.../metadata/EntityTrait.java` | Trait enum (SOFT_DELETABLE, TENANT_AWARE, etc.) |
| `data-core/.../metadata/FieldMetadata.java` | Field metadata interface |
| `data-core/.../metadata/EntityMetadata.java` | Entity metadata interface with `hasTrait()`, `getFields()`, `getField()` |

#### 2.9 Jackson ObjectMapper Configuration

The framework configures a global `ObjectMapper` in `AfgAutoConfiguration` via `JacksonMapper.builder()`. This is the natural integration point for serialization-level field filtering.

**File:** `core/.../util/JacksonMapper.java`, `core/.../autoconfigure/AfgAutoConfiguration.java:72-73`

---

### 3. External Framework Patterns

#### 3.1 PostgreSQL Row Security Policies (RLS)

PostgreSQL supports row-level security policies via `CREATE POLICY`. Column-level access is handled through standard SQL `GRANT SELECT(column_name)` which controls which columns a database role can access.

**Relevant pattern:**
```sql
-- Column-level GRANT
GRANT SELECT (user_name, uid, gid, real_name) ON passwd TO public;
-- pwhash column is NOT accessible to public role
```

**Key insight:** PostgreSQL separates row-level (RLS policies) from column-level (GRANT on columns). The column-level GRANT is a hard gate -- the column literally does not appear in the query result for unauthorized roles. This is the SQL-rewrite approach at the database engine level.

**Limitation:** PostgreSQL column-level GRANT is per-database-role, not per-application-user. Application frameworks typically need their own mapping between application users/roles and database roles.

#### 3.2 Oracle Virtual Private Database (VPD) Column Masking

Oracle VPD supports column-level masking through `DBMS_RLS.ADD_POLICY` with `sec_relevant_cols` parameter:

```sql
BEGIN
  DBMS_RLS.ADD_POLICY(
    object_schema => 'hr',
    object_name   => 'employees',
    policy_name   => 'salary_mask',
    function_schema => 'hr',
    policy_function => 'mask_salary',
    sec_relevant_cols => 'salary,commission_pct',  -- columns to mask
    policy_type   => DBMS_RLS.CONTEXT_SENSITIVE
  );
END;
```

**Key insight:** When a query selects a masked column, the policy function returns a WHERE condition. If the condition fails, the masked column returns NULL while other columns remain visible. This is the "partial row nullification" approach -- the row is still returned, but sensitive columns are NULL.

**Limitation:** Oracle-specific. Not portable to MySQL/PostgreSQL.

#### 3.3 MyBatis-Plus Data Permission Interceptor

MyBatis-Plus provides a `DataPermissionInterceptor` that intercepts SQL statements and modifies them. For column-level control, the approach is:

1. **JSqlParser-based SQL rewriting**: The interceptor parses the SQL using JSqlParser, inspects the SELECT column list, and removes or modifies columns based on permission rules.
2. **Configuration via `IDataPermission` interface**: Users implement this interface to define which tables/columns a user can access.

**Key pattern:** The interceptor approach operates between the application and the database. It rewrites `SELECT col1, col2, salary, col4 FROM user` to `SELECT col1, col2, col4 FROM user` when the user lacks `salary` access.

**Limitation:** This requires parsing and rewriting every SQL statement, which adds overhead. It also interacts poorly with `SELECT *` queries and complex JOIN scenarios.

#### 3.4 jOOQ Row-Level Security and Column Masking

jOOQ supports access control through:

1. **`DSLContext` with `executeListener`**: Can intercept query execution and modify SQL before it runs.
2. **`Record`-level access control**: jOOQ's type-safe query DSL allows programmatic column selection, so code can conditionally include/exclude columns based on security context.
3. **`Converter` and `Binding`**: Similar to MyBatis TypeHandler, can transform values at read/write time.
4. **SQL-level RLS**: jOOQ can generate SQL that uses database-native RLS (PostgreSQL `CREATE POLICY`, Oracle VPD).

**Key pattern for column-level:** jOOQ's type-safe DSL naturally supports column exclusion because the query builder explicitly lists columns:
```java
// Conditionally include salary column
List<Field<?>> fields = new ArrayList<>(List.of(USERS.ID, USERS.NAME));
if (securityContext.canRead("salary")) {
    fields.add(USERS.SALARY);
}
dsl.select(fields).from(USERS).fetch();
```

**Limitation:** jOOQ's approach is code-level, not declarative. Every query must explicitly implement the conditional column selection.

#### 3.5 Hibernate / JPA Column-Level Security

Hibernate has no built-in column-level security. The community approaches are:

1. **`@Filter` and `@FilterDef`**: Hibernate's filter mechanism adds WHERE conditions (row-level), not column-level.
2. **`@ColumnTransformer(read=...)`**: Can apply a database function on read (e.g., `mask_salary(salary)`), but this is per-column, not per-user/role.
3. **Hibernate `@Where` clause**: Row-level filtering, not column-level.
4. **JPA `@Convert` / AttributeConverter**: Can transform values at read/write time, but no security context awareness.
5. **Hibernate Envers**: Audit trail, not access control.
6. **Spring Security `@PreAuthorize` on getter methods**: Can be used with `@Entity` but requires proxy-based lazy loading and is fragile.

**Most common Hibernate/JPA approach:** Jackson serialization-level filtering (see 3.6).

#### 3.6 Jackson Serialization-Level Filtering

This is the most widely used approach across all Java frameworks for **read-side** column-level access control:

**A. `@JsonView` (Built-in Jackson)**

```java
public class User {
    @JsonView(Views.Public.class)
    private String name;

    @JsonView(Views.Internal.class)
    private BigDecimal salary;
}

// Controller
@GetMapping("/users/public")
@JsonView(Views.Public.class)
public List<User> getPublicUsers() { ... }

@GetMapping("/users/internal")
@JsonView(Views.Internal.class)
public List<User> getInternalUsers() { ... }
```

**Limitation:** `@JsonView` is static -- you must define view classes at compile time and select them per endpoint. It does not dynamically adapt to the current user's roles.

**B. `@JsonFilter` (Built-in Jackson)**

```java
@JsonFilter("fieldAccessFilter")
public class User { ... }

// Dynamic filtering at serialization time
ObjectMapper mapper = new ObjectMapper();
FilterProvider filters = new SimpleFilterProvider()
    .addFilter("fieldAccessFilter",
        SimpleBeanPropertyFilter.filterOutAllExcept(allowedFields));
mapper.setFilterProvider(filters);
```

**Advantage:** `@JsonFilter` is dynamic -- the allowed field set can be computed at runtime based on the current user's permissions.

**C. Custom `BeanSerializerModifier` (Most Flexible)**

```java
public class FieldAccessSerializerModifier extends BeanSerializerModifier {
    @Override
    public List<BeanPropertyWriter> changeProperties(
            SerializationConfig config, BeanDescription beanDesc,
            List<BeanPropertyWriter> beanProperties) {
        // Remove or replace properties based on security context
        return beanProperties.stream()
            .filter(p -> fieldAccessService.canRead(
                beanDesc.getBeanClass(), p.getName()))
            .toList();
    }
}
```

**Advantage:** Can inspect security context, remove properties entirely, or replace them with null/masked values. Operates within the Jackson serialization pipeline.

**D. `ResponseBodyAdvice` (Spring MVC Level)**

```java
@ControllerAdvice
public class FieldAccessAdvice implements ResponseBodyAdvice<Object> {
    @Override
    public Object beforeBodyWrite(Object body, ...) {
        // Apply field-level filtering to response body
        return fieldAccessFilter.filter(body);
    }
}
```

**Limitation:** Only works for Spring MVC controller responses. Does not cover WebSocket, SSE, export, or internal data passing.

#### 3.7 Casbin ABAC for Field-Level Policies

Casbin's ABAC model can express field-level permissions by treating fields as resources:

```
[request_definition]
r = sub, dom, obj, field, act

[policy_definition]
p = sub, dom, obj, field, act

[matchers]
m = g(r.sub, r.dom, p.sub) && r.dom == p.dom && keyMatch2(r.obj, p.obj) && r.field == p.field && r.act == p.act
```

With policies like:
```
p, hr_role, default, sys_user, salary, read
p, hr_role, default, sys_user, salary, write
p, eng_role, default, sys_user, salary, !read
```

**Key consideration:** The current AFG Casbin model uses 4-tuple `(sub, dom, obj, act)`. Adding a `field` dimension requires a model migration. An alternative is to encode the field into the `obj` dimension: `obj = sys_user.salary`, `act = read`.

**Files:**

| File Path | Description |
|---|---|
| `security-impl/auth-server/.../casbin/model/CasbinRule.java` | Policy rule with v0-v3 (4 fields) |
| `security-impl/auth-server/.../properties/casbin/CasbinConfig.java:54-71` | Default model text |

---

### 4. Read-Side Approaches (Who can SEE which columns)

#### Approach A: SQL SELECT Column Rewrite

**Description:** Modify the SELECT clause to exclude columns the user is not authorized to see. Instead of `SELECT *`, generate `SELECT id, name, email FROM sys_user` (omitting `salary`).

**How it maps to AFG:**

The `JdbcEntityQuery` already has `selectedFields` and `excludedFields` that control which columns appear in the SELECT clause. Field-level ACL can inject into `resolveFields()` to add unauthorized fields to `excludedFields`.

**Implementation path:**

1. Define `@FieldAccess` annotation on entity fields (or class-level)
2. `FieldAccessService` SPI resolves which fields the current user can read
3. In `JdbcEntityQuery.buildSelectSql()` / `resolveFields()`, after computing selected/excluded fields, apply `FieldAccessService` to exclude unauthorized fields
4. The `EntityMapper` will naturally skip missing ResultSet columns (it checks `colIndex != null` at line 120)

**Integration with existing DataScope:** This approach parallels the existing DataScope pattern. Just as `withDataScope()` adds WHERE conditions, `withFieldAccess()` would add SELECT column restrictions. Both operate at the SQL level.

```
DataScope:  EntityQuery.withDataScope() -> DataScopeSqlBuilder -> appends WHERE conditions
FieldAccess: EntityQuery.withFieldAccess() -> FieldAccessService -> modifies SELECT columns
```

**Pros:**
- Column data never leaves the database -- strongest security guarantee
- No post-processing needed -- entity naturally has null for excluded fields
- Consistent with existing DataScope SQL-rewriting pattern
- Works for all consumers (API, export, logs) -- data is already restricted
- No Jackson dependency in data module

**Cons:**
- Breaks `SELECT *` caching in `SqlBuilder.getSelectBaseSql()` -- column list is now per-user
- Business logic that needs the real value (even for internal use) cannot access it
- Cannot support "mask instead of hide" -- either the column is present or absent
- Different users see different entity shapes, which complicates caching
- Adding/removing columns from SELECT requires new SQL cache keys (per user-role combination)

**Performance:**
- SQL cache miss rate increases because column list varies per user
- But: only a few distinct "field access profiles" exist in practice (e.g., admin, manager, employee)
- Cache key can be based on the field access profile hash, not individual user

#### Approach B: Entity Post-Processing (EntityMapper AfterLoad)

**Description:** After `EntityMapper.map()` creates the entity with all fields populated, a post-processing step nullifies or masks fields the user is not authorized to see.

**How it maps to AFG:**

The `EntityMapper.map()` method already has a post-mapping hook pattern (see `decryptFields()` at line 165). A `restrictFields()` method can be added after decryption and before `afterLoad`:

```java
// EntityMapper.map() line 142-146
decryptFields(entity);
restrictFields(entity);  // NEW: nullify unauthorized fields
LifecycleCallbacks.ifCallback(entity, LifecycleCallbacks::afterLoad);
```

**Pros:**
- Follows the exact same pattern as `decryptFields()` (existing precedent)
- Simple to implement -- one method, no SQL changes
- Entity has consistent shape (all fields exist, restricted ones are null)
- SQL cache is unaffected (always `SELECT *`)

**Cons:**
- Column data is fetched from database and then thrown away -- wasted I/O
- Entity field is null, not absent -- consumers cannot distinguish "no access" from "null value"
- If `afterLoad` callback or business logic needs the real value, it's already gone
- Must be applied to every entity read, even internal/admin reads
- No mask option -- field is either real or null

**Performance:**
- No SQL-level overhead (same `SELECT *` query)
- Minor Java-level overhead (setting fields to null)
- Wasted database I/O for fetched-but-not-used columns

#### Approach C: Jackson Serialization-Level Filtering

**Description:** The entity always holds all real values. Field-level access control is applied at the Jackson serialization layer, filtering out or masking unauthorized fields when the entity is serialized to JSON.

**How it maps to AFG:**

Register a `BeanSerializerModifier` on the global `ObjectMapper` that inspects a `FieldAccessService` to determine which fields to include, exclude, or mask:

```java
public class FieldAccessSerializerModifier extends BeanSerializerModifier {
    @Override
    public List<BeanPropertyWriter> changeProperties(...) {
        return beanProperties.stream()
            .map(p -> fieldAccessService.canRead(beanDesc.getBeanClass(), p.getName())
                ? p : p.withNullSerializer())
            .toList();
    }
}
```

**Pros:**
- Entity always holds real data -- business logic is unaffected
- Role-based filtering is natural (check SecurityContext in serializer)
- Can support both "hide" (exclude from JSON) and "mask" (replace with masked value)
- No SQL changes, no cache invalidation
- Most frameworks (Hibernate/JPA, MyBatis) use this approach for API-level field control

**Cons:**
- Only covers Jackson serialization (API responses). Does NOT cover:
  - Export (Excel/CSV/PDF)
  - Logging
  - WebSocket messages
  - gRPC
  - Internal data passing between services
- Tight coupling between data module and Jackson
- Serializer runs on every field of every entity -- potential overhead
- Must ensure all API endpoints use Jackson (no manual JSON construction)

**Performance:**
- Only executes at serialization time (when data leaves the system)
- Jackson caches serializer/deserializer per class, so per-field checks are fast
- Minor overhead: one `FieldAccessService.canRead()` check per field per serialization

---

### 5. Write-Side Approaches (Who can UPDATE which columns)

Write-side field-level access control is conceptually simpler but operationally more critical because it prevents unauthorized data modification.

#### Approach W1: Update SQL Column Exclusion

**Description:** When building the UPDATE SQL, exclude columns the user is not authorized to modify. The user can submit a full entity, but unauthorized fields are silently ignored in the UPDATE statement.

**How it maps to AFG:**

Modify `SqlBuilder.buildUpdateSql()` or `EntityUpdateHandler.update()` to filter out unauthorized fields from the SET clause:

```sql
-- User submits: UPDATE sys_user SET name='Bob', salary=999999 WHERE id=1
-- After restriction: UPDATE sys_user SET name='Bob' WHERE id=1
-- salary is excluded from SET clause
```

**Pros:**
- Silent protection -- unauthorized fields are simply not updated
- No error thrown for normal operations
- Compatible with partial-update patterns

**Cons:**
- Silent behavior may hide bugs (developer doesn't realize field is restricted)
- Does not distinguish between "field not changed" and "field change rejected"
- Requires knowing the "original" entity to detect which fields were actually changed

#### Approach W2: Update Rejection (Throw Exception)

**Description:** Before executing the UPDATE, compare the submitted entity with the current database state. If any unauthorized field has been modified, throw a `BusinessException`.

**How it maps to AFG:**

In `EntityUpdateHandler.update()`, before building the SQL:
1. Load the current entity from DB (before update)
2. Compare each field value with the submitted entity
3. For each changed field, check `FieldAccessService.canWrite()`
4. If any unauthorized change detected, throw `BusinessException(CommonErrorCode.FIELD_ACCESS_DENIED)`

**Pros:**
- Explicit feedback -- caller knows exactly which field was rejected
- Prevents silent data loss
- Audit-friendly (can log rejected update attempts)

**Cons:**
- Requires an extra SELECT to load the current entity (performance cost)
- More complex implementation (field-by-field comparison)
- May conflict with optimistic locking (version check)

#### Approach W3: Annotation-Based Write Restriction

**Description:** Use an annotation like `@FieldAccess(readOnly=true)` to declare that a field can be read but not written via the DataManager. The `EntityUpdateHandler` checks these annotations and excludes or rejects writes accordingly.

**How it maps to AFG:**

This is a static, compile-time approach. The `@FieldAccess` annotation can declare:
- `readRoles` / `writeRoles`: which roles can read/write this field
- `readOnly`: equivalent to `writeRoles = {}` (no one can write via DataManager)

**Pros:**
- Declarative, visible in entity code
- APT can validate at compile time
- Works with existing metadata infrastructure

**Cons:**
- Static roles cannot adapt to runtime context (e.g., "only the record owner can edit this field")
- Role names in annotations create tight coupling to security model

---

### 6. API Response Serialization: Preventing Field Leakage

Regardless of which read-side approach is chosen, the JSON serialization layer must be hardened to prevent field leakage through API responses.

#### 6.1 The Problem

Even if field-level access control is enforced at the DataManager/SQL level, the entity object still exists in memory. If the API returns the entity directly (or as part of a `Result<>` wrapper), Jackson will serialize all non-null fields by default.

**Leakage vectors:**

| Vector | Risk | Mitigation |
|--------|------|-----------|
| API response (Jackson) | HIGH -- primary data exit point | BeanSerializerModifier / @JsonFilter |
| Export (Excel/CSV) | MEDIUM -- bulk data extraction | DataExporter checks FieldAccessService |
| Logging | MEDIUM -- entity.toString() may leak | Structured logging, @ToString(exclude=...) |
| Error messages | LOW -- exception messages | BusinessException should not include field values |
| Caching | LOW -- internal, same process | Cached entity is the same as application entity |
| SSE/WebSocket | MEDIUM -- real-time data push | Apply same serialization filtering |

#### 6.2 Defense-in-Depth Strategy

The safest approach combines multiple layers:

1. **SQL layer (Approach A):** Exclude unauthorized columns from SELECT when possible. This is the strongest guarantee.
2. **Entity post-processing (Approach B):** Nullify restricted fields after mapping. This catches any query that bypasses the SQL layer (e.g., raw SQL, `SELECT *`).
3. **Jackson serialization (Approach C):** Filter/mask at JSON output. This is the final safety net and also handles masking (partial data visibility).

**For the AFG framework specifically:**

- The existing `DataScope` pattern already demonstrates SQL-level rewriting as the primary access control mechanism
- The existing `EntityMapper.decryptFields()` pattern demonstrates entity post-processing as a secondary mechanism
- Jackson filtering is not currently used for access control but is available

---

### 7. Casbin Integration Patterns

#### 7.1 Encoding Field-Level Permissions in the Current 4-Tuple Model

The current Casbin model uses `(sub, dom, obj, act)`. Field-level permissions can be encoded by using the `obj` dimension for entity+field combinations:

```
p, hr_manager, default, sys_user:salary, read
p, hr_manager, default, sys_user:salary, write
p, employee, default, sys_user:salary, !read
p, employee, default, sys_user:name, read
p, employee, default, sys_user:email, read
```

The `keyMatch2` matcher in the current model supports `:` as a path separator, so `sys_user:salary` works naturally.

**Advantage:** No model migration needed. The current `(sub, dom, obj, act)` 4-tuple is sufficient.

**Disadvantage:** Permission strings become verbose for entities with many fields. Also, the `keyMatch2` matcher with `:` could conflict with URL-style patterns.

#### 7.2 Extending to a 5-Tuple Model

Add a `field` dimension to the Casbin model:

```
[request_definition]
r = sub, dom, obj, field, act

[policy_definition]
p = sub, dom, obj, field, act

[matchers]
m = g(r.sub, r.dom, p.sub) && r.dom == p.dom && keyMatch2(r.obj, p.obj) && (p.field == "*" || r.field == p.field) && r.act == p.act
```

With policies:
```
p, hr_manager, default, sys_user, *, read
p, hr_manager, default, sys_user, salary, write
p, employee, default, sys_user, *, read
p, employee, default, sys_user, salary, !read
```

**Advantage:** Cleaner separation of entity and field. Supports wildcard `*` for "all fields".

**Disadvantage:** Requires Casbin model migration. The `CasbinRule` entity currently has v0-v3 (4 fields), and adding v4 requires a database schema change.

#### 7.3 Permission Resource String Convention

Regardless of the model choice, a convention for naming field-level permission resources is needed:

**Convention A: Colon-separated**
```
sys_user:salary:read    -- entity:field:action
sys_user:*:read         -- all fields read
```

**Convention B: Dot-separated (matches existing CasbinRbacService pattern)**
```java
// CasbinRbacService.hasPermission() already splits on ":"
String[] parts = permission.split(":");
// parts[0] = resource, parts[1] = action
// e.g., "sys_user.salary:read" -> resource="sys_user.salary", action="read"
```

The existing `CasbinRbacService.hasPermission()` at line 31-35 splits on `:` and treats `parts[0]` as resource and `parts[1]` as action. So `sys_user.salary:read` maps naturally.

---

### 8. Performance Implications

| Approach | Read Overhead | Write Overhead | Cache Impact | Notes |
|----------|---------------|----------------|-------------|-------|
| **SQL SELECT rewrite** | Low (fewer columns fetched) | None (uses same pattern) | HIGH -- SQL cache key must include field access profile | Must group users into profiles to maintain cache efficiency |
| **Entity post-processing** | Very Low (nullify in-memory) | None | None -- same `SELECT *` SQL | Wasted DB I/O for unused columns |
| **Jackson serialization** | Very Low (only at API boundary) | N/A | None | Only covers JSON API responses |
| **Combined (SQL + Jackson)** | Low | None | MEDIUM | Best security, moderate complexity |
| **Write rejection (extra SELECT)** | N/A | HIGH (extra SELECT per UPDATE) | None | Can be optimized with dirty-checking |

**SQL Cache Strategy for Approach A:**

The `SqlBuilder.getSelectBaseSql()` method caches the SELECT SQL string. With field-level access control, the SELECT column list varies per user. Solutions:

1. **Profile-based cache key:** Group users into "field access profiles" (e.g., admin, manager, employee). Cache SQL per profile, not per user. In practice, there are typically 3-5 profiles, so cache size is manageable.
2. **Always use `SELECT *`:** Let the EntityMapper handle column exclusion post-fetch. This avoids cache fragmentation but wastes DB I/O.
3. **No cache for restricted queries:** Only cache the full `SELECT *` SQL. Restricted queries are built dynamically without caching.

---

### 9. Feasible Approaches for AFG Framework (JDBC + Casbin RBAC)

Based on the analysis above, three feasible approaches are recommended for evaluation:

#### Approach 1: SQL-Level Column Exclusion + Jackson Safety Net

**Description:** Primary enforcement at the SQL layer (modify SELECT to exclude unauthorized columns). Jackson `BeanSerializerModifier` as a safety net for any query that bypasses the SQL layer.

**Components:**

| Component | Package | Description |
|---|---|---|
| `@FieldAccess` | `data-core` / annotation | RUNTIME retention annotation on entity fields. Attributes: `readRoles`, `writeRoles`, `readOnly` |
| `FieldAccessControl` | `data-core` | SPI interface: `Set<String> getReadableFields(Class<?> entityClass)`, `Set<String> getWritableFields(Class<?> entityClass)` |
| `CasbinFieldAccessControl` | `auth-server` | Implementation that checks `AfgEnforcer.enforce(subject, "entity:field", "read/write")` |
| `NoOpFieldAccessControl` | `data-core` | Returns all fields (no restriction) |
| `EntityTrait.FIELD_ACCESS_CONTROLLED` | `data-core` | New trait for entities with `@FieldAccess` fields |
| `FieldAccessMetadata` | `data-core` | Record: fieldName + readRoles + writeRoles + readOnly |
| `EntityMetadata.getFieldAccessFields()` | `data-core` | New method for field access metadata |
| `JdbcEntityQuery` integration | `data-jdbc` | In `resolveFields()`, apply `FieldAccessControl.getReadableFields()` to compute excluded fields |
| `EntityUpdateHandler` integration | `data-jdbc` | Before UPDATE, check `FieldAccessControl.getWritableFields()` and exclude or reject |
| `FieldAccessSerializerModifier` | `core` | Jackson `BeanSerializerModifier` as safety net |
| `FieldAccessAutoConfiguration` | `core` | AutoConfiguration registering the modifier |

**Data flow (Read):**
```
EntityQuery.resolveFields()
  -> FieldAccessControl.getReadableFields(User.class)
  -> ["id", "name", "email"]  // salary excluded
  -> buildSelectSql() -> "SELECT id, name, email FROM sys_user"
  -> EntityMapper.map() -> entity with salary=null (field not in ResultSet)
  -> Jackson serialization -> FieldAccessSerializerModifier double-checks
```

**Data flow (Write):**
```
EntityUpdateHandler.update(entity)
  -> Compare changed fields with FieldAccessControl.getWritableFields()
  -> If unauthorized field changed: throw BusinessException(FIELD_ACCESS_DENIED)
  -> If authorized: build UPDATE SQL excluding unauthorized fields
  -> Execute UPDATE
```

**Casbin policy format:**
```
p, hr_role, default, sys_user.salary, read
p, hr_role, default, sys_user.salary, write
p, employee, default, sys_user.salary, !read
```

#### Approach 2: Entity Post-Processing + Jackson Serialization

**Description:** Always fetch `SELECT *`. After mapping, `EntityMapper` nullifies unauthorized fields. Jackson `BeanSerializerModifier` provides masking for "partial visibility" scenarios (e.g., show salary range instead of exact amount).

**Components:** Similar to Approach 1, but:
- No SQL-level changes
- `EntityMapper.restrictFields()` added after `decryptFields()`
- Jackson `BeanSerializerModifier` handles both exclusion and masking
- `FieldMaskingService` SPI for masking logic (can reuse `@SensitiveField` annotation from masking feature)

**Data flow (Read):**
```
SELECT * FROM sys_user
  -> EntityMapper.map() -> entity with all fields
  -> EntityMapper.restrictFields() -> salary set to null
  -> Jackson serialization -> FieldAccessSerializerModifier provides masking option
```

**Advantage over Approach 1:**
- Simpler implementation (no SQL cache changes)
- Supports masking (partial value visibility) which SQL exclusion cannot do
- `SELECT *` is always cacheable

**Disadvantage:**
- Data leaves the database even when unauthorized (nullified in Java)
- Wasted DB I/O for columns that are immediately discarded
- `afterLoad` callback and business logic see null values

#### Approach 3: Hybrid -- SQL Exclusion for Hard Deny, Post-Processing for Soft Masking

**Description:** Use SQL column exclusion for "hard deny" (field completely invisible). Use entity post-processing + Jackson for "soft masking" (field visible but masked). The `@FieldAccess` annotation distinguishes between these modes.

**Components:**

| Annotation Attribute | Behavior | Implementation Layer |
|---|---|---|
| `access = DENY` | Field completely invisible | SQL column exclusion |
| `access = MASK` | Field visible but masked | EntityMapper post-processing + Jackson serializer |
| `access = READ_ONLY` | Field visible but not writable | EntityUpdateHandler write check |

**Data flow:**
```
@FieldAccess(readAccess = DENY, forRoles = {"employee"})
private BigDecimal salary;  // Employee: excluded from SELECT

@FieldAccess(readAccess = MASK, maskType = PHONE, forRoles = {"manager"})
private String phone;  // Manager: fetched but masked in output

@FieldAccess(writeAccess = DENY, forRoles = {"operator"})
private Integer status;  // Operator: can read but cannot update
```

**Advantage:** Most flexible. Supports the full spectrum of field-level access control.

**Disadvantage:** Most complex to implement. Two code paths for read restriction.

---

### 10. Comparison of Three Approaches

| Criterion | Approach 1 (SQL + Jackson) | Approach 2 (Post-process + Jackson) | Approach 3 (Hybrid) |
|---|---|---|---|
| **Security strength** | Strong (data never leaves DB) | Moderate (data in Java memory, nullified after) | Strong (hard deny at SQL, soft mask at post-process) |
| **Performance** | Good (fewer columns fetched) | Moderate (fetch all, discard some) | Good (SQL exclusion for hard deny) |
| **SQL cache impact** | Medium (profile-based cache keys) | None (always SELECT *) | Medium (same as Approach 1) |
| **Business logic access** | No (restricted fields are null) | No (restricted fields are null) | Partial (hard deny = null, mask = masked value) |
| **Masking support** | No (field is either present or absent) | Yes (post-processing can mask) | Yes (hybrid supports both) |
| **Implementation complexity** | Medium | Low | High |
| **Consistency with existing DataScope** | High (both are SQL-rewriting) | Low (different mechanism) | Medium (SQL for hard deny) |
| **Write-side protection** | Yes (exclude from UPDATE SET) | Yes (exclude from UPDATE SET) | Yes (exclude from UPDATE SET) |
| **Export coverage** | Full (data is restricted at source) | Full (data is restricted at entity level) | Full |
| **Log leakage risk** | Low (field is null in entity) | Low (field is null in entity) | Low (hard deny = null, mask = masked) |
| **Casbin integration** | `enforce(sub, "entity:field", "read")` | Same | Same |
| **EntityMetadata extension** | New `FIELD_ACCESS_CONTROLLED` trait | Same | Same |

---

### 11. Casbin Policy Design for Field-Level ACL

Regardless of which approach is chosen, the Casbin policy format needs to support field-level granularity. Two options:

#### Option A: Encode in Existing 4-Tuple (Recommended for Initial Implementation)

Reuse the current `(sub, dom, obj, act)` model with `obj` encoding entity+field:

```
# Resource naming convention: entity.field
p, hr_manager, default, sys_user.*, read
p, hr_manager, default, sys_user.*, write
p, hr_manager, default, sys_user.salary, read
p, hr_manager, default, sys_user.salary, write
p, employee, default, sys_user.*, read
p, employee, default, sys_user.salary, !read
p, employee, default, sys_user.salary, !write
```

**Advantage:** No model migration, no database schema change. The `keyMatch2` matcher supports `*` wildcard.

**Implementation:** `CasbinFieldAccessControl` calls `AfgEnforcer.enforce(userId, "sys_user.salary", "read")`.

#### Option B: Extend to 5-Tuple (Future Enhancement)

Add a `field` dimension only if the 4-tuple proves insufficient for complex policies.

---

### Related Specs

- `.trellis/spec/backend/security-module.md` -- Security module architecture, Casbin RBAC
- `.trellis/spec/backend/data-manager-api.md` -- DataManager API design, EntityProxy, EntityQuery
- `.trellis/spec/backend/entity-design.md` -- Entity base classes, FieldMetadata, EntityTrait
- `.trellis/tasks/06-15-data-module-enhancement/research/data-masking-patterns.md` -- Data masking research (distinct from field-level ACL)
- `.trellis/tasks/06-15-data-module-enhancement/prd.md` -- PRD with field-level access control requirement

## Caveats / Not Found

1. **No existing field-level access control in the codebase** -- the closest patterns are DataScope (row-level) and `@EncryptedField` (field encryption, not authorization).
2. **MyBatis-Plus DataPermissionInterceptor** -- the exact source code could not be fetched from GitHub (404 on file paths). Knowledge is based on community documentation and blog posts.
3. **jOOQ column masking** -- jOOQ does not have built-in column-level masking. The approach is programmatic (conditional column selection in DSL).
4. **Oracle VPD column masking** -- the Oracle documentation page returned 404. Knowledge is based on Oracle documentation from other sources and general VPD knowledge.
5. **PostgreSQL column-level GRANT** -- this is per-database-role, not per-application-user. Application frameworks must maintain their own user-to-role mapping.
6. **The `CasbinRule` entity has v0-v3** -- adding a 5th policy dimension requires a database schema change (adding `v4` column). The 4-tuple encoding is recommended for initial implementation.
7. **SQL cache fragmentation** -- Approach 1 requires careful cache key design to avoid per-user SQL cache entries. Profile-based caching (grouping users by their field access profile) is essential for production performance.
8. **Business logic vs. access control conflict** -- any approach that removes data from the entity (SQL exclusion or post-processing nullification) prevents business logic from accessing the real value. This is a fundamental tension that must be resolved per use case. Internal/service-layer access should bypass field-level ACL, while API/export/logging access should enforce it.
