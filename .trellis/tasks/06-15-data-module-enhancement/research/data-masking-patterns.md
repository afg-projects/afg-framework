# Research: Data Masking/Redaction Patterns in Enterprise Java Frameworks

- **Query**: How enterprise Java frameworks implement data masking/redaction at the data access layer
- **Scope**: Mixed (internal codebase analysis + external framework patterns)
- **Date**: 2026-06-15

## Findings

### 1. Internal Codebase: Existing Related Infrastructure

The AFG framework already has several building blocks that a data masking feature would need to integrate with.

#### 1.1 Field Encryption (Precedent Pattern)

The `@EncryptedField` annotation + `FieldEncryptor` SPI is the closest existing precedent. It demonstrates the full pipeline:

| Layer | Component | Role |
|---|---|---|
| APT API | `@EncryptedField` (SOURCE retention) | Compile-time metadata marker on entity fields |
| APT Impl | `EntityMetadataProcessor.validateEncryptedFields()` | Validates `@EncryptedField` only on String fields |
| Data Core | `EncryptedFieldMetadata` record | Carries fieldName + algorithm + keyRef at runtime |
| Data Core | `EntityTrait.ENCRYPTED` | Trait flag on EntityMetadata |
| Data Core | `EntityMetadata.getEncryptedFields()` | Returns list of encrypted field metadata |
| Data Core | `FieldEncryptor` SPI + `NoOpFieldEncryptor` | Encryption/decryption contract |
| Data JDBC | `EntityMapper.decryptFields()` | After ResultSet mapping, before afterLoad callback |
| Data JDBC | `EntityInsertHandler.encryptFields()` | Before SQL INSERT |

**Key files:**

| File Path | Description |
|---|---|
| `apt-api/.../EncryptedField.java` | APT annotation (SOURCE retention) |
| `data-core/.../entity/FieldEncryptor.java` | SPI interface |
| `data-core/.../entity/EncryptedFieldMetadata.java` | Metadata record |
| `data-core/.../entity/NoOpFieldEncryptor.java` | NoOp fallback |
| `data-core/.../metadata/EntityTrait.java:77-82` | ENCRYPTED trait |
| `data-core/.../metadata/EntityMetadata.java:155-166` | `isEncrypted()` and `getEncryptedFields()` |
| `data-jdbc/.../EntityMapper.java:141-182` | AfterLoad decryption |
| `data-jdbc/.../EntityInsertHandler.java:419-432` | BeforeInsert encryption |

#### 1.2 PII Detection (AI Module)

The `ai-core` module has a comprehensive PII detection/masking SPI:

| Component | Description |
|---|---|
| `PiiDetector` interface | `detect()`, `mask()`, `unmask()` methods |
| `PiiType` enum | NAME, EMAIL, PHONE, ID_NUMBER, CREDIT_CARD, BANK_ACCOUNT, ADDRESS, SSN, PASSPORT, etc. |
| `MaskingStrategy` enum | FULL_MASK, PARTIAL_MASK, HASH_MASK, RANDOM_MASK, TYPE_LABEL |
| `PiiMaskingResult` | maskedText + MaskingToken (for reversible masking) |
| `PiiContext` | userId + tenantId + detectTypes + maskingStrategy + minConfidence |

**Key files:**

| File Path | Description |
|---|---|
| `ai-core/.../api/security/PiiDetector.java` | Full PII detection/masking API |
| `ai-core/.../service/SecurityManagementService.java` | Service facade for PII masking |
| `ai-spring-ai/.../advisor/PiiDetectionAdvisor.java` | Spring AI advisor for auto-masking |
| `ai-langchain4j/.../advisor/Lc4jPiiAdvisor.java` | LangChain4J advisor for auto-masking |

#### 1.3 SensitiveMaskProcessor (Invocation Framework)

A basic sensitive field masker in the invocation result processor chain:

| File Path | Description |
|---|---|
| `core/.../invocation/processor/SensitiveMaskProcessor.java` | Masks `password`, `secret`, `token`, `credential` fields with `***` |

This is very limited -- it only handles a hardcoded set of field names and replaces them entirely with `***`. It operates on the `@AfService` invocation result pipeline, not at the data access layer.

#### 1.4 Export Pipeline

| File Path | Description |
|---|---|
| `core/.../importexport/ExcelColumn.java` | Column annotation for export/import |
| `core/.../api/importexport/DataExporter.java` | SPI for exporting data |
| `core/.../api/importexport/CsvDataExporter.java` | CSV export implementation |

The export pipeline reads field values via getter reflection (`getFieldValue()`). No masking hook exists in the export flow.

#### 1.5 Jackson Configuration

| File Path | Description |
|---|---|
| `core/.../autoconfigure/AfgAutoConfiguration.java:72-73` | ObjectMapper bean |
| `core/.../util/JacksonMapper.java` | Jackson builder utility |

Jackson is available through `core` module. Custom serializers/deserializers can be registered.

---

### 2. External Framework Patterns

#### 2.1 MyBatis-Plus Data Masking (Desensitization)

MyBatis-Plus provides data masking through **TypeHandler** mechanism combined with annotations:

**Approach:** `@SensitiveField` annotation + `SensitiveType` enum + custom `TypeHandler`

```java
// Annotation on entity field
@SensitiveField(type = SensitiveType.PHONE)
private String phone;

@SensitiveField(type = SensitiveType.ID_CARD)
private String idCard;

@SensitiveField(type = SensitiveType.EMAIL)
private String email;

@SensitiveField(type = SensitiveType.BANK_CARD)
private String bankCard;

@SensitiveField(type = SensitiveType.NAME)
private String name;

@SensitiveField(type = SensitiveType.ADDRESS)
private String address;

@SensitiveField(type = SensitiveType.CUSTOM, strategy = "customMaskHandler")
private String custom;
```

**Built-in SensitiveType values:**

| Type | Example Input | Example Output |
|---|---|---|
| PHONE | 13812345678 | 138****5678 |
| ID_CARD | 110101199001011234 | 110101****1234 |
| EMAIL | test@example.com | t**@example.com |
| BANK_CARD | 6222021234567890123 | 622202*********0123 |
| NAME | ZhangSan | Zha*** |
| ADDRESS | No.123 Some Road, Beijing | No.123*** |
| PASSWORD | anypassword | ****** |
| CHINESE_NAME | Zhang San | 张** |
| CUSTOM | (user-defined) | (user-defined) |

**How it works internally:**
1. `@SensitiveField` annotation marks the field and specifies masking type
2. A custom `TypeHandler` intercepts the ResultSet reading (non-masked from DB) and the parameter setting (writes back to DB without masking)
3. The `SensitiveStrategy` interface defines the masking algorithm per type
4. Masking happens at the **entity mapping layer** -- the entity field always holds the masked value after read
5. Original values are lost after mapping (no reversible masking)

**Limitation:** MyBatis-Plus masking operates at the TypeHandler/ResultSet mapping layer. This means:
- The entity object itself contains masked values after read
- Business logic cannot access the original value
- No role-based or context-aware masking -- it's always-on
- Not suitable for scenarios where some users need full data

#### 2.2 Hibernate Data Masking

Hibernate approaches data masking through several mechanisms:

**A. Column Transformers / Formula**
```java
@Column(name = "phone")
@ColumnTransformer(read = "mask_phone(phone)", write = "?")
private String phone;
```
Uses database functions to mask at the SQL level. Requires database-side functions.

**B. Custom UserType / TypeHandler**
```java
public class MaskedStringType implements UserType<String> {
    // mask on nullSafeGet, unmask not possible
}
```
Similar to MyBatis TypeHandler approach.

**C. Hibernate Interceptor / Event Listener**
```java
public class MaskingInterceptor extends EmptyInterceptor {
    @Override
    public Object instantiate(String entityName, EntityMode entityMode, Serializable id) {
        // PostLoad event: apply masking based on security context
    }
}
```
Operates at the session/event level. Can access security context.

**D. Jackson Serialization Masking (Preferred in JPA)**
```java
@JsonSerialize(using = PhoneMaskingSerializer.class)
private String phone;

public class PhoneMaskingSerializer extends JsonSerializer<String> {
    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider provider) {
        gen.writeString(maskPhone(value));
    }
}
```
This is the most common approach in Hibernate/JPA projects. The entity holds the real value, masking only happens during JSON serialization.

**Key insight from Hibernate ecosystem:** The dominant pattern is **Jackson serialization-level masking** combined with **JPA `@Convert` / AttributeConverter** for stored-data masking. The entity itself always holds the real value; masking is an output concern.

#### 2.3 Spring Data Data Masking

Spring Data doesn't have built-in masking. The community approach combines:

1. **Jackson `@JsonSerialize` with custom serializers** -- most common
2. **Spring AOP with custom annotations** -- intercepts controller return values
3. **`ResponseBodyAdvice`** -- global controller response modification
4. **Projection interfaces** -- Spring Data projections can define a subset of fields with different types

**Spring Data Projection approach:**
```java
// Entity with full data
public class User {
    private String phone;  // full phone number
}

// Projection for API responses
public interface UserProjection {
    String getName();
    @Value("#{target.phone != null ? target.phone.substring(0,3) + '****' + target.phone.substring(7) : null}")
    String getPhone();
}
```

---

### 3. Common Masking Strategies

| Strategy Type | Phone | ID Card | Email | Bank Card | Name | Address | Custom |
|---|---|---|---|---|---|---|---|
| **Partial Mask** | 138****5678 | 110101****1234 | t**@example.com | 622202********0123 | 张** / Zha*** | 北京市***路 | (user-defined) |
| **Full Mask** | ******* | ******* | ******* | ******* | ******* | ******* | ******* |
| **Hash Mask** | sha256(phone)[:8] | sha256(id)[:8] | sha256(email)[:8] | sha256(card)[:8] | sha256(name)[:8] | sha256(addr)[:8] | sha256(val)[:8] |
| **Random Mask** | 13800005678 | 1101010000001234 | r**@example.com | 6222020000000123 | 张* | 北京市000路 | (randomized) |
| **Type Label** | [PHONE] | [ID_CARD] | [EMAIL] | [BANK_CARD] | [NAME] | [ADDRESS] | [CUSTOM] |

**Partial mask rules (Chinese standard GB/T 35273):**

| Data Type | Mask Rule | Keep Prefix | Keep Suffix |
|---|---|---|---|
| Phone (11 digits) | Keep first 3 + last 4 | 3 | 4 |
| ID Card (18 digits) | Keep first 6 + last 4 | 6 | 4 |
| Email | Keep first char + domain | 1 char | @domain |
| Bank Card (16-19 digits) | Keep first 6 + last 4 | 6 | 4 |
| Chinese Name | Keep first surname char | 1 char | 0 |
| English Name | Keep first 3 chars | 3 | 0 |
| Address | Keep first 6 chars | 6 | 0 |

---

### 4. Masking Interaction with Export

**The critical question: should masking be applied at the entity level or at the serialization/export level?**

| Layer | Pros | Cons |
|---|---|---|
| **Entity mapping (afterLoad)** | Simple, consistent; masked data never enters entity | Business logic loses access to real values; no role-based differentiation; breaks write-back scenarios |
| **Jackson serialization** | Entity holds real data; role-based masking possible via SecurityContext; API-safe by default | Must ensure all API endpoints use Jackson; log statements may leak; export must also use Jackson or duplicate masking logic |
| **AOP / ResponseBodyAdvice** | Centralized; can inspect security context; works across all controllers | Only covers HTTP responses; doesn't cover exports, logs, or internal data passing; reflection overhead |
| **Export-specific** | Targeted; can apply different rules for export vs API | Must be maintained separately; risk of inconsistent masking rules |

**Best practice for enterprise frameworks:**

1. **API responses:** Mask at Jackson serialization level via `@JsonSerialize` custom serializers. The serializer can check SecurityContext for role-based visibility.
2. **Export (Excel/CSV/PDF):** The export pipeline should also respect masking annotations. This can be achieved by having the `DataExporter` SPI check for `@SensitiveField` annotations and apply the same masking strategy.
3. **Logs:** Never log entity objects directly. Use structured logging with explicit field selection, or implement `toString()` that respects masking.
4. **Internal business logic:** The entity should hold the real value. Masking is purely an output/presentation concern.

---

### 5. Best Practices for Preventing Data Leaks

| Vector | Prevention Strategy |
|---|---|
| **API Responses** | Jackson custom serializers with `@SensitiveField` annotation; `ResponseBodyAdvice` as safety net |
| **Excel/CSV Export** | `DataExporter` checks `@SensitiveField` and applies masking before writing |
| **Logs** | Entity `toString()` respects masking; log structured messages instead of full entities |
| **Error Messages** | `BusinessException` should not include sensitive field values in messages |
| **Debug/Trace** | Spring Actuator endpoints filter sensitive data; distributed tracing redacts spans |
| **Database Query Results** | Field encryption (`@EncryptedField`) for storage-level protection; masking is separate from encryption |
| **Caching** | If entity caching is enabled, cached entities hold real values; masking still applies at output time |
| **Serialization (non-JSON)** | For non-JSON serialization (e.g., gRPC, XML), ensure masking is applied similarly |

**Key distinction: Encryption vs. Masking**

| Aspect | `@EncryptedField` (Existing) | `@SensitiveField` (Proposed) |
|---|---|---|
| Purpose | Protect data at rest (database) | Protect data in output (API/export/log) |
| Where applied | DB write (encrypt) / DB read (decrypt) | Serialization / export / log output |
| Reversibility | Yes (decrypt back to plaintext) | No (masking is one-way by default) |
| When real value available | After EntityMapper.decryptFields() | Always (entity holds real value) |
| Data stored in DB | Ciphertext | Plaintext (or encrypted separately) |

---

### 6. Performance Implications by Layer

| Layer | Overhead | Impact | Notes |
|---|---|---|---|
| **Entity mapping (afterLoad)** | O(n) per entity, where n = number of masked fields | Medium -- affects every DB read | String operations are fast; but entity always holds masked value |
| **Jackson serialization** | O(n) per serialization, where n = number of masked fields | Low -- only affects API responses | Serializer checks annotation + security context per field |
| **AOP / ResponseBodyAdvice** | O(n*m) where n = entities, m = fields per entity (reflection-based) | Medium-High -- reflection overhead on every response | Must traverse all fields via reflection |
| **Export pipeline** | O(n*m) per export | Low -- batch operation, not in hot path | Export is typically infrequent |
| **Database function masking** | O(1) per field | Very Low -- DB-level computation | Requires DB-side functions; not portable |

**Recommendation for JDBC-based framework:** Jackson serialization-level masking has the best performance profile because:
- It only executes when data is actually being sent out
- It leverages Jackson's annotation scanning (cached per class)
- It can access SecurityContext for role-based decisions
- Entity holds real data for business logic
- No reflection overhead beyond what Jackson already does

---

### 7. Role-Based / Context-Aware Masking

**Challenge:** Admin sees full phone number `13812345678`, regular user sees `138****5678`.

**Approaches:**

**A. SecurityContext-aware Jackson Serializer**
```java
public class SensitiveFieldSerializer extends JsonSerializer<String> {
    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider provider) {
        SecurityContext ctx = SecurityContextHolder.getContext();
        if (ctx.getAuthentication().getAuthorities().contains(new SimpleGrantedAuthority("SENSITIVE_VIEW"))) {
            gen.writeString(value);  // Full value for privileged users
        } else {
            gen.writeString(maskPhone(value));  // Masked for others
        }
    }
}
```
Pros: Works with Spring Security naturally. Cons: Tight coupling to SecurityContext.

**B. Masking Context SPI**
```java
public interface MaskingContext {
    boolean canViewSensitive(String fieldName, SensitiveType type);
    MaskingStrategy getStrategy(String fieldName, SensitiveType type);
}
```
Pros: Decoupled from SecurityContext. Testable. Cons: Another SPI to implement.

**C. DataScope-based (Existing Pattern)**
The framework already has `DataScope` and `DataScopeType`. Role-based masking could reuse the same permission infrastructure:
- `DataScopeType.ALL` --> full access to sensitive fields
- `DataScopeType.SELF` --> own data visible, others masked
- Custom scope --> custom masking rules

---

### 8. Feasible Approaches for AFG Framework (JDBC-based, no JPA/Hibernate)

#### Approach 1: Annotation + Jackson Serializer (Recommended)

**Description:** Add `@SensitiveField` annotation to entity fields. Register a Jackson `BeanSerializerModifier` that dynamically applies masking serializers to annotated fields. The serializer checks a `MaskingContext` SPI (which defaults to checking Spring Security roles).

**Components:**

| Component | Package | Description |
|---|---|---|
| `@SensitiveField` | `apt-api` / annotation | SOURCE retention, specifies `SensitiveType` and optional `strategy` |
| `SensitiveType` | `data-core` | Enum: PHONE, ID_CARD, EMAIL, BANK_CARD, NAME, ADDRESS, CUSTOM |
| `MaskingStrategy` | `data-core` | SPI interface: `String mask(String value, SensitiveType type, String fieldName)` |
| `MaskingContext` | `data-core` | SPI interface: `boolean canView(String fieldName, SensitiveType type)` |
| `DefaultMaskingStrategy` | `data-core` | Built-in partial masking for all standard types |
| `NoOpMaskingContext` | `data-core` | Always returns false (always mask) |
| `SensitiveFieldSerializerModifier` | `core` | Jackson BeanSerializerModifier that wraps annotated fields with masking serializers |
| `SensitiveFieldAutoConfiguration` | `core` | AutoConfiguration registering the modifier |
| `SensitiveFieldMetadata` | `data-core` | Record: fieldName + sensitiveType + customStrategy |
| `EntityTrait.SENSITIVE` | `data-core` | New trait for entities with sensitive fields |
| `EntityMetadata.getSensitiveFields()` | `data-core` | New method to get sensitive field metadata |

**Data flow:**
```
Entity (real value) --> Jackson serialization --> SensitiveFieldSerializerModifier
  --> checks MaskingContext.canView() --> if false: applies MaskingStrategy.mask()
  --> if true: writes original value
```

**Export integration:**
- `CsvDataExporter` / `ExcelDataExporter` read `@SensitiveField` and apply masking before writing
- Uses the same `MaskingStrategy` SPI for consistency

**APT integration:**
- APT processor detects `@SensitiveField` and adds `SENSITIVE` trait + `SensitiveFieldMetadata` to generated metadata
- `EntityMetadata.getSensitiveFields()` returns list (similar to `getEncryptedFields()`)

**Pros:**
- Entity always holds real data; business logic is unaffected
- Role-based masking via `MaskingContext` SPI
- Consistent masking across API and export
- Follows the same pattern as `@EncryptedField` for APT/metadata integration
- Performance: only masks when serializing/exporting
- Can coexist with `@EncryptedField` (encryption for storage, masking for output)

**Cons:**
- Only covers Jackson serialization; non-JSON outputs need separate handling
- `toString()` and logging must be handled separately
- Requires adding Jackson `BeanSerializerModifier` (slightly more complex than a simple serializer)

#### Approach 2: EntityMapper afterLoad Hook (Similar to EncryptedField Pattern)

**Description:** Apply masking in `EntityMapper.map()` after field mapping and decryption, similar to how `decryptFields()` works. Add a `maskSensitiveFields()` method that replaces real values with masked values.

**Components:**

| Component | Package | Description |
|---|---|---|
| `@SensitiveField` | `apt-api` | Annotation on entity fields |
| `SensitiveType` | `data-core` | Enum of masking types |
| `FieldMasker` | `data-core` | SPI interface: `String mask(String value, SensitiveType type)` |
| `NoOpFieldMasker` | `data-core` | NoOp fallback |
| `EntityMapper.maskSensitiveFields()` | `data-jdbc` | After decryption, before afterLoad |

**Data flow:**
```
ResultSet --> EntityMapper.map() --> decryptFields() --> maskSensitiveFields() --> afterLoad callback --> entity
```

**Pros:**
- Follows the exact same pattern as `@EncryptedField`
- Very simple to implement (add one method to EntityMapper)
- Masked data never exists in entity -- strong guarantee against leaks

**Cons:**
- **Business logic cannot access real values** -- this is the critical flaw
- No role-based differentiation -- every user sees the same masked data
- Breaks write-back scenarios (update would write masked value back to DB)
- Cannot be used for scenarios where some users need full data
- Incompatible with `@EncryptedField` (decrypted value would be immediately masked)
- Requires maintaining a "real value store" if business logic needs original values

#### Approach 3: Dual-View Pattern (DTO + Entity)

**Description:** Entities always hold real values. A separate `SensitiveView` annotation or DTO mechanism creates masked views for API responses. The `DataExporter` and Jackson serialization operate on the view, not the entity.

**Components:**

| Component | Package | Description |
|---|---|---|
| `@SensitiveField` | `apt-api` | Annotation with SensitiveType |
| `SensitiveViewResolver` | `data-core` | SPI: creates a masked copy/view of an entity |
| `DefaultSensitiveViewResolver` | `data-core` | BeanUtils copy + mask annotated fields |
| `@SensitiveResponseBodyAdvice` | `core` | Controller advice that wraps responses through view resolver |
| `SensitiveFieldExportWrapper` | `core` | Wraps export data through view resolver |

**Data flow:**
```
Entity (real) --> Controller returns entity --> ResponseBodyAdvice
  --> SensitiveViewResolver.createView(entity) --> masked copy --> Jackson serialization
```

**Pros:**
- Clean separation: entity is always real, view is always masked
- Can create different views for different roles
- Export and API use the same view resolution

**Cons:**
- Object copy overhead (every API response creates a new object)
- Requires `ResponseBodyAdvice` which only works for Spring MVC controllers
- Must handle collections, pages, nested objects
- More complex than Approach 1
- Risk of developers returning entity directly (bypassing the advice)

---

### 9. Comparison of Approaches

| Criterion | Approach 1 (Jackson) | Approach 2 (EntityMapper) | Approach 3 (DTO View) |
|---|---|---|---|
| Role-based masking | Yes (via MaskingContext) | No | Yes (via view resolver) |
| Business logic access to real values | Yes | No | Yes |
| Implementation complexity | Medium | Low | High |
| Performance | Good (only at serialization) | Best (no extra pass) | Medium (object copy) |
| Export integration | Needs explicit handling | Automatic | Needs explicit handling |
| Log leak prevention | Needs separate handling | Automatic | Needs separate handling |
| Coexistence with `@EncryptedField` | Yes | Problematic | Yes |
| APT/metadata pattern match | Yes (same as EncryptedField) | Yes (same as EncryptedField) | Partial |
| Spring Security integration | Natural | None | Natural |

---

### 10. Recommended Architecture (Based on Approach 1)

The recommended approach combines the Jackson serialization-level masking with APT metadata integration, following the same patterns as `@EncryptedField`:

```
Layer 1 - APT: @SensitiveField annotation detected by APT processor
    --> Adds SENSITIVE trait + SensitiveFieldMetadata to generated metadata

Layer 2 - Data Core: SensitiveType enum + MaskingStrategy SPI + MaskingContext SPI
    --> Default implementations for all standard masking types
    --> MaskingContext checks Spring Security for role-based decisions

Layer 3 - Core: Jackson BeanSerializerModifier + AutoConfiguration
    --> Intercepts serialization of @SensitiveField annotated properties
    --> Checks MaskingContext; applies MaskingStrategy if masking needed

Layer 4 - Export: DataExporter checks @SensitiveField / EntityMetadata.getSensitiveFields()
    --> Applies same MaskingStrategy before writing

Layer 5 - Safety Net: toString() guidance + log masking utilities
    --> Entity.toString() should use @SensitiveField awareness
    --> Framework provides SensitiveLogUtils for safe logging
```

---

## Caveats / Not Found

1. **No existing data masking implementation in the codebase** -- the `SensitiveMaskProcessor` in the invocation framework only handles hardcoded field names and is not suitable for entity-level masking.
2. **The `@EncryptedField` annotation is `SOURCE` retention** -- a new `@SensitiveField` annotation should likely be `RUNTIME` retention so it can be read by Jackson at serialization time (unlike `@EncryptedField` which only needs APT-time visibility).
3. **MyBatis-Plus `@SensitiveField` is not a standard** -- the name and approach are framework-specific. AFG should define its own annotation and SPI.
4. **The `ReflectiveEntityMetadata` does not detect `@EncryptedField` traits** -- it only detects structural traits (SOFT_DELETABLE, TENANT_AWARE, etc.) via field presence. Adding `SENSITIVE` trait detection would require scanning for the `@SensitiveField` annotation at runtime via reflection.
5. **Performance concern with `MaskingContext` SPI** -- if the SPI checks Spring Security on every field serialization, this adds overhead. Consider caching the masking decision per request (e.g., via RequestScope bean or ThreadLocal).
6. **Export pipeline currently has no annotation awareness** -- `CsvDataExporter` reads `@ExcelColumn` but not `@SensitiveField`. Integration requires adding masking awareness to the export metadata resolution.
