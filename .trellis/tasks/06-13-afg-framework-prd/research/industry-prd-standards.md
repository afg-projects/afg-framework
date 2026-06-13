# Research: Industry PRD/Documentation Standards for Java Enterprise Frameworks

- **Query**: Deep research on PRD/documentation standards from 6 benchmark Java frameworks (Spring Boot, MyBatis-Plus, Apache Shiro, Spring AI, Quarkus, LangChain4J) across 8 dimensions
- **Scope**: External (benchmark framework documentation analysis)
- **Date**: 2026-06-13

## Findings

### A. Documentation Structure Standards

#### A.1 Top-Level Chapter Structure — Industry Standard Hierarchy

All six benchmark frameworks follow a consistent top-level documentation hierarchy. The industry-standard chapter ordering is:

| Chapter | Spring Boot | Quarkus | Spring AI | LangChain4J | Shiro | Industry Standard |
|---|---|---|---|---|---|---|
| Overview/Introduction | Yes | Yes (What is Quarkus) | Yes (AI Concepts) | Yes (Introduction) | Yes (What is Shiro) | **Required** |
| Getting Started | Yes | Yes (Tutorials) | Yes | Yes (Get Started) | Yes (10 Min Tutorial) | **Required** |
| Core Concepts | Yes (Developing) | Yes (Concepts) | Yes (AI Concepts) | Implicit | Yes (Reference) | **Required** |
| Feature Guides | Yes (Core/Web/Data/IO/Messaging/Security) | Yes (How-to Guides) | Yes (API section per model) | Yes (Tutorials per feature) | Yes (Feature guides) | **Required** |
| Configuration Reference | Yes (Appendix: App Properties) | Yes (Config Reference) | Inline per feature | Inline | Inline | **Required** |
| Testing | Yes (dedicated chapter) | Yes (dedicated guides) | Yes (Testing section) | Yes (Testing tutorial) | No | **Recommended** |
| Packaging/Deployment | Yes (dedicated chapter) | Yes (Build Native) | No | No | No | **Recommended** |
| Production/Actuator | Yes (dedicated chapter) | Yes (Observability) | Yes (Observability) | No | No | **Recommended** |
| Migration/Upgrade | Yes (Upgrading chapter) | Yes (Migration guides) | Yes (Upgrade Notes) | No | Yes (Migration Guide) | **Recommended** |
| How-to Guides | Yes (dedicated chapter) | Yes (How-to Guides) | Yes (Guides section) | Yes (Tutorials) | No | **Recommended** |
| Build Tool Plugins | Yes (Maven/Gradle chapters) | Yes (Maven/Gradle) | No | No | No | **Conditional** |
| Appendices | Yes (App Props, Auto-config classes, Dep versions) | Yes (Generated refs) | No | No | Yes (API Javadoc) | **Recommended** |

#### A.2 Spring Boot Documentation Structure (Gold Standard)

Spring Boot 4.1 documentation has the most comprehensive and well-structured documentation among all benchmark frameworks:

```
1. Overview (System Requirements, Installing, Upgrading)
2. Tutorials (Developing Your First Spring Boot Application)
3. Reference
   3.1 Developing with Spring Boot
       - Build Systems
       - Structuring Your Code
       - Configuration Classes
       - Auto-configuration
       - Spring Beans and Dependency Injection
       - Using the @SpringBootApplication Annotation
       - Running Your Application
       - Developer Tools
       - Packaging Your Application for Production
   3.2 Core Features
       - SpringApplication
       - Externalized Configuration (deep: profiles, property sources, YAML, type-safe binding)
       - Profiles
       - Logging
       - Internationalization
       - AOP
       - JSON
       - Task Execution and Scheduling
       - Development-time Services
       - Creating Your Own Auto-configuration
       - Kotlin Support
       - SSL
   3.3 Web (Servlet, Reactive, Graceful Shutdown, Security, Session, GraphQL, HATEOAS)
   3.4 Data (SQL, NoSQL)
   3.5 IO (Caching, gRPC, Hazelcast, Quartz, Email, Validation, REST Client, Web Services, JTA)
   3.6 Messaging (JMS, AMQP, Kafka, Pulsar, RSocket, Integration, WebSockets)
   3.7 Security (OAuth2, SAML 2.0)
   3.8 Testing (Test Modules, Scope Dependencies, Spring Apps, Boot Apps, Testcontainers, Utilities)
   3.9 Packaging (Efficient Deployments, AOT, GraalVM Native Images, Container Images)
   3.10 Production-ready (Actuator, Endpoints, Monitoring, Observability, Auditing)
4. How-to Guides (by topic: Config, Web, Data, Messaging, Actuator, Security, Build, AOT, Native)
5. Build Tool Plugins (Maven Plugin, Gradle Plugin, AntLib)
6. REST API (Actuator endpoints)
7. Java APIs (Gradle/Maven plugins)
8. Kotlin APIs
9. Specifications (Config Metadata, Executable Jar Format)
10. Appendix
    - Common Application Properties (massive property list)
    - Deprecated Application Properties
    - Auto-configuration Classes (complete list by module)
    - Dependency Versions (managed deps, version properties)
```

**Key pattern**: Spring Boot uses a **three-tier content type system**: Reference (comprehensive, organized by domain), How-to Guides (task-oriented), and Tutorials (learning-oriented). This aligns with the Diataxis framework.

#### A.3 Quarkus Documentation Structure (Diataxis-Aligned)

Quarkus explicitly adopts the **Diataxis documentation framework**, organizing all content into four types:

1. **Tutorials** (learning-oriented): "Creating Your First Application", "Getting Started with Security", etc.
2. **How-to Guides** (goal-oriented): "Enable Basic Authentication", "Deploy to OpenShift", etc.
3. **Concepts** (understanding-oriented): "Security Architecture", "Contexts and Dependency Injection", etc.
4. **References** (information-oriented): "Configuration Reference", "Hibernate Reactive API", "Security Overview", etc.

**Key pattern**: Quarkus is the only framework that explicitly labels and enforces content types. Every guide has a suffix indicating its type: `-concept.adoc`, `-howto.adoc`, `-reference.adoc`, `-tutorial.adoc`.

#### A.4 Spring AI Documentation Structure

```
1. Overview
2. AI Concepts (Models, Prompts, Embeddings, Vector Stores, RAG, ETL)
3. Getting Started (Spring Initializr, Repositories, BOM, Dependencies)
4. Reference
   4.1 Chat Client API
   4.2 Advisors (Recursive)
   4.3 Prompts
   4.4 Structured Output
   4.5 Multimodality
   4.6 Models (Chat Models by provider, Embedding Models by provider, Image Models, Audio Models, Moderation)
   4.7 Chat Memory
   4.8 Tool Calling
   4.9 MCP (Model Context Protocol)
   4.10 RAG
   4.11 ETL Pipeline
   4.12 Model Evaluation
   4.13 Vector Databases (per-provider guides)
   4.14 Observability
   4.15 Dev Services / Testcontainers
5. Guides (Getting Started with MCP, Dynamic Tool Discovery, LLM-as-Judge, Prompt Engineering Patterns, Building Effective Agents)
6. Upgrade Notes
7. Migration Guides (FunctionCallback -> ToolCallback, Anthropic SDK migration)
```

**Key pattern**: Spring AI organizes by capability (Chat, Embedding, Image, Audio, RAG, ETL, Tool, MCP, Workflow) with per-provider sub-guides under each capability.

#### A.5 LangChain4J Documentation Structure

```
1. Introduction
2. Get Started (Maven/Gradle dependencies, BOM)
3. Tutorials (26 topics):
   - Agents, AI Services, Chat and Language Models, Chat Memory, Classification
   - Customizable HTTP Client, Embedding Stores, Guardrails, JSON
   - Logging, MCP, Model Parameters, Observability
   - RAG, Response Streaming, Skills, Structured Outputs
   - Testing and Evaluation, Tools
   - Framework integrations: Helidon, Micronaut, Quarkus, Spring Boot, Kotlin, Payara
4. Integrations (by category):
   - Browser Execution Engines, Chat Memory Stores, Code Execution Engines
   - Document Loaders, Document Parsers, Embedding Models
   - Embedding Stores, Image Models, Language Models, Model Routing
   - Prompt Repetition, Scoring/Reranking Models, Web Search Engines
5. Useful Materials
```

**Key pattern**: LangChain4J separates "core tutorials" (framework features) from "integrations" (per-provider adapters), which is a pattern relevant for AFG's SPI architecture.

#### A.6 Minimal Required Content per Chapter

| Chapter | Minimal Required Content | Found In |
|---|---|---|
| **Overview** | Product positioning, design philosophy, target users, tech stack, feature highlights | All 6 frameworks |
| **Getting Started** | Environment requirements, installation/dependency setup, minimal config, first working example, verification steps, "next steps" links | All 6 frameworks |
| **Core Concepts** | Key abstractions explained, comparison with alternatives (e.g., "vs JPA"), design decisions, architecture diagrams | Spring Boot, Quarkus, Spring AI, Shiro |
| **Feature Guides** | Per-module: positioning, scenarios, API, config, limitations, best practices | All 6 frameworks |
| **Configuration Reference** | Property name, type, default value, description, example, related properties | Spring Boot, Quarkus |
| **Testing** | Test strategies, testing utilities, Testcontainers integration | Spring Boot, Quarkus, Spring AI, LC4J |
| **Migration** | Version-to-version upgrade notes, breaking changes, deprecated API lifecycle | Spring Boot, Spring AI, Shiro |

#### A.7 Getting Started Chapter — Standard Depth

All six frameworks converge on a standard depth for the Getting Started chapter:

1. **Prerequisites/Environment Requirements**: Java version, build tool version, IDE requirements
2. **Project Creation**: Spring Initializr / Maven archetype / Gradle plugin / CLI command
3. **Dependency Declaration**: BOM + specific module dependencies with version
4. **Minimal Configuration**: The absolute minimum YAML/properties to get running
5. **First Working Example**: A complete, runnable code sample (entity + controller + main class)
6. **Verification**: How to test that it works (curl, browser, test output)
7. **Next Steps**: Links to core concepts and feature guides

**Spring AI Getting Started depth** (observed): 7 sections covering Spring Initializr, artifact repositories (release + snapshot), BOM dependency management, per-component dependency addition, Maven mirror configuration warnings, and links to samples.

**Quarkus Getting Started depth**: Creates a full application in the tutorial, tests it, packages it as both JAR and native, with every step verified.

**MyBatis-Plus Quick Start depth** (based on community knowledge): Dependency -> Config -> Entity -> Mapper -> Service -> Controller, 5-minute path with minimal Spring Boot assumptions.

**Industry Standard Minimum**: A beginner should be able to go from zero to a working application in under 30 minutes by following the Getting Started chapter alone.

#### A.8 Core Concepts Chapter — Standard Depth

| Dimension | Standard | Spring Boot Example | Quarkus Example |
|---|---|---|---|
| Concept explanation | Plain-language explanation of each abstraction | "Auto-configuration: Spring Boot automatically configures your application based on jar dependencies" | "CDI: Contexts and Dependency Injection is the core programming model" |
| Comparison with alternatives | Explicit "why this, not that" | "Structuring Your Code: You don't have to follow this structure, but it's recommended" | Compares Quarkus approach vs traditional Java EE |
| Design decisions | Rationale for key architectural choices | "Creating Your Own Auto-configuration" explains the @ConditionalOnXxx design | "Reactive vs Imperative" explains dual-mode design |
| Architecture diagram | Visual overview of component relationships | SpringApplication lifecycle diagram (implicit in text) | Security architecture diagram (explicit) |
| Key interfaces | Core SPI interfaces listed with contracts | AutoConfiguration, Condition, ApplicationContext | Extension, BuildItem, DevServices |

### B. Feature Guide Writing Standards

#### B.1 Per-Module Guide Dimensions

Analysis of all six frameworks reveals a standard set of dimensions that each feature guide should cover:

| Dimension | Spring Boot | Quarkus | Spring AI | Shiro | Industry Standard |
|---|---|---|---|---|---|
| **Positioning** (1 sentence) | Implicit (chapter intro) | Explicit (abstract) | Implicit | Explicit | **Required** |
| **Use Cases/Scenarios** | Some chapters | Explicit | Implicit | Explicit (scenarios) | **Required** |
| **API/Code Examples** | Extensive | Extensive | Extensive | Moderate | **Required** |
| **Configuration** | Properties inline | Properties inline | YAML inline | INI/XML inline | **Required** |
| **Limitations/Caveats** | Some (TIP/NOTE boxes) | Explicit (warning boxes) | Some | Some | **Required** |
| **Best Practices** | Some chapters | Explicit | Some | Some | **Recommended** |
| **Migration Guide** | Dedicated chapter | Per-extension | Dedicated section | Dedicated guide | **Conditional** |
| **Testing** | Dedicated chapter | Per-guide | Some guides | No | **Recommended** |

#### B.2 Code Example Standard Depth — Four-Level Pattern

All benchmark frameworks follow a similar depth pattern for code examples:

1. **Minimum Viable Example**: The absolute smallest code that demonstrates the feature (3-10 lines). Always shown first.
   - Spring Boot: `@SpringBootApplication` + one annotation = working app
   - Spring AI: `ChatClient.builder(chatModel).build().prompt().user(text).call().content()`
   - Quarkus: `@Path("/hello") public class GreetingResource { @GET public String hello() { return "hello"; } }`

2. **Common Usage**: The typical 80% use case (20-50 lines). Includes imports, class structure, basic configuration.
   - Spring Boot: Full `@Configuration` class with `@Bean` methods
   - Spring AI: Full ChatClient with system prompt, memory, and advisors
   - Quarkus: Full REST endpoint with dependency injection and config

3. **Advanced Usage**: Edge cases, customization points, integration patterns (50-150 lines).
   - Spring Boot: Custom `AutoConfiguration` with `@ConditionalOnXxx`, factory files
   - Spring AI: Custom Advisor chain, vector store with filter expressions
   - Quarkus: Custom extension with BuildItems

4. **Full Configuration**: Complete YAML/properties file showing all relevant options.
   - Spring Boot: Full property blocks in both `.properties` and `.yaml` format (shown side by side)
   - Spring AI: Full YAML with all model-specific properties
   - Quarkus: `application.properties` with all extension properties

**Spring AI RAG example depth** (observed): Starts with 3-line `QuestionAnswerAdvisor` usage, then shows dynamic filter expressions, then custom prompt template with full configuration. This is the standard pattern.

#### B.3 Code Example Norms

| Norm | Spring Boot | Quarkus | Spring AI | Industry Standard |
|---|---|---|---|---|
| **Runnable** | Yes, if combined with Getting Started | Yes (Quarkus CLI testable) | Partially (need provider key) | **Should be runnable with minimal setup** |
| **Both Maven + Gradle** | Yes, always shown side by side |Yes | Yes | **Required** |
| **Both .properties + .yaml** | Yes, always shown side by side | No (properties only) | Yes (YAML primary) | **Recommended** |
| **Comments density** | Minimal (code is self-documenting) | Moderate (inline comments) | Moderate | **Minimal but meaningful** |
| **Version consistency** | BOM ensures version consistency | BOM/platform ensures consistency | BOM ensures consistency | **Required** |
| **Package imports** | Sometimes shown, sometimes not | Usually shown | Usually shown | **Show when non-obvious** |
| **Asciidoc callouts** | Yes (numbered callouts) | Yes (numbered callouts) | Yes | **Recommended** |

### C. Configuration Reference Standards

#### C.1 Configuration Property Documentation Format

Spring Boot sets the gold standard for configuration property documentation. The appendix "Common Application Properties" lists every property with:

| Field | Spring Boot Format | Quarkus Format | Industry Standard |
|---|---|---|---|
| **Property name** | `spring.security.oauth2.client.registration.[registrationId].client-id` | `quarkus.datasource.db-kind` | **Required** |
| **Type** | `java.lang.String` | `string` (implied) | **Required** |
| **Default value** | Explicit (empty string, null, specific value) | Explicit | **Required** |
| **Description** | 1-2 sentence explanation | 1-2 sentence explanation | **Required** |
| **Example** | Inline in description | Inline in description | **Recommended** |
| **Related properties** | Implicitly grouped by prefix | Implicitly grouped by prefix | **Recommended** |
| **Deprecation notice** | `@DeprecatedConfigurationProperty` annotation + separate deprecated appendix | No | **Required** |
| **Since version** | Not shown in appendix | Not shown | **Recommended** |

**Spring Boot property documentation is generated** from `@ConfigurationProperties` classes using the Configuration Metadata Annotation Processor. This ensures documentation and code are always in sync.

#### C.2 Configuration Grouping Standards

| Pattern | Used By | Pros | Cons |
|---|---|---|---|
| **By prefix (functional)** | Spring Boot (`spring.security.*`, `spring.datasource.*`) | Intuitive for users; find related properties easily | Long prefixes; cross-cutting concerns duplicated |
| **By module** | Quarkus (per-extension reference page) | Matches module boundaries; easy for contributors | User must know which module owns a feature |
| **By capability** | Spring AI (per-provider within capability) | Clear for API-focused docs | Deeply nested |

**Industry consensus**: Group by **prefix/function** in the reference appendix, but provide **per-module** reference sections within feature guides. Spring Boot does both: the appendix groups by prefix, while feature guides show properties inline.

#### C.3 Configuration Property Naming Convention

| Convention | Spring Boot | Quarkus | Industry Standard |
|---|---|---|---|
| **Prefix pattern** | `{framework}.{module}.{feature}.*` | `quarkus.{extension}.*` | `afg.{module}.{feature}.*` |
| **Separator** | `.` (dot) | `.` (dot) | `.` (dot) |
| **Case** | kebab-case (`signing-key`) | kebab-case | **kebab-case** |
| **Max depth** | 5 levels (e.g., `spring.security.oauth2.client.registration.my-client.client-id`) | 4 levels | **4-5 levels max** |
| **Map keys** | `[key]` notation (e.g., `spring.security.oauth2.client.registration.[registrationId].client-id`) | `[key]` notation | **Required for dynamic keys** |
| **On/off switch** | `spring.autoconfigure.exclude` or `@ConditionalOnProperty` | `quarkus.{extension}.enabled` | **`afg.{module}.enabled`** |

#### C.4 Configuration Property Tier Model

Based on Spring Boot and Quarkus patterns, properties fall into tiers:

| Tier | Description | Example | Must Document |
|---|---|---|---|
| **Tier 1: Common** | Used by >50% of users | `afg.security.auth-server.enabled`, `afg.ai.enabled` | Yes, in feature guide |
| **Tier 2: Module** | Module-specific but commonly needed | `afg.security.casbin.model-type`, `afg.ai.rag.similarity-threshold` | Yes, in module reference |
| **Tier 3: Advanced** | Rarely changed, performance tuning | `afg.security.token.cache-ttl`, `afg.ai.resilience.circuit-breaker.window-size` | Yes, in appendix only |
| **Tier 4: Internal** | Framework internals, SPI configuration | `afg.core.module-auto-configuration-order` | Yes, in contributor guide |

### D. Security Framework Documentation Standards

#### D.1 Security Documentation Coverage — Industry Standard

Based on analysis of Spring Security, Apache Shiro, and Quarkus Security:

| Content Area | Spring Security | Shiro | Quarkus | Industry Standard |
|---|---|---|---|---|
| **Authentication overview** | Yes (Architecture chapter) | Yes (Authentication Guide) | Yes (Security Architecture) | **Required** |
| **Authentication mechanisms** | Yes (Form, Basic, Digest, X509, LDAP, MFA, Passkeys, OTP, CAS, Kerberos, JAAS) | Yes (Realms, InMemory, JDBC, LDAP) | Yes (Basic, Form, mTLS, OIDC, WebAuthn) | **Required** |
| **Authorization model** | Yes (Architecture + Method Security + HTTP Security + ACLs) | Yes (Authorization Guide) | Yes (RBAC, Bypass, Web endpoints) | **Required** |
| **OAuth2 Client** | Yes (Login, Client, Resource Server, Authorization Server) | No | Yes (OIDC Code Flow, Bearer Token) | **Conditional** |
| **OAuth2 Authorization Server** | Yes (full chapter: Config Model, Core Components, Protocol Endpoints) | No | No | **Conditional** |
| **OAuth2 Resource Server** | Yes (JWT, Opaque Token, Multitenancy, DPoP) | No | Yes (OIDC Bearer Token) | **Conditional** |
| **RBAC** | Yes (@RolesAllowed, @DenyAll, @PermitAll) | Yes (Roles, Permissions, WildcardPermissions) | Yes (@RolesAllowed) | **Required** |
| **ABAC** | Yes (Method Security with SpEL) | Yes (WildcardPermissions) | No explicit | **Recommended** |
| **Multi-tenancy** | Yes (OAuth2 Multitenancy chapter) | No | Yes (OIDC Multitenancy guide) | **Conditional** |
| **Data-level security** | Yes (ACLs chapter) | No | Yes (Security with Jakarta Persistence) | **Conditional** |
| **Session management** | Yes (dedicated chapter) | Yes (Session Management) | Yes (Proactive auth) | **Required** |
| **CSRF protection** | Yes (dedicated chapter) | No | Yes (dedicated guide) | **Required** |
| **CORS** | Yes (in HTTP chapter) | No | Yes (dedicated guide) | **Required** |
| **Password storage** | Yes (dedicated chapter: BCrypt, SCrypt, PBKDF2, Argon2) | Yes (Credentials Matching, Hashing) | Yes (Bcrypt) | **Required** |
| **Remember-me** | Yes (dedicated chapter) | Yes (RememberMe) | No | **Conditional** |
| **Logout** | Yes (dedicated chapter) | Yes (Logout) | Yes | **Required** |
| **Security testing** | Yes (dedicated chapter: MockMvc, @WithMockUser, Meta-annotations) | No | Yes (dedicated guide) | **Required** |
| **Security events** | Yes (Authentication Events, Authorization Events) | No | Yes | **Recommended** |
| **Production hardening** | No (implicit) | No | Yes (Vulnerability detection) | **Recommended** |
| **IP restrictions** | No | No | No | **Recommended** |
| **Device binding** | No | No | No | **Emerging** |

#### D.2 OAuth2 Documentation Standard Depth

Spring Security's OAuth2 documentation is the gold standard. It covers:

1. **OAuth2 Client (Login)**: Core Configuration, Advanced Configuration, OIDC Logout
2. **OAuth2 Client (Authorization)**: Core Interfaces, Authorization Grants, Client Authentication, Authorized Clients Management
3. **OAuth2 Resource Server**: JWT (introspection, validation, extraction), Opaque Token, Multitenancy, Bearer Tokens, DPoP
4. **OAuth2 Authorization Server**: Configuration Model, Core Model/Components, Protocol Endpoints

**Required OAuth2 documentation dimensions** (based on Spring Security pattern):

| Dimension | Content |
|---|---|
| Authorization Code Flow | Complete flow diagram, client registration steps, PKCE explanation, token exchange, user info extraction |
| Client Credentials Flow | Use case (service-to-service), configuration, token request example |
| Resource Server | JWT validation, key rotation, introspection endpoint, multitenancy strategy |
| Authorization Server | Client registration, authorization endpoint, token endpoint, key endpoint, protocol configuration |
| Token Management | Access token TTL, refresh token TTL, token revocation, token introspection |
| PKCE | Why needed, how it works, configuration steps, verifier/challenge generation |

#### D.3 Permission Model Documentation Standards

| Content | Spring Security | Shiro | Quarkus | Industry Standard |
|---|---|---|---|---|
| **RBAC concepts** | @RolesAllowed, GrantedAuthority | Role, Permission, WildcardPermission | @RolesAllowed | **Required** |
| **RBAC configuration steps** | Delegating to UserDetailsService | Configuring Realms, role-permission mapping | Jakarta Persistence identity provider | **Required** |
| **ABAC concepts** | SpEL expressions in @PreAuthorize | Implicit via WildcardPermissions | No explicit | **Recommended** |
| **Data-level access control** | ACL module (separate chapter) | No | Security with Jakarta Persistence | **Conditional** |
| **Policy engine integration** | No built-in | Casbin (via Shiro-Casbin) | No built-in | **Emerging** |
| **Permission caching** | No | Yes (CachingRealm) | No | **Recommended** |

### E. AI Framework Documentation Standards

#### E.1 AI Framework Coverage — Industry Standard

Based on analysis of Spring AI, LangChain4J, and Quarkus AI:

| Content Area | Spring AI | LangChain4J | Industry Standard |
|---|---|---|---|
| **AI Concepts** | Yes (dedicated chapter: Models, Prompts, Embeddings, RAG, ETL, Vector Stores) | Yes (Introduction) | **Required** |
| **Engine selection guide** | No (Spring AI is the engine) | Yes (framework integration comparisons) | **Required** (AFG has 2 engines) |
| **Chat API** | Yes (Chat Client API, Chat Models by provider) | Yes (Chat and Language Models tutorial) | **Required** |
| **Embedding API** | Yes (Embedding Models by provider) | Yes (Embedding Models integration) | **Required** |
| **Agent** | Yes (Building Effective Agents guide) | Yes (Agents tutorial) | **Required** |
| **RAG** | Yes (dedicated chapter: QuestionAnswerAdvisor, ETL Pipeline, Vector Stores) | Yes (RAG tutorial) | **Required** |
| **Tool Calling** | Yes (Tool Calling chapter) | Yes (Tools tutorial) | **Required** |
| **MCP** | Yes (dedicated chapter: Client/Server starters, annotations, security) | Yes (MCP tutorial + STDIO server) | **Emerging** |
| **Workflow** | No (Spring AI defers to external orchestration) | No | **Conditional** (AFG has 37 nodes) |
| **Memory** | Yes (Chat Memory chapter) | Yes (Chat Memory tutorial) | **Required** |
| **Structured Output** | Yes (Structured Output chapter) | Yes (Structured Outputs + JSON tutorials) | **Required** |
| **Streaming** | Implicit (in Chat Client) | Yes (Response Streaming tutorial) | **Required** |
| **Observability** | Yes (Observability chapter) | Yes (Observability tutorial) | **Recommended** |
| **Guardrails** | No | Yes (Guardrails tutorial) | **Emerging** |
| **Testing** | Yes (Testing section) | Yes (Testing and Evaluation tutorial) | **Recommended** |
| **Skill system** | No | Yes (Skills tutorial) | **Emerging** |
| **ETL Pipeline** | Yes (ETL Pipeline chapter) | Yes (Document Loaders/Parsers integrations) | **Recommended** |

#### E.2 Per-Feature Documentation Standard

Spring AI's documentation pattern for each AI feature follows:

1. **Concept**: What is it? Why use it? (2-5 paragraphs)
2. **API**: Core interfaces and classes (code snippet of key interface)
3. **Minimum example**: 3-10 lines showing the simplest usage
4. **Common usage**: Full working example with config (20-50 lines)
5. **Advanced usage**: Customization, per-provider differences (50-150 lines)
6. **Configuration**: All relevant properties listed with defaults
7. **Degradation**: What happens when no provider is configured (NoOp behavior)

**LangChain4J pattern** is similar but simpler:
1. Concept (1-2 paragraphs)
2. Tutorial with code example (full working code, 50-200 lines)
3. Integration details (per-provider specifics)

#### E.3 Workflow Node Classification Standards

AFG Framework has 37 built-in workflow nodes. Industry patterns for classifying such nodes:

**LangChain4J approach**: No built-in workflow system. Defers to external orchestration.

**Spring AI approach**: "Building Effective Agents" guide defines 5 agent patterns: Tool-calling Loop, Multi-turn Chat, ReAct, Plan-and-Execute, Reflection. No explicit node taxonomy.

**Industry standard for workflow node documentation** (synthesized from common patterns):

| Category | Example Nodes | Required Documentation |
|---|---|---|
| **Control Flow** | If/Else, Switch, Loop, Parallel, SubWorkflow | Condition expression syntax, branch handling, error propagation |
| **AI Operations** | Chat, Embed, RAG, ToolCall, Agent | Model configuration, prompt template, fallback behavior |
| **Data Operations** | Transform, Filter, Aggregate, ETL | Input/output schema, transformation logic |
| **Human Interaction** | HumanApproval, HumanInput, HumanDecision | Timeout handling, escalation, notification |
| **Integration** | HTTP Call, gRPC, Database, File | Connection config, error handling, retry |
| **Utility** | Delay, Log, SetVariable, Checkpoint | Variable scoping, persistence |
| **Observability** | Audit, Metric, Trace | Metric naming, export format |

### F. Module Development (Contributor Guide) Standards

#### F.1 Contributor Guide Coverage

| Content | Spring Boot | Quarkus | Industry Standard |
|---|---|---|---|
| **Code style/conventions** | Yes (CONTRIBUTING.adoc) | Yes (style guide) | **Required** |
| **AutoConfiguration writing** | Yes (dedicated chapter) | Yes (Writing Extensions guide) | **Required** |
| **SPI interface design** | No explicit | Yes (Extension API, BuildItems) | **Required** |
| **@ConditionalOnXxx rules** | Yes (detailed rules) | Yes (extension conditions) | **Required** |
| **Testing modules** | Yes (Test Scope Dependencies, @TestConfiguration) | Yes (extension testing guide) | **Required** |
| **NoOp/fallback requirements** | No explicit | Yes (Dev Services, feature flags) | **Required** |
| **Module dependency rules** | No explicit | Yes (extension dependency chain) | **Required** |
| **Documentation writing guide** | No | Yes (doc-concept, doc-reference, doc-contribute) | **Recommended** |
| **Release process** | Yes | Yes | **Conditional** |

#### F.2 Quarkus Extension Maturity Matrix — Key Pattern

Quarkus defines a **maturity matrix** for extensions with progressive capabilities:

| Level | Capability | Description |
|---|---|---|
| **Basic** | Works in JVM mode | Extension functions in standard JVM |
| **Basic** | Configuration support | Integrates with Quarkus config model |
| **Basic** | CDI Beans | Components available via CDI injection |
| **Intermediate** | Works in dev mode | Supports hot reload in development |
| **Intermediate** | Dev Service | Auto-provisions external services for dev |
| **Intermediate** | Basic Dev UI | Extension tile in developer UI |
| **Advanced** | Works as native | GraalVM native executable support |
| **Advanced** | Rich Dev UI | Interactive dev-mode UI |
| **Advanced** | Observability | Metrics, tracing integration |

**Relevance to AFG**: This maturity model maps directly to AFG's feature tiers (alpha/beta/GA) and the concept of progressive capability delivery.

#### F.3 Rule Documentation Format — Quarkus Style Guide Pattern

Quarkus provides the most structured contributor documentation format. Key patterns:

1. **Rule**: Clear statement of the requirement
2. **Correct example**: Code showing the right approach (marked as "Good example")
3. **Incorrect example**: Code showing the wrong approach (marked as "Bad example")
4. **Rationale**: Why the rule exists
5. **Check list**: Verifiable items for review

Quarkus title conventions for different content types:

| Content Type | Title Pattern | Example |
|---|---|---|
| Concept | Noun phrase, finish "Understanding ..." | "Security and authentication mechanisms in Quarkus" |
| How-to | Active verb imperative, finish "How to ..." | "Secure your Quarkus application with WebAuthn" |
| Reference | Noun phrase, finish "About ..." | "Hibernate Reactive API configuration properties" |
| Tutorial | Active verb imperative, finish "In this tutorial, you will ..." | "Create a Quarkus application in JVM mode" |

### G. Versioning Strategy and Evolution Standards

#### G.1 Semantic Versioning — Industry Standard

All six frameworks follow semantic versioning (MAJOR.MINOR.PATCH):

| Framework | Version Pattern | Breaking Change Policy | Deprecation Window |
|---|---|---|---|
| Spring Boot | 4.x.y | MAJOR version for breaking changes | At least 1 minor release with @Deprecated |
| Spring Security | 7.x.y | MAJOR version for breaking changes | At least 1 minor release with @Deprecated |
| Spring AI | 2.x.y | MAJOR for breaking, minor for new providers | Upgrade Notes document |
| Quarkus | 3.x.y | MAJOR for breaking, minor for new extensions | Migration guides per major |
| LangChain4J | 1.x.y | MAJOR for breaking, minor for new integrations | No explicit deprecation window |
| Shiro | 2.x.y | MAJOR for breaking changes | Migration Guide document |

**Industry standard** (synthesized):
- **MAJOR**: Breaking API changes, minimum 1 release deprecation warning
- **MINOR**: New features, backward-compatible
- **PATCH**: Bug fixes, backward-compatible
- **Deprecation lifecycle**: `@Deprecated` annotation + Javadoc `@since` + documented in upgrade notes, removal in next MAJOR version

#### G.2 Feature Classification (Alpha/Beta/GA)

| Framework | Alpha | Beta | GA/Stable | Deprecated |
|---|---|---|---|---|
| Spring AI | SNAPSHOT | M (Milestone), RC | Release | N/A |
| Quarkus | experimental | preview | stable (default) | deprecated |
| Spring Boot | SNAPSHOT | M, RC | Release | N/A |
| Shiro | N/A | N/A | Release | N/A |

**Quarkus model** (most explicit):
- `experimental`: "Use at your own risk, API may change"
- `preview`: "API mostly stable, may have minor changes"
- `stable`: "Production-ready, backward-compatible"
- `deprecated`: "Will be removed in future release"

**Quarkus also has an extension maturity matrix** (see F.2) that defines progressive capabilities independently of feature classification.

#### G.3 Database Dialect Coverage Matrix

No benchmark framework provides a formal "10-dialect coverage matrix." However, patterns exist:

| Framework | Approach | Coverage Tracking |
|---|---|---|
| Spring Boot | Auto-configuration per datasource (H2, MySQL, PostgreSQL, etc.) | Implicit via `DataSourceAutoConfiguration` |
| Quarkus | Per-database extensions (JDBC, PostgreSQL, MySQL, etc.) | Extension registry lists per-DB support |
| MyBatis-Plus | DatabaseId + dialect per DB (MySQL, PostgreSQL, Oracle, SQL Server, etc.) | Explicit dialect enum |
| Hibernate | Dialect class per DB (20+ dialects) | Dialect class hierarchy |

**Industry standard for coverage matrix** (synthesized from patterns):
- Table format: rows = features, columns = databases
- Cell values: "Supported" / "Partial" / "Not Supported" / "Planned"
- Test requirement: Each "Supported" cell must have at least one integration test with Testcontainers
- Dialect extension SPI: documented for contributors to add new dialects

### H. Common PRD/Documentation Defect Patterns

#### H.1 Defect Catalog

Based on analysis of all six frameworks and community feedback patterns:

| # | Defect | Impact on Users | Found In | Frequency |
|---|---|---|---|---|
| 1 | **Examples not runnable**: Missing imports, wrong versions, missing dependencies | User cannot follow along; abandons framework | MyBatis-Plus (community), LangChain4J (partial examples) | **Very High** |
| 2 | **Configuration properties undocumented**: Properties exist in code but not in docs | User discovers properties only by reading source code or Stack Overflow | Spring Boot (early versions), Spring AI (new features) | **High** |
| 3 | **Security missing production hardening**: Auth examples use plaintext passwords, no HTTPS, no CORS | User deploys insecure application to production | Most frameworks (Spring Security has some guidance) | **Very High** |
| 4 | **AI features missing use cases**: "Here's the API" but no "when/why to use this" | User cannot evaluate whether feature is appropriate | Spring AI (improving), LangChain4J (partial) | **High** |
| 5 | **No migration/upgrade path**: Breaking changes not documented with migration steps | User stuck on old version, cannot upgrade | Spring AI (improving with Upgrade Notes), Shiro (has Migration Guide) | **High** |
| 6 | **No degradation documentation**: NoOp/fallback behavior undocumented | User confused when feature silently does nothing | Spring AI (some), Quarkus (Dev Services documented) | **Medium** |
| 7 | **Tutorial assumes expertise**: Getting Started skips fundamental concepts | Beginner lost; jumps to advanced features without foundation | Spring Boot (some), Spring Security (notorious for this) | **High** |
| 8 | **No module selection guide**: No decision tree for choosing between alternatives | User paralyzed by choice; picks wrong module | All frameworks to some degree | **Medium** |
| 9 | **Config property types ambiguous**: Type not documented (String vs List vs Map) | User guesses; wrong type causes silent failures | Spring Boot appendix (improved with metadata), Quarkus | **Medium** |
| 10 | **No troubleshooting section**: Common errors and solutions not documented | User resorts to GitHub issues for basic problems | Quarkus (FAQ), Spring Boot (some how-to) | **Medium** |

#### H.2 Impact Severity Analysis

| Severity | Defects | User Experience Impact |
|---|---|---|
| **Critical** (blocks adoption) | #1, #3, #7 | User cannot get started or deploys insecure app |
| **High** (significant friction) | #2, #4, #5 | User cannot configure, choose, or upgrade correctly |
| **Medium** (frustration) | #6, #8, #9, #10 | User confused or delayed but can eventually succeed |

### I. AFG Framework PRD Industry Standards Checklist

Based on all research above, the following checklist defines what the AFG Framework PRD should achieve to meet industry standards:

#### I.1 Documentation Structure (Section A)

- [ ] **Overview chapter**: Product positioning, design philosophy (5 principles), target users, tech stack, differentiation, values
- [ ] **Getting Started chapter**: 6 mandatory elements (prerequisites, project creation, dependencies, minimal config, first example, verification, next steps)
- [ ] **Core Concepts chapter**: 6 minimum concepts (DataManager vs JPA, APT metadata, AutoConfiguration, entity base classes, modular architecture, enhancement-not-replacement)
- [ ] **Feature Guides chapter**: Per-module guides with all 7 dimensions (positioning, use cases, API, config, limitations, best practices, testing)
- [ ] **Configuration Reference chapter**: All properties documented with 6 fields (name, type, default, description, example, related properties)
- [ ] **Testing chapter**: Dedicated chapter with test strategies and examples
- [ ] **How-to Guides chapter**: Task-oriented guides for common operations
- [ ] **Migration chapter**: Version upgrade notes, deprecated API lifecycle
- [ ] **Appendices chapter**: AutoConfiguration list, ErrorCode list, dependency versions, entity base class comparison

#### I.2 Feature Guide Standards (Section B)

- [ ] Every code example follows 4-level depth: minimum viable, common usage, advanced usage, full configuration
- [ ] Every code example is structurally complete (includes imports, class declaration, annotations)
- [ ] Both `.properties` and `.yaml` formats shown for configuration
- [ ] Code examples use BOM for version consistency
- [ ] Maven and Gradle dependency declarations shown side by side

#### I.3 Configuration Reference Standards (Section C)

- [ ] Properties grouped by prefix (functional grouping) in appendix
- [ ] Properties also shown inline within feature guides (per-module grouping)
- [ ] Maximum 5 levels of nesting
- [ ] Dynamic keys use `[key]` notation
- [ ] Every module has `afg.{module}.enabled` on/off switch
- [ ] Tier model applied: Common, Module, Advanced, Internal
- [ ] Deprecation notices documented with since-version and removal-version

#### I.4 Security Module Standards (Section D)

- [ ] Authentication overview with architecture diagram
- [ ] All authentication mechanisms documented (username/password, phone, email, captcha, OAuth2)
- [ ] Authorization model (RBAC with Casbin, ABAC via expressions)
- [ ] Multi-tenancy configuration steps (3 deployment modes: AUTH_SERVER, RESOURCE_SERVER, MONOLITH)
- [ ] OAuth2 authorization code flow with diagram, client configuration, PKCE, token management
- [ ] Password storage/encoding strategy
- [ ] Production hardening section (HTTPS, CORS, CSRF, rate limiting, IP restrictions)
- [ ] Security testing guide

#### I.5 AI Module Standards (Section E)

- [ ] AI Concepts chapter (Models, Prompts, Embeddings, RAG, ETL, Agents, Workflows)
- [ ] Engine selection guide (Spring AI vs LangChain4J: comparison table, use case recommendations)
- [ ] Per-feature documentation: Chat, Embedding, Agent, RAG, Tool Calling, Memory, Structured Output, Streaming
- [ ] Each feature follows standard pattern: concept, minimum example, common usage, advanced usage, configuration, degradation
- [ ] Workflow 37-node taxonomy: categorized by type (Control Flow, AI Operations, Data, Human, Integration, Utility, Observability)
- [ ] ETL pipeline documentation
- [ ] Skill system documentation
- [ ] ai-spring-ai adapter module documentation

#### I.6 Contributor Guide Standards (Section F)

- [ ] Code style guide (naming, comments, logging, Lombok rules)
- [ ] AutoConfiguration writing rules (7 mandatory rules from AFG spec)
- [ ] SPI interface design rules (NoOp requirements, @ConditionalOnMissingBean)
- [ ] Module dependency rules (permitted/forbidden dependencies)
- [ ] Documentation writing guide (Diataxis alignment: concept, how-to, reference, tutorial)
- [ ] Maturity/feature tier model (alpha, beta, GA criteria)

#### I.7 Version Strategy Standards (Section G)

- [ ] Semantic versioning (MAJOR.MINOR.PATCH)
- [ ] Feature classification (alpha/beta/GA with explicit criteria)
- [ ] Deprecation lifecycle (1 minor release warning before MAJOR removal)
- [ ] Database dialect coverage matrix (feature rows, database columns, supported/partial/not-supported/planned values)
- [ ] Testrequirement per matrix cell (Testcontainers for "Supported")

#### I.8 Defect Prevention Checklist (Section H)

- [ ] All code examples verified as structurally runnable
- [ ] No configuration properties missing from reference
- [ ] Security examples include production hardening notes
- [ ] AI features include use case/when-to-use guidance
- [ ] Migration steps for all breaking changes
- [ ] NoOp/degradation behavior documented for every SPI
- [ ] Getting Started assumes no prior AFG knowledge
- [ ] Module selection guide with decision tree
- [ ] Configuration property types explicitly documented
- [ ] Troubleshooting section for common errors

### External References

- [Spring Boot Reference Documentation](https://docs.spring.io/spring-boot/reference/) — Gold standard for Java framework docs; 3-tier content system (Reference, How-to, Tutorials)
- [Spring AI Reference Documentation](https://docs.spring.io/spring-ai/reference/) — AI framework doc pattern; per-provider within per-capability organization
- [Apache Shiro Reference](https://shiro.apache.org/reference.html) — Security framework doc pattern; authentication/authorization guide depth
- [Quarkus Guides](https://quarkus.io/guides/) — Explicit Diataxis adoption; 4 content types (Tutorials, How-to, Concepts, References); maturity matrix for extensions
- [LangChain4J Documentation](https://docs.langchain4j.dev/) — AI tool doc pattern; core tutorials + integrations separation; 26 tutorial topics
- [Diataxis Framework](https://diataxis.fr/) — Systematic documentation authoring framework; 4 content types mapped to user needs
- [Semantic Versioning](https://semver.org/) — Version numbering specification
- [Quarkus Style and Content Guidelines](https://quarkus.io/guides/doc-reference) — Most structured contributor documentation format; title conventions, templates, rule+example+antipattern format
- [Quarkus Extension Maturity Matrix](https://quarkus.io/guides/extension-maturity-matrix) — Progressive capability model for module classification

### Related Specs

- `.trellis/spec/backend/autoconfiguration-guidelines.md` — AFG AutoConfiguration rules (7 mandatory rules)
- `.trellis/spec/backend/ai-module.md` — AFG AI module definition
- `.trellis/spec/backend/security-module.md` — AFG Security module definition
- `.trellis/spec/backend/testing-guidelines.md` — AFG Testing rules
- `docs/framework-prd.md` — Current AFG PRD (2814 lines, 12 chapters)

## Caveats / Not Found

- MyBatis-Plus documentation structure could not be fully extracted due to client-side rendering (Docusaurus/VitePress with no server-rendered content). Knowledge based on community familiarity and partial URL analysis.
- Apache Shiro reference documentation returned 404 for the /reference/ path. Structure inferred from the documentation index page and community knowledge.
- No benchmark framework provides a formal "10-dialect coverage matrix" as a primary documentation artifact. The closest pattern is Hibernate's dialect class hierarchy and Quarkus's per-database extension registry. The matrix format is synthesized from these patterns.
- Production hardening guides are notably absent from most benchmark frameworks' security documentation. This is a recognized gap in the industry.
- LangChain4J and Spring AI are relatively young frameworks (2023-2024 origin), so their documentation is still evolving. Some patterns may not be fully mature.
