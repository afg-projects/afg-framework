# ai-core 模块精细化打磨 — 16 AutoConfiguration + NoOp + BusinessException

## Goal

对 ai-core 模块进行精细化打磨，重点：
1. AutoConfiguration 依赖排序 — 18 个 AutoConfiguration 添加 @AutoConfigureAfter
2. SPI NoOp fallback 补充 — ToolAuditLogger/ToolPermissionChecker/HumanInteraction
3. IllegalArgumentException→BusinessException 替换（约 30 处）

## Requirements

### T1: @AutoConfigureAfter 依赖排序（18 个）

| AutoConfiguration | after |
|---|---|
| AiCoreAutoConfiguration | AfgAutoConfiguration (afterName) |
| AiChatAutoConfiguration | AfgAutoConfiguration (afterName), AiCoreAutoConfiguration |
| AiModelAutoConfiguration | AfgAutoConfiguration (afterName), AiCoreAutoConfiguration |
| AiAgentAutoConfiguration | AfgAutoConfiguration (afterName), AiChatAutoConfiguration, AiToolAutoConfiguration |
| AiPipelineAutoConfiguration | AfgAutoConfiguration (afterName), AiChatAutoConfiguration, AiRagAutoConfiguration |
| AiPersistenceAutoConfiguration | AfgAutoConfiguration (afterName), AiCoreAutoConfiguration |
| AiResilienceAutoConfiguration | AfgAutoConfiguration (afterName), AiCoreAutoConfiguration |
| AiPerformanceAutoConfiguration | AfgAutoConfiguration (afterName), AiCoreAutoConfiguration |
| AiObservabilityAutoConfiguration | AfgAutoConfiguration (afterName), AiCoreAutoConfiguration |
| AiSecurityAutoConfiguration | AfgAutoConfiguration (afterName), AiCoreAutoConfiguration |
| AiRagAutoConfiguration | AfgAutoConfiguration (afterName), AiModelAutoConfiguration |
| AiEtlAutoConfiguration | AfgAutoConfiguration (afterName), AiCoreAutoConfiguration |
| AiToolAutoConfiguration | AfgAutoConfiguration (afterName), AiSecurityAutoConfiguration |
| AiSkillAutoConfiguration | AfgAutoConfiguration (afterName), AiToolAutoConfiguration |
| AiWorkflowAutoConfiguration | AfgAutoConfiguration (afterName), AiAgentAutoConfiguration |
| AiEntityAutoConfiguration | AfgAutoConfiguration (afterName), afterName DataManagerAutoConfiguration |
| persistence/PersistenceAutoConfiguration | AfgAutoConfiguration (afterName), AiCoreAutoConfiguration |
| persistence/JdbcPersistenceAutoConfiguration | PersistenceAutoConfiguration, afterName DataManagerAutoConfiguration |

### T2: SPI NoOp fallback 补充

| SPI 接口 | NoOp 实现 | 注册位置 |
|---|---|---|
| ToolAuditLogger | NoOpToolAuditLogger | AiToolAutoConfiguration |
| ToolPermissionChecker | NoOpToolPermissionChecker | AiToolAutoConfiguration |
| HumanInteraction | NoOpHumanInteraction | AiAgentAutoConfiguration |

### T3: IllegalArgumentException→BusinessException 替换

**服务层（18 处 — 资源未找到）：**
- AgentService: 4 处 → ENTITY_NOT_FOUND
- WorkflowService: 2 处 → ENTITY_NOT_FOUND
- ApplicationPublishService: 2 处 → ENTITY_NOT_FOUND
- KnowledgeDocumentService: 3 处 → ENTITY_NOT_FOUND
- KnowledgeExtensionService: 3 处 → ENTITY_NOT_FOUND
- ModelTestService: 1 处 → ENTITY_NOT_FOUND
- ModelDiscoveryServiceImpl: 1 处 → ENTITY_NOT_FOUND
- ToolManagementService: 2 处 → ENTITY_NOT_FOUND

**基础设施层（12 处 — 查找/校验）：**
- ModelRouteAspect/DefaultModelRegistry/DefaultChatClientRegistry/DefaultEmbeddingClientRegistry: 模型/客户端未注册 → ENTITY_NOT_FOUND
- SimpleKnowledgeBaseService: 2 处 → ENTITY_NOT_FOUND
- InMemoryStateManager: 2 处 → ENTITY_NOT_FOUND
- DefaultDslValidator: 5 处 → PARAM_ERROR
- JsonDslParser: 1 处 → PARAM_FORMAT_ERROR

**保留 IllegalArgumentException：**
- DefaultAgent maxIterations — 编程约束
- RecursiveCharacterTextSplitter chunkSize/Overlap — 编程约束
- DefaultApiKeyManager 加密密钥长度 — 编程约束
- AiMedia/Document 值对象构建约束 — 编程约束

**catch 块同步修改：**
- AiModelController catch(IllegalArgumentException) → catch(BusinessException)
- PiiService catch(IllegalArgumentException) → 需评估

## Acceptance Criteria

- [ ] 18 个 AutoConfiguration 添加 @AutoConfigureAfter
- [ ] 3 个 NoOp 实现创建并注册
- [ ] 约 30 处 IllegalArgumentException→BusinessException 替换
- [ ] 全量构建通过
