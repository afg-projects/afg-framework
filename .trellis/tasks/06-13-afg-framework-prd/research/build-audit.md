# Research: Build Audit

- **Query**: Full build audit of afg-framework - compile status, test status, module inventory, dependency issues
- **Scope**: Internal
- **Date**: 2026-06-14

## Findings

### Module Inventory

#### Included in settings.gradle.kts (23 modules)

| # | Module Path | Has build.gradle.kts | Has Test Source | Test Report |
|---|---|---|---|---|
| 1 | `gradle-plugin` | Yes | Yes (Kotlin) | Yes (5 tests) |
| 2 | `commons` | Yes | Yes | Yes (188 tests) |
| 3 | `apt-api` | Yes | No | N/A |
| 4 | `apt-impl` | Yes | Yes | Yes (60 tests) |
| 5 | `core` | Yes | Yes | Yes (582 tests) |
| 6 | `ai-core` | Yes | Yes | Yes (243 tests, 6 skipped) |
| 7 | `ai-impl:ai-langchain4j` | Yes | No | N/A |
| 8 | `ai-impl:ai-spring-ai` | Yes | No | N/A |
| 9 | `data-core` | Yes | Yes | Yes (895 tests) |
| 10 | `data-impl:data-sql` | Yes | Yes | Yes (239 tests) |
| 11 | `data-impl:data-jdbc` | Yes | Yes | Yes (99 tests) |
| 12 | `data-impl:data-liquibase` | Yes | Yes | Yes (48 tests) |
| 13 | `security-core` | Yes | Yes | Yes (370 tests) |
| 14 | `security-impl:auth-server` | Yes | Yes | Yes (183 tests) |
| 15 | `security-impl:resource-server` | Yes | No | N/A |
| 16 | `integration:afg-redis` | Yes | No | N/A |
| 17 | `integration:afg-jdbc` | Yes | No | N/A |
| 18 | `integration:afg-rabbitmq` | Yes | No | N/A |
| 19 | `integration:afg-websocket` | Yes | No | N/A |
| 20 | `integration:afg-storage` | Yes | No | N/A |
| 21 | `governance:proto` | Yes | No | N/A |
| 22 | `governance:client` | Yes | No | N/A |
| 23 | `governance:server` | Yes | No | N/A |

#### Not included but has build.gradle.kts

None. All directories with `build.gradle.kts` are included in `settings.gradle.kts`. (The earlier `comm` comparison showed false positives because `settings.gradle.kts` uses colon notation like `data-impl:data-jdbc` while filesystem uses slashes like `data-impl/data-jdbc`.)

### Compile Status

**BUILD SUCCESSFUL** - All 23 modules compile without errors.

- `./gradlew build -x test` completed in 11s, 144 actionable tasks (all up-to-date on second run)
- No compilation errors in any module
- PMD: 0 violations across all modules (pmdTest is disabled globally)

### Test Status

**BUILD SUCCESSFUL** - All tests pass, 0 failures, 0 errors.

| Module | Tests | Failures | Errors | Skipped | Notes |
|---|---|---|---|---|---|
| commons | 188 | 0 | 0 | 0 | |
| apt-impl | 60 | 0 | 0 | 0 | |
| core | 582 | 0 | 0 | 0 | |
| data-core | 895 | 0 | 0 | 0 | |
| data-impl:data-sql | 239 | 0 | 0 | 0 | |
| data-impl:data-jdbc | 99 | 0 | 0 | 0 | |
| data-impl:data-liquibase | 48 | 0 | 0 | 0 | |
| security-core | 370 | 0 | 0 | 0 | |
| security-impl:auth-server | 183 | 0 | 0 | 0 | |
| ai-core | 243 | 0 | 0 | 6 | Skipped: Ollama not available |
| gradle-plugin | 5 | 0 | 0 | 0 | |
| **TOTAL** | **2912** | **0** | **0** | **6** | |

#### Skipped Tests Detail (ai-core, 6 total)

All 6 skipped tests are in `ai-core` and are skipped due to Ollama not being available at `localhost:11434`. These use `TestAbortedException` (JUnit 5 assumptions) which is the correct pattern for conditional test execution.

| Test Class | Test Method | Skip Reason |
|---|---|---|
| `AiChatControllerTest` | `shouldSendMessage_whenPostMessage()` | Ollama not available |
| `AiChatControllerTest` | `shouldStreamMessage_whenPostMessageWithStream()` | Ollama not available |
| `AiFullFlowTest` | `shouldCompleteFullFlow_whenCreateModelToChat()` | Ollama not available |
| `AiKnowledgeControllerTest` | `shouldUploadDocument_whenPostMultipart()` | Ollama not available |
| `AiKnowledgeControllerTest` | `shouldSearchKnowledge_whenPostSearchRequest()` | Ollama not available |
| `AiAgentControllerTest` | `shouldExecuteAgent_whenPostExecute()` | Ollama not available |

#### Modules with No Test Source (12 modules)

These modules have no `src/test/` directory at all:

- `apt-api` - annotation definitions only, no logic to test
- `ai-impl:ai-langchain4j` - adapter module, no tests
- `ai-impl:ai-spring-ai` - adapter module, no tests
- `security-impl:resource-server` - no tests
- `integration:afg-redis` - no tests
- `integration:afg-jdbc` - no tests
- `integration:afg-rabbitmq` - no tests
- `integration:afg-websocket` - no tests
- `integration:afg-storage` - no tests
- `governance:proto` - protobuf definitions only
- `governance:client` - no tests
- `governance:server` - no tests

### Issues Found

#### 1. Duplicate dependency declaration in ai-core

**File**: `ai-core/build.gradle.kts` lines 74 and 76

```kotlin
testImplementation(project(":data-impl:data-liquibase"))  // line 74
testImplementation(project(":data-impl:data-liquibase"))  // line 76 (duplicate)
```

**Impact**: No functional impact (Gradle deduplicates), but indicates copy-paste error.

#### 2. Hardcoded dependency versions (not in version catalog)

**ai-core/build.gradle.kts**:
- `org.apache.pdfbox:pdfbox:3.0.1` (line 50)
- `org.commonmark:commonmark:0.22.0` (line 53)
- `org.commonmark:commonmark-ext-gfm-tables:0.22.0` (line 54)
- `com.github.albfernandez:juniversalchardet:2.4.0` (line 57)
- `org.springframework.boot:spring-boot-restclient` (line 70) - no version but also not in version catalog

**gradle-plugin/build.gradle.kts** (expected - gradle-plugin doesn't use Spring Boot BOM):
- `org.liquibase:liquibase-core:5.0.2` (line 57)
- `org.yaml:snakeyaml:2.4` (line 60)
- `org.junit.jupiter:junit-jupiter:5.12.2` (line 72)
- `org.assertj:assertj-core:3.27.3` (line 73)
- `org.awaitility:awaitility:4.3.0` (line 74)
- `org.junit.platform:junit-platform-launcher:1.12.2` (line 75)

**data-impl/data-jdbc/build.gradle.kts**:
- `org.postgresql:postgresql:42.7.5` (line 482) - hardcoded PostgreSQL driver version
- `org.springframework.boot:spring-boot-starter-liquibase` (line 480) - no version catalog entry

**Impact**: Hardcoded versions in ai-core and data-jdbc bypass the version catalog, making version updates harder. The gradle-plugin is expected to have hardcoded versions since it doesn't use Spring Boot BOM.

#### 3. Gradle deprecation warnings

- **Multi-string dependency notation deprecated** in `governance:proto/build.gradle.kts` - protobuf plugin uses multi-string notation for protoc artifacts. Will fail in Gradle 10.
- **Unsupported Kotlin plugin version** warning - Kotlin plugin version may not match Gradle version expectations.
- **Native access warnings** - `java.lang.System::load` called by Gradle's native-platform library. Not actionable by project.

#### 4. Liquibase version inconsistency

- `gradle-plugin` uses `liquibase-core:5.0.2` (hardcoded)
- Other modules use `libs.liquibase.core` which resolves via Spring Boot BOM
- The Spring Boot 4.0.6 BOM likely manages a different Liquibase version

#### 5. 12 modules have zero test coverage

The following modules have no test source at all:
- `security-impl:resource-server` - security-critical module with no tests
- `ai-impl:ai-langchain4j` - adapter module with no tests
- `ai-impl:ai-spring-ai` - adapter module with no tests
- All 5 integration modules (`afg-redis`, `afg-jdbc`, `afg-rabbitmq`, `afg-websocket`, `afg-storage`)
- All 3 governance modules (`proto`, `client`, `server`)

**Impact**: These modules have no automated verification. Changes could break them without detection.

#### 6. spring-boot-restclient not in version catalog

`ai-core/build.gradle.kts` line 70 uses `testImplementation("org.springframework.boot:spring-boot-restclient")` but this artifact is not defined in `gradle/libs.versions.toml`. It works because Spring Boot BOM manages the version, but it's inconsistent with the pattern of using `libs.xxx` references.

### Build Configuration Summary

| Setting | Value |
|---|---|
| Java version | 25 |
| Spring Boot version | 4.0.6 |
| Spring AI version | 2.0.0-M7 |
| Gradle version | 9.4.0 |
| JaCoCo version | 0.8.14 |
| PMD version | 7.23.0 |
| Kotlin version | 2.3.21 |
| gRPC version | 1.81.0 |
| Protobuf version | 3.25.8 |
| LangChain4j version | 1.15.1 |
| Redisson version | 4.3.1 |
| Coverage threshold | 60% global, 70% for data.* packages |

### Files Found

| File Path | Description |
|---|---|
| `settings.gradle.kts` | Module includes (23 modules) |
| `build.gradle.kts` | Root build config with plugins, PMD, JaCoCo, JMH |
| `gradle/libs.versions.toml` | Version catalog (189 lines) |
| `ai-core/build.gradle.kts` | Has duplicate dependency + hardcoded versions |
| `data-impl/data-jdbc/build.gradle.kts` | Has hardcoded PostgreSQL version |
| `gradle-plugin/build.gradle.kts` | Has hardcoded versions (expected) |
| `governance:proto/build.gradle.kts` | Uses deprecated multi-string notation |

## Caveats / Not Found

- JaCoCo CSV reports were not generated (only HTML reports exist). Coverage percentages were not extracted from HTML.
- The `jacocoCoverageCheck` task was not run; coverage threshold compliance is unverified.
- OWASP dependency-check was not run (requires NVD API key and network access).
- The 6 skipped AI tests require a running Ollama instance to execute.
- Test runtime was 36s total. Individual module test times were not profiled.
