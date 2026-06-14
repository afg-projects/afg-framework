# Research: TreeQuery APT/Metadata Issue

- **Query**: Why does TreeQuery integration test fail -- INSERT includes non-existent `children` column
- **Scope**: internal
- **Date**: 2026-06-14

## Findings

### Root Cause Analysis

The failure occurs because `ReflectiveEntityMetadata` (the fallback metadata loader used in tests) includes the `children` field from `TreeEntity` as a regular database column, causing INSERT SQL to reference a `children` column that does not exist in the database table.

**The problem has three layers:**

1. **`TreeEntity.children` lacks the Java `transient` keyword.** The field is declared as `protected List<T> children;` (line 92 of `TreeEntity.java`) with a Javadoc comment "transient, not persisted" but no `transient` modifier. The `Treeable` interface Javadoc also says "children list is transient data, not persisted to the database."

2. **Neither the APT processor nor the ReflectiveMetadataLoader skip the `children` field.** Both metadata extraction paths only skip:
   - Static fields (`Modifier.isStatic`)
   - Association fields (`@ManyToOne`, `@OneToMany`, `@OneToOne`, `@ManyToMany`)
   
   The `children` field has no association annotation and is not static, so both paths treat it as a regular persistent column.

3. **Test entities use `ReflectiveMetadataLoader` instead of `AptMetadataLoader`.** Test entities (e.g., `TestCategory`) do not have `@AfEntity` annotation, so APT does not generate metadata for them. The `EntityMetadataCache` loader chain tries `AptMetadataLoader` first (priority 0), but it returns `false` for `supports()` since no `TestCategoryMetadata` class exists. Then `ReflectiveMetadataLoader` (priority 1000) is used as fallback.

### Files Found

| File Path | Description |
|---|---|
| `data-core/src/main/java/.../entity/TreeEntity.java` | Tree entity base class with `children` field (no `transient` modifier) |
| `data-core/src/main/java/.../entity/Treeable.java` | Treeable interface defining `getChildren()/setChildren()` |
| `data-core/src/main/java/.../metadata/ReflectiveMetadataLoader.java` | Fallback metadata loader using SPI MetadataProvider |
| `data-core/src/main/java/.../metadata/AptMetadataLoader.java` | APT metadata loader (priority 0, highest) |
| `data-core/src/main/java/.../metadata/EntityMetadataCache.java` | Metadata cache with loader chain (APT -> Reflective -> Empty) |
| `data-core/src/main/java/.../metadata/FieldMetadata.java` | Field metadata interface (no isTransient/isInsertable) |
| `data-core/src/main/java/.../metadata/DatabaseFieldMetadata.java` | Extended field metadata with `isInsertable()/isUpdatable()` (defaults true, never used) |
| `data-impl/data-jdbc/src/main/java/.../metadata/ReflectiveEntityMetadata.java` | Reflective metadata impl -- only skips static and association fields |
| `data-impl/data-jdbc/src/main/java/.../metadata/ReflectiveFieldMetadata.java` | Reflective field metadata -- no transient check |
| `data-impl/data-jdbc/src/main/java/.../metadata/JdbcMetadataProvider.java` | SPI provider creating ReflectiveEntityMetadata |
| `data-impl/data-jdbc/src/main/java/.../autoconfigure/DataManagerAutoConfiguration.java` | AutoConfiguration creating JdbcDataManager |
| `data-impl/data-jdbc/src/main/java/.../EntityInsertHandler.java` | INSERT handler using `metadata.getFields()` with only `isGenerated()` filter |
| `data-impl/data-jdbc/src/main/java/.../SqlBuilder.java` | SQL builder using `metadata.getFields()` with no transient filter |
| `data-impl/data-jdbc/src/main/java/.../ParameterExtractor.java` | Parameter extraction using `metadata.getFields()` |
| `data-impl/data-jdbc/src/main/java/.../query/JdbcTreeQuery.java` | TreeQuery JDBC implementation |
| `data-impl/data-jdbc/src/test/java/.../entity/TestCategory.java` | Test tree entity (no @AfEntity, no @Table, no @Column) |
| `data-impl/data-jdbc/src/test/java/.../JdbcDataTestConfiguration.java` | Test config importing auto-configuration chain |
| `data-impl/data-jdbc/src/test/java/.../test/BaseDataTest.java` | Test base class using JdbcDataTestConfiguration |
| `data-impl/data-jdbc/src/test/resources/db/changelog/test-data/v1.0.0/007_test_category.xml` | Liquibase migration for test_category (no `children` column) |
| `apt-impl/src/main/java/.../entity/FieldMetadataGenerator.java` | APT field extractor -- only skips static and relation fields |
| `apt-impl/src/main/java/.../entity/RelationMetadataGenerator.java` | APT relation detector -- only checks @ManyToOne/@OneToMany/etc annotations |
| `apt-impl/src/main/java/.../entity/EntityFeatureDetector.java` | APT feature detector -- does NOT detect TREEABLE trait |
| `apt-impl/src/main/java/.../entity/MetadataCodeGenerator.java` | APT code generator |
| `data-impl/data-jdbc/build.gradle.kts` | No APT annotation processor configured for test sources |

### Code Patterns

**ReflectiveEntityMetadata.extractFields (line 358-379):**
```java
private static List<DatabaseFieldMetadata> extractFields(Class<?> clazz) {
    List<DatabaseFieldMetadata> result = new ArrayList<>();
    Class<?> currentClass = clazz;
    while (currentClass != null && currentClass != Object.class) {
        for (Field field : currentClass.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) { continue; }  // only skips static
            if (ReflectiveFieldMetadata.isAssociationField(field)) { continue; }  // only skips @ManyToOne etc
            result.add(new ReflectiveFieldMetadata(field));  // children ends up here!
        }
        currentClass = currentClass.getSuperclass();
    }
    return result;
}
```

**APT FieldMetadataGenerator.extractFields (line 48-99):**
```java
for (VariableElement field : ElementFilter.fieldsIn(currentClass.getEnclosedElements())) {
    if (field.getModifiers().contains(Modifier.STATIC)) { continue; }
    if (relationMetadataGenerator.isRelationField(field)) { continue; }
    // No check for transient modifier or TREEABLE children field
    fields.add(new FieldInfo(propertyName, columnName, fieldType, isId, isGenerated, hasCustomColumnName));
}
```

**SqlBuilder.doBuildInsertSql (line 54-69):**
```java
for (var field : metadata.getFields()) {
    if (!field.isGenerated()) {
        columns.add(field.getColumnName());  // includes "children" column
        placeholders.add("?");
    }
}
```

**TreeEntity.children field (line 92):**
```java
// Javadoc says "transient, not persisted" but no transient keyword!
protected List<T> children;
```

**TestCategory entity -- no @AfEntity annotation:**
```java
@Getter @Setter
public class TestCategory extends TreeEntity<TestCategory> implements LifecycleCallbacks {
    private String name;
    private String description;
    // ...
}
```

### The Full Failure Chain

1. `TestCategory` extends `TreeEntity<TestCategory>` which has `List<TestCategory> children` field
2. `TestCategory` has no `@AfEntity` annotation, so APT does not generate `TestCategoryMetadata`
3. `AptMetadataLoader.supports(TestCategory.class)` returns `false` (no `TestCategoryMetadata` class)
4. `ReflectiveMetadataLoader` is used, delegates to `JdbcMetadataProvider` which calls `ReflectiveEntityMetadata.create()`
5. `ReflectiveEntityMetadata.extractFields()` includes `children` as a regular field with column name `children`
6. `SqlBuilder.buildInsertSql()` generates `INSERT INTO test_category (name, description, parent_id, level, path, sort_order, created_at, updated_at, children) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`
7. PostgreSQL rejects the INSERT because `children` column does not exist in `test_category` table

### Why APT Would Also Fail (If TestCategory Had @AfEntity)

Even if `TestCategory` had `@AfEntity`, the APT-generated metadata would still include `children` as a field because:
- `FieldMetadataGenerator` does not check `Modifier.TRANSIENT`
- `isRelationField()` only checks for `@ManyToOne`/`@OneToMany`/`@OneToOne`/`@ManyToMany` annotations
- `children` has no association annotation on it

The APT path works for `SecDept` only because `SecDept` does NOT extend `TreeEntity` and has no `children` field at all.

### Additional Gaps Found

1. **`EntityFeatureDetector` does not detect `TREEABLE` trait.** The `FeatureDetectionResult` record has no `treeable` field, and the `detect()` method does not check for `parentId` + `path` fields.

2. **`ReflectiveEntityMetadata.getTraits()` does not detect `TREEABLE` trait.** It checks for `SOFT_DELETABLE`, `TIMESTAMP_SOFT_DELETABLE`, `TENANT_AWARE`, `TIMESTAMPED`, `AUDITABLE`, `VERSIONED`, `DATA_SCOPE_AWARE` but not `TREEABLE`.

3. **`DatabaseFieldMetadata.isInsertable()/isUpdatable()` exist but are never used.** These methods (defaults `true`) could be the mechanism to mark `children` as non-insertable, but neither `SqlBuilder`, `EntityInsertHandler`, nor `ParameterExtractor` consult them.

4. **No `@Transient` annotation equivalent.** The framework has no annotation to mark a field as non-persistent (like JPA's `@Transient` or Spring Data's `@Transient`).

## Caveats / Not Found

- Could not find an existing TreeQuery test file -- the task description says "tests fail" but no test file was found in the repository yet. The test is presumably being written.
- No existing mechanism in the framework for marking fields as transient/non-persistent other than association annotations.
