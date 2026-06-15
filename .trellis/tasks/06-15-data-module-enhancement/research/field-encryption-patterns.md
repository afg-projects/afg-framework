# Research: Field-Level Encryption Patterns

- **Query**: How enterprise Java frameworks implement field-level encryption at the data access layer
- **Scope**: Mixed (internal codebase analysis + external framework research)
- **Date**: 2026-06-15

## Findings

### 1. Current AFG Framework Implementation

The framework already has a foundational field encryption architecture:

| Component | File | Description |
|---|---|---|
| `FieldEncryptor` SPI | `data-core/.../entity/FieldEncryptor.java` | Interface: `encrypt(plaintext, algorithm, keyRef)` / `decrypt(ciphertext, algorithm, keyRef)` |
| `NoOpFieldEncryptor` | `data-core/.../entity/NoOpFieldEncryptor.java` | Default fallback: returns plaintext unchanged, logs WARN once |
| `@EncryptedField` annotation | `apt-api/.../entity/EncryptedField.java` | SOURCE-retention, attributes: `algorithm` (default "AES"), `keyRef` (default "") |
| `EncryptedFieldMetadata` | `data-core/.../entity/EncryptedFieldMetadata.java` | Record: `fieldName`, `algorithm`, `keyRef` |
| `EntityTrait.ENCRYPTED` | `data-core/.../metadata/EntityTrait.java` | Metadata trait flag for entities with encrypted fields |
| AutoConfiguration | `data-jdbc/.../autoconfigure/DataManagerAutoConfiguration.java:77-81` | `@ConditionalOnMissingBean(FieldEncryptor.class)` registers NoOp |

**Encryption flow (write):**
- `EntityInsertHandler.encryptFields()` (line 419) and `EntityUpdateHandler.encryptFields()` (line 194) iterate `metadata.getEncryptedFields()`, call `fieldEncryptor.encrypt(plaintext, algorithm, keyRef)`, set ciphertext back on entity before SQL generation.

**Decryption flow (read):**
- `EntityMapper.decryptFields()` (line 165) runs after ResultSet mapping but before `afterLoad` lifecycle callback. Iterates encrypted fields, calls `fieldEncryptor.decrypt(ciphertext, algorithm, keyRef)`, sets plaintext back.

**Critical gap: WHERE clauses on encrypted fields are NOT handled.** The `ConditionToSqlConverter` passes condition values directly as SQL parameters without any encryption transformation. If a user queries `Conditions.builder(User.class).eq(User::getPhone, "13800001234")`, the plaintext value is sent to the database, which cannot match it against the stored ciphertext.

### 2. External Framework Approaches

#### 2.1 MyBatis-Plus

MyBatis-Plus has two encryption mechanisms:

**A. SafetyEncryptProcessor (config-level encryption)**
- File: `mybatis-plus-boot-starter/.../SafetyEncryptProcessor.java`
- Implements `EnvironmentPostProcessor` -- decrypts property values prefixed with `mpw:` at boot time using AES
- Key passed via command-line: `--mpw.key=xxx`
- This is for **configuration properties** encryption, not field-level data encryption
- Uses `AES.decrypt()` from `com.baomidou.mybatisplus.core.toolkit.AES`

**B. MyBatis Interceptor pattern (community approach)**
- Third-party `mybatis-encrypt-spring-boot-starter` (julxxy/mybatis-encrypt-spring-boot-parent, 43 stars)
- Uses `MybatisPlusInterceptor` intercepting `Executor.update` and `Executor.query`
- On INSERT/UPDATE: reflects fields annotated with `@Encryption`, encrypts plaintext before SQL execution
- On SELECT: reflects result objects, decrypts ciphertext after SQL execution
- **WHERE clause handling**: Provides `EncryptStrategy.convert(plainBean, encryptType)` static method that encrypts query parameter fields before passing to MyBatis. **Requires manual call by developer** -- the interceptor does NOT automatically transform WHERE clause parameters.
- AES implementation: `AES/CBC/NoPadding` with fixed IV (deterministic -- same plaintext always produces same ciphertext, enabling equality search but vulnerable to known-plaintext attacks)

**Key insight**: MyBatis-Plus does NOT have built-in searchable encryption. The community solution requires developers to manually encrypt query parameters.

#### 2.2 Hibernate @Convert / AttributeConverter

Jakarta Persistence `AttributeConverter<X, Y>` is the standard JPA mechanism:

```java
@Converter(autoApply = true)
public class EncryptedStringConverter implements AttributeConverter<String, String> {
    @Override
    public String convertToDatabaseColumn(String plaintext) {
        return aesEncryptor.encrypt(plaintext);  // called on write
    }

    @Override
    public String convertToEntityAttribute(String ciphertext) {
        return aesEncryptor.decrypt(ciphertext);  // called on read
    }
}
```

**Pros**: Transparent to entity code, works with all JPA operations
**Cons**:
- WHERE clauses still fail: `entityManager.createQuery("WHERE phone = :phone")` sends plaintext to DB, which compares against ciphertext
- Hibernate does NOT apply `AttributeConverter` to query parameters -- only to entity field values
- No built-in searchable encryption support
- Key management is entirely the developer's responsibility

#### 2.3 jOOQ Binding

jOOQ uses `org.jooq.Binding<T, U>` for custom type handling:

```java
public class EncryptedBinding implements Binding<String, String> {
    @Override
    public Converter<String, String> converter() {
        return new Converter<>() {
            public String from(String dbValue) { return decrypt(dbValue); }
            public String to(String userValue) { return encrypt(userValue); }
        };
    }

    @Override
    public void sql(BindingSQLContext<String> ctx) {
        ctx.render().visit(ctx.value());  // uses encrypted value in SQL
    }

    @Override
    public void set(BindingSetStatementContext<String> ctx) {
        ctx.statement().setString(ctx.index(), encrypt(ctx.value()));
    }

    @Override
    public void get(BindingGetResultSetContext<String> ctx) {
        ctx.result(decrypt(ctx.resultSet().getString(ctx.index())));
    }
}
```

**Key advantage**: The `sql()` and `set()` methods allow intercepting query parameter binding, enabling automatic encryption of WHERE clause parameters. However, this only works for equality with deterministic encryption.

#### 2.4 Spring Security Crypto

`org.springframework.security.crypto.encrypt` provides:

| Class | Algorithm | Notes |
|---|---|---|
| `Encryptors.stronger(password, salt)` | AES-256-GCM + PBKDF2 + random IV | **Non-deterministic** -- same plaintext produces different ciphertext each time |
| `Encryptors.standard(password, salt)` | AES-256-CBC + PBKDF2 + random IV | Non-deterministic, not authenticated |
| `Encryptors.delux(password, salt)` | stronger + hex encoding | Text-oriented wrapper |
| `Encryptors.noOpText()` | Identity | For testing only |

**Critical point**: Spring Security's encryptors use random IVs, making them **non-deterministic**. This is correct for password/secret storage but **unsuitable for searchable encryption** -- you cannot compare two ciphertexts to determine if they encrypt the same plaintext.

### 3. Searchable Encryption Approaches

This is the hardest problem in field-level encryption. The fundamental challenge: standard AEAD encryption (AES-GCM, AES-CBC with random IV) produces different ciphertext for the same plaintext, making database-level equality comparison impossible.

#### 3.1 Deterministic Encryption

**Approach**: Use AES with fixed IV/nonce (e.g., AES-SIV, AES-ECB, or AES-CBC with zero IV).

| Aspect | Detail |
|---|---|
| How it works | Same plaintext always produces same ciphertext, so `WHERE phone = encrypt('13800001234')` works |
| Security | **WEAK** -- vulnerable to frequency analysis (attacker can see which values are common), known-plaintext attacks, and chosen-plaintext attacks |
| Suitable for | Low-sensitivity fields where equality search is required and data distribution is uniform |
| Java implementation | `AES/CBC/PKCS5Padding` with fixed IV (what mybatis-encrypt uses) |
| Standard | NIST SP 800-175B recommends AES-SIV for deterministic encryption (synthetic IV mode) |

**AES-SIV** (RFC 5297) is the cryptographically correct approach for deterministic encryption. It is nonce-misuse resistant and provides deterministic encryption with authenticity. Available in Bouncy Castle as `AES-SIV`.

#### 3.2 Blind Index / Searchable Hash (CipherSweet Pattern)

**Approach**: Store encrypted ciphertext in the main column, plus a separate "blind index" column containing `HMAC(key, plaintext)` (truncated to N bits).

| Aspect | Detail |
|---|---|
| How it works | Main column: `AES-GCM(plaintext)` (randomized). Index column: `HMAC-SHA256(indexKey, plaintext)[0:N bits]`. Query: `WHERE phone_index = HMAC(key, '13800001234')[0:N]` |
| Security | **Strong** -- main ciphertext is semantically secure (random IV). Blind index leaks only equality information (same plaintext = same hash). Truncation adds controlled false positives to limit frequency analysis. |
| False positives | Shorter index = more false positives (more secure but slower post-filtering). Longer index = fewer false positives (faster but more leakage). CipherSweet provides a `FieldIndexPlanner` to calculate safe index sizes based on population. |
| Key separation | Each field uses a different encryption key AND a different index key, all derived from a master key via HKDF. |
| Suitable for | Exact-match queries on sensitive fields (SSN, phone, email) |
| Not suitable for | Range queries, LIKE queries, sorting on encrypted values |
| Reference | [CipherSweet](https://github.com/paragonie/ciphersweet) (PHP), [CipherSweet-JS](https://github.com/paragonie/ciphersweet-js) (Node.js). No mature Java port exists. |

**Blind index variants:**
- **Fast index**: `HMAC-SHA256(key, plaintext)` truncated -- for low-collision domains
- **Slow index**: `PBKDF2-SHA384(key, plaintext, iterations=50000)` -- for high-collision domains (e.g., last names) to slow brute-force
- **Compound index**: `HMAC(key, field1 || field2)` -- for multi-field queries
- **Partial index**: `HMAC(key, substring(plaintext, 0, 3))` -- for prefix search (e.g., area code)

#### 3.3 HMAC-Based Indexing (AWS Database Encryption SDK Beacon Pattern)

AWS Database Encryption SDK (v3) uses "beacons" for searchable encryption:

| Aspect | Detail |
|---|---|
| Beacon definition | Truncated HMAC tag: `HMAC-SHA256(beaconKey, plaintext)[0:beaconLength bits]` |
| Stored alongside | Each encrypted field gets a companion beacon field |
| Query flow | Application computes beacon for query value, sends to DB, DB matches beacon field, SDK filters false positives client-side |
| Beacon length | Trade-off: shorter = more security (more false positives), longer = more performance. AWS provides a planner. |
| Multi-tenant | Each tenant uses a distinct beacon key, preventing cross-tenant leakage |
| Limitation | Beacons only work on **new, unpopulated databases** -- existing data cannot be retroactively beaconed (must be re-encrypted) |
| Reference | [AWS DB Encryption SDK](https://docs.aws.amazon.com/database-encryption-sdk/latest/devguide/searchable-encryption.html) |

#### 3.4 Database-Side Encryption Functions

**MySQL**: `AES_ENCRYPT(data, key)` / `AES_DECRYPT(data, key)` -- can be used in WHERE clauses:
```sql
WHERE AES_ENCRYPT('13800001234', 'key') = encrypted_phone  -- deterministic (ECB mode default)
```

**PostgreSQL (pgcrypto)**: `pgp_sym_encrypt(data, password)` / `pgp_sym_decrypt(data, password)` -- uses random salt by default (non-deterministic). For searchable encryption, use `digest(data, 'sha256')` as a separate index column.

**Limitation**: Key material must be present in the SQL statement, exposing it in query logs and requiring careful key management at the application level.

### 4. Key Management Patterns

| Pattern | Description | Pros | Cons |
|---|---|---|---|
| **Embedded key** | Key in application config (YAML/properties), possibly encrypted with master password | Simple, no external dependency | Key in config files = attack surface; rotation requires restart; no audit |
| **HashiCorp Vault Transit** | Encryption/decryption as a service via API. Keys never leave Vault. | Key never in app memory; automatic rotation; audit logging; rewrap endpoint for key rotation | Network latency per encrypt/decrypt call (~1-5ms); Vault HA required; Spring Vault integration needed |
| **AWS KMS Envelope** | Generate data key from KMS, encrypt data key with KMS, cache plaintext data key locally | Scalable; IAM-based access control; audit via CloudTrail; data key caching reduces API calls | KMS API call per data key (cached after); envelope encryption adds complexity; region-bound |
| **AWS KMS Direct** | Call KMS Encrypt/Decrypt for each field value | Simplest KMS integration | **Extremely expensive** at scale: $0.03 per 10K requests, 1M fields = $3/day; high latency |
| **HSM** | Keys stored in Hardware Security Module (Thales, AWS CloudHSM) | Highest security; FIPS 140-2 Level 3 | Expensive; complex; limited availability |

**Vault Transit API** (most relevant for enterprise Java):
```java
// Spring Vault VaultTransitOperations
String ciphertext = vaultTransitOperations.encrypt("my-key", plaintext);
String plaintext = vaultTransitOperations.decrypt("my-key", ciphertext);

// Key rotation: rewrap existing ciphertext with new key version
String rewrapped = vaultTransitOperations.rewrap("my-key", ciphertext);
```

**Recommended pattern for AFG framework**: SPI with multiple implementations:
1. `AesFieldEncryptor` (embedded key, AES-GCM for non-searchable, AES-SIV for searchable) -- default
2. `VaultTransitFieldEncryptor` (HashiCorp Vault Transit) -- enterprise
3. `KmsFieldEncryptor` (AWS KMS envelope encryption) -- cloud
4. `NoOpFieldEncryptor` -- development only

### 5. WHERE Clause Handling on Encrypted Fields

This is the hardest problem. Three feasible approaches for a JDBC-based framework:

#### Approach A: Parameter Encryption in ConditionToSqlConverter

**Mechanism**: When building WHERE clause, if the condition field is encrypted, encrypt the parameter value before adding it to the SQL parameter list.

**Implementation point**: `ConditionToSqlConverter.convertCriterion()` or `EntityConditionQueryHandler.findAll()`.

```java
// In ConditionToSqlConverter or a wrapper:
if (metadata.isEncrypted(fieldName)) {
    EncryptedFieldMetadata encMeta = metadata.getEncryptedField(fieldName);
    value = fieldEncryptor.encrypt((String) value, encMeta.algorithm(), encMeta.keyRef());
}
```

**Requirements**:
- Deterministic encryption (same plaintext = same ciphertext) for equality to work
- `ConditionToSqlConverter` needs access to `EntityMetadata` (currently it does not have it)
- Only `EQ`, `NE`, `IN`, `NOT_IN` operators are valid on encrypted fields
- `LIKE`, `GT`, `LT`, `GE`, `LE`, `BETWEEN` must be rejected at query time

**Pros**: Minimal schema change; transparent to developer; works with existing Condition API
**Cons**: Requires deterministic encryption (weaker security); no range/LIKE support

#### Approach B: Blind Index Columns

**Mechanism**: Add companion index columns (e.g., `phone_index VARCHAR(64)`) alongside encrypted columns. Query uses the index column instead of the encrypted column.

**Schema**:
```sql
CREATE TABLE sys_user (
    phone VARCHAR(256),           -- AES-GCM ciphertext
    phone_index VARCHAR(64),      -- HMAC-SHA256(phone)[0:N hex chars]
    ...
);
CREATE INDEX idx_user_phone ON sys_user(phone_index);
```

**Query transformation**:
```java
// Developer writes:
Conditions.builder(User.class).eq(User::getPhone, "13800001234")

// Framework transforms to:
// WHERE phone_index = HMAC(phoneIndexKey, "13800001234")[0:N]
```

**Implementation points**:
- `@EncryptedField(searchable = true)` or `@EncryptedField(blindIndex = true)` annotation attribute
- `EncryptedFieldMetadata` gains `searchable` / `indexFieldName` / `indexLength` attributes
- `EntityInsertHandler` / `EntityUpdateHandler` also write the blind index value
- `ConditionToSqlConverter` redirects encrypted field conditions to the index column
- `FieldEncryptor` SPI gains `computeIndex(plaintext, algorithm, keyRef)` method

**Pros**: Strong security (main ciphertext is randomized); supports exact match; industry standard (CipherSweet, AWS beacons)
**Cons**: Schema change (additional columns); additional index storage; false positives require client-side filtering; no range/LIKE support

#### Approach C: Hybrid (Deterministic for Low-Sensitivity + Blind Index for High-Sensitivity)

**Mechanism**: Let developers choose per-field:
- `@EncryptedField(mode = DETERMINISTIC)` -- AES-SIV, searchable via direct ciphertext comparison
- `@EncryptedField(mode = SEARCHABLE, indexLength = 16)` -- AES-GCM + blind index column
- `@EncryptedField(mode = ENCRYPTED)` -- AES-GCM only, not searchable

**Pros**: Flexibility; appropriate security level per field
**Cons**: Complexity; developer must understand trade-offs

### 6. Performance Implications

| Operation | Overhead | Notes |
|---|---|---|
| AES-GCM encrypt/decrypt | ~1-5 microseconds per field | Negligible for typical entity with 1-3 encrypted fields |
| AES-SIV encrypt/decrypt | ~2-8 microseconds per field | Slightly slower than GCM due to S2V computation |
| HMAC-SHA256 index computation | ~0.5-1 microseconds per field | Very fast |
| Vault Transit encrypt | ~2-5 milliseconds per call (network) | **100-1000x slower than local AES**. Batch encryption not supported by Transit API. |
| AWS KMS GenerateDataKey | ~10-30 milliseconds per call | Mitigated by data key caching (1 call per cache TTL, e.g., 5 minutes) |
| Blind index false positive filtering | O(false_positives) per query | With 16-bit index on 50K rows: ~1 false positive per query. With 8-bit: ~200 false positives. |
| Migration (plaintext to encrypted) | O(rows * fields) encrypt operations | For 1M rows with 2 encrypted fields: ~2M encrypt calls. With local AES: ~10 seconds. With Vault: ~3 hours. |

**Recommendation**: Default to local AES (embedded key) for performance. Vault/KMS as opt-in for highest-security deployments. Cache Vault/KMS data keys locally with TTL.

### 7. Plaintext-to-Encrypted Migration

This is a critical operational concern. Approaches:

#### 7.1 Liquibase ChangeSet with Custom SQL

```sql
-- Step 1: Add encrypted column (nullable)
ALTER TABLE sys_user ADD COLUMN phone_encrypted VARCHAR(256);
ALTER TABLE sys_user ADD COLUMN phone_index VARCHAR(64);

-- Step 2: Migrate data (application code or SQL function)
-- Cannot use pure SQL for application-managed encryption (key must be in app)
-- Need a custom Liquibase Change or Spring Boot CommandLineRunner

-- Step 3: Drop old column, rename new column
ALTER TABLE sys_user DROP COLUMN phone;
ALTER TABLE sys_user RENAME COLUMN phone_encrypted TO phone;
```

**Problem**: Liquibase changeSets run at app startup, but encryption requires the application's `FieldEncryptor` bean. Custom Liquibase `Change` implementations can access Spring context but add complexity.

#### 7.2 Application-Level Migration (Recommended)

```java
@Component
public class FieldEncryptionMigrator implements CommandLineRunner {
    @Override
    public void run(String... args) {
        if (!migrationEnabled) return;
        List<User> users = dataManager.findAll(User.class);
        for (User user : users) {
            // Re-save triggers encryption via EntityInsertHandler
            dataManager.save(User.class, user);
        }
    }
}
```

**Problems**: Loads all entities into memory; runs in a single transaction (too large for production); no resume capability.

**Better approach**: Batch migration with cursor/pagination:

```java
@Transactional
public void migrateBatch(int offset, int limit) {
    List<User> batch = jdbcClient.sql("SELECT * FROM sys_user WHERE phone NOT LIKE 'enc:%' LIMIT ? OFFSET ?")
        .params(limit, offset).query(rowMapper).list();
    for (User user : batch) {
        dataManager.save(User.class, user);
    }
}
```

#### 7.3 Dual-Write / Shadow Column Pattern

1. Add new encrypted column alongside existing plaintext column
2. Dual-write: on save, write both plaintext and encrypted
3. Backfill: migrate existing rows in batches
4. Verify: compare decrypted values with plaintext
5. Switch: application reads from encrypted column
6. Cleanup: drop plaintext column

This is the safest production migration strategy but requires schema changes and dual-write period.

### 8. The "Default NoOp" Problem

The AFG framework already handles this better than most:

**Current AFG approach**:
- `NoOpFieldEncryptor` logs a WARN message once (using `AtomicBoolean` to avoid spam)
- `@ConditionalOnMissingBean(FieldEncryptor.class)` ensures NoOp is replaced when a real implementation is registered
- Javadoc explicitly warns about the security risk

**Comparison with other frameworks**:
- Spring Security: `Encryptors.noOpText()` exists but is clearly named and documented as test-only
- MyBatis-Plus: `SafetyEncryptProcessor` does nothing if `mpw.key` is not provided (silent failure)
- Hibernate: No built-in encryption, so no NoOp problem (but also no help)

**Enhancements to consider**:
1. **Startup failure mode**: `afg.data.field-encryption.strict=true` causes application to fail to start if `@EncryptedField` is used but only NoOp is available
2. **Health indicator**: Actuator endpoint showing which entities have encrypted fields and whether real encryption is active
3. **Audit log**: Log every encrypt/decrypt operation at TRACE level with field name and entity type (never log plaintext/ciphertext values)

### 9. Feasible Approaches for AFG Framework

Based on the analysis above, here are 2-3 feasible approaches ranked by implementation complexity:

#### Approach 1: Deterministic Encryption + Condition Parameter Transformation (Minimal)

**What**: Add AES-SIV deterministic encryption implementation. Transform WHERE clause parameters for encrypted fields by encrypting them before SQL execution.

**Changes**:
- New `AesSivFieldEncryptor` implementation (AES-SIV via Bouncy Castle)
- `ConditionToSqlConverter` gains `EntityMetadata` awareness; encrypts condition values for encrypted fields
- `@EncryptedField` gains `mode` attribute: `DETERMINISTIC` (default, searchable) vs `RANDOMIZED` (not searchable)
- Reject `LIKE`, `GT`, `LT`, `GE`, `LE`, `BETWEEN` on encrypted fields at query time
- `FieldEncryptor` SPI unchanged (encrypt/decrypt already sufficient)

**Effort**: Medium (1-2 days)
**Security**: Acceptable for low-sensitivity fields. AES-SIV is NIST-approved for deterministic encryption.

#### Approach 2: Blind Index Columns (Recommended)

**What**: Add companion index columns for searchable encrypted fields. Use AES-GCM for main ciphertext + HMAC-SHA256 for blind index.

**Changes**:
- `@EncryptedField` gains `searchable` (boolean) and `indexLength` (int, bits) attributes
- `EncryptedFieldMetadata` gains `searchable`, `indexFieldName`, `indexLength` fields
- `FieldEncryptor` SPI gains `String computeIndex(String plaintext, String algorithm, String keyRef, int indexLength)` method
- `EntityInsertHandler` / `EntityUpdateHandler` write blind index values to companion columns
- `EntityMapper` skips blind index columns (they are not entity fields)
- `ConditionToSqlConverter` redirects encrypted field conditions to index columns
- Liquibase migration support: generate index column DDL
- `AesGcmFieldEncryptor` (default, randomized) + `AesSivFieldEncryptor` (deterministic, opt-in)

**Effort**: Medium-High (3-5 days)
**Security**: Strong. Main ciphertext is semantically secure. Blind index leaks only equality (controlled by index length).

#### Approach 3: Full Suite (Deterministic + Blind Index + Key Management SPI)

**What**: Both Approach 1 and Approach 2, plus a `FieldEncryptionKeyProvider` SPI for key management.

**Changes**:
- Everything from Approach 1 + Approach 2
- `FieldEncryptionKeyProvider` SPI: `getKey(keyRef)` / `getIndexKey(keyRef)`
- Implementations: `ConfigFieldEncryptionKeyProvider` (from `afg.data.field-encryption.keys.*`), `VaultFieldEncryptionKeyProvider`, `KmsFieldEncryptionKeyProvider`
- `@EncryptedField(mode = DETERMINISTIC | SEARCHABLE | ENCRYPTED)`
- Migration tool: `FieldEncryptionMigrator` with batch processing, resume capability, verification
- Strict mode: fail startup if encrypted fields exist without real encryptor
- Actuator health indicator for encryption status

**Effort**: High (5-8 days)
**Security**: Enterprise-grade. Appropriate security level per field. External key management. Audit trail.

## Caveats / Not Found

- No mature Java port of CipherSweet exists. The blind index implementation would need to be built from scratch, following CipherSweet's design.
- AES-SIV requires Bouncy Castle (not in JDK). This adds a dependency. Alternatively, AES-CBC with fixed IV can be used (weaker but no extra dependency).
- The `ConditionToSqlConverter` currently has no access to `EntityMetadata`, which is required for both Approach 1 and Approach 2. This is a required refactoring.
- The `Conditions.builder(Class)` API uses lambda method references (e.g., `User::getPhone`) which are resolved to field names at build time. The field name resolution does not currently distinguish encrypted from non-encrypted fields.
- LIKE queries on encrypted fields are fundamentally impossible with any client-side encryption approach. The only solution is database-side encryption (pgcrypto, MySQL AES_ENCRYPT) which requires key material in SQL.
- Range queries (GT, LT, BETWEEN) on encrypted fields require order-preserving encryption (OPE), which is extremely weak and not recommended. The framework should reject these at query time.
