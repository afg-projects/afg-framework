# AI 核心模块规格

> PRD 来源：§5.6 AI 核心模块（ai-core + ai-spring-ai + ai-langchain4j）
> CLAUDE.md 来源：AI 核心模块章节

## 1. 定位

AI 是一等公民——Chat/Agent/Workflow/RAG/Tool 全链路 AI 能力，双引擎适配（Spring AI + LangChain4J），声明式注解驱动，韧性+安全+审计内置。

## 2. 模块结构

```
ai-core/                 # AI 核心：接口 + 通用逻辑 + 本地默认实现 + 自动配置 + 注解 + AOP + 实体 + 迁移
ai-impl/
├── ai-spring-ai/        # Spring AI 适配
└── ai-langchain4j/      # LangChain4J 适配
```

### 2.1 依赖链

```
core → ai-core → ai-spring-ai / ai-langchain4j
```

## 3. 双引擎架构

AI 核心模块提供双引擎适配，两个引擎可以同时使用。

### 3.1 引擎选择指南

| 维度 | Spring AI | LangChain4J |
|------|-----------|-------------|
| **生态** | Spring 官方，与 Spring Boot 深度集成 | 独立生态，模型支持更广 |
| **模型支持** | OpenAI / Anthropic / Ollama / Vertex AI | 30+ 模型提供商（含国内通义/智谱/百川等） |
| **RAG** | VectorStore + Advisor | EmbeddingStore + ContentRetriever |
| **Agent** | ChatClient + Advisor 链 | AiServices + @Tool |
| **流式** | Flux\<String\> 原生支持 | Token 流支持 |
| **国内模型** | 有限 | 丰富（通义千问、智谱、百川、月之暗面等） |
| **推荐场景** | 已深度使用 Spring 生态 | 需要国内模型 / 更多模型选择 |

### 3.2 引擎指定方式

通过 `@AiChat(client = "spring-ai")` 或 `@AiChat(client = "langchain4j")` 指定使用哪个引擎。

### 3.3 SPI 适配模块

| 模块 | 适配内容 |
|------|----------|
| `ai-spring-ai` | `SpringAiChatClient`, `SpringAiEmbeddingClient`, Advisor, ChatMemory, Observation, Rag |
| `ai-langchain4j` | `Lc4jChatClient`, `Lc4jEmbeddingClient`, ToolAdapter, ChatMemory, Observation, Advisor |

## 4. API 接口层

包路径：`io.github.afgprojects.framework.ai.core.api`

| 子包 | 核心接口 |
|------|----------|
| `agent` | `Agent`, `AgentExecutor`, `AgentRequest`, `AgentResponse` |
| `chat` | `AfgChatClient`, `AfgEmbeddingClient`, `ChatClientRegistry`, `EmbeddingClientRegistry` |
| `etl` | `EtlPipeline`, `EtlPipelineBuilder`, `Source`, `Transformer` |
| `memory` | `ConversationMemory` |
| `model` | `ModelInfo`, `ModelRegistry`, `ModelType` |
| `multiagent` | `Coordinator`, `Orchestrator`, `AgentWorkflow` |
| `multiagent.communication` | `AgentMessage`, `CommunicationBus` |
| `multiagent.decomposition` | `TaskDecomposer`, `SubTask` |
| `multiagent.human` | `HumanInteraction`, `HumanDecision` |
| `multiagent.node` | `AgentNode`, `HumanNode`, `ParallelNode`, `RouterNode` |
| `observability` | `AuditLogger`, `MetricsCollector`, `Tracer` |
| `performance` | `Cache`, `RateLimiter` |
| `persistence` | `SessionStore`, `MessageHistoryStore` |
| `pipeline` | `ChatPipeline`, `ApplicationConfig`, `SearchMode` |
| `planning` | `ReActExecutor`, `PlanExecuteExecutor`, `ReflectionExecutor` |
| `rag` | `VectorStore`, `EmbeddingService`, `KnowledgeBaseService` |
| `resilience` | `CircuitBreaker`, `RetryPolicy`, `ResilienceExecutor` |
| `security` | `ApiKeyManager`, `ContentSafetyChecker`, `PiiDetector` |
| `tool` | `Tool`, `SecureTool`, `ToolRegistry` |
| `tool.remote` | `ToolDiscoveryClient`, `ToolEndpoint` |
| `workflow` | `DagEngine`, `WorkflowNode`, `ExecutionContext` |
| `workflow.checkpoint` | `CheckpointManager` |
| `workflow.definition` | `WorkflowDefinition`, `NodeDefinition`, `EdgeDefinition` |
| `workflow.dsl` | `DslConverter`, `DslValidator`, `VariableResolver` |

## 5. 功能实现层

包路径：`io.github.afgprojects.framework.ai.core`

包含子包：`chat/`, `agent/`, `model/`, `tool/`, `skill/`, `rag/`, `workflow/`, `pipeline/`, `persistence/`, `resilience/`, `performance/`, `security/`, `observability/`, `etl/`

## 6. 注解

| 注解 | 目标 | 功能 | 关键属性 | 默认值 |
|------|------|------|----------|--------|
| `@AiChat` | METHOD | 声明式 AI 对话 | `client`, `systemPrompt`, `memoryKey`, `temperature`, `maxTokens`, `streaming` | `streaming = false` |
| `@AiAgent` | METHOD | 声明式 Agent 执行 | `value`, `maxIterations`, `timeoutMs`, `chatClient` | `maxIterations = 10`, `timeoutMs = 30000` |
| `@ModelRoute` | METHOD | 模型路由选择 | — | — |
| `@Workflow` | METHOD | 声明式工作流执行 | `value`（工作流 ID）, `async` | — |
| `@AiResilient` | METHOD | AI 调用韧性 | `retry`, `retryIntervalMs`, `circuitBreaker`, `fallbackMethod` | `retry = 3`, `retryIntervalMs = 1000` |
| `@AiRateLimited` | METHOD | AI 接口限流 | `key`, `permitsPerSecond`, `timeoutMs` | `permitsPerSecond = 10` |
| `@ContentSafety` | METHOD | 内容安全检查 | `checkInput`, `checkOutput`, `block` | 均为 `true` |
| `@AiAudited` | METHOD | AI 操作审计 | `operation`, `level`（`MINIMAL` / `NORMAL` / `DETAILED`） | — |
| `@ToolExecution` | METHOD | 工具执行审计 | `value`, `audited`, `timeoutMs` | `audited = true`, `timeoutMs = 30000` |

### 6.1 注解使用约束

- **`@AiResilient` 的 `fallbackMethod` 必须与原方法签名一致**（参数类型和返回类型相同），否则运行时抛异常。
- **`@ContentSafety` 与 Security 模块是正交关系**：`@ContentSafety` 处理 AI 内容安全（输入/输出内容审查、PII 检测），Security 模块处理访问控制（认证/授权/权限）。两者互不依赖，可独立使用。
- **`@AiAudited` 应与 core 模块的 `@Audited` 统一格式和存储**，确保审计日志查询的一致性。

## 7. AI 工作流 37 种节点

| 分类 | 节点类型 | 说明 |
|------|---------|------|
| **INPUT** | `InputNode` | 标准输入 |
| | `FileInputNode` | 文件输入（支持多格式） |
| | `HttpRequestNode` | HTTP 请求输入 |
| | `DatabaseQueryNode` | 数据库查询输入 |
| **AI** | `AiServiceNode` | AI 服务调用（prompt 模板 + 系统提示） |
| | `AiAgentNode` | AI Agent 执行 |
| | `AiChatNode` | AI 对话 |
| | `AiEmbeddingNode` | 文本向量化 |
| **LOGIC** | `ConditionNode` | 条件分支（if-else） |
| | `LoopNode` | 循环节点（for/while） |
| | `ParallelNode` | 并行执行 |
| | `SwitchNode` | 多路分支（switch-case） |
| | `MergeNode` | 并行结果合并 |
| | `DelayNode` | 延迟等待 |
| | `SubWorkflowNode` | 子流程调用 |
| **TOOL** | `ToolNode` | 工具调用 |
| | `HttpCallNode` | HTTP 调用 |
| | `DatabaseWriteNode` | 数据库写入 |
| | `CodeExecuteNode` | 代码执行（沙箱） |
| | `McpToolNode` | MCP 协议工具 |
| **OUTPUT** | `OutputNode` | 标准输出 |
| | `FileOutputNode` | 文件输出 |
| | `NotificationNode` | 通知发送 |
| | `WebhookNode` | Webhook 回调 |
| | `LogOutputNode` | 日志输出 |
| **HUMAN** | `HumanApprovalNode` | 人工审批 |
| | `HumanInputNode` | 人工输入 |
| | `HumanChoiceNode` | 人工选择 |
| **TRANSFORM** | `JsonTransformNode` | JSON 转换 |
| | `TextTransformNode` | 文本转换 |
| | `MappingNode` | 字段映射 |
| | `FilterNode` | 数据过滤 |
| | `AggregateNode` | 数据聚合 |
| **RAG** | `RetrievalNode` | 知识检索 |
| | `EmbeddingNode` | 文本嵌入 |
| | `ReRankNode` | 重排序 |
| **CHECKPOINT** | `CheckpointNode` | 断点保存 |
| | `RecoveryNode` | 断点恢复 |

### 7.1 节点约束

- 工作流节点的输入/输出 Schema 需要匹配，框架在执行前校验。
- DAG 引擎确保节点按拓扑序执行，循环依赖在定义时即被拒绝。

## 8. 实体层

包路径：`io.github.afgprojects.framework.ai.core.entity`

| 子包 | 实体 |
|------|------|
| `model` | `ModelProviderEntity`, `ModelConfigEntity`, `ModelUsageEntity` |
| `application` | `ApplicationEntity`, `ApplicationVersionEntity` |
| `agent` | `AgentDefinitionEntity`, `AgentSessionEntity` |
| `workflow` | `WorkflowDefinitionEntity`, `WorkflowExecutionEntity` |
| `knowledge` | `KnowledgeBaseEntity`, `DocumentEntity`, `DocumentChunkEntity` |
| `chat` | `ChatLogEntity` |
| `security` | `ApiKeyEntity`, `AuditLogEntity` |
| `tool` | `ToolRegistryEntity`, `ToolExecutionEntity` |

## 9. AutoConfiguration（16 个）

| AutoConfiguration | 功能 |
|-------------------|------|
| `AiCoreAutoConfiguration` | AI 核心初始化 |
| `AiChatAutoConfiguration` | Chat 客户端注册 |
| `AiAgentAutoConfiguration` | Agent 执行器 |
| `AiModelAutoConfiguration` | 模型注册管理 |
| `AiWorkflowAutoConfiguration` | 工作流 DAG 引擎 |
| `AiPipelineAutoConfiguration` | 对话管线 |
| `AiPersistenceAutoConfiguration` | 持久化（会话、消息历史） |
| `AiResilienceAutoConfiguration` | 韧性（熔断、重试、降级） |
| `AiPerformanceAutoConfiguration` | 性能（缓存、限流） |
| `AiSecurityAutoConfiguration` | 安全（API Key、内容安全、PII） |
| `AiObservabilityAutoConfiguration` | 可观测性（审计、指标、链路追踪） |
| `AiRagAutoConfiguration` | RAG（向量存储、知识库、ETL） |
| `AiEtlAutoConfiguration` | ETL 管线 |
| `AiToolAutoConfiguration` | 工具注册与执行 |
| `AiSkillAutoConfiguration` | 技能路由 |
| `AiEntityAutoConfiguration` | AI 实体自动配置 |

此外，SPI 适配模块各有 7 个 AutoConfiguration：
- **ai-spring-ai**：`SpringAiChatAutoConfiguration` 等 7 个
- **ai-langchain4j**：`Lc4jChatAutoConfiguration` 等 7 个

## 10. 本地降级实现

AI 模块的每个功能都有内存/NoOp 默认实现，不引入 AI 引擎依赖即可运行：

| 默认实现 | 替代方案 |
|----------|----------|
| `DefaultChatClientRegistry` | 内存注册表 |
| `InMemoryModelRegistry` | 内存模型注册 |
| `NoOpVectorStore` | 空操作向量存储 |
| `DefaultSessionStore` | 内存会话存储 |
| `DefaultCircuitBreaker` | 内存熔断器 |
| `DefaultCache` | Caffeine 本地缓存 |

数据库/JDBC 实现通过 `@ConditionalOnBean(DataManager)` 按需激活。

## 11. 配置

配置前缀：`afg.ai`

```yaml
afg:
  ai:
    enabled: true
    chat:
      enabled: true
      default-name: default
    agent:
      enabled: true
      max-iterations: 10
    model:
      enabled: true
    workflow:
      enabled: true
    pipeline:
      enabled: true
    rag:
      enabled: true
      embedding-dimensions: 1536
      search-mode: BLEND          # SEARCH / EMBEDDING / BLEND
      similarity-threshold: 0.7
      top-n: 5
    persistence:
      enabled: true
    resilience:
      enabled: true
      retry:
        max-retries: 3
      circuit-breaker:
        window-size: 100
        failure-rate-threshold: 0.5
    performance:
      enabled: true
      cache:
        max-size: 1000
        ttl-seconds: 300
    security:
      enabled: true
      content-safety:
        enabled: true
      pii:
        enabled: true
    observability:
      enabled: true
      audit:
        enabled: true
    etl:
      enabled: true
      splitter:
        chunk-size: 500
        chunk-overlap: 50
    tool:
      enabled: true
    skill:
      enabled: true
    application:
      enabled: true
```

LLM 的连接配置（API Key、Base URL 等）由 Spring AI 或 LangChain4J 的原生配置管理，不属于 `afg.ai` 前缀范围。

## 12. 使用示例

### 12.1 AI 对话

```java
@Service
public class ChatService {
    @AiChat(client = "default", systemPrompt = "你是客服助手", temperature = 0.7)
    public String chat(String message) { return message; }

    // 流式响应
    @AiChat(client = "default", streaming = true)
    public Flux<String> chatStream(String message) { return Flux.just(message); }
}
```

### 12.2 AI Agent

```java
@AiAgent(value = "code-reviewer", maxIterations = 5, timeoutMs = 30000)
public AgentResponse reviewCode(String code) { ... }
```

### 12.3 RAG 知识库

```java
// 上传文档
knowledgeBaseService.upload(knowledgeBaseId, document);

// 查询
List<SearchResult> results = knowledgeBaseService.search(knowledgeBaseId, "如何退款", 5);
```

### 12.4 AI 工作流

```java
WorkflowDefinition workflow = WorkflowDefinition.builder()
    .name("customer-support")
    .node("input", InputNode.class)
    .node("classify", AiServiceNode.class, Map.of("prompt", "分类用户问题"))
    .node("search", ToolNode.class, Map.of("tool", "knowledgeSearch"))
    .node("answer", AiServiceNode.class, Map.of("prompt", "根据搜索结果回答"))
    .edge("input", "classify")
    .edge("classify", "search")
    .edge("search", "answer")
    .build();

DagResult result = dagEngine.execute(workflow, inputContext);
```

### 12.5 AI 韧性

```java
@AiResilient(retry = 3, retryIntervalMs = 1000, circuitBreaker = "ai-cb", fallbackMethod = "fallback")
@AiChat(client = "default")
public String chat(String message) { return message; }

public String fallback(String message) { return "AI 服务暂时不可用，请稍后重试"; }
```

### 12.6 AI 安全

```java
@ContentSafety(checkInput = true, checkOutput = true, block = true)
@AiChat(client = "default")
public String chat(String message) { return message; }
```

### 12.7 工具注册

```java
@Tool(name = "weather", description = "查询天气")
public String getWeather(@ToolParam("城市") String city) { ... }
```

### 12.8 技能路由

```java
@Skill(name = "refund", description = "退款处理", intentKeywords = {"退款", "退钱", "退费"})
public SkillResult handleRefund(SkillContext context) { ... }
```
