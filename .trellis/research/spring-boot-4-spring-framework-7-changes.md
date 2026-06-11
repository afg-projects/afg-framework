# Research: Spring Boot 4 / Spring Framework 7 Best Practices and Breaking Changes

- **Query**: Spring Boot 4 / Spring Framework 7 major changes, deprecated patterns removed, new best practices for framework libraries
- **Scope**: External (official documentation, migration guides, release notes)
- **Date**: 2026-06-11

## Findings

### 1. Major Changes in Spring Boot 4.0 / Spring Framework 7.0

#### Spring Boot 4.0 — System Requirements
- **Java 17+** baseline (JDK 25 LTS recommended)
- **Kotlin 2.2+** required
- **GraalVM 25+** for native images (new "exact reachability metadata" format)
- **Jakarta EE 11** baseline: Servlet 6.1 (Tomcat 11.0, Jetty 12.1), JPA 3.2, Bean Validation 3.1
- **Spring Framework 7.x** required

#### Spring Boot 4.0 — New Modular Design (CRITICAL for framework libraries)
Spring Boot 4.0 has a **new modular design** that ships smaller focused modules instead of several large jars. This is the single most impactful change for framework library authors.

**Module naming convention:**
- All Spring Boot modules: `spring-boot-<technology>` (e.g., `spring-boot-jdbc`, `spring-boot-security`)
- Root package of each module: `org.springframework.boot.<technology>`
- All starters: `spring-boot-starter-<technology>`
- All test starters: `spring-boot-starter-<technology>-test`
- Test modules: `spring-boot-<technology>-test`

**Key starter renames (deprecated old names):**

| Old Name | New Name |
|----------|----------|
| `spring-boot-starter-web` | `spring-boot-starter-webmvc` |
| `spring-boot-starter-web-services` | `spring-boot-starter-webservices` |
| `spring-boot-starter-oauth2-authorization-server` | `spring-boot-starter-security-oauth2-authorization-server` |
| `spring-boot-starter-oauth2-client` | `spring-boot-starter-security-oauth2-client` |
| `spring-boot-starter-oauth2-resource-server` | `spring-boot-starter-security-oauth2-resource-server` |
| `spring-boot-starter-aop` | `spring-boot-starter-aspectj` |

**Classic Starters for migration:**
- `spring-boot-starter-classic` replaces `spring-boot-starter`
- `spring-boot-starter-test-classic` replaces `spring-boot-starter-test`
- These include all modules but exclude transitive dependencies (similar to Boot 3.x classpath)
- Intended as an intermediate migration step, NOT for long-term use

**Test starters now transitively include `spring-boot-starter-test`:**
- No need to declare `spring-boot-starter-test` separately
- Declare only the technology-specific test starters needed

**Framework library implication:** Supporting both Spring Boot 3 and Spring Boot 4 in the same artifact is **strongly discouraged** by the Spring team.

#### Spring Framework 7.0 — Key New Features

1. **JSpecify Nullability annotations** — entire codebase annotated with JSpecify, replacing JSR 305. Affects Kotlin consumers and null-checker users.
2. **Programmatic Bean Registration** — New `BeanRegistrar` contract for flexible bean registration outside `@Bean` methods.
3. **Built-in Resilience** — `RetryTemplate`, `@Retryable`, `@ConcurrencyLimit` moved from Spring Retry into `spring-core`/`spring-context`. Enable via `@EnableResilientMethods`.
4. **Jackson 3.x as default** — Jackson 3 uses `tools.jackson` package. Jackson 2.x support is deprecated.
5. **Class-File API** — Uses Java 24+ Class-File API (JEP 484) instead of ASM for metadata reading.
6. **CGLIB proxy type defaulting** — Consistent CGLIB proxy defaulting across all proxy processors. New `@Proxyable` annotation to opt out per bean.
7. **JmsClient** — New `JmsClient` API similar to `JdbcClient` and `RestClient`.
8. **GraalVM unified reachability metadata format** — New glob-pattern syntax for resource hints.

---

### 2. Deprecated Patterns from Spring Boot 3.x REMOVED in Spring Boot 4.0

| Removed Feature | Replacement / Notes |
|-----------------|---------------------|
| **Undertow support** | Dropped entirely. Servlet 6.1 baseline; Undertow not compatible. |
| **Pulsar Reactive** | Removed reactive Pulsar client and auto-configuration. |
| **Embedded launch scripts (fully executable jars)** | Use `java -jar` or Gradle application plugin. |
| **Spring Session Hazelcast** | Moved to Hazelcast team. |
| **Spring Session MongoDB** | Moved to MongoDB team. |
| **Spock integration** | Removed (Groovy 5 incompatible). |
| **Classic uber-jar loader** | Must remove `CLASSIC` loader implementation config. |
| **`@MockBean` / `@SpyBean`** | Replaced by `@MockitoBean` / `@MockitoSpyBean`. Cannot be used in `@Configuration` classes anymore. |
| **`MockitoTestExecutionListener`** | Use `MockitoExtension` from Mockito. |
| **`@SpringBootTest` + MockMVC** | Must add `@AutoConfigureMockMvc` explicitly. |
| **`@SpringBootTest` + WebClient/TestRestTemplate** | Must add `@AutoConfigureTestRestTemplate` or `@AutoConfigureRestTestClient`. |
| **`@PropertyMapping` package** | Moved from `org.springframework.boot.test.autoconfigure.properties` to `org.springframework.boot.test.context`. |
| **Spring Retry dependency management** | Retry features now in Spring Framework core. |
| **Spring Authorization Server separate dependency management** | Now part of Spring Security. |

---

### 3. Best Practices for Framework Libraries

#### 3.1 AutoConfiguration Registration

**Current mechanism (unchanged):** `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

The `.imports` file remains the standard mechanism. `spring.factories` for auto-configuration was removed in Spring Boot 3.0 and does NOT return.

**New feature: AutoConfiguration.replacements file**
- Path: `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.replacements`
- Format: `old.class.Name=new.class.Name`
- Used when renaming/moving auto-configuration classes
- The `.imports` file should reference ONLY the replacement class

**Rules for auto-configuration classes:**
1. Must be loaded ONLY by being named in the `.imports` file
2. Must NOT be the target of component scanning
3. Must NOT enable component scanning themselves (use `@Import` instead)
4. Annotate with `@AutoConfiguration` (meta-annotated with `@Configuration`)
5. Use `@ConditionalOnClass` / `@ConditionalOnMissingBean` as typical conditions

**Auto-configuration ordering:**
- Use `@AutoConfigureBefore` / `@AutoConfigureAfter` (or `before`/`after` attributes on `@AutoConfiguration`)
- Use `@AutoConfigureOrder` for ordering without direct knowledge of each other
- For cross-module references without compile dependency, use `beforeName`/`afterName` with string class names
- Order only affects bean definition order, not bean creation order (which is determined by dependencies and `@DependsOn`)

**Package changes in Spring Boot 4:**
- `BootstrapRegistry` moved from `org.springframework.boot` to `org.springframework.boot.bootstrap`
- `EnvironmentPostProcessor` moved from `org.springframework.boot.env` to `org.springframework.boot`
- The deprecated form of `EnvironmentPostProcessor` is still available in Boot 4.0 but will be removed later

#### 3.2 Property Binding and Configuration Properties

**No fundamental changes to `@ConfigurationProperties` mechanism**, but notable items:

- `@ConstructorBinding` is no longer needed (since Boot 3.0); constructor binding is automatic when there's a single parameterized constructor
- `spring-boot-configuration-processor` still generates metadata
- Property migrator module available: `spring-boot-properties-migrator` (runtime scope, remove after migration)

**Property changes in Boot 4.0:**
- `spring.dao.exceptiontranslation.enabled` → `spring.persistence.exceptiontranslation.enabled`
- `spring.jackson.read.*` / `spring.jackson.write.*` → `spring.jackson.json.read.*` / `spring.jackson.json.write.*`
- `spring.data.mongodb.*` (connection props) → `spring.mongodb.*`
- MongoDB management properties: `mongo` → `mongodb` in property names
- `server.forward-headers-strategy` no longer works for war deployments
- New `spring.jackson.find-and-add-modules` property (default: true, auto-detects all Jackson modules)

#### 3.3 Security Configuration Patterns

**Spring Security 7.0 changes:**
- Spring Authorization Server is now part of Spring Security
- Jackson 3 support: Use `SecurityJacksonModules` with `JsonMapper.Builder` instead of `SecurityJackson2Modules` with `ObjectMapper`
- Security test infrastructure now requires `spring-boot-starter-security-test` (not just `spring-security-test`)
- `@WithMockUser` / `@WithUserDetails` require `spring-boot-starter-security-test`

**New starter names:**
- `spring-boot-starter-security` (main)
- `spring-boot-starter-security-test` (test)
- `spring-boot-starter-security-oauth2-authorization-server`
- `spring-boot-starter-security-oauth2-client`
- `spring-boot-starter-security-oauth2-resource-server`
- `spring-boot-starter-security-saml2`

#### 3.4 Observability and Metrics

**New modular structure:**
- `spring-boot-micrometer-metrics` module (main code)
- `spring-boot-micrometer-observation` module
- `spring-boot-micrometer-tracing` module
- `spring-boot-micrometer-tracing-brave` module
- `spring-boot-micrometer-tracing-opentelemetry` module

**New starters:**
- `spring-boot-starter-micrometer-metrics` + test
- `spring-boot-starter-opentelemetry` + test
- `spring-boot-starter-zipkin` + test
- `spring-boot-starter-actuator` + test

**Liveness/readiness probes now enabled by default** in Boot 4.0. Disable with `management.endpoint.health.probes.enabled=false` if not needed.

#### 3.5 Native Image / AOT Compatibility

**GraalVM 25+ required** for native images in Boot 4.

**Spring Framework 7 reachability metadata changes:**
1. **Resource hints syntax changed** from `java.util.regex.Pattern` to glob patterns:
   - Old: `/files/*.ext` matched both `/files/a.ext` and `/files/folder/b.ext`
   - New: `/files/*.ext` matches only `/files/a.ext`; use `/files/**/*.ext` for both
   - Registration of "excludes" has been removed completely
2. **Reflection hints simplified:**
   - Registering a type hint now implies methods, constructors, and fields introspection
   - `ExecutableMode.INTROSPECT` and most `MemberCategory` values (except `INVOKE_*`) are deprecated
   - Replace `hints.reflection().registerType(MyType.class, MemberCategory.DECLARED_FIELDS)` with `hints.reflection().registerType(MyType.class)`
   - `MemberCategory.PUBLIC_FIELDS` / `DECLARED_FIELDS` replaced by `INVOKE_PUBLIC_FIELDS` / `INVOKE_DECLARED_FIELDS`

**Java 25 required for buildpack build-image Graal support.**

#### 3.6 Dependency Management and BOM Usage

**Spring Boot BOM still manages dependency versions.** Key changes:

- **Jackson 3** is the new default JSON library
  - New group ID: `tools.jackson` (was `com.fasterxml.jackson`)
  - Exception: `jackson-annotations` stays at `com.fasterxml.jackson.core`
  - `Jackson2ObjectMapperBuilderCustomizer` → `JsonMapperBuilderCustomizer`
  - `@JsonComponent` → `@JacksonComponent`
  - `@JsonMixin` → `@JacksonMixin`
  - Jackson 2 compatibility module: `spring-boot-jackson2` (deprecated, stop-gap)
  - Jackson 2 properties under `spring.jackson2.*`
- **Hibernate 7.1/7.2** managed (was Hibernate 6.x)
  - `hibernate-jpamodelgen` → `hibernate-processor`
  - `hibernate-proxool` and `hibernate-vibur` no longer published
- **Spring Retry** dependency management removed (moved to Spring Framework core)
- **Spring Authorization Server** managed via Spring Security BOM now
- **JUnit 6** (was JUnit 5)

**Portfolio version upgrades:**
- Spring AMQP 4.0
- Spring Batch 6.0
- Spring Data 2025.1
- Spring GraphQL 2.0
- Spring Integration 7.0
- Spring for Apache Kafka 4.0
- Spring for Apache Pulsar 2.0
- Spring Security 7.0
- Spring REST Docs 4.0
- Spring Session 4.0
- Spring WS 5.0

#### 3.7 Testing Patterns

**Major testing changes in Boot 4.0:**

1. **`@MockBean` / `@SpyBean` REMOVED** → Use `@MockitoBean` / `@MockitoSpyBean`
   - Key difference: new annotations can only be used as fields in test classes, NOT in `@Configuration` classes
   - For shared mocked beans: use `@MockitoBean(types = {...})` on test class or create a custom annotation
2. **`MockitoTestExecutionListener` REMOVED** → Use `MockitoExtension`
3. **`@SpringBootTest` no longer provides MockMVC** → Add `@AutoConfigureMockMvc` explicitly
   - HtmlUnit settings changed: `@AutoConfigureMockMvc(htmlUnit = @HtmlUnit(webClient = false, webDriver = false))`
4. **`@SpringBootTest` no longer provides WebClient/TestRestTemplate** → Add `@AutoConfigureTestRestTemplate` or `@AutoConfigureRestTestClient`
   - `TestRestTemplate` moved to `org.springframework.boot.resttestclient` package
   - New `RestTestClient` class as replacement for `TestRestTemplate`
   - Requires `spring-boot-resttestclient` and `spring-boot-restclient` dependencies
5. **`@PropertyMapping` package moved** → `org.springframework.boot.test.context`
6. **Test starters** — Each technology now has a dedicated test starter that brings `spring-boot-starter-test` transitively
7. **JUnit 4 support deprecated** in Spring TestContext Framework

#### 3.8 Jakarta EE 11 Changes

- **Servlet 6.1** baseline (Tomcat 11+, Jetty 12.1+)
- **JPA 3.2** — `EntityManager` can be injected via `@Inject`/`@Autowired` directly now, including qualifier support
- **Bean Validation 3.1** (Hibernate Validator 9.0/9.1)
- **WebSocket 2.2**
- `javax.annotation` annotations NO LONGER SUPPORTED → Must use `jakarta.annotation`
- `javax.inject` annotations NO LONGER SUPPORTED → Must use `jakarta.inject`

---

### 4. Recommended Patterns for Framework Libraries

#### Conditional Bean Registration

**Same patterns as Boot 3.x, no changes:**
- `@ConditionalOnClass` / `@ConditionalOnMissingClass` — classpath conditions
- `@ConditionalOnBean` / `@ConditionalOnMissingBean` — bean presence conditions
- `@ConditionalOnProperty` / `@ConditionalOnBooleanProperty` — property conditions
- `@ConditionalOnResource` — resource presence
- `@ConditionalOnWebApplication` / `@ConditionalOnNotWebApplication` — web app type
- `@ConditionalOnExpression` — SpEL (note: referencing a bean causes early initialization)

**Best practice:** Use `@ConditionalOnMissingBean` on `@Bean` methods in auto-configuration classes only, since auto-configurations are guaranteed to load after user-defined beans.

**For `@ConditionalOnClass` on `@Bean` methods:** Use a separate `@Configuration` class with `@ConditionalOnClass` to isolate the condition, since the JVM may load the return type class before the condition is evaluated.

#### Auto-configuration Ordering

Same mechanisms, unchanged:
- `@AutoConfiguration(after = {...}, before = {...})` or `@AutoConfigureAfter` / `@AutoConfigureBefore`
- `@AutoConfigureOrder` for ordering without direct knowledge
- For cross-module references: use `afterName` / `beforeName` with string class names to avoid compile-time dependency

#### Configuration Properties Validation

No changes from Boot 3.x:
- Use `@Validated` on `@ConfigurationProperties` classes
- Use Jakarta Validation annotations (`@NotNull`, `@Size`, etc.)
- Constructor binding is automatic (no `@ConstructorBinding` needed since Boot 3.0)
- Use `spring-boot-configuration-processor` for metadata generation

#### Spring Factories vs AutoConfiguration.imports

**`spring.factories` for auto-configuration is dead** (removed in Boot 3.0, does not return in Boot 4).

Current state:
- Auto-configuration registration: `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- `EnvironmentPostProcessor` registration: Still uses `spring.factories` (deprecated form in Boot 4, will be removed later; new location is `META-INF/spring/org.springframework.context.ApplicationContextInitializer.imports`)
- Auto-configuration replacements: `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.replacements` (NEW in Boot 4)

#### RestClient vs RestTemplate

**RestTemplate is deprecated** in Spring Framework 7.0 (reference documentation deprecated; will be marked `@Deprecated` in 7.1).

- **Use `RestClient`** for new code (introduced in Spring Framework 6.1, Boot 3.2)
- `RestClient` is the imperative equivalent of `WebClient`
- Spring Boot 4 provides `spring-boot-starter-restclient` starter
- New `RestTestClient` for testing (replaces `TestRestTemplate`)
- Spring Boot 4 auto-configures `RestClient.Builder` beans

---

### 5. Impact Analysis for afg-framework

Based on the project's current codebase (Spring Boot 4.0.6), the following patterns were identified:

#### Items Already Correctly Handled
- Using `AutoConfiguration.imports` (not `spring.factories` for auto-configuration) in all modules
- Using `@AutoConfiguration` annotation on auto-configuration classes
- No `@MockBean` / `@SpyBean` usage found in the codebase
- No Undertow references
- Using Jakarta namespace (not javax) in application code

#### Items Needing Attention
1. **`spring.factories` for `EnvironmentPostProcessor`** — `core/src/main/resources/META-INF/spring.factories` still uses the deprecated mechanism. The `EnvironmentPostProcessor` class has also moved packages (`org.springframework.boot.env` → `org.springframework.boot`). The deprecated form still works in Boot 4.0 but will be removed later.

2. **`spring-boot-starter-web` reference** — `libs.versions.toml` still references `spring-boot-starter-web`, which is deprecated in favor of `spring-boot-starter-webmvc`.

3. **`HttpMessageConverters` usage** — Found in `ai-core/src/test/.../AiTestConfiguration.java`. This class is deprecated in Boot 4; should migrate to `ClientHttpMessageConvertersCustomizer` / `ServerHttpMessageConvertersCustomizer`.

4. **`RestTemplate` usage** — Project uses `RestClient` in some places, but some test classes still reference `RestTemplate`. With Spring Framework 7 deprecating RestTemplate, plan migration to `RestClient`.

5. **Jackson 2 (`com.fasterxml.jackson`)** — Multiple modules use `com.fasterxml.jackson` directly. Jackson 3 is the new default in Boot 4. Jackson 2 compatibility module (`spring-boot-jackson2`) is available but deprecated.

6. **`javax.annotation` in APT module** — The `apt-impl` module uses `javax.annotation` (for APT processing, which is different from runtime). This is in the annotation processor context, so it may be acceptable since APT runs during compilation and uses its own classpath.

7. **`@AutoConfigureAfter` / `@AutoConfigureBefore`** — Project comments mention these but actual usage in auto-configuration classes is minimal. The project's CLAUDE.md already documents the correct pattern of using `afterName` for cross-module references.

8. **Liveness/readiness probes** — Now enabled by default in Boot 4. May need explicit disable if not desired.

9. **Test infrastructure** — `@SpringBootTest` no longer provides MockMVC or TestRestTemplate. Test classes may need `@AutoConfigureMockMvc` or `@AutoConfigureTestRestTemplate`.

10. **Module dependencies** — Spring Boot 4's modular design means that Flyway and Liquibase now require their own starters (`spring-boot-starter-flyway`, `spring-boot-starter-liquibase`) instead of just the third-party dependency. The project's `data-liquibase` module should verify this.

---

### External References

- [Spring Boot 4.0 Migration Guide (GitHub Wiki)](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide) — Primary migration reference, last edited Apr 30, 2026
- [Spring Framework 7.0 Release Notes (GitHub Wiki)](https://github.com/spring-projects/spring-framework/wiki/Spring-Framework-7.0-Release-Notes) — Framework-level changes, last edited Jun 10, 2026
- [Spring Security 7 Migration Guide](https://docs.spring.io/spring-security/reference/migration/index.html) — Security migration steps
- [Spring Boot 4 Reference Documentation](https://docs.spring.io/spring-boot/reference/) — Current reference docs
- [Spring Boot Custom Auto-configuration](https://docs.spring.io/spring-boot/reference/features/developing-auto-configuration.html) — How to build auto-configuration and starters

## Caveats / Not Found

- Spring Framework 7's "What's New" page was not accessible (URL returns 404 with the tested path patterns). Content was instead obtained from the GitHub wiki release notes.
- The Spring Boot 4.0 Migration Guide mentions that `EnvironmentPostProcessor` has moved packages and the deprecated form still works but will be removed "at a later date" — the exact version for removal was not specified.
- Specific Spring Security 7 detailed migration content was limited to what's on the official docs site; the GitHub wiki for Spring Security does not have a dedicated 7.0 migration guide page.
- Spring Boot 4.1.0 is already released (referenced in docs navigation), meaning 4.0 is not the latest — the 4.1 changes were not researched here.
